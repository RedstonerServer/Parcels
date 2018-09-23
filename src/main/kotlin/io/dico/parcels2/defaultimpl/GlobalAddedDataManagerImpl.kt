@file:Suppress("UNCHECKED_CAST")

package io.dico.parcels2.defaultimpl

import io.dico.parcels2.*
import io.dico.parcels2.util.ext.alsoIfTrue
import java.util.Collections

class GlobalAddedDataManagerImpl(val plugin: ParcelsPlugin) : GlobalAddedDataManager {
    private val map = mutableMapOf<PlayerProfile, GlobalAddedData>()

    override fun get(owner: PlayerProfile): GlobalAddedData {
        return map[owner] ?: GlobalAddedDataImpl(owner).also { map[owner] = it }
    }

    private inner class GlobalAddedDataImpl(override val owner: PlayerProfile,
                                            data: MutableAddedDataMap = emptyData)
        : AddedDataHolder(data), GlobalAddedData {

        private inline var data get() = addedMap; set(value) = run { addedMap = value }
        private inline val isEmpty get() = data === emptyData

        override fun setStatus(key: StatusKey, status: AddedStatus): Boolean {
            if (isEmpty) {
                if (status == AddedStatus.DEFAULT) return false
                data = mutableMapOf()
            }
            return super.setStatus(key, status).alsoIfTrue {
                plugin.storage.setGlobalAddedStatus(owner, key, status)
            }
        }
    }

    private companion object {
        val emptyData = Collections.emptyMap<Any, Any>() as MutableAddedDataMap
    }

}