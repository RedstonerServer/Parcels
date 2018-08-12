package io.dico.parcels2.util

data class Region(val origin: Vec3i, val size: Vec3i) {
    val blockCount: Int get() = size.x * size.y * size.z

    val center: Vec3d
        get() {
            val x = (origin.x + size.x) / 2.0
            val y = (origin.y + size.y) / 2.0
            val z = (origin.z + size.z) / 2.0
            return Vec3d(x, y, z)
        }
}