package io.dico.parcels2.storage

import io.dico.parcels2.AddedStatus
import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelData
import io.dico.parcels2.ParcelOwner
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

interface Storage {
    val name: String
    val syncDispatcher: CoroutineDispatcher
    val asyncDispatcher: CoroutineDispatcher
    val isConnected: Boolean

    fun init(): Job

    fun shutdown(): Job


    fun readParcelData(parcelFor: Parcel): Deferred<ParcelData?>

    fun readParcelData(parcelsFor: Sequence<Parcel>): ReceiveChannel<Pair<Parcel, ParcelData?>>

    fun readAllParcelData(): ReceiveChannel<Pair<SerializableParcel, ParcelData?>>

    fun getOwnedParcels(user: ParcelOwner): Deferred<List<SerializableParcel>>

    fun getNumParcels(user: ParcelOwner): Deferred<Int>


    fun setParcelData(parcelFor: Parcel, data: ParcelData?): Job

    fun setParcelOwner(parcelFor: Parcel, owner: ParcelOwner?): Job

    fun setParcelPlayerStatus(parcelFor: Parcel, player: UUID, status: AddedStatus): Job

    fun setParcelAllowsInteractInventory(parcel: Parcel, value: Boolean): Job

    fun setParcelAllowsInteractInputs(parcel: Parcel, value: Boolean): Job


    fun readAllGlobalAddedData(): ReceiveChannel<Pair<ParcelOwner, MutableMap<UUID, AddedStatus>>>

    fun readGlobalAddedData(owner: ParcelOwner): Deferred<MutableMap<UUID, AddedStatus>?>

    fun setGlobalAddedStatus(owner: ParcelOwner, player: UUID, status: AddedStatus): Job
}

class StorageWithCoroutineBacking internal constructor(val backing: Backing) : Storage {
    override val name get() = backing.name
    override val syncDispatcher = Executor { it.run() }.asCoroutineDispatcher()
    val poolSize: Int get() = 4
    override val asyncDispatcher = Executors.newFixedThreadPool(poolSize) { Thread(it, "Parcels2_StorageThread") }.asCoroutineDispatcher()
    override val isConnected get() = backing.isConnected
    val channelCapacity = 16

    @Suppress("NOTHING_TO_INLINE")
    private inline fun <T> defer(noinline block: suspend CoroutineScope.() -> T): Deferred<T> {
        return async(context = asyncDispatcher, start = CoroutineStart.ATOMIC, block = block)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun job(noinline block: suspend CoroutineScope.() -> Unit): Job {
        return launch(context = asyncDispatcher, start = CoroutineStart.ATOMIC, block = block)
    }

    override fun init() = job { backing.init() }

    override fun shutdown() = job { backing.shutdown() }


    override fun readParcelData(parcelFor: Parcel) = defer { backing.readParcelData(parcelFor) }

    override fun readParcelData(parcelsFor: Sequence<Parcel>) =
        produce(asyncDispatcher, capacity = channelCapacity) { with(backing) { produceParcelData(parcelsFor) } }

    override fun readAllParcelData(): ReceiveChannel<Pair<SerializableParcel, ParcelData?>> =
        produce(asyncDispatcher, capacity = channelCapacity) { with(backing) { produceAllParcelData() } }

    override fun getOwnedParcels(user: ParcelOwner) = defer { backing.getOwnedParcels(user) }

    override fun getNumParcels(user: ParcelOwner) = defer { backing.getNumParcels(user) }

    override fun setParcelData(parcelFor: Parcel, data: ParcelData?) = job { backing.setParcelData(parcelFor, data) }

    override fun setParcelOwner(parcelFor: Parcel, owner: ParcelOwner?) = job { backing.setParcelOwner(parcelFor, owner) }

    override fun setParcelPlayerStatus(parcelFor: Parcel, player: UUID, status: AddedStatus) = job { backing.setLocalPlayerStatus(parcelFor, player, status) }

    override fun setParcelAllowsInteractInventory(parcel: Parcel, value: Boolean) = job { backing.setParcelAllowsInteractInventory(parcel, value) }

    override fun setParcelAllowsInteractInputs(parcel: Parcel, value: Boolean) = job { backing.setParcelAllowsInteractInputs(parcel, value) }


    override fun readAllGlobalAddedData(): ReceiveChannel<Pair<ParcelOwner, MutableMap<UUID, AddedStatus>>> =
        produce(asyncDispatcher, capacity = channelCapacity) { with(backing) { produceAllGlobalAddedData() } }

    override fun readGlobalAddedData(owner: ParcelOwner): Deferred<MutableMap<UUID, AddedStatus>?> = defer { backing.readGlobalAddedData(owner) }

    override fun setGlobalAddedStatus(owner: ParcelOwner, player: UUID, status: AddedStatus) = job { backing.setGlobalPlayerStatus(owner, player, status) }
}
