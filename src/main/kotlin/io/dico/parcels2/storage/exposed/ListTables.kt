@file:Suppress("PropertyName", "LocalVariableName", "NOTHING_TO_INLINE")

package io.dico.parcels2.storage.exposed

import io.dico.parcels2.*
import io.dico.parcels2.Privilege.DEFAULT
import kotlinx.coroutines.channels.SendChannel
import org.jetbrains.exposed.sql.*

object PrivilegesLocalT : PrivilegesTable<ParcelId>("parcels_added_local", ParcelsT)
object PrivilegesGlobalT : PrivilegesTable<PlayerProfile>("parcels_added_global", ProfilesT)

object ParcelOptionsT : Table("parcel_options") {
    val parcel_id = integer("parcel_id").primaryKey().references(ParcelsT.id, ReferenceOption.CASCADE)
    val interact_bitmask = binary("interact_bitmask", 4)
}

typealias PrivilegesSendChannel<AttachT> = SendChannel<Pair<AttachT, MutablePrivilegeMap>>

sealed class PrivilegesTable<AttachT>(name: String, val idTable: IdTransactionsTable<*, AttachT>) : Table(name) {
    val attach_id = integer("attach_id").references(idTable.id, ReferenceOption.CASCADE)
    val profile_id = integer("profile_id").references(ProfilesT.id, ReferenceOption.CASCADE)
    val privilege = integer("privilege")
    val index_pair = uniqueIndexR("index_pair", attach_id, profile_id)

    fun setPrivilege(attachedOn: AttachT, player: PlayerProfile.Real, privilege: Privilege) {
        privilege.requireNonTransient()

        if (privilege == DEFAULT) {
            val player_id = ProfilesT.getId(player) ?: return
            idTable.getId(attachedOn)?.let { holder ->
                deleteWhere { (attach_id eq holder) and (profile_id eq player_id) }
            }
            return
        }

        val holder = idTable.getOrInitId(attachedOn)
        val player_id = ProfilesT.getOrInitId(player)
        upsert(conflictIndex = index_pair) {
            it[attach_id] = holder
            it[profile_id] = player_id
            it[this.privilege] = privilege.number
        }
    }

    fun readPrivileges(id: Int): MutablePrivilegeMap {
        val list = slice(profile_id, privilege).select { attach_id eq id }
        val result = MutablePrivilegeMap()
        for (row in list) {
            val profile = ProfilesT.getRealItem(row[profile_id]) ?: continue
            result[profile] = Privilege.getByNumber(row[privilege]) ?: continue
        }
        return result
    }

    fun sendAllAddedData(channel: PrivilegesSendChannel<AttachT>) {
        val iterator = selectAll().orderBy(attach_id).iterator()

        if (iterator.hasNext()) {
            val firstRow = iterator.next()
            var id: Int = firstRow[attach_id]
            var attach: AttachT? = null
            var map: MutablePrivilegeMap? = null

            fun initAttachAndMap() {
                attach = idTable.getItem(id)
                map = attach?.let { mutableMapOf() }
            }

            fun sendIfPresent() {
                if (attach != null && map != null && map!!.isNotEmpty()) {
                    channel.offer(attach!! to map!!)
                }
                attach = null
                map = null
            }

            initAttachAndMap()

            for (row in iterator) {
                val rowId = row[attach_id]
                if (rowId != id) {
                    sendIfPresent()
                    id = rowId
                    initAttachAndMap()
                }

                if (attach == null) {
                    continue // owner not found for this owner id
                }

                val profile = ProfilesT.getRealItem(row[profile_id]) ?: continue
                val privilege = Privilege.getByNumber(row[privilege]) ?: continue
                map!![profile] = privilege
            }

            sendIfPresent()
        }
    }

}
