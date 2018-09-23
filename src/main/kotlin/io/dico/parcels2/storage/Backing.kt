package io.dico.parcels2.storage

import io.dico.parcels2.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.joda.time.DateTime
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


    fun getWorldCreationTime(worldId: ParcelWorldId): DateTime?

    fun setWorldCreationTime(worldId: ParcelWorldId, time: DateTime)

    fun getPlayerUuidForName(name: String): UUID?

    fun updatePlayerName(uuid: UUID, name: String)

    fun transmitParcelData(channel: SendChannel<DataPair>, parcels: Sequence<ParcelId>)

    fun transmitAllParcelData(channel: SendChannel<DataPair>)

    fun readParcelData(parcel: ParcelId): ParcelData?

    fun getOwnedParcels(user: PlayerProfile): List<ParcelId>

    fun getNumParcels(user: PlayerProfile): Int = getOwnedParcels(user).size


    fun setParcelData(parcel: ParcelId, data: ParcelData?)

    fun setParcelOwner(parcel: ParcelId, owner: PlayerProfile?)

    fun setParcelOwnerSignOutdated(parcel: ParcelId, outdated: Boolean)

    fun setLocalPlayerStatus(parcel: ParcelId, player: PlayerProfile, status: AddedStatus)

    fun setParcelAllowsInteractInventory(parcel: ParcelId, value: Boolean)

    fun setParcelAllowsInteractInputs(parcel: ParcelId, value: Boolean)


    fun transmitAllGlobalAddedData(channel: SendChannel<AddedDataPair<PlayerProfile>>)

    fun readGlobalAddedData(owner: PlayerProfile): MutableAddedDataMap

    fun setGlobalPlayerStatus(owner: PlayerProfile, player: PlayerProfile, status: AddedStatus)
}
