package io.dico.parcels2.storage

import io.dico.parcels2.*
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import java.util.UUID

interface Backing {

    val name: String

    val isConnected: Boolean

    val dispatcher: CoroutineDispatcher

    fun launchJob(job: Backing.() -> Unit): Job

    fun <T> launchFuture(future: Backing.() -> T): Deferred<T>

    fun <T> openChannel(future: Backing.(SendChannel<T>) -> Unit): ReceiveChannel<T>

    fun <T> openChannelForWriting(future: Backing.(T) -> Unit): SendChannel<T>


    fun init()

    fun shutdown()


    fun getPlayerUuidForName(name: String): UUID?

    fun transmitParcelData(channel: SendChannel<DataPair>, parcels: Sequence<ParcelId>)

    fun transmitAllParcelData(channel: SendChannel<DataPair>)

    fun readParcelData(parcel: ParcelId): ParcelData?

    fun getOwnedParcels(user: PlayerProfile): List<ParcelId>

    fun getNumParcels(user: PlayerProfile): Int = getOwnedParcels(user).size


    fun setParcelData(parcel: ParcelId, data: ParcelData?)

    fun setParcelOwner(parcel: ParcelId, owner: PlayerProfile?)

    fun setLocalPlayerStatus(parcel: ParcelId, player: PlayerProfile, status: AddedStatus)

    fun setParcelAllowsInteractInventory(parcel: ParcelId, value: Boolean)

    fun setParcelAllowsInteractInputs(parcel: ParcelId, value: Boolean)


    fun transmitAllGlobalAddedData(channel: SendChannel<AddedDataPair<PlayerProfile>>)

    fun readGlobalAddedData(owner: PlayerProfile): MutableAddedDataMap

    fun setGlobalPlayerStatus(owner: PlayerProfile, player: PlayerProfile, status: AddedStatus)
}
