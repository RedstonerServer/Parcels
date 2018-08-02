@file:Suppress("UNCHECKED_CAST")

package io.dico.parcels2.defaultimpl

import io.dico.parcels2.*
import java.util.Collections
import java.util.UUID

class GlobalAddedDataManagerImpl(val plugin: ParcelsPlugin) : GlobalAddedDataManager {
    private val map = mutableMapOf<ParcelOwner, GlobalAddedData>()

    override fun get(owner: ParcelOwner): GlobalAddedData {
        return map[owner] ?: GlobalAddedDataImpl(owner).also { map[owner] = it }
    }

    private inner class GlobalAddedDataImpl(override val owner: ParcelOwner,
                                            data: MutableAddedDataMap = emptyData)
        : AddedDataHolder(data), GlobalAddedData {

        private inline var data get() = addedMap; set(value) = run { addedMap = value }
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
        val emptyData = Collections.emptyMap<Any, Any>() as MutableAddedDataMap
    }

}