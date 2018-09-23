package io.dico.parcels2

import io.dico.parcels2.blockvisitor.*
import io.dico.parcels2.util.Region
import io.dico.parcels2.util.Vec2i
import io.dico.parcels2.util.get
import kotlinx.coroutines.CoroutineScope
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import java.util.Random

abstract class ParcelGenerator : ChunkGenerator() {
    abstract val worldName: String

    abstract val world: World

    abstract override fun generateChunkData(world: World?, random: Random?, chunkX: Int, chunkZ: Int, biome: BiomeGrid?): ChunkData

    abstract fun populate(world: World?, random: Random?, chunk: Chunk?)

    abstract override fun getFixedSpawnLocation(world: World?, random: Random?): Location

    override fun getDefaultPopulators(world: World?): MutableList<BlockPopulator> {
        return mutableListOf(object : BlockPopulator() {
            override fun populate(world: World?, random: Random?, chunk: Chunk?) {
                this@ParcelGenerator.populate(world, random, chunk)
            }
        })
    }

    abstract fun makeParcelLocatorAndBlockManager(worldId: ParcelWorldId,
                                                  container: ParcelContainer,
                                                  coroutineScope: CoroutineScope,
                                                  worktimeLimiter: WorktimeLimiter): Pair<ParcelLocator, ParcelBlockManager>
}

interface ParcelBlockManager {
    val world: World
    val worktimeLimiter: WorktimeLimiter
    val parcelTraverser: RegionTraverser

    // fun getBottomBlock(parcel: ParcelId): Vec2i

    fun getHomeLocation(parcel: ParcelId): Location

    fun getRegion(parcel: ParcelId): Region

    fun getEntities(parcel: ParcelId): Collection<Entity>

    fun setOwnerBlock(parcel: ParcelId, owner: PlayerProfile?)

    fun setBiome(parcel: ParcelId, biome: Biome): Worker

    fun clearParcel(parcel: ParcelId): Worker

    fun submitBlockVisitor(parcelId: ParcelId, task: TimeLimitedTask): Worker

    /**
     * Used to update owner blocks in the corner of the parcel
     */
    fun getParcelsWithOwnerBlockIn(chunk: Chunk): Collection<Vec2i>
}

inline fun ParcelBlockManager.doBlockOperation(parcel: ParcelId,
                                               traverser: RegionTraverser,
                                               crossinline operation: suspend WorkerScope.(Block) -> Unit) = submitBlockVisitor(parcel) {
    val region = getRegion(parcel)
    val blockCount = region.blockCount.toDouble()
    val blocks = traverser.traverseRegion(region)
    for ((index, vec) in blocks.withIndex()) {
        markSuspensionPoint()
        operation(world[vec])
        setProgress((index + 1) / blockCount)
    }
}

abstract class ParcelBlockManagerBase : ParcelBlockManager {

    override fun getEntities(parcel: ParcelId): Collection<Entity> {
        val region = getRegion(parcel)
        val center = region.center
        val centerLoc = Location(world, center.x, center.y, center.z)
        val centerDist = (center - region.origin).add(0.2, 0.2, 0.2)
        return world.getNearbyEntities(centerLoc, centerDist.x, centerDist.y, centerDist.z)
    }

}
