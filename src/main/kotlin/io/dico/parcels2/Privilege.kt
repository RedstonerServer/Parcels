package io.dico.parcels2

import io.dico.parcels2.Privilege.*
import io.dico.parcels2.PrivilegeChangeResult.*
import io.dico.parcels2.util.ext.PERM_ADMIN_MANAGE
import io.dico.parcels2.util.ext.PERM_BAN_BYPASS
import io.dico.parcels2.util.ext.PERM_BUILD_ANYWHERE
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

enum class Privilege(
    val number: Int,
    val transient: Boolean = false
) {
    BANNED(1),
    DEFAULT(2),
    CAN_BUILD(3),
    CAN_MANAGE(4),

    OWNER(-1, transient = true),
    ADMIN(-1, transient = true);

    fun isDistanceGrEq(other: Privilege): Boolean =
        when { // used for example when disallowBuild is called and CAN_MANAGE is the privilege.
            other > DEFAULT -> this >= other
            other == DEFAULT -> this == other
            else -> this <= other
        }

    fun isChangeInDirection(positiveDirection: Boolean, update: Privilege): Boolean =
        if (positiveDirection) update > this
        else update < this

    fun requireNonTransient(): Privilege {
        if (transient) {
            throw IllegalArgumentException("Transient privilege $this is invalid")
        }
        return this
    }

    /*
    fun canEnter() = this >= BANNED
    fun canBuild() = this >= CAN_BUILD
    fun canManage() = this >= CAN_MANAGE
    */

    companion object {
        fun getByNumber(id: Int) =
            when (id) {
                1 -> BANNED
                2 -> DEFAULT
                3 -> CAN_BUILD
                4 -> CAN_MANAGE
                else -> null
            }
    }
}

typealias PrivilegeKey = PlayerProfile.Real
typealias MutablePrivilegeMap = MutableMap<PrivilegeKey, Privilege>
typealias PrivilegeMap = Map<PrivilegeKey, Privilege>

@Suppress("FunctionName")
fun MutablePrivilegeMap(): MutablePrivilegeMap = hashMapOf()

interface PrivilegesMinimal {
    val privilegeMap: PrivilegeMap

    fun getStoredPrivilege(key: PrivilegeKey): Privilege
    fun setStoredPrivilege(key: PrivilegeKey, privilege: Privilege): Boolean
}

interface Privileges : PrivilegesMinimal {
    val keyOfOwner: PlayerProfile.Real?

    fun privilege(player: OfflinePlayer): Privilege {
        val key = player.privilegeKey
        return if (key == keyOfOwner) OWNER
        else getStoredPrivilege(key)
    }

    fun privilege(player: OfflinePlayer, adminPerm: String): Privilege =
        if (player is Player && player.hasPermission(adminPerm)) ADMIN
        else {
            val key = player.privilegeKey
            if (key == keyOfOwner) OWNER
            else getStoredPrivilege(key)
        }

    fun changePrivilege(key: PrivilegeKey, positive: Boolean, update: Privilege): PrivilegeChangeResult =
        if (key == keyOfOwner) FAIL_OWNER
        else if (getStoredPrivilege(key).isChangeInDirection(positive, update)
            && setStoredPrivilege(key, update)
        ) SUCCESS
        else FAIL

    fun canManage(player: OfflinePlayer) = privilege(player, PERM_ADMIN_MANAGE) >= CAN_MANAGE
    fun allowManage(player: OfflinePlayer) = changePrivilege(player.privilegeKey, true, CAN_MANAGE)
    fun disallowManage(player: OfflinePlayer) = changePrivilege(player.privilegeKey, false, CAN_BUILD)

    fun canBuild(player: OfflinePlayer) = privilege(player, PERM_BUILD_ANYWHERE) >= CAN_BUILD
    fun allowBuild(player: OfflinePlayer) = changePrivilege(player.privilegeKey, true, CAN_BUILD)
    fun disallowBuild(player: OfflinePlayer) = changePrivilege(player.privilegeKey, false, DEFAULT)

    fun canEnter(player: OfflinePlayer) = privilege(player, PERM_BAN_BYPASS) >= DEFAULT
    fun ban(player: OfflinePlayer) = changePrivilege(player.privilegeKey, false, BANNED)
    fun unban(player: OfflinePlayer) = changePrivilege(player.privilegeKey, true, DEFAULT)

    /**
     * same as [canBuild] but doesn't perform a permission check for admin perms
     */
    fun canBuildFast(player: OfflinePlayer) = player.privilegeKey.let { if (it == keyOfOwner) OWNER else getStoredPrivilege(it)} >= CAN_BUILD
}

enum class PrivilegeChangeResult {
    SUCCESS, FAIL, FAIL_OWNER
}

val OfflinePlayer.privilegeKey: PrivilegeKey
    inline get() = PlayerProfile.nameless(this)

open class PrivilegesHolder(override var privilegeMap: MutablePrivilegeMap = MutablePrivilegeMap()) : PrivilegesMinimal {
    private var privilegeOfStar: Privilege = DEFAULT

    override fun getStoredPrivilege(key: PrivilegeKey) =
        if (key.isStar) privilegeOfStar
        else privilegeMap.getOrDefault(key, privilegeOfStar)

    override fun setStoredPrivilege(key: PrivilegeKey, privilege: Privilege): Boolean {
        privilege.requireNonTransient()

        if (key.isStar) {
            if (privilegeOfStar == privilege) return false
            privilegeOfStar = privilege
            return true
        }

        return if (privilege == DEFAULT) privilegeMap.remove(key) != null
        else privilegeMap.put(key, privilege) != privilege
    }
}

interface GlobalPrivileges : Privileges {
    override val keyOfOwner: PlayerProfile.Real
}

interface GlobalPrivilegesManager {
    operator fun get(owner: PlayerProfile.Real): GlobalPrivileges
    operator fun get(owner: OfflinePlayer): GlobalPrivileges = get(owner.privilegeKey)
}
