package io.dico.parcels2.util.math

data class Vec2i(
    val x: Int,
    val z: Int
) {
    fun add(ox: Int, oz: Int) = Vec2i(x + ox, z + oz)
    fun toChunk() = Vec2i(x shr 4, z shr 4)
}
