package io.dico.parcels2.listener

import gnu.trove.TLongCollection
import io.dico.dicore.ListenerMarker
import io.dico.dicore.RegistratorListener
import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelProvider
import io.dico.parcels2.ParcelWorld
import io.dico.parcels2.statusKey
import io.dico.parcels2.util.*
import org.bukkit.Material.*
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.block.data.Directional
import org.bukkit.block.data.type.Bed
import org.bukkit.entity.*
import org.bukkit.entity.minecart.ExplosiveMinecart
import org.bukkit.event.EventPriority.NORMAL
import org.bukkit.event.block.*
import org.bukkit.event.entity.*
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.InventoryInteractEvent
import org.bukkit.event.player.*
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.inventory.InventoryHolder

@Suppress("NOTHING_TO_INLINE")
class ParcelListeners(val parcelProvider: ParcelProvider, val entityTracker: ParcelEntityTracker) {
    private inline fun Parcel?.canBuildN(user: Player) = isPresentAnd { canBuild(user) } || user.hasBuildAnywhere

    /**
     * Get the world and parcel that the block resides in
     * wo is the world, ppa is the parcel
     * ppa for possibly a parcel - it will be null if not in an existing parcel
     * returns null if not in a registered parcel world
     */
    private fun getWoAndPPa(block: Block): Pair<ParcelWorld, Parcel?>? {
        val world = parcelProvider.getWorld(block.world) ?: return null
        return world to world.getParcelAt(block)
    }

    /*
     * Prevents players from entering plots they are banned from
     */
    @field:ListenerMarker(priority = NORMAL)
    val onPlayerMoveEvent = RegistratorListener<PlayerMoveEvent> l@{ event ->
        val user = event.player
        if (user.hasBanBypass) return@l
        val parcel = parcelProvider.getParcelAt(event.to) ?: return@l
        if (parcel.isBanned(user.statusKey)) {
            parcelProvider.getParcelAt(event.from)?.also {
                user.teleport(it.world.getHomeLocation(it.id))
                user.sendParcelMessage(nopermit = true, message = "You are banned from this parcel")
            } ?: run { event.to = event.from }
        }
    }

