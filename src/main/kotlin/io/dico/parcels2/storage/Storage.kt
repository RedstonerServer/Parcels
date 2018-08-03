@file:Suppress("NOTHING_TO_INLINE")

package io.dico.parcels2.storage

import io.dico.parcels2.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import java.util.UUID

typealias DataPair = Pair<ParcelId, ParcelData?>
typealias AddedDataPair<TAttach> = Pair<TAttach, MutableAddedDataMap>

interface Storage {
    val name: String
    val isConnected: Boolean

    fun init(): Job

    fun shutdown(): Job


    fun readParcelData(parcel: ParcelId): Deferred<ParcelData?>

    fun readParcelData(parcels: Sequence<ParcelId>): ReceiveChannel<DataPair>

    fun readAllParcelData(): ReceiveChannel<DataPair>

    fun getOwnedParcels(user: ParcelOwner): Deferred<List<ParcelId>>

    fun getNumParcels(user: ParcelOwner): Deferred<Int>


    fun setParcelData(parcel: ParcelId, data: ParcelData?): Job

    fun setParcelOwner(parcel: ParcelId, owner: ParcelOwner?): Job

    fun setParcelPlayerStatus(parcel: ParcelId, player: UUID, status: AddedStatus): Job

    fun setParcelAllowsInteractInventory(parcel: ParcelId, value: Boolean): Job

    fun setParcelAllowsInteractInputs(parcel: ParcelId, value: Boolean): Job


    fun readAllGlobalAddedData(): ReceiveChannel<AddedDataPair<ParcelOwner>>

    fun readGlobalAddedData(owner: ParcelOwner): Deferred<MutableAddedDataMap?>

    fun setGlobalAddedStatus(owner: ParcelOwner, player: UUID, status: AddedStatus): Job
}

class BackedStorage internal constructor(val b: Backing) : Storage {
    override val name get() = b.name
    override val isConnected get() = b.isConnected

    override fun init() = b.launchJob { init() }

    override fun shutdown() = b.launchJob { shutdown() }


    override fun readParcelData(parcel: ParcelId) = b.launchFuture { readParcelData(parcel) }

    override fun readParcelData(parcels: Sequence<ParcelId>) = b.openChannel<DataPair> { produceParcelData(it, parcels) }

    override fun readAllParcelData() = b.openChannel<DataPair> { produceAllParcelData(it) }

    override fun getOwnedParcels(user: ParcelOwner) = b.launchFuture { getOwnedParcels(user) }

    override fun getNumParcels(user: ParcelOwner) = b.launchFuture { getNumParcels(user) }

    override fun setParcelData(parcel: ParcelId, data: ParcelData?) = b.launchJob { setParcelData(parcel, data) }

    override fun setParcelOwner(parcel: ParcelId, owner: ParcelOwner?) = b.launchJob { setParcelOwner(parcel, owner) }

    override fun setParcelPlayerStatus(parcel: ParcelId, player: UUID, status: AddedStatus) = b.launchJob { setLocalPlayerStatus(parcel, player, status) }

    override fun setParcelAllowsInteractInventory(parcel: ParcelId, value: Boolean) = b.launchJob { setParcelAllowsInteractInventory(parcel, value) }

    override fun setParcelAllowsInteractInputs(parcel: ParcelId, value: Boolean) = b.launchJob { setParcelAllowsInteractInputs(parcel, value) }


    override fun readAllGlobalAddedData(): ReceiveChannel<AddedDataPair<ParcelOwner>> = b.openChannel { produceAllGlobalAddedData(it) }

    override fun readGlobalAddedData(owner: ParcelOwner): Deferred<MutableAddedDataMap?> = b.launchFuture { readGlobalAddedData(owner) }

    override fun setGlobalAddedStatus(owner: ParcelOwner, player: UUID, status: AddedStatus) = b.launchJob { setGlobalPlayerStatus(owner, player, status) }
}
