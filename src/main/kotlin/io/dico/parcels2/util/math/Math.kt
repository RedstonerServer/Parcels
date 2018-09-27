package io.dico.parcels2.util.math

fun Double.floor(): Int {
    val down = toInt()
    if (down.toDouble() != this && (java.lang.Double.doubleToRawLongBits(this).ushr(63).toInt()) == 1) {
        return down - 1
    }
    return down
}

infix fun Int.umod(divisor: Int): Int {
    val out = this % divisor
    if (out < 0) {
        return out + divisor
    }
    return out
}

val Int.even: Boolean get() = and(1) == 0

fun IntRange.clamp(min: Int, max: Int): IntRange {
    if (first < min) {
        if (last > max) {
            return IntRange(min, max)
        }
        return IntRange(min, last)
    }
    if (last > max) {
        return IntRange(first, max)
    }
    return this
}

// the name coerceAtMost is bad
fun Int.clampMax(max: Int) = coerceAtMost(max)
fun Double.clampMin(min: Double) = coerceAtLeast(min)
fun Double.clampMax(max: Double) = coerceAtMost(max)

// Why does this not exist?
infix fun Int.ceilDiv(divisor: Int): Int {
    return -Math.floorDiv(-this, divisor)
}