    /*
     * Prevents players from breaking blocks outside of their parcels
     * Prevents containers from dropping their contents when broken, if configured
     */
    @field:ListenerMarker(priority = NORMAL)
    val onBlockBreakEvent = RegistratorListener<BlockBreakEvent> l@{ event ->
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
    @field:ListenerMarker(priority = NORMAL)
    val onBlockPlaceEvent = RegistratorListener<BlockPlaceEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.block) ?: return@l
        if (!event.player.hasBuildAnywhere && ppa.isNullOr { !canBuild(event.player) }) {
            event.isCancelled = true
        }
    }

    /*
     * Control pistons
     */
    @field:ListenerMarker(priority = NORMAL)
    val onBlockPistonExtendEvent = RegistratorListener<BlockPistonExtendEvent> l@{ event ->
        checkPistonMovement(event, event.blocks)
    }

    @field:ListenerMarker(priority = NORMAL)
    val onBlockPistonRetractEvent = RegistratorListener<BlockPistonRetractEvent> l@{ event ->
        checkPistonMovement(event, event.blocks)
    }

    // Doing some unnecessary optimizations here..
    //@formatter:off
    private inline fun Column(x: Int, z: Int): Long = x.toLong() or (z.toLong().shl(32))

    private inline val Long.columnX get() = and(0xFFFF_FFFFL).toInt()
    private inline val Long.columnZ get() = ushr(32).and(0xFFFF_FFFFL).toInt()
    private inline fun TLongCollection.troveForEach(block: (Long) -> Unit) = iterator().let { while (it.hasNext()) block(it.next()) }
    //@formatter:on
    private fun checkPistonMovement(event: BlockPistonEvent, blocks: List<Block>) {
        val world = parcelProvider.getWorld(event.block.world) ?: return
        val direction = event.direction
        val columns = gnu.trove.set.hash.TLongHashSet(blocks.size * 2)

        blocks.forEach {
            columns.add(Column(it.x, it.z))
            it.getRelative(direction).let { columns.add(Column(it.x, it.z)) }
        }

        columns.troveForEach {
            val ppa = world.getParcelAt(it.columnX, it.columnZ)
            if (ppa.isNullOr { hasBlockVisitors }) {
                event.isCancelled = true
                return
            }
        }
    }

    /*
     * Prevents explosions if enabled by the configs for that world
     */
    @field:ListenerMarker(priority = NORMAL)
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
    @field:ListenerMarker(priority = NORMAL)
    val onEntityExplodeEvent = RegistratorListener<EntityExplodeEvent> l@{ event ->
        entityTracker.untrack(event.entity)
        val world = parcelProvider.getWorld(event.entity.world) ?: return@l
        if (world.options.disableExplosions || world.getParcelAt(event.entity).isPresentAnd { hasBlockVisitors }) {
            event.isCancelled = true
        }
    }

    /*
     * Prevents creepers and tnt minecarts from exploding if explosions are disabled
     */
    @field:ListenerMarker(priority = NORMAL)
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
    @field:ListenerMarker(priority = NORMAL)
    val onPlayerInteractEvent = RegistratorListener<PlayerInteractEvent> l@{ event ->
        val user = event.player
        val world = parcelProvider.getWorld(user.world) ?: return@l
        val clickedBlock = event.clickedBlock
        val parcel = clickedBlock?.let { world.getParcelAt(it) }

        if (!user.hasBuildAnywhere && parcel.isPresentAnd { isBanned(user.statusKey) }) {
            user.sendParcelMessage(nopermit = true, message = "You cannot interact with parcels you're banned from")
            event.isCancelled = true; return@l
        }

        when (event.action) {
            Action.RIGHT_CLICK_BLOCK -> run {
                when (clickedBlock.type) {
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
                onPlayerInteractEvent_RightClick(event, world, parcel)
            }

            Action.RIGHT_CLICK_AIR -> onPlayerInteractEvent_RightClick(event, world, parcel)
            Action.PHYSICAL -> if (!user.hasBuildAnywhere && !parcel.isPresentAnd { canBuild(user) || allowInteractInputs }) {
                user.sendParcelMessage(nopermit = true, message = "You cannot use inputs in this parcel")
                event.isCancelled = true; return@l
            }
        }
    }

    @Suppress("NON_EXHAUSTIVE_WHEN")
    private fun onPlayerInteractEvent_RightClick(event: PlayerInteractEvent, world: ParcelWorld, parcel: Parcel?) {
        if (event.hasItem()) {
            val item = event.item.type
            if (world.options.blockedItems.contains(item)) {
                event.player.sendParcelMessage(nopermit = true, message = "You cannot use this item because it is disabled in this world")
                event.isCancelled = true; return
            }

            if (!parcel.canBuildN(event.player)) {
                when (item) {
                    LAVA_BUCKET, WATER_BUCKET, BUCKET, FLINT_AND_STEEL -> event.isCancelled = true
                }
            }
        }
    }

    /*
     * Prevents players from breeding mobs, entering or opening boats/minecarts,
     * rotating item frames, doing stuff with leashes, and putting stuff on armor stands.
     */
    @Suppress("NON_EXHAUSTIVE_WHEN")
    @field:ListenerMarker(priority = NORMAL)
    val onPlayerInteractEntityEvent = RegistratorListener<PlayerInteractEntityEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.rightClicked.location.block) ?: return@l
        if (ppa.canBuildN(event.player)) return@l
        when (event.rightClicked.type) {
            EntityType.BOAT,
            EntityType.MINECART,
            EntityType.MINECART_CHEST,
            EntityType.MINECART_COMMAND,
            EntityType.MINECART_FURNACE,
            EntityType.MINECART_HOPPER,
            EntityType.MINECART_MOB_SPAWNER,
            EntityType.MINECART_TNT,

            EntityType.ARMOR_STAND,
            EntityType.PAINTING,
            EntityType.ITEM_FRAME,
            EntityType.LEASH_HITCH,

            EntityType.CHICKEN,
            EntityType.COW,
            EntityType.HORSE,
            EntityType.SHEEP,
            EntityType.VILLAGER,
            EntityType.WOLF -> event.isCancelled = true
        }
    }

    /*
     * Prevents endermen from griefing.
     * Prevents sand blocks from exiting the parcel in which they became an entity.
     */
    @field:ListenerMarker(priority = NORMAL)
    val onEntityChangeBlockEvent = RegistratorListener<EntityChangeBlockEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.block) ?: return@l
        if (event.entity.type == EntityType.ENDERMAN || ppa.isNullOr { hasBlockVisitors }) {
            event.isCancelled = true; return@l
        }

        if (event.entity.type == EntityType.FALLING_BLOCK) {
            // a sand block started falling. Track it and delete it if it gets out of this parcel.
            entityTracker.track(event.entity, ppa)
        }
    }

    /*
     * Prevents portals from being created if set so in the configs for that world
     */
    @field:ListenerMarker(priority = NORMAL)
    val onEntityCreatePortalEvent = RegistratorListener<EntityCreatePortalEvent> l@{ event ->
        val world = parcelProvider.getWorld(event.entity.world) ?: return@l
        if (world.options.blockPortalCreation) event.isCancelled = true
    }

    /*
     * Prevents players from dropping items
     */
    @field:ListenerMarker(priority = NORMAL)
    val onPlayerDropItemEvent = RegistratorListener<PlayerDropItemEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.itemDrop.location.block) ?: return@l
        if (!ppa.canBuildN(event.player) && !ppa.isPresentAnd { allowInteractInventory }) event.isCancelled = true
    }

    /*
     * Prevents players from picking up items
     */
    @field:ListenerMarker(priority = NORMAL)
    val onEntityPickupItemEvent = RegistratorListener<EntityPickupItemEvent> l@{ event ->
        val user = event.entity as? Player ?: return@l
        val (wo, ppa) = getWoAndPPa(event.item.location.block) ?: return@l
        if (!ppa.canBuildN(user)) event.isCancelled = true
    }

    /*
     * Prevents players from editing inventories
     */
    @field:ListenerMarker(priority = NORMAL, events = ["inventory.InventoryClickEvent", "inventory.InventoryDragEvent"])
    val onInventoryClickEvent = RegistratorListener<InventoryInteractEvent> l@{ event ->
        val user = event.whoClicked as? Player ?: return@l
        if ((event.inventory ?: return@l).holder === user) return@l // inventory null: hotbar
        val (wo, ppa) = getWoAndPPa(event.inventory.location.block) ?: return@l
        if (ppa.isNullOr { !canBuild(user) && !allowInteractInventory }) {
            event.isCancelled = true
        }
    }

    /*
     * Cancels weather changes and sets the weather to sunny if requested by the config for that world.
     */
    @field:ListenerMarker(priority = NORMAL)
    val onWeatherChangeEvent = RegistratorListener<WeatherChangeEvent> l@{ event ->
        val world = parcelProvider.getWorld(event.world) ?: return@l
        if (world.options.noWeather && event.toWeatherState()) {
            event.isCancelled = true
        }
    }

    private fun resetWeather(world: World) {
        world.setStorm(false)
        world.isThundering = false
        world.weatherDuration = Int.MAX_VALUE
    }

