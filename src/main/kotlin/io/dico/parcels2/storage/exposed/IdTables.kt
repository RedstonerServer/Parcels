@file:Suppress("NOTHING_TO_INLINE", "PARAMETER_NAME_CHANGED_ON_OVERRIDE", "unused", "MemberVisibilityCanBePrivate")

package io.dico.parcels2.storage.exposed

import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelOwner
import io.dico.parcels2.ParcelWorld
import io.dico.parcels2.storage.SerializableParcel
import io.dico.parcels2.storage.SerializableWorld
import io.dico.parcels2.util.Vec2i
import io.dico.parcels2.util.toByteArray
import io.dico.parcels2.util.toUUID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.util.*

sealed class IdTransactionsTable<TableT : IdTransactionsTable<TableT, QueryObj, SerializableObj>,
    QueryObj, SerializableObj>(tableName: String, columnName: String)
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
    fun getSerializable(id: Int): SerializableObj? = select { this@IdTransactionsTable.id eq id }.firstOrNull()?.let { getSerializable(it) }
    abstract fun getSerializable(row: ResultRow): SerializableObj?
}

object WorldsT : IdTransactionsTable<WorldsT, ParcelWorld, SerializableWorld>("parcel_worlds", "world_id") {
    val name = varchar("name", 50)
    val uid = binary("uid", 16)
    val index_uid = uniqueIndexR("index_uid", uid)

    internal inline fun getId(binaryUid: ByteArray): Int? = getId { uid eq binaryUid }
    internal inline fun getId(uid: UUID): Int? = getId(uid.toByteArray())
    internal inline fun getOrInitId(worldUid: UUID, worldName: String): Int = worldUid.toByteArray().let { binaryUid ->
        getId(binaryUid)
            ?: insertAndGetId("world named $worldName") { it[uid] = binaryUid; it[name] = worldName }
    }

    override fun getId(world: ParcelWorld): Int? = getId(world.world.uid)
    override fun getOrInitId(world: ParcelWorld): Int = world.world.let { getOrInitId(it.uid, it.name) }

    override fun getSerializable(row: ResultRow): SerializableWorld {
        return SerializableWorld(row[name], row[uid].toUUID())
    }
}

object ParcelsT : IdTransactionsTable<ParcelsT, Parcel, SerializableParcel>("parcels", "parcel_id") {
    val world_id = integer("world_id").references(WorldsT.id)
    val px = integer("px")
    val pz = integer("pz")
    val owner_id = integer("owner_id").references(OwnersT.id).nullable()
    val claim_time = datetime("claim_time").nullable()
    val index_location = uniqueIndexR("index_location", world_id, px, pz)

    private inline fun getId(worldId: Int, parcelX: Int, parcelZ: Int): Int? = getId { world_id.eq(worldId) and px.eq(parcelX) and pz.eq(parcelZ) }
    private inline fun getId(worldUid: UUID, parcelX: Int, parcelZ: Int): Int? = WorldsT.getId(worldUid)?.let { getId(it, parcelX, parcelZ) }
    private inline fun getOrInitId(worldUid: UUID, worldName: String, parcelX: Int, parcelZ: Int): Int {
        val worldId = WorldsT.getOrInitId(worldUid, worldName)
        return getId(worldId, parcelX, parcelZ)
            ?: insertAndGetId("parcel at $worldName($parcelX, $parcelZ)") { it[world_id] = worldId; it[px] = parcelX; it[pz] = parcelZ }
    }

    override fun getId(parcel: Parcel): Int? = getId(parcel.world.world.uid, parcel.pos.x, parcel.pos.z)
    override fun getOrInitId(parcel: Parcel): Int = parcel.world.world.let { getOrInitId(it.uid, it.name, parcel.pos.x, parcel.pos.z) }

    private inline fun getRow(id: Int): ResultRow? = select { ParcelsT.id eq id }.firstOrNull()
    fun getRow(parcel: Parcel): ResultRow? = getId(parcel)?.let { getRow(it) }

    override fun getSerializable(row: ResultRow): SerializableParcel? {
        val worldId = row[world_id]
        val world = WorldsT.getSerializable(worldId) ?: return null
        return SerializableParcel(world, Vec2i(row[px], row[pz]))
    }
}

object OwnersT : IdTransactionsTable<OwnersT, ParcelOwner, ParcelOwner>("parcel_owners", "owner_id") {
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

    override fun getSerializable(row: ResultRow): ParcelOwner {
        return row[uuid]?.toUUID()?.let { ParcelOwner(it) } ?: ParcelOwner(row[name])
    }
}
