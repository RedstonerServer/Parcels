package io.dico.parcels2.util.math

data class Region(val origin: Vec3i, val size: Vec3i) {
    val blockCount: Int get() = size.x * size.y * size.z

    val center: Vec3d
        get() {
            val x = (origin.x + size.x) / 2.0
            val y = (origin.y + size.y) / 2.0
            val z = (origin.z + size.z) / 2.0
            return Vec3d(x, y, z)
        }

    val end: Vec3i
        get() = origin + size

    val max: Vec3i
        get() = Vec3i(origin.x + size.x - 1, origin.y + size.y - 1, origin.z + size.z - 1)

    fun withSize(size: Vec3i): Region {
        if (size == this.size) return this
        return Region(origin, size)
    }

    operator fun contains(loc: Vec3i): Boolean = getFirstUncontainedDimensionOf(loc) == null

    fun getFirstUncontainedDimensionOf(loc: Vec3i): Dimension? {
        val max = max
        return when {
            loc.x !in origin.x..max.x -> Dimension.X
            loc.z !in origin.z..max.z -> Dimension.Z
            loc.y !in origin.y..max.y -> Dimension.Y
            else -> null
        }
    }

}