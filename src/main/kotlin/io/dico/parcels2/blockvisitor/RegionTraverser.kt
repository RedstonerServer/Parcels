package io.dico.parcels2.blockvisitor

import io.dico.parcels2.util.math.Dimension
import io.dico.parcels2.util.math.Region
import io.dico.parcels2.util.math.Vec3i
import io.dico.parcels2.util.math.clampMax

private typealias Scope = SequenceScope<Vec3i>
/*
class ParcelTraverser(
    val parcelProvider: ParcelProvider,
    val delegate: RegionTraverser,
    scope: CoroutineScope
) : RegionTraverser(), CoroutineScope by scope {

    class OccupiedException(parcelId: ParcelId) : Exception("Parcel $parcelId is occupied")

    /**
     * Traverse the blocks of parcel's land
     * The iterator must be exhausted, else the permit to traverse it will not be reclaimed.
     *
     * @throws OccupiedException if a parcel is maintained with the given parcel id and an
     *   iterator exists for it that has not been exhausted
     */
    fun traverseParcel(parcelId: ParcelId): Iterator<Vec3i> {
        val world = parcelProvider.getWorldById(parcelId.worldId)
            ?: throw IllegalArgumentException()
        val parcel = parcelProvider.getParcelById(parcelId)

        val medium = if (parcel != null) {
            if (parcel.hasBlockVisitors || parcel !is ParcelImpl) {
                throw OccupiedException(parcelId)
            }
            parcel.hasBlockVisitors = true
            TraverserMedium { parcel.hasBlockVisitors = false }
        } else {
            TraverserMedium.DoNothing
        }

        val region = world.blockManager.getRegion(parcelId)
        return traverseRegion(region, world.world.maxHeight, medium)
    }

    override suspend fun Scope.build(region: Region, medium: TraverserMedium) {
        with(delegate) {
            return build(region, medium)
        }
    }

}

@Suppress("FunctionName")
inline fun TraverserMedium(crossinline whenComplete: () -> Unit) =
    object : TraverserMedium {
        override fun iterationCompleted() {
            whenComplete()
        }
    }

/**
 * An object that is able to communicate with an iterator returned by [RegionTraverser]
 *
 */
interface TraverserMedium {

    /**
     * Called by the traverser during first [Iterator.hasNext] call that returns false
     */
    fun iterationCompleted()

    /**
     * The default [TraverserMedium], which does nothing.
     */
    object DoNothing : TraverserMedium {
        override fun iterationCompleted() {}
    }
}*/

sealed class RegionTraverser {

    /**
     * Get an iterator traversing [region] using this traverser.
     * Depending on the implementation, [region] might be traversed in a specific order and direction.
     */
    fun traverseRegion(
        region: Region,
        worldHeight: Int = 256/*,
        medium: TraverserMedium = TraverserMedium.DoNothing*/
    ): Iterator<Vec3i> = iterator { build(validify(region, worldHeight)/*, medium*/) }

    abstract suspend fun Scope.build(region: Region/*, medium: TraverserMedium = TraverserMedium.DoNothing*/)

    companion object {
        val upward = Directional(TraverseDirection(1, 1, 1), TraverseOrderFactory.createWith(Dimension.Y, Dimension.X))
        val downward = Directional(TraverseDirection(1, -1, 1), TraverseOrderFactory.createWith(Dimension.Y, Dimension.X))
        val toClear get() = downward
        val toFill get() = upward

        /**
         * The returned [RegionTraverser] will traverse the regions
         * * below and including absolute level [y] first, in [upward] direction.
         * * above absolute level [y] last, in [downward] direction.
         */
        fun convergingTo(y: Int) = Slicing(y, upward, downward, true)

        /**
         * The returned [RegionTraverser] will traverse the regions
         * * above absolute level [y] first, in [upward] direction.
         * * below and including absolute level [y] second, in [downward] direction.
         */
        fun separatingFrom(y: Int) = Slicing(y, downward, upward, false)

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

        override suspend fun Scope.build(region: Region/*, medium: TraverserMedium*/) {
            val order = order
            val (primary, secondary, tertiary) = order.toArray()
            val (origin, size) = region

            val maxOfPrimary = size[primary] - 1
            val maxOfSecondary = size[secondary] - 1
            val maxOfTertiary = size[tertiary] - 1

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

            /*medium.iterationCompleted()*/
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
                val bottom = Region(region.origin, region.size.withY(atY + 1))
                val top = Region(region.origin.withY(atY + 1), region.size.addY(-atY - 1))
                return bottom to top
            }
            return region to null
        }

        override suspend fun Scope.build(region: Region/*, medium: TraverserMedium*/) {
            val (bottom, top) = slice(region, bottomSectionMaxY)

            if (bottomFirst) {
                with(bottomTraverser) { build(bottom) }
                top?.let { with(topTraverser) { build(it) } }
            } else {
                top?.let { with(topTraverser) { build(it) } }
                with(bottomTraverser) { build(bottom) }
            }

            /*medium.iterationCompleted()*/
        }
    }

    /**
     * Returns [Directional] instance that would be responsible for
     * emitting the given position if it is contained in a region.
     * [Directional] instance has a set order and direction
     */
    fun childForPosition(position: Vec3i): Directional {
        var cur = this
        while (true) {
            when (cur) {
                /*is ParcelTraverser -> cur = cur.delegate*/
                is Directional -> return cur
                is Slicing ->
                    cur =
                        if (position.y <= cur.bottomSectionMaxY) cur.bottomTraverser
                        else cur.topTraverser
            }
        }
    }

    /**
     * Returns true if and only if this traverser would visit the given
     * [block] position before the given [current] position.
     * If at least one of [block] and [current] is not contained in a
     * region being traversed the result is undefined.
     */
    fun comesFirst(current: Vec3i, block: Vec3i): Boolean {
        var cur = this
        while (true) {
            when (cur) {
                /*is ParcelTraverser -> cur = cur.delegate*/
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

    fun add(vec: Vec3i, p: Int, s: Int, t: Int): Vec3i =
    // optimize this, will be called lots
        when (orderNum) {
            0 -> vec.add(p, s, t) // xyz
            1 -> vec.add(t, p, s) // yzx
            2 -> vec.add(s, t, p) // zxy
            3 -> vec.add(p, t, s) // xzy
            4 -> vec.add(s, p, t) // yxz
            5 -> vec.add(t, s, p) // zyx
            else -> error("Invalid orderNum $orderNum")
        }
}

inline class TraverseDirection(val bits: Int) {
    fun isIncreasing(dimension: Dimension) = (1 shl dimension.ordinal) and bits != 0

    fun comesFirst(current: Vec3i, block: Vec3i, dimension: Dimension): Boolean =
        if (isIncreasing(dimension))
            block[dimension] <= current[dimension]
        else
            block[dimension] >= current[dimension]

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
