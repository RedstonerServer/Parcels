package io.dico.parcels2.listener

import gnu.trove.TLongCollection
import io.dico.dicore.ListenerMarker
import io.dico.dicore.RegistratorListener
import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelWorld
import io.dico.parcels2.Worlds
import io.dico.parcels2.util.hasBanBypass
import io.dico.parcels2.util.hasBuildAnywhere
import io.dico.parcels2.util.sendParcelMessage
import io.dico.parcels2.util.uuid
import org.bukkit.Material.*
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.block.data.type.Bed
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority.NORMAL
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.InventoryHolder

@Suppress("NOTHING_TO_INLINE")
class ParcelEditListener(val worlds: Worlds) {
    val entityTracker = ParcelEntityTracker()

    private inline fun <T> T?.isNullOr(condition: T.() -> Boolean): Boolean = this == null || condition()
    private inline fun <T> T?.isPresentAnd(condition: T.() -> Boolean): Boolean = this != null && condition()
    private inline fun Parcel?.canBuildN(user: Player) = isPresentAnd { canBuild(user) } || user.hasBuildAnywhere

    /**
     * Get the world and parcel that the block resides in
     * wo is the world, ppa is the parcel
     * ppa for possibly a parcel - it will be null if not in an existing parcel
     * returns null if not in a registered parcel world
     */
    private fun getWoAndPPa(block: Block): Pair<ParcelWorld, Parcel?>? {
        val world = worlds.getWorld(block.world) ?: return null
        return world to world.parcelAt(block)
    }

    /*
     * Prevents players from entering plots they are banned from
     */
    @ListenerMarker(priority = NORMAL)
    val onPlayerMove = RegistratorListener<PlayerMoveEvent> l@{ event ->
        val user = event.player
        if (user.hasBanBypass) return@l
        val parcel = worlds.getParcelAt(event.to) ?: return@l
        if (parcel.isBanned(user.uuid)) {
            worlds.getParcelAt(event.from)?.also {
                user.teleport(it.homeLocation)
                user.sendParcelMessage(nopermit = true, message = "You are banned from this parcel")
            } ?: run { event.to = event.from }
        }
    }

