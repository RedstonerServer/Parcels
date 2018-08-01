@file:Suppress("UNCHECKED_CAST")

package io.dico.parcels2

import java.util.*

interface GlobalAddedData : AddedData {
    val owner: ParcelOwner
}

interface GlobalAddedDataManager {
    operator fun get(owner: ParcelOwner): GlobalAddedData
}

class GlobalAddedDataManagerImpl(val plugin: ParcelsPlugin) : GlobalAddedDataManager {
    private val map = mutableMapOf<ParcelOwner, GlobalAddedData>()

    override fun get(owner: ParcelOwner): GlobalAddedData {
        return map[owner] ?: GlobalAddedDataImpl(owner).also { map[owner] = it }
    }

    private inner class GlobalAddedDataImpl(override val owner: ParcelOwner,
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
                if (it) plugin.storage.setGlobalAddedStatus(owner, uuid, status)
            }
        }

    }

    private companion object {
        val emptyData = mapOf<UUID, AddedStatus>() as MutableMap<UUID, AddedStatus>
    }

}



