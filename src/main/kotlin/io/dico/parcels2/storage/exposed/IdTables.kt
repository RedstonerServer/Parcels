@file:Suppress("NOTHING_TO_INLINE", "PARAMETER_NAME_CHANGED_ON_OVERRIDE", "unused", "MemberVisibilityCanBePrivate")

package io.dico.parcels2.storage.exposed

import io.dico.parcels2.ParcelId
import io.dico.parcels2.ParcelOwner
import io.dico.parcels2.ParcelWorldId
import io.dico.parcels2.util.toByteArray
import io.dico.parcels2.util.toUUID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.util.UUID

sealed class IdTransactionsTable<TableT : IdTransactionsTable<TableT, QueryObj>, QueryObj>(tableName: String, columnName: String)
    : Table(tableName) {
    val id = integer(columnName).autoIncrement().primaryKey()

    @Suppress("UNCHECKED_CAST")
    inline val table: TableT
        get() = this as TableT

    internal inline fun getId(where: SqlExpressionBuilder.(TableT) -> Op<Boolean>): Int? {
        return select { where(table) }.firstOrNull()?.let { it[id] }
    }

    internal inline fun insertAndGetId(objName: String, noinline body: TableT.(InsertStatement<Number>) -> Unit): Int {
        return table.insert(body)[id] ?: insertError(objName)
    }

    private inline fun insertError(obj: String): Nothing = throw ExposedDatabaseException("This should not happen - failed to insert $obj and getParcelDeferred its id")

    abstract fun getId(obj: QueryObj): Int?
    abstract fun getOrInitId(obj: QueryObj): Int
    fun getId(id: Int): QueryObj? = select { this@IdTransactionsTable.id eq id }.firstOrNull()?.let { getId(it) }
    abstract fun getId(row: ResultRow): QueryObj?
}

object WorldsT : IdTransactionsTable<WorldsT, ParcelWorldId>("parcel_worlds", "world_id") {
    val name = varchar("name", 50)
    val uid = binary("uid", 16).nullable()
    val index_name = uniqueIndexR("index_name", name)
    val index_uid = uniqueIndexR("index_uid", uid)

    internal inline fun getId(worldName: String, binaryUid: ByteArray?): Int? = getId { (name eq worldName).let { if (binaryUid == null) it else it or (uid eq binaryUid) } }
    internal inline fun getId(worldName: String, uid: UUID?): Int? = getId(worldName, uid?.toByteArray())
    internal inline fun getOrInitId(worldName: String, worldUid: UUID?): Int = worldUid?.toByteArray().let { binaryUid ->
        getId(worldName, binaryUid)
            ?: insertAndGetId("world named $worldName") { it[name] = worldName; binaryUid?.let { buid -> it[uid] = buid } }
    }

    override fun getId(world: ParcelWorldId): Int? = getId(world.name, world.uid)
    override fun getOrInitId(world: ParcelWorldId): Int = getOrInitId(world.name, world.uid)

    override fun getId(row: ResultRow): ParcelWorldId {
        return ParcelWorldId(row[name], row[uid]?.toUUID())
    }
}

object ParcelsT : IdTransactionsTable<ParcelsT, ParcelId>("parcels", "parcel_id") {
    val world_id = integer("world_id").references(WorldsT.id)
    val px = integer("px")
    val pz = integer("pz")
    val owner_id = integer("owner_id").references(OwnersT.id).nullable()
    val claim_time = datetime("claim_time").nullable()
    val index_location = uniqueIndexR("index_location", world_id, px, pz)

    private inline fun getId(worldId: Int, parcelX: Int, parcelZ: Int): Int? = getId { world_id.eq(worldId) and px.eq(parcelX) and pz.eq(parcelZ) }
    private inline fun getId(worldName: String, worldUid: UUID?, parcelX: Int, parcelZ: Int): Int? = WorldsT.getId(worldName, worldUid)?.let { getId(it, parcelX, parcelZ) }
    private inline fun getOrInitId(worldName: String, worldUid: UUID?, parcelX: Int, parcelZ: Int): Int {
        val worldId = WorldsT.getOrInitId(worldName, worldUid)
        return getId(worldId, parcelX, parcelZ)
            ?: insertAndGetId("parcel at $worldName($parcelX, $parcelZ)") { it[world_id] = worldId; it[px] = parcelX; it[pz] = parcelZ }
    }

    override fun getId(parcel: ParcelId): Int? = getId(parcel.worldId.name, parcel.worldId.uid, parcel.x, parcel.z)
    override fun getOrInitId(parcel: ParcelId): Int = getOrInitId(parcel.worldId.name, parcel.worldId.uid, parcel.x, parcel.z)

    private inline fun getRow(id: Int): ResultRow? = select { ParcelsT.id eq id }.firstOrNull()
    fun getRow(parcel: ParcelId): ResultRow? = getId(parcel)?.let { getRow(it) }

    override fun getId(row: ResultRow): ParcelId? {
        val worldId = row[world_id]
        val world = WorldsT.getId(worldId) ?: return null
        return ParcelId(world, row[px], row[pz])
    }
}

object OwnersT : IdTransactionsTable<OwnersT, ParcelOwner>("parcel_owners", "owner_id") {
    val uuid = binary("uuid", 16).nullable()
    val name = varchar("name", 32)
    val index_pair = uniqueIndexR("index_pair", uuid, name)

    private inline fun getId(binaryUuid: ByteArray) = getId { uuid eq binaryUuid }
    private inline fun getId(uuid: UUID) = getId(uuid.toByteArray())
    private inline fun getId(nameIn: String) = getId { uuid.isNull() and (name eq nameIn) }

    private inline fun getOrInitId(uuid: UUID, name: String) = uuid.toByteArray().let { binaryUuid ->
        getId(binaryUuid) ?: insertAndGetId("owner(uuid = $uuid)") {
            it[this@OwnersT.uuid] = binaryUuid
            it[this@OwnersT.name] = name
        }
    }

    private inline fun getOrInitId(name: String) =
        getId(name) ?: insertAndGetId("owner(name = $name)") { it[OwnersT.name] = name }

    override fun getId(owner: ParcelOwner): Int? =
        if (owner.hasUUID) getId(owner.uuid!!)
        else getId(owner.name!!)

    override fun getOrInitId(owner: ParcelOwner): Int =
        if (owner.hasUUID) getOrInitId(owner.uuid!!, owner.notNullName)
        else getOrInitId(owner.name!!)

    override fun getId(row: ResultRow): ParcelOwner {
        return row[uuid]?.toUUID()?.let { ParcelOwner(it) } ?: ParcelOwner(row[name])
    }
}
