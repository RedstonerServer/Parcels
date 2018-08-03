package io.dico.parcels2.storage

import io.dico.parcels2.*
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import java.util.UUID

interface Backing {

    val name: String

    val isConnected: Boolean

    fun launchJob(job: Backing.() -> Unit): Job

    fun <T> launchFuture(future: Backing.() -> T): Deferred<T>

    fun <T> openChannel(future: Backing.(SendChannel<T>) -> Unit): ReceiveChannel<T>


    fun init()

    fun shutdown()


    /**
     * This producer function is capable of constantly reading parcels from a potentially infinite sequence,
     * and provide parcel data for it as read from the database.
     */
    fun produceParcelData(channel: SendChannel<DataPair>, parcels: Sequence<ParcelId>)

    fun produceAllParcelData(channel: SendChannel<DataPair>)

    fun readParcelData(parcel: ParcelId): ParcelData?

    fun getOwnedParcels(user: ParcelOwner): List<ParcelId>

    fun getNumParcels(user: ParcelOwner): Int = getOwnedParcels(user).size


    fun setParcelData(parcel: ParcelId, data: ParcelData?)

    fun setParcelOwner(parcel: ParcelId, owner: ParcelOwner?)

    fun setLocalPlayerStatus(parcel: ParcelId, player: UUID, status: AddedStatus)

    fun setParcelAllowsInteractInventory(parcel: ParcelId, value: Boolean)

    fun setParcelAllowsInteractInputs(parcel: ParcelId, value: Boolean)


    fun produceAllGlobalAddedData(channel: SendChannel<AddedDataPair<ParcelOwner>>)

    fun readGlobalAddedData(owner: ParcelOwner): MutableAddedDataMap

    fun setGlobalPlayerStatus(owner: ParcelOwner, player: UUID, status: AddedStatus)

}

abstract class AbstractBacking(val dispatcher: CoroutineDispatcher) {

    fun launchJob(job: Backing.() -> Unit): Job

    fun <T> launchFuture(future: Backing.() -> T): Deferred<T>

    fun <T> openChannel(future: Backing.(SendChannel<T>) -> Unit): ReceiveChannel<T>

}
