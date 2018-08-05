@file:Suppress("NOTHING_TO_INLINE", "PARAMETER_NAME_CHANGED_ON_OVERRIDE", "unused", "MemberVisibilityCanBePrivate")

package io.dico.parcels2.storage.exposed

import io.dico.parcels2.ParcelId
import io.dico.parcels2.ParcelWorldId
import io.dico.parcels2.PlayerProfile
import io.dico.parcels2.util.toByteArray
import io.dico.parcels2.util.toUUID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
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

    internal inline fun getOrInitId(getId: () -> Int?, noinline body: TableT.(UpdateBuilder<*>) -> Unit, objName: () -> String): Int {
        return getId() ?: table.insertIgnore(body)[id] ?: getId() ?: throw ExposedDatabaseException("This should not happen - failed to insert ${objName()} and get its id")
    }

    abstract fun getId(obj: QueryObj): Int?
    abstract fun getOrInitId(obj: QueryObj): Int
    fun getItem(id: Int): QueryObj? = select { this@IdTransactionsTable.id eq id }.firstOrNull()?.let { getItem(it) }
    abstract fun getItem(row: ResultRow): QueryObj?

    fun getId(obj: QueryObj, init: Boolean): Int? = if (init) getOrInitId(obj) else getId(obj)
}

object WorldsT : IdTransactionsTable<WorldsT, ParcelWorldId>("parcel_worlds", "world_id") {
    val name = varchar("name", 50)
    val uid = binary("uid", 16).nullable()
    val index_name = uniqueIndexR("index_name", name)
    val index_uid = uniqueIndexR("index_uid", uid)

    internal inline fun getId(worldName: String, binaryUid: ByteArray?): Int? = getId { (name eq worldName).let { if (binaryUid == null) it else it or (uid eq binaryUid) } }
    internal inline fun getId(worldName: String, uid: UUID?): Int? = getId(worldName, uid?.toByteArray())
    internal inline fun getOrInitId(worldName: String, worldUid: UUID?): Int = worldUid?.toByteArray().let { binaryUid ->
        return getOrInitId(
            { getId(worldName, binaryUid) },
            { it[name] = worldName; it[uid] = binaryUid },
            { "world named $worldName" })
    }

    override fun getId(world: ParcelWorldId): Int? = getId(world.name, world.uid)
    override fun getOrInitId(world: ParcelWorldId): Int = getOrInitId(world.name, world.uid)

    override fun getItem(row: ResultRow): ParcelWorldId {
        return ParcelWorldId(row[name], row[uid]?.toUUID())
    }
}

object ParcelsT : IdTransactionsTable<ParcelsT, ParcelId>("parcels", "parcel_id") {
    val world_id = integer("world_id").references(WorldsT.id)
    val px = integer("px")
    val pz = integer("pz")
    val owner_id = integer("owner_id").references(ProfilesT.id).nullable()
    val claim_time = datetime("claim_time").nullable()
    val index_location = uniqueIndexR("index_location", world_id, px, pz)

    private inline fun getId(worldId: Int, parcelX: Int, parcelZ: Int): Int? = getId { world_id.eq(worldId) and px.eq(parcelX) and pz.eq(parcelZ) }
    private inline fun getId(worldName: String, worldUid: UUID?, parcelX: Int, parcelZ: Int): Int? = WorldsT.getId(worldName, worldUid)?.let { getId(it, parcelX, parcelZ) }
    private inline fun getOrInitId(worldName: String, worldUid: UUID?, parcelX: Int, parcelZ: Int): Int {
        val worldId = WorldsT.getOrInitId(worldName, worldUid)
        return getOrInitId(
            { getId(worldId, parcelX, parcelZ) },
            { it[world_id] = worldId; it[px] = parcelX; it[pz] = parcelZ },
            { "parcel at $worldName($parcelX, $parcelZ)" })
    }

    override fun getId(parcel: ParcelId): Int? = getId(parcel.worldId.name, parcel.worldId.uid, parcel.x, parcel.z)
    override fun getOrInitId(parcel: ParcelId): Int = getOrInitId(parcel.worldId.name, parcel.worldId.uid, parcel.x, parcel.z)

    private inline fun getRow(id: Int): ResultRow? = select { ParcelsT.id eq id }.firstOrNull()
    fun getRow(parcel: ParcelId): ResultRow? = getId(parcel)?.let { getRow(it) }

    override fun getItem(row: ResultRow): ParcelId? {
        val worldId = row[world_id]
        val world = WorldsT.getItem(worldId) ?: return null
        return ParcelId(world, row[px], row[pz])
    }
}

object ProfilesT : IdTransactionsTable<ProfilesT, PlayerProfile>("parcel_profiles", "owner_id") {
    val uuid = binary("uuid", 16).nullable()
    val name = varchar("name", 32)
    val index_pair = uniqueIndexR("index_pair", uuid, name)

    private inline fun getId(binaryUuid: ByteArray) = getId { uuid eq binaryUuid }
    private inline fun getId(uuid: UUID) = getId(uuid.toByteArray())
    private inline fun getId(nameIn: String) = getId { uuid.isNull() and (name.lowerCase() eq nameIn.toLowerCase()) }
    private inline fun getRealId(nameIn: String) = getId { uuid.isNotNull() and (name.lowerCase() eq nameIn.toLowerCase()) }

    private inline fun getOrInitId(uuid: UUID, name: String) = uuid.toByteArray().let { binaryUuid -> getOrInitId(
            { getId(binaryUuid) },
            { it[this@ProfilesT.uuid] = binaryUuid; it[this@ProfilesT.name] = name },
            { "profile(uuid = $uuid, name = $name)" })
    }

    private inline fun getOrInitId(name: String) = getOrInitId(
        { getId(name) },
        { it[ProfilesT.name] = name },
        { "owner(name = $name)" })


    override fun getId(profile: PlayerProfile): Int? = when (profile) {
        is PlayerProfile.Real -> getId(profile.uuid)
        is PlayerProfile.Fake -> getId(profile.name)
        is PlayerProfile.Unresolved -> getRealId(profile.name)
        else -> throw IllegalArgumentException()
    }

    override fun getOrInitId(profile: PlayerProfile): Int = when (profile) {
        is PlayerProfile.Real -> getOrInitId(profile.uuid, profile.notNullName)
        is PlayerProfile.Fake -> getOrInitId(profile.name)
        else -> throw IllegalArgumentException()
    }

    override fun getItem(row: ResultRow): PlayerProfile {
        return PlayerProfile(row[uuid]?.toUUID(), row[name])
    }

    fun getRealItem(id: Int): PlayerProfile.Real? {
        return getItem(id) as? PlayerProfile.Real
    }

}

// val ParcelsWithOptionsT = ParcelsT.join(ParcelOptionsT, JoinType.INNER, onColumn = ParcelsT.id, otherColumn = ParcelOptionsT.parcel_id)