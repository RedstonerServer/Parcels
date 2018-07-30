package io.dico.parcels2

import io.dico.parcels2.blockvisitor.Worker
import io.dico.parcels2.blockvisitor.RegionTraversal
import io.dico.parcels2.util.*
import org.bukkit.*
import org.bukkit.Bukkit.createBlockData
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Skull
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Sign
import org.bukkit.block.data.type.Slab
import org.bukkit.entity.Entity
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import java.util.*
import kotlin.coroutines.experimental.buildIterator
import kotlin.reflect.KClass

abstract class ParcelGenerator : ChunkGenerator(), ParcelProvider {
    abstract val world: ParcelWorld

    abstract val factory: GeneratorFactory

    abstract override fun generateChunkData(world: World?, random: Random?, chunkX: Int, chunkZ: Int, biome: BiomeGrid?): ChunkData

    abstract fun populate(world: World?, random: Random?, chunk: Chunk?)

    abstract override fun getFixedSpawnLocation(world: World?, random: Random?): Location

    override fun getDefaultPopulators(world: World?): MutableList<BlockPopulator> {
        return Collections.singletonList(object : BlockPopulator() {
            override fun populate(world: World?, random: Random?, chunk: Chunk?) {
                this@ParcelGenerator.populate(world, random, chunk)
            }
        })
    }

    abstract fun updateOwner(parcel: Parcel)

    abstract fun getBottomCoord(parcel: Parcel): Vec2i

    abstract fun getHomeLocation(parcel: Parcel): Location

    abstract fun setBiome(parcel: Parcel, biome: Biome)

    abstract fun getEntities(parcel: Parcel): Collection<Entity>

    abstract fun getBlocks(parcel: Parcel, yRange: IntRange = 0..255): Iterator<Block>

    abstract fun clearParcel(parcel: Parcel): Worker

    abstract fun doBlockOperation(parcel: Parcel, direction: RegionTraversal = RegionTraversal.DOWNWARD, operation: (Block) -> Unit): Worker

}

interface GeneratorFactory {
    companion object GeneratorFactories {
        private val map: MutableMap<String, GeneratorFactory> = HashMap()

        fun registerFactory(generator: GeneratorFactory): Boolean = map.putIfAbsent(generator.name, generator) == null

        fun getFactory(name: String): GeneratorFactory? = map.get(name)

        init {
            registerFactory(DefaultParcelGenerator.Factory)
        }

    }

    val name: String

    val optionsClass: KClass<out GeneratorOptions>

    fun newParcelGenerator(worlds: Worlds, worldName: String, options: GeneratorOptions): ParcelGenerator

}

class DefaultParcelGenerator(val worlds: Worlds, val name: String, private val o: DefaultGeneratorOptions) : ParcelGenerator() {
    override val world: ParcelWorld by lazy { worlds.getWorld(name)!! }
    override val factory = Factory
    val worktimeLimiter = worlds.plugin.worktimeLimiter
    val maxHeight by lazy { world.world.maxHeight }
    val airType = worlds.plugin.server.createBlockData(Material.AIR)

    companion object Factory : GeneratorFactory {
        override val name get() = "default"
        override val optionsClass get() = DefaultGeneratorOptions::class
        override fun newParcelGenerator(worlds: Worlds, worldName: String, options: GeneratorOptions): ParcelGenerator {
            return DefaultParcelGenerator(worlds, worldName, options as DefaultGeneratorOptions)
        }
    }

    val sectionSize = o.parcelSize + o.pathSize
    val pathOffset = (if (o.pathSize % 2 == 0) o.pathSize + 2 else o.pathSize + 1) / 2
    val makePathMain = o.pathSize > 2
    val makePathAlt = o.pathSize > 4

    private inline fun <T> generate(chunkX: Int,
                                    chunkZ: Int,
                                    floor: T, wall:
                                    T, pathMain: T,
                                    pathAlt: T,
                                    fill: T,
                                    setter: (Int, Int, Int, T) -> Unit) {

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
        /*
        generate(chunk!!.x, chunk.z, o.floorType.data, o.wallType.data, o.pathMainType.data, o.pathAltType.data, o.fillType.data) { x, y, z, type ->
            if (type == 0.toByte()) chunk.getBlock(x, y, z).setData(type, false)
        }
        */
    }

    override fun getFixedSpawnLocation(world: World?, random: Random?): Location {
        val fix = if (o.parcelSize.even) 0.5 else 0.0
        return Location(world, o.offsetX + fix, o.floorHeight + 1.0, o.offsetZ + fix)
    }

