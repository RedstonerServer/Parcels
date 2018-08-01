package io.dico.parcels2.blockvisitor

import io.dico.parcels2.util.Region
import io.dico.parcels2.util.Vec3i
import io.dico.parcels2.util.get
import org.bukkit.World
import org.bukkit.block.data.BlockData

// TODO order paste such that attachables are placed after the block they depend on
class Schematic {
    val size: Vec3i get() = _size!!
    private var _size: Vec3i? = null
        set(value) {
            field?.let { throw IllegalStateException() }
            field = value
        }

    private var _data: Array<BlockData?>? = null
    //private var extra: Map<Vec3i, (Block) -> Unit>? = null
    private var isLoaded = false; private set

    fun getLoadTask(world: World, region: Region): TimeLimitedTask = {
        val size = region.size.also { _size = it }
        val data = arrayOfNulls<BlockData>(region.blockCount).also { _data = it }
        //val extra = mutableMapOf<Vec3i, (Block) -> Unit>().also { extra = it }
        val blocks = RegionTraversal.DOWNWARD.regionTraverser(region)

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
        val blocks = RegionTraversal.DOWNWARD.regionTraverser(region)
        val data = _data!!

        for ((index, vec) in blocks.withIndex()) {
            markSuspensionPoint()
            val block = world[vec]
            if (block.y > 255) continue
            data[index]?.let { block.blockData = it }
        }
    }

}
