package io.dico.parcels2

import io.dico.parcels2.util.uuid
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import org.bukkit.OfflinePlayer
import java.util.*

interface GlobalAddedData : AddedData {
    val uuid: UUID
}

class GlobalAddedDataManager(val plugin: ParcelsPlugin) {
    private val map = mutableMapOf<UUID, GlobalAddedData?>()

    operator fun get(player: OfflinePlayer) = get(player.uuid)

    operator fun get(uuid: UUID): GlobalAddedData? {

    }

    fun getDeferred(uuid: UUID): Deferred<AddedData> {
        get(uuid)?.let { return CompletableDeferred(it) }

    }

    private suspend fun getAsync(uuid: UUID): GlobalAddedData {
        val data = plugin.storage.readGlobalAddedData(ParcelOwner(uuid = uuid)).await()
            ?: return GlobalAddedDataImpl(uuid)
        val result = GlobalAddedDataImpl(uuid, data)
        map[uuid] = result
        return result
    }

    private inner class GlobalAddedDataImpl(override val uuid: UUID,
                                            data: MutableMap<UUID, AddedStatus> = emptyData)
        : AddedDataHolder(data), GlobalAddedData {

        private inline var data get() = added; set(value) = run { added = value }
        private inline val isEmpty get() = data === emptyData

        override fun setAddedStatus(uuid: UUID, status: AddedStatus): Boolean {
            if (isEmpty) {
                if (status == AddedStatus.DEFAULT) return false
                data = mutableMapOf()
            }
            return super.setAddedStatus(uuid, status).also {
                if (it) plugin.storage.setGlobalAddedStatus(ParcelOwner(uuid = this.uuid), uuid, status)
            }
        }

    }

    private companion object {
        val emptyData = mapOf<UUID, AddedStatus>() as MutableMap<UUID, AddedStatus>
    }

}





