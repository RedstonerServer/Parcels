package io.dico.parcels2.storage.backing

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

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

class AbstractParcelsDatabase {





}







