@file:Suppress("PropertyName", "LocalVariableName", "NOTHING_TO_INLINE")

package io.dico.parcels2.storage.exposed

import io.dico.parcels2.*
import kotlinx.coroutines.channels.SendChannel
import org.jetbrains.exposed.sql.*
import java.util.UUID

object AddedLocalT : AddedTable<ParcelId>("parcels_added_local", ParcelsT)
object AddedGlobalT : AddedTable<PlayerProfile>("parcels_added_global", ProfilesT)

object ParcelOptionsT : Table("parcel_options") {
    val parcel_id = integer("parcel_id").primaryKey().references(ParcelsT.id, ReferenceOption.CASCADE)
    val interact_inventory = bool("interact_inventory").default(true)
    val interact_inputs = bool("interact_inputs").default(true)
}

typealias AddedStatusSendChannel<AttachT> = SendChannel<Pair<AttachT, MutableAddedDataMap>>

sealed class AddedTable<AttachT>(name: String, val idTable: IdTransactionsTable<*, AttachT>) : Table(name) {
    val attach_id = integer("attach_id").references(idTable.id, ReferenceOption.CASCADE)
    val profile_id = integer("profile_id").references(ProfilesT.id, ReferenceOption.CASCADE)
    val allowed_flag = bool("allowed_flag")
    val index_pair = uniqueIndexR("index_pair", attach_id, profile_id)

    fun setPlayerStatus(attachedOn: AttachT, player: PlayerProfile.Real, status: AddedStatus) {
        if (status.isDefault) {
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
            it[allowed_flag] = status.isAllowed
        }
    }

    fun readAddedData(id: Int): MutableAddedDataMap {
        val list = slice(profile_id, allowed_flag).select { attach_id eq id }
        val result = MutableAddedDataMap()
        for (row in list) {
            val profile = ProfilesT.getRealItem(row[profile_id]) ?: continue
            result[profile] = row[allowed_flag].asAddedStatus()
        }
        return result
    }

    fun sendAllAddedData(channel: AddedStatusSendChannel<AttachT>) {
        val iterator = selectAll().orderBy(attach_id).iterator()

        if (iterator.hasNext()) {
            val firstRow = iterator.next()
            var id: Int = firstRow[attach_id]
            var attach: AttachT? = null
            var map: MutableAddedDataMap? = null

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
                val status = row[allowed_flag].asAddedStatus()
                map!![profile] = status
            }

            sendIfPresent()
        }
    }

    private inline fun Boolean?.asAddedStatus() = if (this == null) AddedStatus.DEFAULT else if (this) AddedStatus.ALLOWED else AddedStatus.BANNED

}