    /*
     * Prevents players from breaking blocks outside of their parcels
     * Prevents containers from dropping their contents when broken, if configured
     */
    @ListenerMarker(priority = NORMAL)
    val onBlockBreak = RegistratorListener<BlockBreakEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.block) ?: return@l
        if (!event.player.hasBuildAnywhere && ppa.isNullOr { !canBuild(event.player) }) {
            event.isCancelled = true; return@l
        }

        if (!wo.options.dropEntityItems) {
            val state = event.block.state
            if (state is InventoryHolder) {
                state.inventory.clear()
                state.update()
            }
        }
    }

    /*
     * Prevents players from placing blocks outside of their parcels
     */
    @ListenerMarker(priority = NORMAL)
    val onBlockPlace = RegistratorListener<BlockBreakEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.block) ?: return@l
        if (!event.player.hasBuildAnywhere && !ppa.isNullOr { !canBuild(event.player) }) {
            event.isCancelled = true
        }
    }

    /*
     * Control pistons
     */
    @ListenerMarker(priority = NORMAL)
    val onBlockPistonExtend = RegistratorListener<BlockPistonExtendEvent> l@{ event ->
        checkPistonMovement(event, event.blocks)
    }

    @ListenerMarker(priority = NORMAL)
    val onBlockPistonRetractEvent = RegistratorListener<BlockPistonRetractEvent> l@{ event ->
        checkPistonMovement(event, event.blocks)
    }

    // Doing some unnecessary optimizations here..
    //@formatter:off
    private inline fun Column(x: Int, z: Int): Long = x.toLong() or (z.toLong().shl(32))

    private inline val Long.columnX get() = and(0xFFFF_FFFFL).toInt()
    private inline val Long.columnZ get() = ushr(32).and(0xFFFF_FFFFL).toInt()
    private inline fun TLongCollection.forEachInline(block: (Long) -> Unit) = iterator().let { while (it.hasNext()) block(it.next()) }
    //@formatter:on
    private fun checkPistonMovement(event: BlockPistonEvent, blocks: List<Block>) {
        val world = worlds.getWorld(event.block.world) ?: return
        val direction = event.direction
        val columns = gnu.trove.set.hash.TLongHashSet(blocks.size * 2)

        blocks.forEach {
            columns.add(Column(it.x, it.z))
            it.getRelative(direction).let { columns.add(Column(it.x, it.z)) }
        }

        columns.forEachInline {
            val ppa = world.parcelAt(it.columnX, it.columnZ)
            if (ppa.isNullOr { hasBlockVisitors }) {
                event.isCancelled = true
                return
            }
        }
    }

    /*
     * Prevents explosions if enabled by the configs for that world
     */
    @ListenerMarker(priority = NORMAL)
    val onExplosionPrimeEvent = RegistratorListener<ExplosionPrimeEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.entity.location.block) ?: return@l
        if (ppa?.hasBlockVisitors == true) {
            event.radius = 0F; event.isCancelled = true
        } else if (wo.options.disableExplosions) {
            event.radius = 0F
        }
    }

    /*
     * Prevents creepers and tnt minecarts from exploding if explosions are disabled
     */
    @ListenerMarker(priority = NORMAL)
    val onEntityExplodeEvent = RegistratorListener<EntityExplodeEvent> l@{ event ->
        entityTracker.untrack(event.entity)
        val world = worlds.getWorld(event.entity.world) ?: return@l
        if (world.options.disableExplosions || world.parcelAt(event.entity).isPresentAnd { hasBlockVisitors }) {
            event.isCancelled = true
        }
    }

    /*
     * Prevents creepers and tnt minecarts from exploding if explosions are disabled
     */
    @ListenerMarker(priority = NORMAL)
    val onBlockFromToEvent = RegistratorListener<BlockFromToEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.toBlock) ?: return@l
        if (ppa.isNullOr { hasBlockVisitors }) event.isCancelled = true
    }

    /*
     * Prevents players from placing liquids, using flint and steel, changing redstone components,
     * using inputs (unless allowed by the plot),
     * and using items disabled in the configuration for that world.
     * Prevents player from using beds in HELL or SKY biomes if explosions are disabled.
     */
    @Suppress("NON_EXHAUSTIVE_WHEN")
    @ListenerMarker(priority = NORMAL)
    val onPlayerInteractEvent = RegistratorListener<PlayerInteractEvent> l@{ event ->
        val user = event.player
        val world = worlds.getWorld(user.world) ?: return@l
        val clickedBlock = event.clickedBlock
        val parcel = clickedBlock?.let { world.parcelAt(it) }

        if (!user.hasBuildAnywhere && parcel.isPresentAnd { isBanned(user.uuid) }) {
            user.sendParcelMessage(nopermit = true, message = "You cannot interact with parcels you're banned from")
            event.isCancelled = true; return@l
        }

        when (event.action) {
            Action.RIGHT_CLICK_BLOCK -> when (clickedBlock.type) {
                REPEATER,
                COMPARATOR -> run {
                    if (!parcel.canBuildN(user)) {
                        event.isCancelled = true; return@l
                    }
                }
                LEVER,
                STONE_BUTTON,
                ANVIL,
                TRAPPED_CHEST,
                OAK_BUTTON, BIRCH_BUTTON, SPRUCE_BUTTON, JUNGLE_BUTTON, ACACIA_BUTTON, DARK_OAK_BUTTON,
                OAK_FENCE_GATE, BIRCH_FENCE_GATE, SPRUCE_FENCE_GATE, JUNGLE_FENCE_GATE, ACACIA_FENCE_GATE, DARK_OAK_FENCE_GATE,
                OAK_DOOR, BIRCH_DOOR, SPRUCE_DOOR, JUNGLE_DOOR, ACACIA_DOOR, DARK_OAK_DOOR,
                OAK_TRAPDOOR, BIRCH_TRAPDOOR, SPRUCE_TRAPDOOR, JUNGLE_TRAPDOOR, ACACIA_TRAPDOOR, DARK_OAK_TRAPDOOR
                -> run {
                    if (!user.hasBuildAnywhere && !parcel.isNullOr { canBuild(user) || allowInteractInputs }) {
                        user.sendParcelMessage(nopermit = true, message = "You cannot use inputs in this parcel")
                        event.isCancelled = true; return@l
                    }
                }

                WHITE_BED, ORANGE_BED, MAGENTA_BED, LIGHT_BLUE_BED, YELLOW_BED, LIME_BED, PINK_BED, GRAY_BED, LIGHT_GRAY_BED, CYAN_BED, PURPLE_BED, BLUE_BED, BROWN_BED, GREEN_BED, RED_BED, BLACK_BED
                -> run {
                    if (world.options.disableExplosions) {
                        val bed = clickedBlock.blockData as Bed
                        val head = if (bed == Bed.Part.FOOT) clickedBlock.getRelative(bed.facing) else clickedBlock
                        when (head.biome) {
                            Biome.NETHER, Biome.THE_END -> run {
                                user.sendParcelMessage(nopermit = true, message = "You cannot use this bed because it would explode")
                                event.isCancelled = true; return@l
                            }
                        }

                    }

                }
            }

            Action.RIGHT_CLICK_AIR -> if (event.hasItem()) {
                val item = event.item.type
                if (world.options.blockedItems.contains(item)) {
                    user.sendParcelMessage(nopermit = true, message = "You cannot use this bed because it would explode")
                    event.isCancelled = true; return@l
                }

                if (!parcel.canBuildN(user)) {
                    when (item) {
                        LAVA_BUCKET, WATER_BUCKET, BUCKET, FLINT_AND_STEEL -> event.isCancelled = true
                    }
                }
            }


            Action.PHYSICAL -> if (!user.hasBuildAnywhere && !parcel.isPresentAnd { canBuild(user) || allowInteractInputs }) {
                event.isCancelled = true; return@l
            }
        }
    }


}