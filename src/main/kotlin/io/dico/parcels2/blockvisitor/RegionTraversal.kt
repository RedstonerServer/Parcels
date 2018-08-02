package io.dico.parcels2.blockvisitor

import io.dico.parcels2.util.Region
import io.dico.parcels2.util.Vec3i
import kotlin.coroutines.experimental.SequenceBuilder
import kotlin.coroutines.experimental.buildIterator

enum class RegionTraversal(private val builder: suspend SequenceBuilder<Vec3i>.(Region) -> Unit) {
    DOWNWARD({ region ->
        val origin = region.origin
        val size = region.size

        repeat(size.y) { y ->
            repeat(size.z) { z ->
                repeat(size.x) { x ->
                    yield(origin.add(x, size.y - y - 1, z))
                }
            }
        }

    }),

    UPWARD({ region ->
        val origin = region.origin
        val size = region.size

        repeat(size.y) { y ->
            repeat(size.z) { z ->
                repeat(size.x) { x ->
                    yield(origin.add(x, y, z))
                }
            }
        }
    }),

    ;

    fun regionTraverser(region: Region) = Iterable { buildIterator { builder(region) } }

}



