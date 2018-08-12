package io.dico.parcels2.blockvisitor

import io.dico.parcels2.util.Region
import io.dico.parcels2.util.Vec3i
import kotlin.coroutines.experimental.SequenceBuilder
import kotlin.coroutines.experimental.buildIterator

abstract class RegionTraverser {
    fun traverseRegion(region: Region): Iterable<Vec3i> = Iterable { buildIterator { build(region) } }

    protected abstract suspend fun SequenceBuilder<Vec3i>.build(region: Region)

    companion object {
        val upward = create { traverseUpward(it) }
        val downward = create { traverseDownward(it) }
        val forClearing get() = downward
        val forFilling get() = upward

        inline fun create(crossinline builder: suspend SequenceBuilder<Vec3i>.(Region) -> Unit) = object : RegionTraverser() {
            override suspend fun SequenceBuilder<Vec3i>.build(region: Region) {
                builder(region)
            }
        }

        private suspend fun SequenceBuilder<Vec3i>.traverseDownward(region: Region) {
            val origin = region.origin
            val size = region.size
            repeat(size.y) { y ->
                repeat(size.z) { z ->
                    repeat(size.x) { x ->
                        yield(origin.add(x, size.y - y - 1, z))
                    }
                }
            }
        }

        private suspend fun SequenceBuilder<Vec3i>.traverseUpward(region: Region) {
            val origin = region.origin
            val size = region.size
            repeat(size.y) { y ->
                repeat(size.z) { z ->
                    repeat(size.x) { x ->
                        yield(origin.add(x, size.y - y - 1, z))
                    }
                }
            }
        }

        private fun slice(region: Region, atY: Int): Pair<Region, Region?> {
            if (atY < region.size.y + 1) {
                val first = Region(region.origin, region.size.withY(atY + 1))
                val second = Region(region.origin.withY(atY), region.size.addY(-atY-1))
                return first to second
            }
            return region to null
        }

        fun upToAndDownUntil(y: Int) = create { region ->
            val (bottom, top) = slice(region, y)
            traverseUpward(bottom)
            top?.let { traverseDownward(it) }
        }

    }


}
