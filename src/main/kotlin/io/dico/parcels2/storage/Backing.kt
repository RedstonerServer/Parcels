package io.dico.parcels2.storage

import io.dico.parcels2.*
import kotlinx.coroutines.experimental.channels.SendChannel
import java.util.UUID

interface Backing {

    val name: String

    val isConnected: Boolean

    suspend fun init()

    suspend fun shutdown()


    /**
     * This producer function is capable of constantly reading parcels from a potentially infinite sequence,
     * and provide parcel data for it as read from the database.
     */
    suspend fun produceParcelData(channel: SendChannel<DataPair>, parcels: Sequence<ParcelId>)

    suspend fun produceAllParcelData(channel: SendChannel<DataPair>)

    suspend fun readParcelData(parcel: ParcelId): ParcelData?

    suspend fun getOwnedParcels(user: ParcelOwner): List<ParcelId>

    suspend fun getNumParcels(user: ParcelOwner): Int = getOwnedParcels(user).size


    suspend fun setParcelData(parcel: ParcelId, data: ParcelData?)

    suspend fun setParcelOwner(parcel: ParcelId, owner: ParcelOwner?)

    suspend fun setLocalPlayerStatus(parcel: ParcelId, player: UUID, status: AddedStatus)

    suspend fun setParcelAllowsInteractInventory(parcel: ParcelId, value: Boolean)

    suspend fun setParcelAllowsInteractInputs(parcel: ParcelId, value: Boolean)


    suspend fun produceAllGlobalAddedData(channel: SendChannel<AddedDataPair<ParcelOwner>>)

    suspend fun readGlobalAddedData(owner: ParcelOwner): MutableAddedDataMap

    suspend fun setGlobalPlayerStatus(owner: ParcelOwner, player: UUID, status: AddedStatus)

}