@file:Suppress("NOTHING_TO_INLINE", "PARAMETER_NAME_CHANGED_ON_OVERRIDE", "LocalVariableName", "UNUSED_EXPRESSION")

package io.dico.parcels2.storage.exposed

import com.zaxxer.hikari.HikariDataSource
import io.dico.parcels2.*
import io.dico.parcels2.storage.AddedDataPair
import io.dico.parcels2.storage.Backing
import io.dico.parcels2.storage.DataPair
import io.dico.parcels2.util.synchronized
import io.dico.parcels2.util.toUUID
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ArrayChannel
import kotlinx.coroutines.experimental.channels.LinkedListChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.DatabaseDialect
import org.joda.time.DateTime
import java.util.UUID
import javax.sql.DataSource

class ExposedDatabaseException(message: String? = null) : Exception(message)

class ExposedBacking(private val dataSourceFactory: () -> DataSource, val poolSize: Int) : Backing {
    override val name get() = "Exposed"
    override val dispatcher: ThreadPoolDispatcher = newFixedThreadPoolContext(poolSize, "Parcels StorageThread")

    private var dataSource: DataSource? = null
    private var database: Database? = null
    private var isShutdown: Boolean = false
    override val isConnected get() = database != null

    override fun launchJob(job: Backing.() -> Unit): Job = launch(dispatcher) { transaction { job() } }
    override fun <T> launchFuture(future: Backing.() -> T): Deferred<T> = async(dispatcher) { transaction { future() } }

    override fun <T> openChannel(future: Backing.(SendChannel<T>) -> Unit): ReceiveChannel<T> {
        val channel = LinkedListChannel<T>()
        launchJob { future(channel) }
        return channel
    }

    override fun <T> openChannelForWriting(action: Backing.(T) -> Unit): SendChannel<T> {
        val channel = ArrayChannel<T>(poolSize * 2)

        repeat(poolSize) {
            launch(dispatcher) {
                try {
                    while (true) {
                        action(channel.receive())
                    }
                } catch (ex: Exception) {
                    // channel closed
                }
            }
        }

        return channel
    }

    private fun <T> transaction(statement: Transaction.() -> T) = transaction(database!!, statement)

    companion object {
        init {
            Database.registerDialect("mariadb") {
                Class.forName("org.jetbrains.exposed.sql.vendors.MysqlDialect").newInstance() as DatabaseDialect
            }
        }
    }

    override fun init() {
        synchronized {
            if (isShutdown || isConnected) throw IllegalStateException()
            dataSource = dataSourceFactory()
            database = Database.connect(dataSource!!)
            transaction(database!!) {
                create(WorldsT, ProfilesT, ParcelsT, ParcelOptionsT, AddedLocalT, AddedGlobalT)
            }
        }
    }

    override fun shutdown() {
        synchronized {
            if (isShutdown) throw IllegalStateException()
            dataSource?.let {
                (it as? HikariDataSource)?.close()
            }
            database = null
            isShutdown = true
        }
    }

    @Suppress("RedundantObjectTypeCheck")
    private fun PlayerProfile.toOwnerProfile(): PlayerProfile {
        if (this is PlayerProfile.Star) return PlayerProfile.Fake(name)
        return this
    }

    private fun PlayerProfile.Unresolved.toResolvedProfile(): PlayerProfile.Real {
        return resolve(getPlayerUuidForName(name) ?: throwException())
    }

    private fun PlayerProfile.toResolvedProfile(): PlayerProfile {
        if (this is PlayerProfile.Unresolved) return toResolvedProfile()
        return this
    }

    private fun PlayerProfile.toRealProfile(): PlayerProfile.Real = when (this) {
        is PlayerProfile.Real -> this
        is PlayerProfile.Fake -> throw IllegalArgumentException("Fake profiles are not accepted")
        is PlayerProfile.Unresolved -> toResolvedProfile()
        else -> throw InternalError("Case should not be reached")
    }

    override fun getPlayerUuidForName(name: String): UUID? {
        return ProfilesT.slice(ProfilesT.uuid).select { ProfilesT.name.upperCase() eq name.toUpperCase() }
            .firstOrNull()?.let { it[ProfilesT.uuid]?.toUUID() }
    }

    override fun transmitParcelData(channel: SendChannel<DataPair>, parcels: Sequence<ParcelId>) {
        for (parcel in parcels) {
            val data = readParcelData(parcel)
            channel.offer(parcel to data)
        }
        channel.close()
    }

