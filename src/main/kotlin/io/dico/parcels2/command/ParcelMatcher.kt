package io.dico.parcels2.command

import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelWorld
import io.dico.parcels2.ParcelsPlugin
import kotlinx.coroutines.experimental.Deferred

interface ParcelTarget {
    val world: ParcelWorld
    val isByID: Boolean
    val isByOwner: Boolean get() = !isByID
    suspend fun ParcelsPlugin.await(): Parcel?
    fun ParcelsPlugin.get(): Deferred<Parcel?> =
}

class ParcelTargetByOwner : ParcelTarget {
    override val isByID get() = false
}

class ParcelTargetByID : ParcelTarget {
    override val isByID get() = true

}
