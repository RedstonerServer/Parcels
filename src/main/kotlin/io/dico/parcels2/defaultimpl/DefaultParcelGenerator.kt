package io.dico.parcels2.defaultimpl

import io.dico.parcels2.*
import io.dico.parcels2.blockvisitor.RegionTraverser
import io.dico.parcels2.blockvisitor.TimeLimitedTask
import io.dico.parcels2.blockvisitor.Worker
import io.dico.parcels2.blockvisitor.WorktimeLimiter
import io.dico.parcels2.options.DefaultGeneratorOptions
import io.dico.parcels2.util.Region
import io.dico.parcels2.util.Vec2i
import io.dico.parcels2.util.Vec3i
import io.dico.parcels2.util.ext.even
import io.dico.parcels2.util.ext.umod
import io.dico.parcels2.util.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.launch
import org.bukkit.*
import org.bukkit.block.Biome
import org.bukkit.block.BlockFace
import org.bukkit.block.Skull
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Sign
import org.bukkit.block.data.type.Slab
import java.lang.IllegalArgumentException
import java.util.Random

private val airType = Bukkit.createBlockData(Material.AIR)

private const val chunkSize = 16

class DefaultParcelGenerator(
    override val worldName: String,
    private val o: DefaultGeneratorOptions
) : ParcelGenerator() {
    private var _world: World? = null
    override val world: World
        get() {
            if (_world == null) _world = Bukkit.getWorld(worldName)!!.also {
                maxHeight = it.maxHeight
                return it
            }
            return _world!!
        }

    private var maxHeight = 0
    val sectionSize = o.parcelSize + o.pathSize
    val pathOffset = (if (o.pathSize % 2 == 0) o.pathSize + 2 else o.pathSize + 1) / 2
    val makePathMain = o.pathSize > 2
    val makePathAlt = o.pathSize > 4

    private inline fun <T> generate(
        chunkX: Int,
        chunkZ: Int,
        floor: T, wall:
        T, pathMain: T,
        pathAlt: T,
        fill: T,
        setter: (Int, Int, Int, T) -> Unit
    ) {

        val floorHeight = o.floorHeight
        val parcelSize = o.parcelSize
        val sectionSize = sectionSize
        val pathOffset = pathOffset
        val makePathMain = makePathMain
        val makePathAlt = makePathAlt

        // parcel bottom x and z
        // umod is unsigned %: the result is always >= 0
        val pbx = ((chunkX shl 4) - o.offsetX) umod sectionSize
        val pbz = ((chunkZ shl 4) - o.offsetZ) umod sectionSize

        var curHeight: Int
        var x: Int
        var z: Int
        for (cx in 0..15) {
            for (cz in 0..15) {
                x = (pbx + cx) % sectionSize - pathOffset
                z = (pbz + cz) % sectionSize - pathOffset
                curHeight = floorHeight

                val type = when {
                    (x in 0 until parcelSize && z in 0 until parcelSize) -> floor
                    (x in -1..parcelSize && z in -1..parcelSize) -> {
                        curHeight++
                        wall
                    }
                    (makePathAlt && x in -2 until parcelSize + 2 && z in -2 until parcelSize + 2) -> pathAlt
                    (makePathMain) -> pathMain
                    else -> {
                        curHeight++
                        wall
                    }
                }

                for (y in 0 until curHeight) {
                    setter(cx, y, cz, fill)
                }
                setter(cx, curHeight, cz, type)
            }
        }
    }

    override fun generateChunkData(world: World?, random: Random?, chunkX: Int, chunkZ: Int, biome: BiomeGrid?): ChunkData {
        val out = Bukkit.createChunkData(world)
        generate(chunkX, chunkZ, o.floorType, o.wallType, o.pathMainType, o.pathAltType, o.fillType) { x, y, z, type ->
            out.setBlock(x, y, z, type)
        }
        return out
    }

    override fun populate(world: World?, random: Random?, chunk: Chunk?) {
        // do nothing
    }

    override fun getFixedSpawnLocation(world: World?, random: Random?): Location {
        val fix = if (o.parcelSize.even) 0.5 else 0.0
        return Location(world, o.offsetX + fix, o.floorHeight + 1.0, o.offsetZ + fix)
    }

    override fun makeParcelLocatorAndBlockManager(
        worldId: ParcelWorldId,
        container: ParcelContainer,
        coroutineScope: CoroutineScope,
        worktimeLimiter: WorktimeLimiter
    ): Pair<ParcelLocator, ParcelBlockManager> {
        return ParcelLocatorImpl(worldId, container) to ParcelBlockManagerImpl(worldId, coroutineScope, worktimeLimiter)
    }

    private inline fun <T> convertBlockLocationToId(x: Int, z: Int, mapper: (Int, Int) -> T): T? {
        val sectionSize = sectionSize
        val parcelSize = o.parcelSize
        val absX = x - o.offsetX - pathOffset
        val absZ = z - o.offsetZ - pathOffset
        val modX = absX umod sectionSize
        val modZ = absZ umod sectionSize
        if (modX in 0 until parcelSize && modZ in 0 until parcelSize) {
            return mapper((absX - modX) / sectionSize + 1, (absZ - modZ) / sectionSize + 1)
        }
        return null
    }

    private inner class ParcelLocatorImpl(
        val worldId: ParcelWorldId,
        val container: ParcelContainer
    ) : ParcelLocator {
        override val world: World = this@DefaultParcelGenerator.world

        override fun getParcelAt(x: Int, z: Int): Parcel? {
            return convertBlockLocationToId(x, z, container::getParcelById)
        }

        override fun getParcelIdAt(x: Int, z: Int): ParcelId? {
            return convertBlockLocationToId(x, z) { idx, idz -> ParcelId(worldId, idx, idz) }
        }
    }

    @Suppress("DEPRECATION")
    private inner class ParcelBlockManagerImpl(
        val worldId: ParcelWorldId,
        coroutineScope: CoroutineScope,
        override val worktimeLimiter: WorktimeLimiter
    ) : ParcelBlockManagerBase(), CoroutineScope by coroutineScope {
        override val world: World = this@DefaultParcelGenerator.world
        override val parcelTraverser: RegionTraverser = RegionTraverser.upToAndDownUntil(o.floorHeight)

        /*override*/ fun getBottomBlock(parcel: ParcelId): Vec2i = Vec2i(
            sectionSize * (parcel.x - 1) + pathOffset + o.offsetX,
            sectionSize * (parcel.z - 1) + pathOffset + o.offsetZ
        )

        override fun getHomeLocation(parcel: ParcelId): Location {
            val bottom = getBottomBlock(parcel)
            val x = bottom.x + (o.parcelSize - 1) / 2.0
            val z = bottom.z - 2
            return Location(world, x + 0.5, o.floorHeight + 1.0, z + 0.5, 0F, 0F)
        }

        override fun getRegion(parcel: ParcelId): Region {
            val bottom = getBottomBlock(parcel)
            return Region(Vec3i(bottom.x, 0, bottom.z), Vec3i(o.parcelSize, maxHeight + 1, o.parcelSize))
        }

        override fun setOwnerBlock(parcel: ParcelId, owner: PlayerProfile?) {
            val b = getBottomBlock(parcel)

            val wallBlock = world.getBlockAt(b.x - 1, o.floorHeight + 1, b.z - 1)
            val signBlock = world.getBlockAt(b.x - 2, o.floorHeight + 1, b.z - 1)
            val skullBlock = world.getBlockAt(b.x - 1, o.floorHeight + 2, b.z - 1)

            if (owner == null) {
                wallBlock.blockData = o.wallType
                signBlock.type = Material.AIR
                skullBlock.type = Material.AIR
            } else {

                val wallBlockType: BlockData = if (o.wallType is Slab)
                    (o.wallType.clone() as Slab).apply { type = Slab.Type.DOUBLE }
                else
                    o.wallType

                wallBlock.blockData = wallBlockType

                signBlock.blockData = (Bukkit.createBlockData(Material.WALL_SIGN) as Sign).apply { rotation = BlockFace.NORTH }

                val sign = signBlock.state as org.bukkit.block.Sign
                sign.setLine(0, "${parcel.x},${parcel.z}")
                sign.setLine(2, owner.name)
                sign.update()

                skullBlock.type = Material.PLAYER_HEAD
                val skull = skullBlock.state as Skull
                if (owner is PlayerProfile.Real) {
                    skull.owningPlayer = owner.playerUnchecked
                } else {
                    skull.owner = owner.name
                }
                skull.rotation = BlockFace.WEST
                skull.update()
            }
        }

        private fun getParcel(parcelId: ParcelId): Parcel? {
            // todo dont rely on this cast
            val world = worldId as? ParcelWorld ?: return null
            return world.getParcelById(parcelId)
        }

        private fun submitBlockVisitor(parcelId: ParcelId, task: TimeLimitedTask): Worker {
            val parcel = getParcel(parcelId) ?: return worktimeLimiter.submit(task)
            if (parcel.hasBlockVisitors) throw IllegalArgumentException("This parcel already has a block visitor")

            val worker = worktimeLimiter.submit(task)

            launch(start = UNDISPATCHED) {
                parcel.withBlockVisitorPermit {
                    worker.awaitCompletion()
                }
            }

            return worker
        }

        override fun setBiome(parcel: ParcelId, biome: Biome): Worker = submitBlockVisitor(parcel) {
            val world = world
            val b = getBottomBlock(parcel)
            val parcelSize = o.parcelSize
            for (x in b.x until b.x + parcelSize) {
                for (z in b.z until b.z + parcelSize) {
                    markSuspensionPoint()
                    world.setBiome(x, z, biome)
                }
            }
        }

        override fun clearParcel(parcel: ParcelId): Worker = submitBlockVisitor(parcel) {
            val region = getRegion(parcel)
            val blocks = parcelTraverser.traverseRegion(region)
            val blockCount = region.blockCount.toDouble()

            val world = world
            val floorHeight = o.floorHeight
            val airType = airType
            val floorType = o.floorType
            val fillType = o.fillType

            for ((index, vec) in blocks.withIndex()) {
                markSuspensionPoint()
                val y = vec.y
                val blockType = when {
                    y > floorHeight -> airType
                    y == floorHeight -> floorType
                    else -> fillType
                }
                world[vec].blockData = blockType
                setProgress((index + 1) / blockCount)
            }
        }

        override fun getParcelsWithOwnerBlockIn(chunk: Chunk): Collection<Vec2i> {
            /*
             * Get the offsets for the world out of the way
             * to simplify the calculation that follows.
             */

            val x = chunk.x.shl(4) - (o.offsetX + pathOffset)
            val z = chunk.z.shl(4) - (o.offsetZ + pathOffset)

            /* Locations of wall corners (where owner blocks are placed) are defined as:
             *
             * x umod sectionSize == sectionSize-1
             *
             * This check needs to be made for all 16 slices of the chunk in 2 dimensions
             * How to optimize this?
             * Let's take the expression
             *
             * x umod sectionSize
             *
             * And call it modX
             * x can be shifted (chunkSize -1) times to attempt to get a modX of 0.
             * This means that if the modX is 1, and sectionSize == (chunkSize-1), there would be a match at the last shift.
             * To check that there are any matches, we can see if the following holds:
             *
             * modX >= ((sectionSize-1) - (chunkSize-1))
             *
             * Which can be simplified to:
             * modX >= sectionSize - chunkSize
             *
             * if sectionSize == chunkSize, this expression can be simplified to
             * modX >= 0
             * which is always true. This is expected.
             * To get the total number of matches on a dimension, we can evaluate the following:
             *
             * (modX - (sectionSize - chunkSize) + sectionSize) / sectionSize
             *
             * We add sectionSize to the lhs because, if the other part of the lhs is 0, we need at least 1.
             * This can be simplified to:
             *
             * (modX + chunkSize) / sectionSize
             */

            val sectionSize = sectionSize

            val modX = x umod sectionSize
            val matchesOnDimensionX = (modX + chunkSize) / sectionSize
            if (matchesOnDimensionX <= 0) return emptyList()

            val modZ = z umod sectionSize
            val matchesOnDimensionZ = (modZ + chunkSize) / sectionSize
            if (matchesOnDimensionZ <= 0) return emptyList()

            /*
             * Now we need to find the first id within the matches,
             * and then return the subsequent matches in a rectangle following it.
             *
             * On each dimension, get the distance to the first match, which is equal to (sectionSize-1 - modX)
             * and add it to the coordinate value
             */
            val firstX = x + (sectionSize - 1 - modX)
            val firstZ = z + (sectionSize - 1 - modZ)

            val firstIdX = (firstX + 1) / sectionSize + 1
            val firstIdZ = (firstZ + 1) / sectionSize + 1

            if (matchesOnDimensionX == 1 && matchesOnDimensionZ == 1) {
                // fast-path optimization
                return listOf(Vec2i(firstIdX, firstIdZ))
            }

            return (0 until matchesOnDimensionX).flatMap { idOffsetX ->
                (0 until matchesOnDimensionZ).map { idOffsetZ -> Vec2i(firstIdX + idOffsetX, firstIdZ + idOffsetZ) }
            }
        }

    }

}