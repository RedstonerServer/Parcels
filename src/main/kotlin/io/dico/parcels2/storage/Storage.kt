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

    fun init(): Deferred<Unit>

    fun shutdown(): Deferred<Unit>


    fun readParcelData(parcelFor: Parcel): Deferred<ParcelData?>

    fun readParcelData(parcelsFor: Sequence<Parcel>, channelCapacity: Int): ReceiveChannel<Pair<Parcel, ParcelData?>>

    fun getOwnedParcels(user: ParcelOwner): Deferred<List<SerializableParcel>>


    fun setParcelData(parcelFor: Parcel, data: ParcelData?): Deferred<Unit>

    fun setParcelOwner(parcelFor: Parcel, owner: ParcelOwner?): Deferred<Unit>

    fun setParcelPlayerState(parcelFor: Parcel, player: UUID, state: Boolean?): Deferred<Unit>

    fun setParcelAllowsInteractInventory(parcel: Parcel, value: Boolean): Deferred<Unit>

    fun setParcelAllowsInteractInputs(parcel: Parcel, value: Boolean): Deferred<Unit>

}

class StorageWithCoroutineBacking internal constructor(val backing: Backing) : Storage {
    override val name get() = backing.name
    override val syncDispatcher = Executor { it.run() }.asCoroutineDispatcher()
    val poolSize: Int get() = 4
    override val asyncDispatcher = Executors.newFixedThreadPool(poolSize) { Thread(it, "Parcels2_StorageThread") }.asCoroutineDispatcher()

    @Suppress("NOTHING_TO_INLINE")
    private inline fun <T> defer(noinline block: suspend CoroutineScope.() -> T): Deferred<T> {
        return async(context = asyncDispatcher, start = CoroutineStart.ATOMIC, block = block)
    }

    override fun init() = defer { backing.init() }

    override fun shutdown() = defer { backing.shutdown() }


    override fun readParcelData(parcelFor: Parcel) = defer { backing.readParcelData(parcelFor) }

    override fun readParcelData(parcelsFor: Sequence<Parcel>, channelCapacity: Int) = produce(asyncDispatcher, capacity = channelCapacity) {
        with(backing) { produceParcelData(parcelsFor) }
    }


    override fun setParcelData(parcelFor: Parcel, data: ParcelData?) = defer { backing.setParcelData(parcelFor, data) }

    override fun getOwnedParcels(user: ParcelOwner) = defer { backing.getOwnedParcels(user) }

    override fun setParcelOwner(parcelFor: Parcel, owner: ParcelOwner?) = defer { backing.setParcelOwner(parcelFor, owner) }

    override fun setParcelPlayerState(parcelFor: Parcel, player: UUID, state: Boolean?) = defer { backing.setParcelPlayerState(parcelFor, player, state) }

    override fun setParcelAllowsInteractInventory(parcel: Parcel, value: Boolean) = defer { backing.setParcelAllowsInteractInventory(parcel, value) }

    override fun setParcelAllowsInteractInputs(parcel: Parcel, value: Boolean) = defer { backing.setParcelAllowsInteractInputs(parcel, value) }
}