// TODO: BlockFormEvent, BlockSpreadEvent, BlockFadeEvent, Fireworks

    /*
     * Prevents natural blocks forming
     */
    @ListenerMarker(priority = NORMAL)
    val onBlockFormEvent = RegistratorListener<BlockFormEvent> l@{ event ->
        val block = event.block
        val (wo, ppa) = getWoAndPPa(block) ?: return@l

        // prevent any generation whatsoever on paths
        if (ppa == null) {
            event.isCancelled = true; return@l
        }

        val hasEntity = event is EntityBlockFormEvent
        val player = (event as? EntityBlockFormEvent)?.entity as? Player

        val cancel: Boolean = when (event.newState.type) {

        // prevent ice generation from Frost Walkers enchantment
            FROSTED_ICE -> player != null && !ppa.canBuild(player)

        // prevent snow generation from weather
            SNOW -> !hasEntity && wo.options.preventWeatherBlockChanges

            else -> false
        }

        if (cancel) {
            event.isCancelled = true
        }
    }

    /*
     * Prevents mobs (living entities) from spawning if that is disabled for that world in the config.
     */
    @field:ListenerMarker(priority = NORMAL)
    val onEntitySpawnEvent = RegistratorListener<EntitySpawnEvent> l@{ event ->
        val world = parcelProvider.getWorld(event.entity.world) ?: return@l
        if (event.entity is Creature && world.options.blockMobSpawning) {
            event.isCancelled = true
        } else if (world.getParcelAt(event.entity).isPresentAnd { hasBlockVisitors }) {
            event.isCancelled = true
        }
    }

    /*
     * Prevents minecarts/boats from moving outside a plot
     */
    @field:ListenerMarker(priority = NORMAL)
    val onVehicleMoveEvent = RegistratorListener<VehicleMoveEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.to.block) ?: return@l
        if (ppa == null) {
            event.vehicle.passengers.forEach {
                if (it.type == EntityType.PLAYER) {
                    (it as Player).sendParcelMessage(except = true, message = "Your ride ends here")
                } else it.remove()
            }
            event.vehicle.eject()
            event.vehicle.remove()
        } else if (ppa.hasBlockVisitors) {
            event.to.subtract(event.to).add(event.from)
        }
    }

    /*
     * Prevents players from removing items from item frames
     * Prevents TNT Minecarts and creepers from destroying entities (This event is called BEFORE EntityExplodeEvent GG)
     * Actually doesn't prevent this because the entities are destroyed anyway, even though the code works?
     */
    @field:ListenerMarker(priority = NORMAL)
    val onEntityDamageByEntityEvent = RegistratorListener<EntityDamageByEntityEvent> l@{ event ->
        val world = parcelProvider.getWorld(event.entity.world) ?: return@l
        if (world.options.disableExplosions && event.damager is ExplosiveMinecart || event.damager is Creeper) {
            event.isCancelled = true; return@l
        }

        val user = event.damager as? Player
            ?: (event.damager as? Projectile)?.let { it.shooter as? Player }
            ?: return@l

        if (!world.getParcelAt(event.entity).canBuildN(user)) {
            event.isCancelled = true
        }
    }

    @field:ListenerMarker(priority = NORMAL)
    val onHangingBreakEvent = RegistratorListener<HangingBreakEvent> l@{ event ->
        val world = parcelProvider.getWorld(event.entity.world) ?: return@l
        if (event.cause == HangingBreakEvent.RemoveCause.EXPLOSION && world.options.disableExplosions) {
            event.isCancelled = true; return@l
        }

        if (world.getParcelAt(event.entity).isPresentAnd { hasBlockVisitors }) {
            event.isCancelled = true
        }
    }

    /*
     * Prevents players from deleting paintings and item frames
     * This appears to take care of shooting with a bow, throwing snowballs or throwing ender pearls.
     */
    @field:ListenerMarker(priority = NORMAL)
    val onHangingBreakByEntityEvent = RegistratorListener<HangingBreakByEntityEvent> l@{ event ->
        val world = parcelProvider.getWorld(event.entity.world) ?: return@l
        val user = event.remover as? Player ?: return@l
        if (!world.getParcelAt(event.entity).canBuildN(user)) {
            event.isCancelled = true
        }
    }

    /*
     * Prevents players from placing paintings and item frames
     */
    @field:ListenerMarker(priority = NORMAL)
    val onHangingPlaceEvent = RegistratorListener<HangingPlaceEvent> l@{ event ->
        val world = parcelProvider.getWorld(event.entity.world) ?: return@l
        val block = event.block.getRelative(event.blockFace)
        if (!world.getParcelAt(block).canBuildN(event.player)) {
            event.isCancelled = true
        }
    }

    /*
     * Prevents stuff from growing outside of plots
     */
    @field:ListenerMarker(priority = NORMAL)
    val onStructureGrowEvent = RegistratorListener<StructureGrowEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.location.block) ?: return@l
        if (ppa == null) {
            event.isCancelled = true; return@l
        }

        if (!event.player.hasBuildAnywhere && !ppa.canBuild(event.player)) {
            event.isCancelled = true; return@l
        }

        event.blocks.removeIf { wo.getParcelAt(it.block) !== ppa }
    }

    /*
     * Prevents dispensers/droppers from dispensing out of parcels
     */
    @field:ListenerMarker(priority = NORMAL)
    val onBlockDispenseEvent = RegistratorListener<BlockDispenseEvent> l@{ event ->
        val block = event.block
        if (!block.type.let { it == DISPENSER || it == DROPPER }) return@l
        val world = parcelProvider.getWorld(block.world) ?: return@l
        val data = block.blockData as Directional
        val targetBlock = block.getRelative(data.facing)
        if (world.getParcelAt(targetBlock) == null) {
            event.isCancelled = true
        }
    }

    /*
     * Track spawned items, making sure they don't leave the parcel.
     */
    @field:ListenerMarker(priority = NORMAL)
    val onItemSpawnEvent = RegistratorListener<ItemSpawnEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.location.block) ?: return@l
        if (ppa == null) event.isCancelled = true
        else entityTracker.track(event.entity, ppa)
    }

    /*
     * Prevents endermen and endermite from teleporting outside their parcel
     */
    @field:ListenerMarker(priority = NORMAL)
    val onEntityTeleportEvent = RegistratorListener<EntityTeleportEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.from.block) ?: return@l
        if (ppa !== wo.getParcelAt(event.to)) {
            event.isCancelled = true
        }
    }

    /*
     * Prevents projectiles from flying out of parcels
     * Prevents players from firing projectiles if they cannot build
     */
    @field:ListenerMarker(priority = NORMAL)
    val onProjectileLaunchEvent = RegistratorListener<ProjectileLaunchEvent> l@{ event ->
        val (wo, ppa) = getWoAndPPa(event.entity.location.block) ?: return@l
        if (ppa == null || (event.entity.shooter as? Player)?.let { !ppa.canBuildN(it) } == true) {
            event.isCancelled = true
        } else {
            entityTracker.track(event.entity, ppa)
        }
    }

    /*
     * Prevents entities from dropping items upon death, if configured that way
     */
    @field:ListenerMarker(priority = NORMAL)
    val onEntityDeathEvent = RegistratorListener<EntityDeathEvent> l@{ event ->
        entityTracker.untrack(event.entity)
        val world = parcelProvider.getWorld(event.entity.world) ?: return@l
        if (!world.options.dropEntityItems) {
            event.drops.clear()
            event.droppedExp = 0
        }
    }

    /*
     * Assigns players their default game mode upon entering the world
     */
    @field:ListenerMarker(priority = NORMAL)
    val onPlayerChangedWorldEvent = RegistratorListener<PlayerChangedWorldEvent> l@{ event ->
        val world = parcelProvider.getWorld(event.player.world) ?: return@l
        if (world.options.gameMode != null && !event.player.hasGamemodeBypass) {
            event.player.gameMode = world.options.gameMode
        }
    }

}