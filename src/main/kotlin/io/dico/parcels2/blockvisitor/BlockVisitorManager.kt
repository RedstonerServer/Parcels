package io.dico.parcels2.blockvisitor

import io.dico.parcels2.util.MutableVec3i
import io.dico.parcels2.util.Region
import kotlinx.coroutines.experimental.Deferred
import org.bukkit.block.Block
import org.bukkit.plugin.Plugin
import kotlin.coroutines.experimental.SequenceBuilder
import kotlin.coroutines.experimental.buildIterator

typealias BlockProcessor = (Block) -> Boolean

class BlockVisitorManager(val plugin: Plugin, var options: BlockVisitorOptions) {


    fun doOperationSynchronously(region: Region, processor: BlockProcessor): Deferred<Unit> {


    }


}

class RegionOperation(val region: Region, val processor: BlockProcessor) {

    fun process(maxMillis: Int) {

    }


}

enum class RegionTraversal(private val builder: suspend SequenceBuilder<MutableVec3i>.(Region) -> Unit) {
    XZY({ region ->
        val origin = region.origin
        val result = MutableVec3i(origin.x, origin.y, origin.z)

        val size = region.size

        repeat(size.y) { y ->
            repeat()

            result.y++
        }

    })

    ;

    fun regionTraverser(region: Region) = Iterable { buildIterator { builder(region) } }

}
