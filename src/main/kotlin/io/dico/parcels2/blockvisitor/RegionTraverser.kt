package io.dico.parcels2.blockvisitor

import io.dico.parcels2.util.Region
import io.dico.parcels2.util.Vec3i
import io.dico.parcels2.util.ext.clampMax

private typealias Scope = SequenceScope<Vec3i>

sealed class RegionTraverser {
    fun traverseRegion(region: Region, worldHeight: Int = 256): Iterable<Vec3i> =
        Iterable { iterator<Vec3i> { build(validify(region, worldHeight)) } }

    private fun validify(region: Region, worldHeight: Int): Region {
        if (region.origin.y < 0) {
            val origin = region.origin withY 0
            val size = region.size.withY((region.size.y + region.origin.y).clampMax(worldHeight))
            return Region(origin, size)
        }

        if (region.origin.y + region.size.y > worldHeight) {
            val size = region.size.withY(worldHeight - region.origin.y)
            return Region(region.origin, size)
        }

        return region
    }

    protected abstract suspend fun Scope.build(region: Region)

    companion object {
        val upward = Directional(TraverseDirection(1, 1, 1), TraverseOrder(Dimension.Y, Dimension.X))
        val downward = Directional(TraverseDirection(1, -1, 1), TraverseOrder(Dimension.Y, Dimension.X))
        val toClear get() = downward
        val toFill get() = upward

        fun convergingTo(y: Int) = Slicing(y, upward, downward, true)

        fun separatingFrom(y: Int) = Slicing(y, downward, upward, false)
    }

    class Directional(
        val direction: TraverseDirection,
        val order: TraverseOrder
    ) : RegionTraverser() {
        override suspend fun Scope.build(region: Region) {
            //traverserLogic(region, order, direction)
        }

    }

    class Slicing(
        val bottomSectionMaxY: Int,
        val bottomTraverser: RegionTraverser,
        val topTraverser: RegionTraverser,
        val bottomFirst: Boolean = true
    ) : RegionTraverser() {

        private fun slice(region: Region, atY: Int): Pair<Region, Region?> {
            if (atY < region.size.y + 1) {
                val first = Region(region.origin, region.size.withY(atY + 1))
                val second = Region(region.origin.withY(atY), region.size.addY(-atY - 1))
                return first to second
            }
            return region to null
        }

        override suspend fun Scope.build(region: Region) {
            val (bottom, top) = slice(region, bottomSectionMaxY)

            if (bottomFirst) {
                with(bottomTraverser) { build(bottom) }
                top?.let { with(topTraverser) { build(it) } }
            } else {
                top?.let { with(topTraverser) { build(it) } }
                with(bottomTraverser) { build(bottom) }
            }
        }
    }

}

enum class Dimension {
    X,
    Y,
    Z;

    fun extract(block: Vec3i) =
        when (this) {
            X -> block.x
            Y -> block.y
            Z -> block.z
        }

    companion object {
        private val values = values()
        operator fun get(ordinal: Int) = values[ordinal]
    }
}

inline class TraverseOrder(val orderNum: Int) {
    private constructor(first: Dimension, swap: Boolean)
        : this(if (swap) first.ordinal + 3 else first.ordinal)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun element(index: Int) = Dimension[(orderNum + index) % 3]

    private val swap inline get() = orderNum >= 3

    /**
     * The slowest changing dimension
     */
    val primary: Dimension get() = element(0)

    /**
     * Second slowest changing dimension
     */
    val secondary: Dimension get() = element(if (swap) 2 else 1)

    /**
     * Dimension that changes every block
     */
    val tertiary: Dimension get() = element(if (swap) 1 else 2)

    /**
     * All 3 dimensions in this order
     */
    fun toArray() = arrayOf(primary, secondary, tertiary)

    companion object {
        private fun isSwap(primary: Dimension, secondary: Dimension) = secondary.ordinal != (primary.ordinal + 1) % 3

        operator fun invoke(primary: Dimension, secondary: Dimension): TraverseOrder {
            // tertiary is implicit
            if (primary == secondary) throw IllegalArgumentException()
            return TraverseOrder(primary, isSwap(primary, secondary))
        }
    }

    fun add(vec: Vec3i, dp: Int, ds: Int, dt: Int): Vec3i =
        // optimize this, will be called lots
        when (orderNum) {
            0 -> vec.add(dp, ds, dt) // xyz
            1 -> vec.add(dt, dp, ds) // yzx
            2 -> vec.add(ds, dt, dp) // zxy
            3 -> vec.add(dp, dt, ds) // xzy
            4 -> vec.add(ds, dp, dt) // yxz
            5 -> vec.add(dt, ds, dp) // zyx
            else -> error("Invalid orderNum $orderNum")
        }
}

