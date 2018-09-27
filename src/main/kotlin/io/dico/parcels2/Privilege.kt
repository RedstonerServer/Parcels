package io.dico.parcels2

import io.dico.parcels2.Privilege.DEFAULT
import java.util.Collections

typealias PrivilegeKey = PlayerProfile.Real
typealias PrivilegeMap = Map<PrivilegeKey, Privilege>
typealias MutablePrivilegeMap = MutableMap<PrivilegeKey, Privilege>

@Suppress("FunctionName")
fun MutablePrivilegeMap(): MutablePrivilegeMap = hashMapOf()

@Suppress("UNCHECKED_CAST")
val EmptyPrivilegeMap = Collections.emptyMap<Any, Any>() as MutablePrivilegeMap

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
        when {
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

interface RawPrivileges {
    val privilegeMap: PrivilegeMap
    var privilegeOfStar: Privilege

    fun getRawStoredPrivilege(key: PrivilegeKey): Privilege
    fun setRawStoredPrivilege(key: PrivilegeKey, privilege: Privilege): Boolean
}

open class PrivilegesHolder(override var privilegeMap: MutablePrivilegeMap = EmptyPrivilegeMap) : RawPrivileges {
    private var _privilegeOfStar: Privilege = DEFAULT

    override /*open*/ var privilegeOfStar: Privilege
        get() = _privilegeOfStar
        set(value) = run { _privilegeOfStar = value }

    protected val isEmpty
        inline get() = privilegeMap === EmptyPrivilegeMap

    override fun getRawStoredPrivilege(key: PrivilegeKey) =
        if (key.isStar) _privilegeOfStar
        else privilegeMap.getOrDefault(key, _privilegeOfStar)

    override fun setRawStoredPrivilege(key: PrivilegeKey, privilege: Privilege): Boolean {
        privilege.requireNonTransient()

        if (key.isStar) {
            if (_privilegeOfStar == privilege) return false
            _privilegeOfStar = privilege
            return true
        }

        if (isEmpty) {
            if (privilege == DEFAULT) return false
            privilegeMap = MutablePrivilegeMap()
        }

        return if (privilege == DEFAULT) privilegeMap.remove(key) != null
        else privilegeMap.put(key, privilege) != privilege
    }

    fun copyPrivilegesFrom(other: PrivilegesHolder) {
        privilegeMap = other.privilegeMap
        privilegeOfStar = other.privilegeOfStar
    }
}

