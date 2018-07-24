package io.dico.parcels2.storage

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

    fun readParcelData(parcelsFor: Sequence<Parcel>, channelCapacity: Int): ReceiveChannel<Pair<Parcel, ParcelData?>>

    fun getOwnedParcels(user: ParcelOwner): Deferred<List<SerializableParcel>>

    fun getNumParcels(user: ParcelOwner): Deferred<Int>


    fun setParcelData(parcelFor: Parcel, data: ParcelData?): Job

    fun setParcelOwner(parcelFor: Parcel, owner: ParcelOwner?): Job

    fun setParcelPlayerState(parcelFor: Parcel, player: UUID, state: Boolean?): Job

    fun setParcelAllowsInteractInventory(parcel: Parcel, value: Boolean): Job

    fun setParcelAllowsInteractInputs(parcel: Parcel, value: Boolean): Job

}

class StorageWithCoroutineBacking internal constructor(val backing: Backing) : Storage {
    override val name get() = backing.name
    override val syncDispatcher = Executor { it.run() }.asCoroutineDispatcher()
    val poolSize: Int get() = 4
    override val asyncDispatcher = Executors.newFixedThreadPool(poolSize) { Thread(it, "Parcels2_StorageThread") }.asCoroutineDispatcher()
    override val isConnected get() = backing.isConnected

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

    override fun readParcelData(parcelsFor: Sequence<Parcel>, channelCapacity: Int) = produce(asyncDispatcher, capacity = channelCapacity) {
        with(backing) { produceParcelData(parcelsFor) }
    }

    override fun getOwnedParcels(user: ParcelOwner) = defer { backing.getOwnedParcels(user) }

    override fun getNumParcels(user: ParcelOwner) = defer { backing.getNumParcels(user) }

    override fun setParcelData(parcelFor: Parcel, data: ParcelData?) = job { backing.setParcelData(parcelFor, data) }

    override fun setParcelOwner(parcelFor: Parcel, owner: ParcelOwner?) = job { backing.setParcelOwner(parcelFor, owner) }

    override fun setParcelPlayerState(parcelFor: Parcel, player: UUID, state: Boolean?) = job { backing.setParcelPlayerState(parcelFor, player, state) }

    override fun setParcelAllowsInteractInventory(parcel: Parcel, value: Boolean) = job { backing.setParcelAllowsInteractInventory(parcel, value) }

    override fun setParcelAllowsInteractInputs(parcel: Parcel, value: Boolean) = job { backing.setParcelAllowsInteractInputs(parcel, value) }
}
