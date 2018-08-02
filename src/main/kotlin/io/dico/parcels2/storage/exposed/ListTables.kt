@file:Suppress("PropertyName", "LocalVariableName", "NOTHING_TO_INLINE")

package io.dico.parcels2.storage.exposed

import io.dico.parcels2.AddedStatus
import io.dico.parcels2.ParcelId
import io.dico.parcels2.ParcelOwner
import io.dico.parcels2.util.toByteArray
import io.dico.parcels2.util.toUUID
import kotlinx.coroutines.experimental.channels.SendChannel
import org.jetbrains.exposed.sql.*
import java.util.UUID

object AddedLocalT : AddedTable<ParcelId>("parcels_added_local", ParcelsT)
object AddedGlobalT : AddedTable<ParcelOwner>("parcels_added_global", OwnersT)

object ParcelOptionsT : Table("parcel_options") {
    val parcel_id = integer("parcel_id").primaryKey().references(ParcelsT.id, ReferenceOption.CASCADE)
    val interact_inventory = bool("interact_inventory").default(true)
    val interact_inputs = bool("interact_inputs").default(true)
}

typealias AddedStatusSendChannel<AttachT> = SendChannel<Pair<AttachT, MutableMap<UUID, AddedStatus>>>

sealed class AddedTable<AttachT>(name: String, val idTable: IdTransactionsTable<*, AttachT>) : Table(name) {
    val attach_id = integer("attach_id").references(idTable.id, ReferenceOption.CASCADE)
    val player_uuid = binary("player_uuid", 16)
    val allowed_flag = bool("allowed_flag")
    val index_pair = uniqueIndexR("index_pair", attach_id, player_uuid)

    fun setPlayerStatus(attachedOn: AttachT, player: UUID, status: AddedStatus) {
        val binaryUuid = player.toByteArray()

        if (status.isDefault) {
            idTable.getId(attachedOn)?.let { id ->
                deleteWhere { (attach_id eq id) and (player_uuid eq binaryUuid) }
            }
            return
        }

        val id = idTable.getOrInitId(attachedOn)
        upsert(conflictIndex = index_pair) {
            it[attach_id] = id
            it[player_uuid] = binaryUuid
            it[allowed_flag] = status.isAllowed
        }
    }

    fun readAddedData(id: Int): MutableMap<UUID, AddedStatus> {
        return slice(player_uuid, allowed_flag).select { attach_id eq id }
            .associateByTo(hashMapOf(), { it[player_uuid].toUUID() }, { it[allowed_flag].asAddedStatus() })
    }

    suspend fun sendAllAddedData(channel: AddedStatusSendChannel<AttachT>) {
        /*
        val iterator = selectAll().orderBy(attach_id).iterator()

        if (iterator.hasNext()) {
            val firstRow = iterator.next()
            var id: Int = firstRow[attach_id]
            var attach: SerializableT? = null
            var map: MutableMap<UUID, AddedStatus>? = null

            fun initAttachAndMap() {
                attach = idTable.getId(id)
                map = attach?.let { mutableMapOf() }
            }

            suspend fun sendIfPresent() {
                if (attach != null && map != null && map!!.isNotEmpty()) {
                    channel.send(attach!! to map!!)
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

                val player_uuid = row[player_uuid].toUUID()
                val status = row[allowed_flag].asAddedStatus()
                map!![player_uuid] = status
            }

            sendIfPresent()
        }*/
    }

    private inline fun Boolean?.asAddedStatus() = if (this == null) AddedStatus.DEFAULT else if (this) AddedStatus.ALLOWED else AddedStatus.BANNED

}
