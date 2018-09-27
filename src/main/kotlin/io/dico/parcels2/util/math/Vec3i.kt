package io.dico.parcels2.util.math

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace

data class Vec3i(
    val x: Int,
    val y: Int,
    val z: Int
) {
    constructor(loc: Location) : this(loc.blockX, loc.blockY, loc.blockZ)

    operator fun plus(o: Vec3i) = Vec3i(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3i) = Vec3i(x - o.x, y - o.y, z - o.z)
    infix fun addX(o: Int) = Vec3i(x + o, y, z)
    infix fun addY(o: Int) = Vec3i(x, y + o, z)
    infix fun addZ(o: Int) = Vec3i(x, y, z + o)
    infix fun withX(o: Int) = Vec3i(o, y, z)
    infix fun withY(o: Int) = Vec3i(x, o, z)
    infix fun withZ(o: Int) = Vec3i(x, y, o)
    fun add(ox: Int, oy: Int, oz: Int) = Vec3i(x + ox, y + oy, z + oz)
    fun neg() = Vec3i(-x, -y, -z)
    fun clampMax(o: Vec3i) = Vec3i(x.clampMax(o.x), y.clampMax(o.y), z.clampMax(o.z))

    operator fun get(dimension: Dimension) =
        when (dimension) {
            Dimension.X -> x
            Dimension.Y -> y
            Dimension.Z -> z
        }

    fun with(dimension: Dimension, value: Int) =
        when (dimension) {
            Dimension.X -> withX(value)
            Dimension.Y -> withY(value)
            Dimension.Z -> withZ(value)
        }

    fun add(dimension: Dimension, value: Int) =
        when (dimension) {
            Dimension.X -> addX(value)
            Dimension.Y -> addY(value)
            Dimension.Z -> addZ(value)
        }

    companion object {
        private operator fun invoke(face: BlockFace) = Vec3i(face.modX, face.modY, face.modZ)
        val down = Vec3i(BlockFace.DOWN)
        val up = Vec3i(BlockFace.UP)
        val north = Vec3i(BlockFace.NORTH)
        val east = Vec3i(BlockFace.EAST)
        val south = Vec3i(BlockFace.SOUTH)
        val west = Vec3i(BlockFace.WEST)

        fun convert(face: BlockFace) = when (face) {
            BlockFace.DOWN -> down
            BlockFace.UP -> up
            BlockFace.NORTH -> north
            BlockFace.EAST -> east
            BlockFace.SOUTH -> south
            BlockFace.WEST -> west
            else -> Vec3i(face)
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun World.get(vec: Vec3i): Block = getBlockAt(vec.x, vec.y, vec.z)

/*
private /*inline */class IVec3i(private val data: Long) {

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
            val result = ushr(offset).toInt().and(mask)
            return if (result > max) result or mask.inv() else result
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
