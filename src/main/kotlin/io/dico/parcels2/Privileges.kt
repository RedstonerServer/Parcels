package io.dico.parcels2

import io.dico.parcels2.PrivilegeChangeResult.*
import io.dico.parcels2.util.ext.PERM_ADMIN_MANAGE
import io.dico.parcels2.util.ext.PERM_BAN_BYPASS
import io.dico.parcels2.util.ext.PERM_BUILD_ANYWHERE
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

interface Privileges : RawPrivileges {
    val keyOfOwner: PlayerProfile.Real?

    fun getStoredPrivilege(key: PrivilegeKey): Privilege {
        return if (key == keyOfOwner) Privilege.OWNER
        else getRawStoredPrivilege(key)
    }

    override var privilegeOfStar: Privilege
        get() = getStoredPrivilege(PlayerProfile.Star)
        set(value) {
            setRawStoredPrivilege(PlayerProfile.Star, value)
        }
}

val OfflinePlayer.privilegeKey: PrivilegeKey
    inline get() = PlayerProfile.nameless(this)

fun Privileges.getEffectivePrivilege(player: OfflinePlayer, adminPerm: String): Privilege =
    if (player is Player && player.hasPermission(adminPerm)) Privilege.ADMIN
    else getStoredPrivilege(player.privilegeKey)

fun Privileges.canManage(player: OfflinePlayer) = getEffectivePrivilege(player, PERM_ADMIN_MANAGE) >= Privilege.CAN_MANAGE
fun Privileges.canManageFast(player: OfflinePlayer) = getStoredPrivilege(player.privilegeKey) >= Privilege.CAN_MANAGE
fun Privileges.canBuild(player: OfflinePlayer) = getEffectivePrivilege(player, PERM_BUILD_ANYWHERE) >= Privilege.CAN_BUILD
fun Privileges.canBuildFast(player: OfflinePlayer) = getStoredPrivilege(player.privilegeKey) >= Privilege.CAN_BUILD
fun Privileges.canEnter(player: OfflinePlayer) = getEffectivePrivilege(player, PERM_BAN_BYPASS) >= Privilege.DEFAULT
fun Privileges.canEnterFast(player: OfflinePlayer) = getStoredPrivilege(player.privilegeKey) >= Privilege.DEFAULT

enum class PrivilegeChangeResult {
    SUCCESS, FAIL, FAIL_OWNER
}

fun Privileges.changePrivilege(key: PrivilegeKey, positive: Boolean, update: Privilege): PrivilegeChangeResult =
    when {
        key == keyOfOwner -> FAIL_OWNER
        getRawStoredPrivilege(key).isChangeInDirection(positive, update) && setRawStoredPrivilege(key, update) -> SUCCESS
        else -> FAIL
    }

fun Privileges.allowManage(key: PrivilegeKey) = changePrivilege(key, true, Privilege.CAN_MANAGE)
fun Privileges.disallowManage(key: PrivilegeKey) = changePrivilege(key, false, Privilege.CAN_BUILD)
fun Privileges.allowBuild(key: PrivilegeKey) = changePrivilege(key, true, Privilege.CAN_BUILD)
fun Privileges.disallowBuild(key: PrivilegeKey) = changePrivilege(key, false, Privilege.DEFAULT)
fun Privileges.allowEnter(key: PrivilegeKey) = changePrivilege(key, true, Privilege.DEFAULT)
fun Privileges.disallowEnter(key: PrivilegeKey) = changePrivilege(key, false, Privilege.BANNED)

interface GlobalPrivileges : RawPrivileges, Privileges {
    override val keyOfOwner: PlayerProfile.Real
}

interface GlobalPrivilegesManager {
    operator fun get(owner: PlayerProfile.Real): GlobalPrivileges
    operator fun get(owner: OfflinePlayer): GlobalPrivileges = get(owner.privilegeKey)
}
