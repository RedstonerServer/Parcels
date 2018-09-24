package io.dico.parcels2

import io.dico.parcels2.Privilege.*
import org.bukkit.OfflinePlayer

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

/**
 * Privileges object never returns a transient privilege.
 */
interface Privileges {
    val map: PrivilegeMap
    var privilegeOfStar: Privilege

    fun privilege(key: PrivilegeKey): Privilege
    fun privilege(player: OfflinePlayer) = privilege(player.privilegeKey)
    fun setPrivilege(key: PrivilegeKey, privilege: Privilege): Boolean
    fun setPrivilege(player: OfflinePlayer, privilege: Privilege) = setPrivilege(player.privilegeKey, privilege)
    fun changePrivilege(key: PrivilegeKey, expect: Privilege, update: Privilege): Boolean =
        privilege(key).isDistanceGrEq(expect) && setPrivilege(key, update)

    fun hasPrivilegeToManage(key: PrivilegeKey) = privilege(key) >= CAN_MANAGE
    fun allowManage(key: PrivilegeKey) = setPrivilege(key, CAN_MANAGE)
    fun disallowManage(key: PrivilegeKey) = changePrivilege(key, CAN_MANAGE, CAN_BUILD)

    fun hasPrivilegeToBuild(key: PrivilegeKey) = privilege(key) >= CAN_BUILD
    fun allowBuild(key: PrivilegeKey) = setPrivilege(key, CAN_BUILD)
    fun disallowBuild(key: PrivilegeKey) = changePrivilege(key, CAN_BUILD, DEFAULT)

    fun isBanned(key: PrivilegeKey) = privilege(key) == BANNED
    fun ban(key: PrivilegeKey) = setPrivilege(key, BANNED)
    fun unban(key: PrivilegeKey) = changePrivilege(key, BANNED, DEFAULT)

    /* OfflinePlayer overloads */

    fun hasPrivilegeToManage(player: OfflinePlayer) = hasPrivilegeToManage(player.privilegeKey)
    fun allowManage(player: OfflinePlayer) = allowManage(player.privilegeKey)
    fun disallowManage(player: OfflinePlayer) = disallowManage(player.privilegeKey)

    fun hasPrivilegeToBuild(player: OfflinePlayer) = hasPrivilegeToBuild(player.privilegeKey)
    fun allowBuild(player: OfflinePlayer) = allowBuild(player.privilegeKey)
    fun disallowBuild(player: OfflinePlayer) = disallowBuild(player.privilegeKey)

    fun isBanned(player: OfflinePlayer) = isBanned(player.privilegeKey)
    fun ban(player: OfflinePlayer) = ban(player.privilegeKey)
    fun unban(player: OfflinePlayer) = unban(player.privilegeKey)
}

val OfflinePlayer.privilegeKey: PrivilegeKey
    inline get() = PlayerProfile.nameless(this)

open class PrivilegesHolder(override var map: MutablePrivilegeMap = MutablePrivilegeMap()) : Privileges {
    override var privilegeOfStar: Privilege = DEFAULT
        set(value) = run { field = value.requireNonTransient() }

    override fun privilege(key: PrivilegeKey): Privilege = map.getOrDefault(key, privilegeOfStar)

    override fun setPrivilege(key: PrivilegeKey, privilege: Privilege): Boolean {
        privilege.requireNonTransient()

        if (key.isStar) {
            if (privilegeOfStar == privilege) return false
            privilegeOfStar = privilege
            return true
        }

        return if (privilege == DEFAULT) map.remove(key) != null
        else map.put(key, privilege) != privilege
    }
}

interface GlobalPrivileges : Privileges {
    val owner: PlayerProfile
}

interface GlobalPrivilegesManager {
    operator fun get(owner: PlayerProfile): GlobalPrivileges
    operator fun get(owner: OfflinePlayer): GlobalPrivileges = get(owner.privilegeKey)
}
