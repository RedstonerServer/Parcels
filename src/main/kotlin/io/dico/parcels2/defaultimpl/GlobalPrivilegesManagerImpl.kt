@file:Suppress("UNCHECKED_CAST")

package io.dico.parcels2.defaultimpl

import io.dico.parcels2.*
import io.dico.parcels2.util.ext.alsoIfTrue
import java.util.Collections

class GlobalPrivilegesManagerImpl(val plugin: ParcelsPlugin) : GlobalPrivilegesManager {
    private val map = mutableMapOf<PlayerProfile, GlobalPrivileges>()

    override fun get(owner: PlayerProfile): GlobalPrivileges {
        return map[owner] ?: GlobalPrivilegesImpl(owner).also { map[owner] = it }
    }

    private inner class GlobalPrivilegesImpl(override val owner: PlayerProfile,
                                             data: MutablePrivilegeMap = emptyData)
        : PrivilegesHolder(data), GlobalPrivileges {

        private inline var data get() = map; set(value) = run { map = value }
        private inline val isEmpty get() = data === emptyData

        override fun setPrivilege(key: PrivilegeKey, privilege: Privilege): Boolean {
            if (isEmpty) {
                if (privilege == Privilege.DEFAULT) return false
                data = mutableMapOf()
            }
            return super.set(key, privilege).alsoIfTrue {
                plugin.storage.setGlobalPrivilege(owner, key, privilege)
            }
        }
    }

    private companion object {
        val emptyData = Collections.emptyMap<Any, Any>() as MutablePrivilegeMap
    }

}