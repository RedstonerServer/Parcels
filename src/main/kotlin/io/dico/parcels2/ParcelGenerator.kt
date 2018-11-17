package io.dico.parcels2

import io.dico.parcels2.blockvisitor.RegionTraverser
import io.dico.parcels2.util.math.Region
import io.dico.parcels2.util.math.Vec2i
import io.dico.parcels2.util.math.Vec3i
import io.dico.parcels2.util.math.get
import kotlinx.coroutines.CoroutineScope
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
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

    abstract fun makeParcelLocatorAndBlockManager(
        parcelProvider: ParcelProvider,
        container: ParcelContainer,
        coroutineScope: CoroutineScope,
        jobDispatcher: JobDispatcher
    ): Pair<ParcelLocator, ParcelBlockManager>
}

interface ParcelBlockManager {
    val world: World
    val jobDispatcher: JobDispatcher
    val parcelTraverser: RegionTraverser

    fun getRegionOrigin(parcel: ParcelId) = getRegion(parcel).origin.toVec2i()

    fun getHomeLocation(parcel: ParcelId): Location

    fun getRegion(parcel: ParcelId): Region

    fun getEntities(region: Region): Collection<Entity>

    fun isParcelInfoSectionLoaded(parcel: ParcelId): Boolean

    fun updateParcelInfo(parcel: ParcelId, owner: PlayerProfile?)

    fun getParcelForInfoBlockInteraction(block: Vec3i, type: Material, face: BlockFace): Parcel?

    fun setBiome(parcel: ParcelId, biome: Biome): Job?

    fun clearParcel(parcel: ParcelId): Job?

    /**
     * Used to update owner blocks in the corner of the parcel
     */
    fun getParcelsWithOwnerBlockIn(chunk: Chunk): Collection<Vec2i>
}

inline fun ParcelBlockManager.tryDoBlockOperation(
    parcelProvider: ParcelProvider,
    parcel: ParcelId,
    traverser: RegionTraverser,
    crossinline operation: suspend JobScope.(Block) -> Unit
) = parcelProvider.trySubmitBlockVisitor(Permit(), arrayOf(parcel)) {
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

    override fun getEntities(region: Region): Collection<Entity> {
        val center = region.center
        val centerLoc = Location(world, center.x, center.y, center.z)
        val centerDist = (center - region.origin).add(0.2, 0.2, 0.2)
        return world.getNearbyEntities(centerLoc, centerDist.x, centerDist.y, centerDist.z)
    }

}
