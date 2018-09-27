@file:Suppress("UNCHECKED_CAST")

package io.dico.parcels2.defaultimpl

import io.dico.parcels2.*
import io.dico.parcels2.util.ext.alsoIfTrue
import java.util.Collections

class GlobalPrivilegesManagerImpl(val plugin: ParcelsPlugin) : GlobalPrivilegesManager {
    private val map = mutableMapOf<PlayerProfile, GlobalPrivileges>()

    override fun get(owner: PlayerProfile.Real): GlobalPrivileges {
        return map[owner] ?: GlobalPrivilegesImpl(owner).also { map[owner] = it }
    }

    private inner class GlobalPrivilegesImpl(override val keyOfOwner: PlayerProfile.Real) : PrivilegesHolder(), GlobalPrivileges {
        override var privilegeOfStar: Privilege
            get() = super<GlobalPrivileges>.privilegeOfStar
            set(value) = run { super<GlobalPrivileges>.privilegeOfStar = value }

        override fun setRawStoredPrivilege(key: PrivilegeKey, privilege: Privilege): Boolean {
            return super.setRawStoredPrivilege(key, privilege).alsoIfTrue {
                plugin.storage.setGlobalPrivilege(keyOfOwner, key, privilege)
            }
        }
    }

}