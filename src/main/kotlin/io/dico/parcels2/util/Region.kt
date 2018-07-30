package io.dico.parcels2.util

data class Region(val origin: Vec3i, val size: Vec3i) {
    val blockCount: Int get() = size.x * size.y * size.z
}