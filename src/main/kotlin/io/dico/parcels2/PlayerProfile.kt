@file:Suppress("unused", "UsePropertyAccessSyntax", "DEPRECATION")

package io.dico.parcels2

import io.dico.parcels2.storage.Storage
import io.dico.parcels2.util.ext.PLAYER_NAME_PLACEHOLDER
import io.dico.parcels2.util.ext.isValid
import io.dico.parcels2.util.ext.uuid
import io.dico.parcels2.util.getOfflinePlayer
import io.dico.parcels2.util.getPlayerName
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.UUID

interface PlayerProfile {
    val uuid: UUID? get() = null
    val name: String?
    val nameOrBukkitName: String?
    val notNullName: String
    val isStar: Boolean get() = this is Star
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

            return getOfflinePlayer(input)?.let { PlayerProfile(it) } ?: Unresolved(input)
        }
    }

    interface Real : PlayerProfile {
        override val uuid: UUID
        override val nameOrBukkitName: String?
            // If a player is online, their name is prioritized to get name changes right immediately
            get() = Bukkit.getPlayer(uuid)?.name ?: name ?: getPlayerName(uuid)
        override val notNullName: String
            get() = nameOrBukkitName ?: PLAYER_NAME_PLACEHOLDER

        val player: OfflinePlayer? get() = getOfflinePlayer(uuid)

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
        override val name get() = "*"
        override val nameOrBukkitName get() = name
        override val notNullName get() = name

        // hopefully nobody will have this random UUID :)
        override val uuid: UUID = UUID.fromString("7d09c4c6-117d-4f36-9778-c4d24618cee1")

        override fun matches(player: OfflinePlayer, allowNameMatch: Boolean): Boolean {
            return true
        }

        override fun toString() = "Star"
    }

    abstract class NameOnly(override val name: String) : BaseImpl() {
        override val notNullName get() = name
        override val nameOrBukkitName: String get() = name

        override fun matches(player: OfflinePlayer, allowNameMatch: Boolean): Boolean {
            return allowNameMatch && player.name == name
        }

        override fun toString() = "${javaClass.simpleName}($name)"
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

        suspend fun tryResolveSuspendedly(storage: Storage): Real? {
            return storage.getPlayerUuidForName(name).await()?.let { resolve(it) }
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

    private class RealImpl(override val uuid: UUID, override val name: String?) : BaseImpl(), Real {
        override fun toString() = "Real($notNullName)"
    }

}

suspend fun PlayerProfile.resolved(storage: Storage, resolveToFake: Boolean = false): PlayerProfile? =
    when (this) {
        is PlayerProfile.Unresolved -> tryResolveSuspendedly(storage)
            ?: if (resolveToFake) PlayerProfile.Fake(name) else null
        else -> this
    }