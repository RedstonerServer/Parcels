@file:Suppress("NOTHING_TO_INLINE", "PARAMETER_NAME_CHANGED_ON_OVERRIDE", "LocalVariableName", "UNUSED_EXPRESSION")

package io.dico.parcels2.storage.exposed

import com.zaxxer.hikari.HikariDataSource
import io.dico.parcels2.*
import io.dico.parcels2.storage.Backing
import io.dico.parcels2.storage.DataPair
import io.dico.parcels2.util.toUUID
import kotlinx.coroutines.experimental.*
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

class ExposedBacking(private val dataSourceFactory: () -> DataSource,
                     private val poolSize: Int) : Backing {
    override val name get() = "Exposed"
    val dispatcher: CoroutineDispatcher = newFixedThreadPoolContext(4, "Parcels StorageThread")

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

    private fun <T> transaction(statement: Transaction.() -> T) = transaction(database!!, statement)

    companion object {
        init {
            Database.registerDialect("mariadb") {
                Class.forName("org.jetbrains.exposed.sql.vendors.MysqlDialect").newInstance() as DatabaseDialect
            }
        }
    }

    override fun init() {
        if (isShutdown || isConnected) throw IllegalStateException()

        dataSource = dataSourceFactory()
        database = Database.connect(dataSource!!)
        transaction(database!!) {
            create(WorldsT, OwnersT, ParcelsT, ParcelOptionsT, AddedLocalT, AddedGlobalT)
        }
    }

    override fun shutdown() {
        if (isShutdown) throw IllegalStateException()
        dataSource?.let {
            (it as? HikariDataSource)?.close()
        }
        database = null
        isShutdown = true
    }

    override fun produceParcelData(channel: SendChannel<DataPair>, parcels: Sequence<ParcelId>) {
        for (parcel in parcels) {
            val data = readParcelData(parcel)
            channel.offer(parcel to data)
        }
        channel.close()
    }

    override fun produceAllParcelData(channel: SendChannel<Pair<ParcelId, ParcelData?>>) = ctransaction<Unit> {
        ParcelsT.selectAll().forEach { row ->
            val parcel = ParcelsT.getId(row) ?: return@forEach
            val data = rowToParcelData(row)
            channel.send(parcel to data)
        }
        channel.close()
    }

    override fun readParcelData(parcel: ParcelId): ParcelData? = transaction {
        val row = ParcelsT.getRow(parcel) ?: return@transaction null
        rowToParcelData(row)
    }

    override fun getOwnedParcels(user: ParcelOwner): List<ParcelId> = transaction {
        val user_id = OwnersT.getId(user) ?: return@transaction emptyList()
        ParcelsT.select { ParcelsT.owner_id eq user_id }
            .orderBy(ParcelsT.claim_time, isAsc = true)
            .mapNotNull(ParcelsT::getId)
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

        for ((uuid, status) in data.addedMap) {
            setLocalPlayerStatus(parcel, uuid, status)
        }

        setParcelAllowsInteractInputs(parcel, data.allowInteractInputs)
        setParcelAllowsInteractInventory(parcel, data.allowInteractInventory)
    }

    override fun setParcelOwner(parcel: ParcelId, owner: ParcelOwner?) = transaction {
        val id = if (owner == null)
            ParcelsT.getId(parcel) ?: return@transaction
        else
            ParcelsT.getOrInitId(parcel)

        val owner_id = owner?.let { OwnersT.getOrInitId(it) }
        val time = owner?.let { DateTime.now() }

        ParcelsT.update({ ParcelsT.id eq id }) {
            it[ParcelsT.owner_id] = owner_id
            it[claim_time] = time
        }
    }

    override fun setLocalPlayerStatus(parcel: ParcelId, player: UUID, status: AddedStatus) = transaction {
        AddedLocalT.setPlayerStatus(parcel, player, status)
    }

    override fun setParcelAllowsInteractInventory(parcel: ParcelId, value: Boolean): Unit = transaction {
        val id = ParcelsT.getOrInitId(parcel)
        ParcelOptionsT.upsert(ParcelOptionsT.parcel_id) {
            it[ParcelOptionsT.parcel_id] = id
            it[ParcelOptionsT.interact_inventory] = value
        }
    }

    override fun setParcelAllowsInteractInputs(parcel: ParcelId, value: Boolean): Unit = transaction {
        val id = ParcelsT.getOrInitId(parcel)
        ParcelOptionsT.upsert(ParcelOptionsT.parcel_id) {
            it[ParcelOptionsT.parcel_id] = id
            it[ParcelOptionsT.interact_inputs] = value
        }
    }

    override fun produceAllGlobalAddedData(channel: SendChannel<Pair<ParcelOwner, MutableMap<UUID, AddedStatus>>>) = ctransaction<Unit> {
        AddedGlobalT.sendAllAddedData(channel)
        channel.close()
    }

    override fun readGlobalAddedData(owner: ParcelOwner): MutableMap<UUID, AddedStatus> = transaction {
        return@transaction AddedGlobalT.readAddedData(OwnersT.getId(owner) ?: return@transaction hashMapOf())
    }

    override fun setGlobalPlayerStatus(owner: ParcelOwner, player: UUID, status: AddedStatus) = transaction {
        AddedGlobalT.setPlayerStatus(owner, player, status)
    }

    private fun rowToParcelData(row: ResultRow) = ParcelDataHolder().apply {
        owner = row[ParcelsT.owner_id]?.let { OwnersT.getId(it) }
        since = row[ParcelsT.claim_time]

        val parcelId = row[ParcelsT.id]
        addedMap = AddedLocalT.readAddedData(parcelId)

        AddedLocalT.select { AddedLocalT.attach_id eq parcelId }.forEach {
            val uuid = it[AddedLocalT.player_uuid].toUUID()
            val status = if (it[AddedLocalT.allowed_flag]) AddedStatus.ALLOWED else AddedStatus.BANNED
            setAddedStatus(uuid, status)
        }

        ParcelOptionsT.select { ParcelOptionsT.parcel_id eq parcelId }.firstOrNull()?.let {
            allowInteractInputs = it[ParcelOptionsT.interact_inputs]
            allowInteractInventory = it[ParcelOptionsT.interact_inventory]
        }
    }

}

