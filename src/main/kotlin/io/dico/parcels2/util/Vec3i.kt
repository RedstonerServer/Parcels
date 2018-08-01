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

/*
inline class IVec3i(private val data: Long) {

    private companion object {
        const val mask = 0x001F_FFFF
        const val max: Int = 0x000F_FFFF // +1048575
        const val min: Int = -max - 1    // -1048575 // 0xFFF0_0000

        @Suppress("NOTHING_TO_INLINE")
        inline fun Int.compressIntoLong(offset: Int): Long {
            if (this !in min..max) throw IllegalArgumentException()
            return and(mask).toLong().shl(offset)
        }

        @Suppress("NOTHING_TO_INLINE")
        inline fun Long.extractInt(offset: Int): Int {
            return ushr(offset).toInt().and(mask)
        }
    }

    constructor(x: Int, y: Int, z: Int) : this(
        x.compressIntoLong(42)
            or y.compressIntoLong(21)
            or z.compressIntoLong(0))

    val x: Int get() = data.extractInt(42)
    val y: Int get() = data.extractInt(21)
    val z: Int get() = data.extractInt(0)

}
*/
