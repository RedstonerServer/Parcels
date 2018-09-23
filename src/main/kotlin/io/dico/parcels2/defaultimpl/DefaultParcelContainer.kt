package io.dico.parcels2.defaultimpl

import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelContainer
import io.dico.parcels2.ParcelId
import io.dico.parcels2.ParcelWorld

class DefaultParcelContainer(val world: ParcelWorld) : ParcelContainer {
    private var parcels: Array<Array<Parcel>>

    init {
        parcels = initArray(world.options.axisLimit, world)
    }

    fun resizeIfSizeChanged() {
        if (parcels.size != world.options.axisLimit * 2 + 1) {
            resize(world.options.axisLimit)
        }
    }

    fun resize(axisLimit: Int) {
        parcels = initArray(axisLimit, world, this)
    }

    fun initArray(axisLimit: Int, world: ParcelWorld, cur: DefaultParcelContainer? = null): Array<Array<Parcel>> {
        val arraySize = 2 * axisLimit + 1
        return Array(arraySize) {
            val x = it - axisLimit
            Array(arraySize) {
                val z = it - axisLimit
                cur?.getParcelById(x, z) ?: ParcelImpl(world, x, z)
            }
        }
    }

    override fun getParcelById(x: Int, z: Int): Parcel? {
        return parcels.getOrNull(x + world.options.axisLimit)?.getOrNull(z + world.options.axisLimit)
    }

    override fun getParcelById(id: ParcelId): Parcel? {
        if (!world.id.equals(id.worldId)) throw IllegalArgumentException()
        return when (id) {
            is Parcel -> id
            else -> getParcelById(id.x, id.z)
        }
    }

    override fun nextEmptyParcel(): Parcel? {
        return walkInCircle().find { it.owner == null }
    }

    private fun walkInCircle(): Iterable<Parcel> = Iterable {
        iterator {
            val center = world.options.axisLimit
            for (radius in 0..center) {
                var x = center - radius;
                var z = center - radius
                repeat(radius * 2) { yield(parcels[x++][z]) }
                repeat(radius * 2) { yield(parcels[x][z++]) }
                repeat(radius * 2) { yield(parcels[x--][z]) }
                repeat(radius * 2) { yield(parcels[x][z--]) }
            }
        }
    }

    fun allParcels(): Sequence<Parcel> = sequence {
        for (array in parcels) {
            yieldAll(array.iterator())
        }
    }

}