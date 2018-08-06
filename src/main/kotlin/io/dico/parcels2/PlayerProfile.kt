@file:Suppress("unused", "UsePropertyAccessSyntax", "DEPRECATION")

package io.dico.parcels2

import io.dico.parcels2.storage.Storage
import io.dico.parcels2.util.getPlayerNameOrDefault
import io.dico.parcels2.util.isValid
import io.dico.parcels2.util.uuid
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.UUID

interface PlayerProfile {
    val uuid: UUID? get() = null
    val name: String?
    val nameOrBukkitName: String?
    val notNullName: String
    val isStar: Boolean get() = false
    val exists: Boolean get() = this is RealImpl

    fun matches(player: OfflinePlayer, allowNameMatch: Boolean = false): Boolean

    fun equals(other: PlayerProfile): Boolean

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int

    val isFake: Boolean get() = this is Fake
    val isReal: Boolean get() = this is Real

    companion object {
        fun safe(uuid: UUID?, name: String?): PlayerProfile? {
            if (uuid != null) return Real(uuid, name)
            if (name != null) return invoke(name)
            return null
        }

        operator fun invoke(uuid: UUID?, name: String?): PlayerProfile {
            return safe(uuid, name) ?: throw IllegalArgumentException("One of uuid and name must not be null")
        }

        operator fun invoke(uuid: UUID): Real {
            if (uuid == Star.uuid) return Star
            return RealImpl(uuid, null)
        }

        operator fun invoke(name: String): PlayerProfile {
            if (name == Star.name) return Star
            return Fake(name)
        }

        operator fun invoke(player: OfflinePlayer): PlayerProfile {
            return if (player.isValid) Real(player.uuid, player.name) else Fake(player.name)
        }

        fun nameless(player: OfflinePlayer): Real {
            if (!player.isValid) throw IllegalArgumentException("The given OfflinePlayer is not valid")
            return RealImpl(player.uuid, null)
        }

        fun byName(input: String, allowReal: Boolean = true, allowFake: Boolean = false): PlayerProfile {
            if (!allowReal) {
                if (!allowFake) throw IllegalArgumentException("at least one of allowReal and allowFake must be true")
                return Fake(input)
            }

            if (input == Star.name) return Star

            return Bukkit.getOfflinePlayer(input).takeIf { it.isValid }?.let { PlayerProfile(it) }
                ?: Unresolved(input)
        }
    }

    interface Real : PlayerProfile {
        override val uuid: UUID
        override val nameOrBukkitName: String?
            get() = name ?: Bukkit.getOfflinePlayer(uuid).takeIf { it.isValid }?.name
        override val notNullName: String
            get() = name ?: getPlayerNameOrDefault(uuid)

        val player: OfflinePlayer? get() = Bukkit.getOfflinePlayer(uuid).takeIf { it.isValid }
        val playerUnchecked: OfflinePlayer get() = Bukkit.getOfflinePlayer(uuid)

        override fun matches(player: OfflinePlayer, allowNameMatch: Boolean): Boolean {
            return uuid == player.uuid || (allowNameMatch && name?.let { it == player.name } == true)
        }

        override fun equals(other: PlayerProfile): Boolean {
            return other is Real && uuid == other.uuid
        }

        companion object {
            fun byName(name: String): PlayerProfile {
                if (name == Star.name) return Star
                return Unresolved(name)
            }

            operator fun invoke(uuid: UUID, name: String?): Real {
                if (name == Star.name || uuid == Star.uuid) return Star
                return RealImpl(uuid, name)
            }

            fun safe(uuid: UUID?, name: String?): Real? {
                if (name == Star.name || uuid == Star.uuid) return Star
                if (uuid == null) return null
                return RealImpl(uuid, name)
            }

        }
    }

    object Star : BaseImpl(), Real {
        override val name: String = "*"
        override val uuid: UUID = UUID.fromString("7d09c4c6-117d-4f36-9778-c4d24618cee1")
        override val isStar: Boolean get() = true

        override fun matches(player: OfflinePlayer, allowNameMatch: Boolean): Boolean {
            return true
        }
    }

    abstract class NameOnly(override val name: String) : BaseImpl() {
        override val notNullName get() = name
        override val nameOrBukkitName: String get() = name

        override fun matches(player: OfflinePlayer, allowNameMatch: Boolean): Boolean {
            return allowNameMatch && player.name == name
        }
    }

    class Fake(name: String) : NameOnly(name) {
        override fun equals(other: PlayerProfile): Boolean {
            return other is Fake && other.name == name
        }
    }

    class Unresolved(name: String) : NameOnly(name) {
        override fun equals(other: PlayerProfile): Boolean {
            return other is Unresolved && name == other.name
        }

        fun tryResolve(storage: Storage): Deferred<Real?> {
            return async(Unconfined) { tryResolveSuspendedly(storage) }
        }

        suspend fun tryResolveSuspendedly(storage: Storage): Real? {
            return storage.getPlayerUuidForName(name).await()?.let { RealImpl(it, name) }
        }

