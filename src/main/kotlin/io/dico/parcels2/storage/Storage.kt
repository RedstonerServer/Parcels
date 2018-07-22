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

    private fun <T> future(block: suspend CoroutineScope.() -> T) = async(context = asyncDispatcher, start = CoroutineStart.ATOMIC, block = block)

    override fun init() = future { backing.init() }

    override fun shutdown() = future { backing.shutdown() }

    override fun readParcelData(parcelFor: Parcel) = future { backing.readParcelData(parcelFor) }

    override fun readParcelData(parcelsFor: Sequence<Parcel>, channelCapacity: Int) = produce(asyncDispatcher, capacity = channelCapacity) {
        with(backing) { produceParcelData(parcelsFor) }
    }

    override fun getOwnedParcels(user: ParcelOwner) = future { backing.getOwnedParcels(user) }

    override fun setParcelOwner(parcelFor: Parcel, owner: ParcelOwner?) = future { backing.setParcelOwner(parcelFor, owner) }

    override fun setParcelPlayerState(parcelFor: Parcel, player: UUID, state: Boolean?) = future { backing.setParcelPlayerState(parcelFor, player, state) }

    override fun setParcelAllowsInteractInventory(parcel: Parcel, value: Boolean) = future { backing.setParcelAllowsInteractInventory(parcel, value) }

    override fun setParcelAllowsInteractInputs(parcel: Parcel, value: Boolean) = future { backing.setParcelAllowsInteractInputs(parcel, value) }
}