    override fun transmitAllParcelData(channel: SendChannel<DataPair>) {
        ParcelsT.selectAll().forEach { row ->
            val parcel = ParcelsT.getItem(row) ?: return@forEach
            val data = rowToParcelData(row)
            channel.offer(parcel to data)
        }
        channel.close()
    }

    override fun readParcelData(parcel: ParcelId): ParcelData? {
        val row = ParcelsT.getRow(parcel) ?: return null
        return rowToParcelData(row)
    }

    override fun getOwnedParcels(user: PlayerProfile): List<ParcelId> {
        val user_id = ProfilesT.getId(user.toOwnerProfile()) ?: return emptyList()
        return ParcelsT.select { ParcelsT.owner_id eq user_id }
            .orderBy(ParcelsT.claim_time, isAsc = true)
            .mapNotNull(ParcelsT::getItem)
            .toList()
    }

    override fun setParcelData(parcel: ParcelId, data: ParcelData?) {
        if (data == null) {
            transaction {
                ParcelsT.getId(parcel)?.let { id ->
                    ParcelsT.deleteIgnoreWhere { ParcelsT.id eq id }

                    // Below should cascade automatically
                    /*
                    AddedLocalT.deleteIgnoreWhere { AddedLocalT.parcel_id eq id }
                    ParcelOptionsT.deleteIgnoreWhere(limit = 1) { ParcelOptionsT.parcel_id eq id }
                    */
                }

            }
            return
        }

        transaction {
            val id = ParcelsT.getOrInitId(parcel)
            AddedLocalT.deleteIgnoreWhere { AddedLocalT.attach_id eq id }
        }

        setParcelOwner(parcel, data.owner)

        for ((profile, status) in data.addedMap) {
            AddedLocalT.setPlayerStatus(parcel, profile, status)
        }

        setParcelAllowsInteractInputs(parcel, data.allowInteractInputs)
        setParcelAllowsInteractInventory(parcel, data.allowInteractInventory)
    }

    override fun setParcelOwner(parcel: ParcelId, owner: PlayerProfile?) {
        val id = if (owner == null)
            ParcelsT.getId(parcel) ?: return
        else
            ParcelsT.getOrInitId(parcel)

        val owner_id = owner?.let { ProfilesT.getOrInitId(it.toOwnerProfile()) }
        val time = owner?.let { DateTime.now() }

        ParcelsT.update({ ParcelsT.id eq id }) {
            it[ParcelsT.owner_id] = owner_id
            it[claim_time] = time
        }
    }

    override fun setLocalPlayerStatus(parcel: ParcelId, player: PlayerProfile, status: AddedStatus) {
        AddedLocalT.setPlayerStatus(parcel, player.toRealProfile(), status)
    }

    override fun setParcelAllowsInteractInventory(parcel: ParcelId, value: Boolean) {
        val id = ParcelsT.getOrInitId(parcel)
        ParcelOptionsT.upsert(ParcelOptionsT.parcel_id) {
            it[ParcelOptionsT.parcel_id] = id
            it[ParcelOptionsT.interact_inventory] = value
        }
    }

    override fun setParcelAllowsInteractInputs(parcel: ParcelId, value: Boolean) {
        val id = ParcelsT.getOrInitId(parcel)
        ParcelOptionsT.upsert(ParcelOptionsT.parcel_id) {
            it[ParcelOptionsT.parcel_id] = id
            it[ParcelOptionsT.interact_inputs] = value
        }
    }

    override fun transmitAllGlobalAddedData(channel: SendChannel<AddedDataPair<PlayerProfile>>) {
        AddedGlobalT.sendAllAddedData(channel)
        channel.close()
    }

    override fun readGlobalAddedData(owner: PlayerProfile): MutableAddedDataMap {
        return AddedGlobalT.readAddedData(ProfilesT.getId(owner.toOwnerProfile()) ?: return hashMapOf())
    }

    override fun setGlobalPlayerStatus(owner: PlayerProfile, player: PlayerProfile, status: AddedStatus) {
        AddedGlobalT.setPlayerStatus(owner, player.toRealProfile(), status)
    }

    private fun rowToParcelData(row: ResultRow) = ParcelDataHolder().apply {
        owner = row[ParcelsT.owner_id]?.let { ProfilesT.getItem(it) }
        since = row[ParcelsT.claim_time]

        val id = row[ParcelsT.id]
        ParcelOptionsT.select { ParcelOptionsT.parcel_id eq id }.firstOrNull()?.let { optrow ->
            allowInteractInputs = optrow[ParcelOptionsT.interact_inputs]
            allowInteractInventory = optrow[ParcelOptionsT.interact_inventory]
        }

        addedMap = AddedLocalT.readAddedData(id)
    }

}

