@file:Suppress("NOTHING_TO_INLINE")

package io.dico.parcels2.storage

import io.dico.parcels2.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.util.UUID
import kotlin.coroutines.CoroutineContext

typealias DataPair = Pair<ParcelId, ParcelData?>
typealias AddedDataPair<TAttach> = Pair<TAttach, MutablePrivilegeMap>

interface Storage {
    val name: String
    val isConnected: Boolean

    fun init(): Job

    fun shutdown(): Job


    fun getWorldCreationTime(worldId: ParcelWorldId): Deferred<DateTime?>

    fun setWorldCreationTime(worldId: ParcelWorldId, time: DateTime): Job

    fun getPlayerUuidForName(name: String): Deferred<UUID?>

    fun updatePlayerName(uuid: UUID, name: String): Job

    fun readParcelData(parcel: ParcelId): Deferred<ParcelData?>

    fun transmitParcelData(parcels: Sequence<ParcelId>): ReceiveChannel<DataPair>

    fun transmitAllParcelData(): ReceiveChannel<DataPair>

    fun getOwnedParcels(user: PlayerProfile): Deferred<List<ParcelId>>

    fun getNumParcels(user: PlayerProfile): Deferred<Int>


    fun setParcelData(parcel: ParcelId, data: ParcelData?): Job

    fun setParcelOwner(parcel: ParcelId, owner: PlayerProfile?): Job

    fun setParcelOwnerSignOutdated(parcel: ParcelId, outdated: Boolean): Job

    fun setLocalPrivilege(parcel: ParcelId, player: PlayerProfile, privilege: Privilege): Job

    fun setParcelOptionsInteractConfig(parcel: ParcelId, config: InteractableConfiguration): Job


    fun transmitAllGlobalAddedData(): ReceiveChannel<AddedDataPair<PlayerProfile>>

    fun readGlobalPrivileges(owner: PlayerProfile): Deferred<MutablePrivilegeMap?>

    fun setGlobalPrivilege(owner: PlayerProfile, player: PlayerProfile, status: Privilege): Job


    fun getChannelToUpdateParcelData(): SendChannel<Pair<ParcelId, ParcelData>>
}

class BackedStorage internal constructor(val b: Backing) : Storage, CoroutineScope {
    override val name get() = b.name
    override val isConnected get() = b.isConnected
    override val coroutineContext: CoroutineContext get() = b.coroutineContext

    override fun init() = launch { b.init() }

    override fun shutdown() = launch { b.shutdown() }


    override fun getWorldCreationTime(worldId: ParcelWorldId): Deferred<DateTime?> = b.launchFuture { b.getWorldCreationTime(worldId) }

    override fun setWorldCreationTime(worldId: ParcelWorldId, time: DateTime): Job = b.launchJob { b.setWorldCreationTime(worldId, time) }

    override fun getPlayerUuidForName(name: String): Deferred<UUID?> = b.launchFuture { b.getPlayerUuidForName(name) }

    override fun updatePlayerName(uuid: UUID, name: String): Job = b.launchJob { b.updatePlayerName(uuid, name) }

    override fun readParcelData(parcel: ParcelId) = b.launchFuture { b.readParcelData(parcel) }

    override fun transmitParcelData(parcels: Sequence<ParcelId>) = b.openChannel<DataPair> { b.transmitParcelData(it, parcels) }

    override fun transmitAllParcelData() = b.openChannel<DataPair> { b.transmitAllParcelData(it) }

    override fun getOwnedParcels(user: PlayerProfile) = b.launchFuture { b.getOwnedParcels(user) }

    override fun getNumParcels(user: PlayerProfile) = b.launchFuture { b.getNumParcels(user) }

    override fun setParcelData(parcel: ParcelId, data: ParcelData?) = b.launchJob { b.setParcelData(parcel, data) }

    override fun setParcelOwner(parcel: ParcelId, owner: PlayerProfile?) = b.launchJob { b.setParcelOwner(parcel, owner) }

    override fun setParcelOwnerSignOutdated(parcel: ParcelId, outdated: Boolean): Job = b.launchJob { b.setParcelOwnerSignOutdated(parcel, outdated) }

    override fun setLocalPrivilege(parcel: ParcelId, player: PlayerProfile, privilege: Privilege) = b.launchJob { b.setLocalPrivilege(parcel, player, privilege) }

    override fun setParcelOptionsInteractConfig(parcel: ParcelId, config: InteractableConfiguration) = b.launchJob { b.setParcelOptionsInteractConfig(parcel, config) }


    override fun transmitAllGlobalAddedData(): ReceiveChannel<AddedDataPair<PlayerProfile>> = b.openChannel { b.transmitAllGlobalAddedData(it) }

    override fun readGlobalPrivileges(owner: PlayerProfile): Deferred<MutablePrivilegeMap?> = b.launchFuture { b.readGlobalPrivileges(owner) }

    override fun setGlobalPrivilege(owner: PlayerProfile, player: PlayerProfile, status: Privilege) = b.launchJob { b.setGlobalPrivilege(owner, player, status) }

    override fun getChannelToUpdateParcelData(): SendChannel<Pair<ParcelId, ParcelData>> = b.openChannelForWriting { b.setParcelData(it.first, it.second) }
}
