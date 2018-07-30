package io.dico.parcels2.util

import org.bukkit.World
import org.bukkit.block.Block

data class Vec3i(
    val x: Int,
    val y: Int,
    val z: Int
) {
    operator fun plus(o: Vec3i) = Vec3i(x + o.x, y + o.y, z + o.z)
    infix fun addX(o: Int) = Vec3i(x + o, y, z)
    infix fun addY(o: Int) = Vec3i(x, y + o, z)
    infix fun addZ(o: Int) = Vec3i(x, y, z + o)
    fun add(ox: Int, oy: Int, oz: Int) = Vec3i(x + ox, y + oy, z + oz)
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun World.get(vec: Vec3i): Block = getBlockAt(vec.x, vec.y, vec.z)