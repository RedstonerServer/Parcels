package io.dico.parcels2.storage

import com.zaxxer.hikari.HikariDataSource
import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelData
import io.dico.parcels2.ParcelOwner
import kotlinx.coroutines.experimental.channels.ProducerScope
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import java.util.*
import javax.sql.DataSource

object ParcelsTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val px = integer("px")
    val pz = integer("pz")
    val world_uuid = binary("world_uuid", 16).also { uniqueIndex("location", it, px, pz) }
    val world = varchar("world", 32).nullable()
    val owner_uuid = binary("owner_uuid", 16).nullable()
    val owner = varchar("owner", 16).nullable()
}

object ParcelsAddedTable : Table() {
    val id = integer("id").references(ParcelsTable.id, ReferenceOption.CASCADE)
    val player_uuid = binary("player_uuid", 16).also { uniqueIndex("pair", id, it) }
    val allowed_flag = bool("allowed_flag")
}

object PlayerAddedTable : Table() {
    val owner_uuid = binary("owner_uuid", 16)
    val player_uuid = binary("player_uuid", 16).also { uniqueIndex("pair", owner_uuid, it) }
    val allowed_flag = bool("allowed_flag")
}

class ExposedBacking(val dataSource: DataSource) : Backing {
    override val name get() = "Exposed"
    lateinit var database: Database

    override suspend fun init() {
        database = Database.connect(dataSource)
    }

    override suspend fun shutdown() {
        if (dataSource is HikariDataSource) {
            dataSource.close()
        }
    }

    override suspend fun ProducerScope<Pair<Parcel, ParcelData?>>.produceParcelData(parcels: Sequence<Parcel>) {
        TODO()
    }

    override suspend fun readParcelData(plotFor: Parcel): ParcelData? {
        TODO()
    }

    override suspend fun getOwnedParcels(user: ParcelOwner): List<SerializableParcel> {
        TODO()
    }

    override suspend fun setParcelOwner(plotFor: Parcel, owner: ParcelOwner?) {
        TODO()
    }

    override suspend fun setParcelPlayerState(plotFor: Parcel, player: UUID, state: Boolean?) {
        TODO()
    }

    override suspend fun setParcelAllowsInteractInventory(plot: Parcel, value: Boolean) {
        TODO()
    }

    override suspend fun setParcelAllowsInteractInputs(plot: Parcel, value: Boolean) {
        TODO()
    }

}







