package io.dico.parcels2.blockvisitor

import io.dico.parcels2.util.Region
import io.dico.parcels2.util.Vec3i
import io.dico.parcels2.util.get
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
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
    //private var extra: Map<Vec3i, (Block) -> Unit>? = null
    private var isLoaded = false; private set
    private val traverser: RegionTraverser = RegionTraverser.upward

    fun getLoadTask(world: World, region: Region): TimeLimitedTask = {
        _size = region.size

        val data = arrayOfNulls<BlockData>(region.blockCount).also { blockDatas = it }
        //val extra = mutableMapOf<Vec3i, (Block) -> Unit>().also { extra = it }
        val blocks = traverser.traverseRegion(region)

        for ((index, vec) in blocks.withIndex()) {
            markSuspensionPoint()
            val block = world[vec]
            if (block.y > 255) continue
            val blockData = block.blockData
            data[index] = blockData
        }

        isLoaded = true
    }

    fun getPasteTask(world: World, position: Vec3i): TimeLimitedTask = {
        if (!isLoaded) throw IllegalStateException()
        val region = Region(position, _size!!)
        val blocks = traverser.traverseRegion(region, worldHeight = world.maxHeight)
        val blockDatas = blockDatas!!

        val postponed = mutableListOf<Pair<Vec3i, BlockData>>()

        for ((index, vec) in blocks.withIndex()) {
            markSuspensionPoint()
            val block = world[vec]
            val type = blockDatas[index] ?: air
            if (type !== air && isAttachable(type.material)) {


                postponed += vec to type
            } else {
                block.blockData = type
            }
        }

        for ((vec, data) in postponed) {

        }
    }

}
