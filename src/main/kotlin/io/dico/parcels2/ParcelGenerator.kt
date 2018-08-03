package io.dico.parcels2

import io.dico.parcels2.blockvisitor.RegionTraversal
import io.dico.parcels2.blockvisitor.Worker
import io.dico.parcels2.blockvisitor.WorktimeLimiter
import io.dico.parcels2.defaultimpl.DefaultParcelGenerator
import io.dico.parcels2.options.GeneratorOptions
import io.dico.parcels2.util.Vec2i
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import java.util.HashMap
import java.util.Random
import kotlin.reflect.KClass

abstract class ParcelGenerator : ChunkGenerator() {
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

    abstract fun makeParcelBlockManager(worktimeLimiter: WorktimeLimiter): ParcelBlockManager

    abstract fun makeParcelLocator(container: ParcelContainer): ParcelLocator
}

interface ParcelBlockManager {
    val world: World
    val worktimeLimiter: WorktimeLimiter

    fun getBottomBlock(parcel: ParcelId): Vec2i

    fun getHomeLocation(parcel: ParcelId): Location

    fun setOwnerBlock(parcel: ParcelId, owner: ParcelOwner?)

    @Deprecated("")
    fun getEntities(parcel: ParcelId): Collection<Entity> = TODO()

    @Deprecated("")
    fun getBlocks(parcel: ParcelId, yRange: IntRange = 0..255): Iterator<Block> = TODO()

    fun setBiome(parcel: ParcelId, biome: Biome): Worker

    fun clearParcel(parcel: ParcelId): Worker

    fun doBlockOperation(parcel: ParcelId, direction: RegionTraversal = RegionTraversal.DOWNWARD, operation: (Block) -> Unit): Worker
}