        fun resolve(uuid: UUID): Real {
            return RealImpl(uuid, name)
        }

        fun throwException(): Nothing {
            throw IllegalArgumentException("A UUID for the player $name can not be found")
        }
    }

    abstract class BaseImpl : PlayerProfile {
        override fun equals(other: Any?): Boolean {
            return this === other || (other is PlayerProfile && equals(other))
        }

        override fun hashCode(): Int {
            return uuid?.hashCode() ?: name!!.hashCode()
        }
    }

    private class RealImpl(override val uuid: UUID, override val name: String?) : BaseImpl(), Real

}


/*


/**
 * This class can represent:
 *
 * An existing player
 * A fake player (with only a name)
 * An existing player who must have its uuid resolved from the database (after checking against Bukkit OfflinePlayer)
 * STAR profile, which matches everyone. This profile is considered a REAL player, because it can have an added status.
 */
class PlayerProfile2 private constructor(uuid: UUID?,
                                        val name: String?,
                                        val isReal: Boolean = uuid != null) {
    private var _uuid: UUID? = uuid
    val notNullName: String get() = name ?: getPlayerNameOrDefault(uuid!!)

    val uuid: UUID? get() = _uuid ?: if (isReal) throw IllegalStateException("This PlayerProfile must be resolved first") else null

    companion object {
        // below uuid is just a randomly generated one (version 4). Hopefully no minecraft player will ever have it :)
        val star = PlayerProfile(UUID.fromString("7d09c4c6-117d-4f36-9778-c4d24618cee1"), "*", true)

        fun nameless(player: OfflinePlayer): PlayerProfile {
            if (!player.isValid) throw IllegalArgumentException("The given OfflinePlayer is not valid")
            return PlayerProfile(player.uuid)
        }

        fun fromNameAndUuid(name: String?, uuid: UUID?): PlayerProfile? {
            if (name == null && uuid == null) return null
            if (star.name == name && star._uuid == uuid) return star
            return PlayerProfile(uuid, name)
        }

        fun realPlayerByName(name: String): PlayerProfile {
            return fromString(name, allowReal = true, allowFake = false)
        }

        fun fromString(input: String, allowReal: Boolean = true, allowFake: Boolean = false): PlayerProfile {
            if (!allowReal) {
                if (!allowFake) throw IllegalArgumentException("at least one of allowReal and allowFake must be true")
                return PlayerProfile(input)
            }

            if (input == star.name) return star

            return Bukkit.getOfflinePlayer(input).takeIf { it.isValid }?.let { PlayerProfile(it) }
                ?: PlayerProfile(null, input, !allowFake)
        }

        operator fun invoke(name: String): PlayerProfile {
            if (name == star.name) return star
            return PlayerProfile(null, name)
        }

        operator fun invoke(uuid: UUID): PlayerProfile {
            if (uuid == star.uuid) return star
            return PlayerProfile(uuid, null)
        }

        operator fun invoke(player: OfflinePlayer): PlayerProfile {
            // avoid UUID comparison against STAR
            return if (player.isValid) PlayerProfile(player.uuid, player.name) else invoke(player.name)
        }
    }

    val isStar: Boolean get() = this === star || (name == star.name && _uuid == star._uuid)
    val hasUUID: Boolean get() = _uuid != null
    val mustBeResolved: Boolean get() = isReal && _uuid == null

    val onlinePlayer: Player? get() = uuid?.let { Bukkit.getPlayer(uuid) }

    val onlinePlayerAllowingNameMatch: Player? get() = onlinePlayer ?: name?.let { Bukkit.getPlayerExact(name) }
    val offlinePlayer: OfflinePlayer? get() = uuid?.let { Bukkit.getOfflinePlayer(it).takeIf { it.isValid } }
    val offlinePlayerAllowingNameMatch: OfflinePlayer?
        get() = offlinePlayer ?: Bukkit.getOfflinePlayer(name).takeIf { it.isValid }

    fun matches(player: OfflinePlayer, allowNameMatch: Boolean = false): Boolean {
        if (isStar) return true
        return uuid?.let { it == player.uniqueId } ?: false
            || (allowNameMatch && name?.let { it == player.name } ?: false)
    }

    fun equals(other: PlayerProfile): Boolean {
        return if (_uuid != null) _uuid == other._uuid
        else other._uuid == null && isReal == other.isReal && name == other.name
    }

    override fun equals(other: Any?): Boolean {
        return other is PlayerProfile && equals(other)
    }

    override fun hashCode(): Int {
        return _uuid?.hashCode() ?: name!!.hashCode()
    }

    /**
     * resolve the uuid of this player profile if [mustBeResolved], using specified [storage].
     * returns true if the PlayerProfile has a uuid after this call.
     */
    suspend fun resolve(storage: Storage): Boolean {
        if (mustBeResolved) {
            val uuid = storage.getPlayerUuidForName(name!!).await()
            _uuid = uuid
            return uuid != null
        }
        return _uuid != null
    }

    fun resolve(uuid: UUID) {
        if (isReal && _uuid == null) {
            _uuid = uuid
        }
    }
}
*/
