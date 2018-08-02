@file:Suppress("NOTHING_TO_INLINE")

package io.dico.parcels2.storage

import io.dico.parcels2.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

typealias DataPair = Pair<ParcelId, ParcelData?>
typealias AddedDataPair<TAttach> = Pair<TAttach, MutableAddedDataMap>

interface Storage {
    val name: String
    val syncDispatcher: CoroutineDispatcher
    val asyncDispatcher: CoroutineDispatcher
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

class StorageWithCoroutineBacking internal constructor(val backing: Backing) : Storage {
    override val name get() = backing.name
    override val syncDispatcher = Executor { it.run() }.asCoroutineDispatcher()
    val poolSize: Int get() = 4
    override val asyncDispatcher = Executors.newFixedThreadPool(poolSize) { Thread(it, "Parcels2_StorageThread") }.asCoroutineDispatcher()
    override val isConnected get() = backing.isConnected
    val channelCapacity = 16

    private inline fun <T> defer(noinline block: suspend CoroutineScope.() -> T): Deferred<T> {
        return async(context = asyncDispatcher, start = CoroutineStart.ATOMIC, block = block)
    }

    private inline fun job(noinline block: suspend CoroutineScope.() -> Unit): Job {
        return launch(context = asyncDispatcher, start = CoroutineStart.ATOMIC, block = block)
    }

    private inline fun <T> openChannel(noinline block: suspend ProducerScope<T>.() -> Unit): ReceiveChannel<T> {
        return produce(asyncDispatcher, capacity = channelCapacity, block = block)
    }

    override fun init() = job { backing.init() }

    override fun shutdown() = job { backing.shutdown() }


    override fun readParcelData(parcel: ParcelId) = defer { backing.readParcelData(parcel) }

    override fun readParcelData(parcels: Sequence<ParcelId>) = openChannel<DataPair> { backing.produceParcelData(channel, parcels) }

    override fun readAllParcelData() = openChannel<DataPair> { backing.produceAllParcelData(channel) }

    override fun getOwnedParcels(user: ParcelOwner) = defer { backing.getOwnedParcels(user) }

    override fun getNumParcels(user: ParcelOwner) = defer { backing.getNumParcels(user) }

    override fun setParcelData(parcel: ParcelId, data: ParcelData?) = job { backing.setParcelData(parcel, data) }

    override fun setParcelOwner(parcel: ParcelId, owner: ParcelOwner?) = job { backing.setParcelOwner(parcel, owner) }

    override fun setParcelPlayerStatus(parcel: ParcelId, player: UUID, status: AddedStatus) = job { backing.setLocalPlayerStatus(parcel, player, status) }

    override fun setParcelAllowsInteractInventory(parcel: ParcelId, value: Boolean) = job { backing.setParcelAllowsInteractInventory(parcel, value) }

    override fun setParcelAllowsInteractInputs(parcel: ParcelId, value: Boolean) = job { backing.setParcelAllowsInteractInputs(parcel, value) }


    override fun readAllGlobalAddedData(): ReceiveChannel<AddedDataPair<ParcelOwner>> = openChannel { backing.produceAllGlobalAddedData(channel) }

    override fun readGlobalAddedData(owner: ParcelOwner): Deferred<MutableAddedDataMap?> = defer { backing.readGlobalAddedData(owner) }

    override fun setGlobalAddedStatus(owner: ParcelOwner, player: UUID, status: AddedStatus) = job { backing.setGlobalPlayerStatus(owner, player, status) }
}
