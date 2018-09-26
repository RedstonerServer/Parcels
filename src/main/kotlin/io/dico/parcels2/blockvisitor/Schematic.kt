package io.dico.parcels2.blockvisitor

import io.dico.parcels2.util.Region
import io.dico.parcels2.util.Vec3i
import io.dico.parcels2.util.get
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData

private val air = Bukkit.createBlockData(Material.AIR)

// TODO order paste such that attachables are placed after the block they depend on
class Schematic {
    val size: Vec3i get() = _size!!
    private var _size: Vec3i? = null
        set(value) {
            field?.let { throw IllegalStateException() }
            field = value
        }

    private var blockDatas: Array<BlockData?>? = null
    private val extra = mutableMapOf<Vec3i, (Block) -> Unit>()
    private var isLoaded = false; private set
    private val traverser: RegionTraverser = RegionTraverser.upward

    suspend fun WorkerScope.load(world: World, region: Region) {
        _size = region.size

        val data = arrayOfNulls<BlockData>(region.blockCount).also { blockDatas = it }
        val blocks = traverser.traverseRegion(region)
        val total = region.blockCount.toDouble()

        for ((index, vec) in blocks.withIndex()) {
            markSuspensionPoint()
            setProgress(index / total)

            val block = world[vec]
            if (block.y > 255) continue
            val blockData = block.blockData
            data[index] = blockData
        }

        isLoaded = true
    }

    suspend fun WorkerScope.paste(world: World, position: Vec3i) {
        if (!isLoaded) throw IllegalStateException()
        val region = Region(position, _size!!)
        val blocks = traverser.traverseRegion(region, worldHeight = world.maxHeight)
        val blockDatas = blockDatas!!
        var postponed = hashMapOf<Vec3i, BlockData>()

        // 90% of the progress of this job is allocated to this code block
        delegateWork(0.9) {
            for ((index, vec) in blocks.withIndex()) {
                markSuspensionPoint()
                val block = world[vec]
                val type = blockDatas[index] ?: air
                if (type !== air && isAttachable(type.material)) {
                    val supportingBlock = vec + getSupportingBlock(type)

                    if (!postponed.containsKey(supportingBlock) && traverser.comesFirst(vec, supportingBlock)) {
                        block.blockData = type
                    } else {
                        postponed[vec] = type
                    }

                } else {
                    block.blockData = type
                }
            }
        }

        delegateWork {
            while (!postponed.isEmpty()) {
                val newMap = hashMapOf<Vec3i, BlockData>()
                for ((vec, type) in postponed) {
                    val supportingBlock = vec + getSupportingBlock(type)
                    if (supportingBlock in postponed && supportingBlock != vec) {
                        newMap[vec] = type
                    } else {
                        world[vec].blockData = type
                    }
                }
                postponed = newMap
            }
        }
    }

    fun getLoadTask(world: World, region: Region): TimeLimitedTask = {
        load(world, region)
    }

    fun getPasteTask(world: World, position: Vec3i): TimeLimitedTask = {
        paste(world, position)
    }

}
