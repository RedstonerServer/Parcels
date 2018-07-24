package io.dico.parcels2.storage

import com.zaxxer.hikari.HikariDataSource
import io.dico.parcels2.*
import io.dico.parcels2.math.Vec2i
import io.dico.parcels2.util.synchronized
import io.dico.parcels2.util.toByteArray
import io.dico.parcels2.util.toUUID
import kotlinx.coroutines.experimental.channels.ProducerScope
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import javax.sql.DataSource

object WorldsT : Table("worlds") {
    val id = integer("id").autoIncrement().primaryKey()
    val name = varchar("name", 50)
    val uid = binary("uid", 16)
        .also { uniqueIndex("index_uid", it) }
}

object ParcelsT : Table("parcels") {
    val id = integer("id").autoIncrement().primaryKey()
    val px = integer("px")
    val pz = integer("pz")
    val world_id = integer("id")
        .also { uniqueIndex("index_location", it, px, pz) }
        .references(WorldsT.id)
    val owner_uuid = binary("owner_uuid", 16).nullable()
    val owner_name = varchar("owner_name", 16).nullable()
}

object AddedLocalT : Table("parcels_added_local") {
    val parcel_id = integer("parcel_id")
        .references(ParcelsT.id, ReferenceOption.CASCADE)
    val player_uuid = binary("player_uuid", 16)
        .also { uniqueIndex("index_pair", parcel_id, it) }
    val allowed_flag = bool("allowed_flag")
}

object AddedGlobalT : Table("parcels_added_global") {
    val owner_uuid = binary("owner_uuid", 16)
    val player_uuid = binary("player_uuid", 16)
        .also { uniqueIndex("index_pair", owner_uuid, it) }
    val allowed_flag = bool("allowed_flag")
}

object ParcelOptionsT : Table("parcel_options") {
    val parcel_id = integer("parcel_id")
        .also { uniqueIndex("index_parcel_id", it) }
        .references(ParcelsT.id, ReferenceOption.CASCADE)
    val interact_inventory = bool("interact_inventory").default(false)
    val interact_inputs = bool("interact_inputs").default(false)
}

private class ExposedDatabaseException(message: String? = null) : Exception(message)

@Suppress("NOTHING_TO_INLINE")
class ExposedBacking(val dataSource: DataSource) : Backing {
    override val name get() = "Exposed"
    private var database: Database? = null
    private var isShutdown: Boolean = false

    override val isConnected get() = database != null

    override suspend fun init() {
        synchronized {
            if (isShutdown) throw IllegalStateException()
            database = Database.connect(dataSource)
            transaction(database) {
                create(WorldsT, ParcelsT, AddedLocalT, ParcelOptionsT)
            }
        }
    }

    override suspend fun shutdown() {
        synchronized {
            if (isShutdown) throw IllegalStateException()
            if (dataSource is HikariDataSource) {
                dataSource.close()
            }
            database = null
            isShutdown = true
        }
    }

    private fun <T> transaction(statement: Transaction.() -> T) = transaction(database, statement)

    private inline fun Transaction.getWorldId(binaryUid: ByteArray): Int? {
        return WorldsT.select { WorldsT.uid eq binaryUid }.firstOrNull()?.let { it[WorldsT.id] }
    }

    private inline fun Transaction.getWorldId(worldUid: UUID): Int? {
        return getWorldId(worldUid.toByteArray()!!)
    }

    private inline fun Transaction.getOrInitWorldId(worldUid: UUID, worldName: String): Int {
        val binaryUid = worldUid.toByteArray()!!
        return getWorldId(binaryUid)
            ?: WorldsT.insertIgnore { it[uid] = binaryUid; it[name] = worldName }.get(WorldsT.id)
            ?: throw ExposedDatabaseException("This should not happen - failed to insert world named $worldName and get its id")
    }

    private inline fun Transaction.getParcelId(worldId: Int, parcelX: Int, parcelZ: Int): Int? {
        return ParcelsT.select { (ParcelsT.world_id eq worldId) and (ParcelsT.px eq parcelX) and (ParcelsT.pz eq parcelZ) }
            .firstOrNull()?.let { it[ParcelsT.id] }
    }

    private inline fun Transaction.getParcelId(worldUid: UUID, parcelX: Int, parcelZ: Int): Int? {
        return getWorldId(worldUid)?.let { getParcelId(it, parcelX, parcelZ) }
    }

    private inline fun Transaction.getOrInitParcelId(worldUid: UUID, worldName: String, parcelX: Int, parcelZ: Int): Int {
        val worldId = getOrInitWorldId(worldUid, worldName)
        return getParcelId(worldId, parcelX, parcelZ)
            ?: ParcelsT.insertIgnore { it[world_id] = worldId; it[px] = parcelX; it[pz] = parcelZ }.get(ParcelsT.id)
            ?: throw ExposedDatabaseException("This should not happen - failed to insert parcel at $worldName($parcelX, $parcelZ)")
    }

    private inline fun Transaction.getParcelRow(id: Int): ResultRow? {
        return ParcelsT.select { ParcelsT.id eq id }.firstOrNull()
    }

    fun Transaction.getWorldId(world: ParcelWorld): Int? {
        return getWorldId(world.world.uid)
    }

    fun Transaction.getOrInitWorldId(world: ParcelWorld): Int {
        return world.world.let { getOrInitWorldId(it.uid, it.name) }
    }

    fun Transaction.getParcelId(parcel: Parcel): Int? {
        return getParcelId(parcel.world.world.uid, parcel.pos.x, parcel.pos.z)
    }

    fun Transaction.getOrInitParcelId(parcel: Parcel): Int {
        return parcel.world.world.let { getOrInitParcelId(it.uid, it.name, parcel.pos.x, parcel.pos.z) }
    }

