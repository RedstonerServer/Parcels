package io.dico.parcels2.blockvisitor

import io.dico.parcels2.util.Region
import io.dico.parcels2.util.Vec3i
import io.dico.parcels2.util.get
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Sign
import org.bukkit.block.data.BlockData

private val air = Bukkit.createBlockData(Material.AIR)

class Schematic {
    val size: Vec3i get() = _size!!
    private var _size: Vec3i? = null
        set(value) {
            field?.let { throw IllegalStateException() }
            field = value
        }

    private var blockDatas: Array<BlockData?>? = null
    private val extra = mutableListOf<Pair<Vec3i, ExtraBlockChange>>()
    private var isLoaded = false; private set
    private val traverser: RegionTraverser = RegionTraverser.upward

    suspend fun WorkerScope.load(world: World, region: Region) {
        _size = region.size

        val data = arrayOfNulls<BlockData>(region.blockCount).also { blockDatas = it }
        val blocks = traverser.traverseRegion(region)
        val total = region.blockCount.toDouble()

        loop@ for ((index, vec) in blocks.withIndex()) {
            markSuspensionPoint()
            setProgress(index / total)

            val block = world[vec]
            if (block.y > 255) continue
            val blockData = block.blockData
            data[index] = blockData

            val extraChange = when (blockData.material) {
                Material.SIGN,
                Material.WALL_SIGN -> SignStateChange(block.state as Sign)
                else -> continue@loop
            }

            extra += (vec - region.origin) to extraChange
        }

        isLoaded = true
    }

    suspend fun WorkerScope.paste(world: World, position: Vec3i) {
        if (!isLoaded) throw IllegalStateException()

        val region = Region(position, _size!!)
        val blocks = traverser.traverseRegion(region, worldHeight = world.maxHeight)
        val blockDatas = blockDatas!!
        var postponed = hashMapOf<Vec3i, BlockData>()

        val total = region.blockCount.toDouble()
        var processed = 0

        for ((index, vec) in blocks.withIndex()) {
            markSuspensionPoint()
            setProgress(index / total)

            val block = world[vec]
            val type = blockDatas[index] ?: air
            if (type !== air && isAttachable(type.material)) {
                val supportingBlock = vec + getSupportingBlock(type)

                if (!postponed.containsKey(supportingBlock) && traverser.comesFirst(vec, supportingBlock)) {
                    block.blockData = type
                    setProgress(++processed / total)
                } else {
                    postponed[vec] = type
                }

            } else {
                block.blockData = type
                setProgress(++processed / total)
            }
        }

        while (!postponed.isEmpty()) {
            markSuspensionPoint()
            val newMap = hashMapOf<Vec3i, BlockData>()
            for ((vec, type) in postponed) {
                val supportingBlock = vec + getSupportingBlock(type)
                if (supportingBlock in postponed && supportingBlock != vec) {
                    newMap[vec] = type
                } else {
                    world[vec].blockData = type
                    setProgress(++processed / total)
                }
            }
            postponed = newMap
        }

        // Should be negligible so we don't track progress
        for ((vec, extraChange) in extra) {
            markSuspensionPoint()
            val block = world[position + vec]
            extraChange.update(block)
        }
    }

    fun getLoadTask(world: World, region: Region): WorkerTask = {
        load(world, region)
    }

    fun getPasteTask(world: World, position: Vec3i): WorkerTask = {
        paste(world, position)
    }

}
