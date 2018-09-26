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
        val upward = Directional(TraverseDirection(1, 1, 1), TraverseOrderFactory.createWith(Dimension.Y, Dimension.X))
        val downward = Directional(TraverseDirection(1, -1, 1), TraverseOrderFactory.createWith(Dimension.Y, Dimension.X))
        val toClear get() = downward
        val toFill get() = upward

        fun convergingTo(y: Int) = Slicing(y, upward, downward, true)

        fun separatingFrom(y: Int) = Slicing(y, downward, upward, false)
    }

    class Directional(
        val direction: TraverseDirection,
        val order: TraverseOrder
    ) : RegionTraverser() {

        private inline fun iterate(max: Int, increasing: Boolean, action: (Int) -> Unit) {
            for (i in 0..max) {
                action(if (increasing) i else max - i)
            }
        }

        override suspend fun Scope.build(region: Region) {
            val order = order
            val (primary, secondary, tertiary) = order.toArray()
            val (origin, size) = region

            val maxOfPrimary = primary.extract(size) - 1
            val maxOfSecondary = secondary.extract(size) - 1
            val maxOfTertiary = tertiary.extract(size) - 1

            val isPrimaryIncreasing = direction.isIncreasing(primary)
            val isSecondaryIncreasing = direction.isIncreasing(secondary)
            val isTertiaryIncreasing = direction.isIncreasing(tertiary)

            iterate(maxOfPrimary, isPrimaryIncreasing) { p ->
                iterate(maxOfSecondary, isSecondaryIncreasing) { s ->
                    iterate(maxOfTertiary, isTertiaryIncreasing) { t ->
                        yield(order.add(origin, p, s, t))
                    }
                }
            }

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

    fun childForPosition(position: Vec3i): Directional {
        var cur = this
        while (true) {
            when (cur) {
                is Directional ->
                    return cur
                is Slicing ->
                    cur =
                        if (position.y <= cur.bottomSectionMaxY) cur.bottomTraverser
                        else cur.topTraverser
            }
        }
    }

    fun comesFirst(current: Vec3i, block: Vec3i): Boolean {
        var cur = this
        while (true) {
            when (cur) {
                is Directional -> return cur.direction.comesFirst(current, block)
                is Slicing -> {
                    val border = cur.bottomSectionMaxY
                    cur = when {
                        current.y <= border && block.y <= border -> cur.bottomTraverser
                        current.y <= border -> return !cur.bottomFirst
                        block.y <= border -> return cur.bottomFirst
                        else -> cur.topTraverser
                    }
                }
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

object TraverseOrderFactory {
    private fun isSwap(primary: Dimension, secondary: Dimension) = secondary.ordinal != (primary.ordinal + 1) % 3

    fun createWith(primary: Dimension, secondary: Dimension): TraverseOrder {
        // tertiary is implicit
        if (primary == secondary) throw IllegalArgumentException()
        return TraverseOrder(primary, isSwap(primary, secondary))
    }
}

inline class TraverseOrder(val orderNum: Int) {
    constructor(first: Dimension, swap: Boolean)
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

inline class TraverseDirection(val bits: Int) {
    fun isIncreasing(dimension: Dimension) = (1 shl dimension.ordinal) and bits != 0

    fun comesFirst(current: Vec3i, block: Vec3i, dimension: Dimension): Boolean =
        if (isIncreasing(dimension))
            dimension.extract(block) <= dimension.extract(current)
        else
            dimension.extract(block) >= dimension.extract(current)

    fun comesFirst(current: Vec3i, block: Vec3i) =
        comesFirst(current, block, Dimension.X)
            && comesFirst(current, block, Dimension.Y)
            && comesFirst(current, block, Dimension.Z)

    companion object {
        operator fun invoke(x: Int, y: Int, z: Int) = invoke(Vec3i(x, y, z))

        operator fun invoke(block: Vec3i): TraverseDirection {
            if (block.x == 0 || block.y == 0 || block.z == 0) throw IllegalArgumentException()
            var bits = 0
            if (block.x > 0) bits = bits or 1
            if (block.y > 0) bits = bits or 2
            if (block.z > 0) bits = bits or 4
            return TraverseDirection(bits)
        }
    }

}