    fun Transaction.getParcelRow(parcel: Parcel): ResultRow? {
        return getParcelId(parcel)?.let { getParcelRow(it) }
    }

    override suspend fun ProducerScope<Pair<Parcel, ParcelData?>>.produceParcelData(parcels: Sequence<Parcel>) {
        for (parcel in parcels) {
            val data = readParcelData(parcel)
            channel.send(parcel to data)
        }
        channel.close()
    }

    override suspend fun readParcelData(parcelFor: Parcel): ParcelData? = transaction {
        val row = getParcelRow(parcelFor) ?: return@transaction null

        ParcelDataHolder().apply {

            owner = ParcelOwner.create(
                uuid = row[ParcelsT.owner_uuid]?.toUUID(),
                name = row[ParcelsT.owner_name]
            )

            val parcelId = row[ParcelsT.id]
            AddedLocalT.select { AddedLocalT.parcel_id eq parcelId }.forEach {
                val uuid = it[AddedLocalT.player_uuid].toUUID()!!
                val status = if (it[AddedLocalT.allowed_flag]) AddedStatus.ALLOWED else AddedStatus.BANNED
                setAddedStatus(uuid, status)
            }

            ParcelOptionsT.select { ParcelOptionsT.parcel_id eq parcelId }.firstOrNull()?.let {
                allowInteractInputs = it[ParcelOptionsT.interact_inputs]
                allowInteractInventory = it[ParcelOptionsT.interact_inventory]
            }

        }

    }

    // TODO order by some new column
    override suspend fun getOwnedParcels(user: ParcelOwner): List<SerializableParcel> = transaction {
        val where: SqlExpressionBuilder.() -> Op<Boolean>

        if (user.uuid != null) {
            val binaryUuid = user.uuid.toByteArray()
            where = { ParcelsT.owner_uuid eq binaryUuid }
        } else {
            val name = user.name
            where = { ParcelsT.owner_name eq name }
        }

        ParcelsT.select(where)
            .map { parcelRow ->
                val worldId = parcelRow[ParcelsT.world_id]
                val worldRow = WorldsT.select({ WorldsT.id eq worldId }).firstOrNull()
                    ?: return@map null

                val world = SerializableWorld(worldRow[WorldsT.name], worldRow[WorldsT.uid].toUUID())
                SerializableParcel(world, Vec2i(parcelRow[ParcelsT.px], parcelRow[ParcelsT.pz]))
            }
            .filterNotNull()
            .toList()
    }


    override suspend fun setParcelData(parcelFor: Parcel, data: ParcelData?) {
        if (data == null) {
            transaction {
                getParcelId(parcelFor)?.let { id ->
                    ParcelsT.deleteIgnoreWhere(limit = 1) { ParcelsT.id eq id }

                    // Below should cascade automatically
                    /*
                    AddedLocalT.deleteIgnoreWhere { AddedLocalT.parcel_id eq id }
                    ParcelOptionsT.deleteIgnoreWhere(limit = 1) { ParcelOptionsT.parcel_id eq id }
                    */
                }

            }
            return
        }

        val id = transaction {
            val id = getOrInitParcelId(parcelFor)
            AddedLocalT.deleteIgnoreWhere { AddedLocalT.parcel_id eq id }
            id
        }

        setParcelOwner(parcelFor, data.owner)

        for ((uuid, status) in data.added) {
            val state = status.asBoolean
            setParcelPlayerState(parcelFor, uuid, state)
        }

        setParcelAllowsInteractInputs(parcelFor, data.allowInteractInputs)
        setParcelAllowsInteractInventory(parcelFor, data.allowInteractInventory)
    }

    override suspend fun setParcelOwner(parcelFor: Parcel, owner: ParcelOwner?) = transaction {
        val binaryUuid = owner?.uuid?.toByteArray()
        val name = owner?.name

        val id = if (owner == null)
            getParcelId(parcelFor) ?: return@transaction
        else
            getOrInitParcelId(parcelFor)

        ParcelsT.update({ ParcelsT.id eq id }, limit = 1) {
            it[ParcelsT.owner_uuid] = binaryUuid
            it[ParcelsT.owner_name] = name
        }
    }

    override suspend fun setParcelPlayerState(parcelFor: Parcel, player: UUID, state: Boolean?) = transaction {
        val binaryUuid = player.toByteArray()!!

        if (state == null) {
            getParcelId(parcelFor)?.let { id ->
                AddedLocalT.deleteWhere { (AddedLocalT.parcel_id eq id) and (AddedLocalT.player_uuid eq binaryUuid) }
            }
            return@transaction
        }

        val id = getOrInitParcelId(parcelFor)
        AddedLocalT.insertOrUpdate(AddedLocalT.allowed_flag) {
            it[AddedLocalT.parcel_id] = id
            it[AddedLocalT.player_uuid] = binaryUuid
        }
    }

    override suspend fun setParcelAllowsInteractInventory(parcel: Parcel, value: Boolean): Unit = transaction {
        val id = getOrInitParcelId(parcel)
        ParcelOptionsT.insertOrUpdate(ParcelOptionsT.interact_inventory) {
            it[ParcelOptionsT.parcel_id] = id
            it[ParcelOptionsT.interact_inventory] = value
        }
    }

    override suspend fun setParcelAllowsInteractInputs(parcel: Parcel, value: Boolean): Unit = transaction {
        val id = getOrInitParcelId(parcel)
        ParcelOptionsT.insertOrUpdate(ParcelOptionsT.interact_inputs) {
            it[ParcelOptionsT.parcel_id] = id
            it[ParcelOptionsT.interact_inputs] = value
        }
    }

}