class AltTraverser(val size: Vec3i,
                   val order: TraverseOrder,
                   val direction: TraverseDirection) {


    suspend fun Scope.build() {
        doPrimary()
    }

    private suspend fun Scope.doPrimary() {
        val dimension = order.primary
        direction.directionOf(dimension).traverse(dimension.extract(size)) { value ->

        }
    }

    private fun Dimension.setValue(value: Int) {

    }

}

enum class Increment(val offset: Int) {
    UP(1),
    DOWN(-1);

    companion object {
        fun convert(bool: Boolean) = if (bool) UP else DOWN
    }

    inline fun traverse(size: Int, op: (Int) -> Unit) {
        when (this) {
            UP -> repeat(size, op)
            DOWN -> repeat(size) { op(size - it - 1) }
        }
    }

}

inline class TraverseDirection(val bits: Int) {

    fun directionOf(dimension: Dimension) = Increment.convert((1 shl dimension.ordinal) and bits != 0)

    companion object {
        operator fun invoke(x: Int, y: Int, z: Int) = invoke(Vec3i(x, y, z))

        operator fun invoke(block: Vec3i): TraverseDirection {
            if (block.x == 0 || block.y == 0 || block.z == 0) throw IllegalArgumentException()
            var bits = 0
            if (block.x > 0) bits = bits or 1
            if (block.y > 0) bits = bits or 2
            if (block.z > 0) bits = bits or 3
            return TraverseDirection(bits)
        }
    }

}

/*
private typealias Scope = SequenceScope<Vec3i>
private typealias ScopeAction = suspend Scope.(Int, Int, Int) -> Unit

@Suppress("NON_EXHAUSTIVE_WHEN")
suspend fun Scope.traverserLogic(
    region: Region,
    order: TraverseOrder,
    direction: TraverseDirection
) = with(direction) {
    val (primary, secondary, tertiary) = order.toArray()
    val (origin, size) = region

    when (order.primary) {
        Dimension.X ->
            when (order.secondary) {
                Dimension.Y -> {
                    directionOf(primary).traverse(primary.extract(size)) { p ->
                        directionOf(secondary).traverse(secondary.extract(size)) { s ->
                            directionOf(tertiary).traverse(tertiary.extract(size)) { t ->
                                yield(origin.add(p, s, t))
                            }
                        }
                    }
                }
                Dimension.Z -> {
                    directionOf(primary).traverse(primary.extract(size)) { p ->
                        directionOf(secondary).traverse(secondary.extract(size)) { s ->
                            directionOf(tertiary).traverse(tertiary.extract(size)) { t ->
                                yield(origin.add(p, t, s))
                            }
                        }
                    }
                }
            }

        Dimension.Y ->
            when (order.secondary) {
                Dimension.X -> {
                    directionOf(primary).traverse(primary.extract(size)) { p ->
                        directionOf(secondary).traverse(secondary.extract(size)) { s ->
                            directionOf(tertiary).traverse(tertiary.extract(size)) { t ->
                                yield(origin.add(s, p, t))
                            }
                        }
                    }
                }
                Dimension.Z -> {
                    directionOf(primary).traverse(primary.extract(size)) { p ->
                        directionOf(secondary).traverse(secondary.extract(size)) { s ->
                            directionOf(tertiary).traverse(tertiary.extract(size)) { t ->
                                yield(origin.add(t, p, s))
                            }
                        }
                    }
                }
            }

        Dimension.Z ->
            when (order.secondary) {
                Dimension.X -> {
                    directionOf(primary).traverse(primary.extract(size)) { p ->
                        directionOf(secondary).traverse(secondary.extract(size)) { s ->
                            directionOf(tertiary).traverse(tertiary.extract(size)) { t ->
                                yield(origin.add(s, t, p))
                            }
                        }
                    }
                }
                Dimension.Y -> {
                    directionOf(primary).traverse(primary.extract(size)) { p ->
                        directionOf(secondary).traverse(secondary.extract(size)) { s ->
                            directionOf(tertiary).traverse(tertiary.extract(size)) { t ->
                                yield(origin.add(t, s, p))
                            }
                        }
                    }
                }
            }
    }
}

*/