    override fun parcelAt(x: Int, z: Int): Parcel? {
        val sectionSize = sectionSize
        val parcelSize = o.parcelSize
        val absX = x - o.offsetX - pathOffset
        val absZ = z - o.offsetZ - pathOffset
        val modX = absX umod sectionSize
        val modZ = absZ umod sectionSize
        if (0 <= modX && modX < parcelSize && 0 <= modZ && modZ < parcelSize) {
            return world.parcelByID((absX - modX) / sectionSize, (absZ - modZ) / sectionSize)
        }
        return null
    }

    override fun getBottomCoord(parcel: Parcel): Vec2i = Vec2i(sectionSize * parcel.pos.x + pathOffset + o.offsetX,
        sectionSize * parcel.pos.z + pathOffset + o.offsetZ)

    override fun getHomeLocation(parcel: Parcel): Location {
        val bottom = getBottomCoord(parcel)
        return Location(world.world, bottom.x.toDouble(), o.floorHeight + 1.0, bottom.z + (o.parcelSize - 1) / 2.0, -90F, 0F)
    }

    override fun updateOwner(parcel: Parcel) {
        val world = this.world.world
        val b = getBottomCoord(parcel)

        val wallBlock = world.getBlockAt(b.x - 1, o.floorHeight + 1, b.z - 1)
        val signBlock = world.getBlockAt(b.x - 2, o.floorHeight + 1, b.z - 1)
        val skullBlock = world.getBlockAt(b.x - 1, o.floorHeight + 2, b.z - 1)

        val owner = parcel.owner
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

            signBlock.blockData = (createBlockData(Material.WALL_SIGN) as Sign).apply { rotation = BlockFace.NORTH }

            val sign = signBlock.state as org.bukkit.block.Sign
            sign.setLine(0, parcel.id)
            sign.setLine(2, owner.playerName)
            sign.update()

            skullBlock.type = Material.PLAYER_HEAD
            val skull = skullBlock.state as Skull
            if (owner.uuid != null) {
                skull.owningPlayer = owner.offlinePlayer
            } else {
                skull.owner = owner.name
            }
            skull.rotation = BlockFace.WEST
            skull.update()
        }
    }

    override fun setBiome(parcel: Parcel, biome: Biome) {
        val world = this.world.world
        val b = getBottomCoord(parcel)
        val parcelSize = o.parcelSize
        for (x in b.x until b.x + parcelSize) {
            for (z in b.z until b.z + parcelSize) {
                world.setBiome(x, z, biome)
            }
        }
    }

    override fun getEntities(parcel: Parcel): Collection<Entity> {
        val world = this.world.world
        val b = getBottomCoord(parcel)
        val parcelSize = o.parcelSize
        val center = Location(world, (b.x + parcelSize) / 2.0, 128.0, (b.z + parcelSize) / 2.0)
        return world.getNearbyEntities(center, parcelSize / 2.0 + 0.2, 128.0, parcelSize / 2.0 + 0.2)
    }

    override fun getBlocks(parcel: Parcel, yRange: IntRange): Iterator<Block> = buildIterator {
        val range = yRange.clamp(0, 255)
        val world = this@DefaultParcelGenerator.world.world
        val b = getBottomCoord(parcel)
        val parcelSize = o.parcelSize
        for (x in b.x until b.x + parcelSize) {
            for (z in b.z until b.z + parcelSize) {
                for (y in range) {
                    yield(world.getBlockAt(x, y, z))
                }
            }
        }
    }

    override fun clearParcel(parcel: Parcel) = worktimeLimiter.submit {
        val bottom = getBottomCoord(parcel)
        val region = Region(Vec3i(bottom.x, 0, bottom.z), Vec3i(o.parcelSize, maxHeight + 1, o.parcelSize))
        val blocks = RegionTraversal.DOWNWARD.regionTraverser(region)
        val blockCount = region.blockCount.toDouble()

        val world = world.world
        val floorHeight = o.floorHeight
        val airType = airType; val floorType = o.floorType; val fillType = o.fillType

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

    override fun doBlockOperation(parcel: Parcel, direction: RegionTraversal, operation: (Block) -> Unit) = worktimeLimiter.submit {
        val bottom = getBottomCoord(parcel)
        val region = Region(Vec3i(bottom.x, 0, bottom.z), Vec3i(o.parcelSize, maxHeight + 1, o.parcelSize))
        val blocks = direction.regionTraverser(region)
        val blockCount = region.blockCount.toDouble()
        val world = world.world

        for ((index, vec) in blocks.withIndex()) {
            markSuspensionPoint()
            operation(world[vec])
            setProgress((index + 1) / blockCount)
        }
    }

}