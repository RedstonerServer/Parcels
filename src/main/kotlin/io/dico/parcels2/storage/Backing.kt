package io.dico.parcels2.storage

import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelData
import io.dico.parcels2.ParcelOwner
import io.dico.parcels2.storage.SerializableParcel
import kotlinx.coroutines.experimental.channels.ProducerScope
import java.util.*

interface Backing {

    val name: String

    suspend fun init()

    suspend fun shutdown()

    /**
     * This producer function is capable of constantly reading plots from a potentially infinite sequence,
     * and provide plotdata for it as read from the database.
     */

    suspend fun ProducerScope<Pair<Parcel, ParcelData?>>.produceParcelData(parcels: Sequence<Parcel>)

    suspend fun readParcelData(plotFor: Parcel): ParcelData?

    suspend fun getOwnedParcels(user: ParcelOwner): List<SerializableParcel>

    suspend fun setParcelOwner(plotFor: Parcel, owner: ParcelOwner?)

    suspend fun setParcelPlayerState(plotFor: Parcel, player: UUID, state: Boolean?)

    suspend fun setParcelAllowsInteractInventory(plot: Parcel, value: Boolean)

    suspend fun setParcelAllowsInteractInputs(plot: Parcel, value: Boolean)

}