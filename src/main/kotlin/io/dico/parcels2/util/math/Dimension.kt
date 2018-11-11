package io.dico.parcels2.util.math

enum class Dimension {
    X,
    Y,
    Z;

    val otherDimensions
        get() = when (this) {
            X -> Y to Z
            Y -> X to Z
            Z -> X to Y
        }

    companion object {
        private val values = values()
        operator fun get(ordinal: Int) = values[ordinal]
    }
}