package io.dico.parcels2.util.math

import io.dico.parcels2.util.math.ext.floor
import org.bukkit.Location
import kotlin.math.sqrt

data class Vec3d(
    val x: Double,
    val y: Double,
    val z: Double
) {
    constructor(loc: Location) : this(loc.x, loc.y, loc.z)

    operator fun plus(o: Vec3d) = Vec3d(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3i) = Vec3d(x - o.x, y - o.y, z - o.z)
    infix fun addX(o: Double) = Vec3d(x + o, y, z)
    infix fun addY(o: Double) = Vec3d(x, y + o, z)
    infix fun addZ(o: Double) = Vec3d(x, y, z + o)
    infix fun withX(o: Double) = Vec3d(o, y, z)
    infix fun withY(o: Double) = Vec3d(x, o, z)
    infix fun withZ(o: Double) = Vec3d(x, y, o)
    fun add(ox: Double, oy: Double, oz: Double) = Vec3d(x + ox, y + oy, z + oz)
    fun toVec3i() = Vec3i(x.floor(), y.floor(), z.floor())

    fun distanceSquared(o: Vec3d): Double {
        val dx = o.x - x
        val dy = o.y - y
        val dz = o.z - z
        return dx * dx + dy * dy + dz * dz
    }

    fun distance(o: Vec3d) = sqrt(distanceSquared(o))

    operator fun get(dimension: Dimension) =
        when (dimension) {
            Dimension.X -> x
            Dimension.Y -> y
            Dimension.Z -> z
        }

    fun with(dimension: Dimension, value: Double) =
        when (dimension) {
            Dimension.X -> withX(value)
            Dimension.Y -> withY(value)
            Dimension.Z -> withZ(value)
        }

    fun add(dimension: Dimension, value: Double) =
        when (dimension) {
            Dimension.X -> addX(value)
            Dimension.Y -> addY(value)
            Dimension.Z -> addZ(value)
        }
}