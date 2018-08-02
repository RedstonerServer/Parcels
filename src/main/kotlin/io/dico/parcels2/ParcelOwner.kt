@file:Suppress("unused", "UsePropertyAccessSyntax", "DEPRECATION")

package io.dico.parcels2

import io.dico.parcels2.util.getPlayerNameOrDefault
import io.dico.parcels2.util.isValid
import io.dico.parcels2.util.uuid
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.UUID

class ParcelOwner(val uuid: UUID?,
                  val name: String?) {
    val notNullName: String by lazy { name ?: getPlayerNameOrDefault(uuid!!) }

    constructor(name: String) : this(null, name)
    constructor(uuid: UUID) : this(uuid, null)
    constructor(player: OfflinePlayer) : this(player.uuid, player.name)

    companion object {
        fun nameless(player: OfflinePlayer) = ParcelOwner(player.uuid, null)
    }

    inline val hasUUID: Boolean get() = uuid != null

    val onlinePlayer: Player? get() = uuid?.let { Bukkit.getPlayer(uuid) }

    val onlinePlayerAllowingNameMatch: Player? get() = onlinePlayer ?: name?.let { Bukkit.getPlayerExact(name) }
    val offlinePlayer: OfflinePlayer? get() = uuid?.let { Bukkit.getOfflinePlayer(it).takeIf { it.isValid } }
    val offlinePlayerAllowingNameMatch: OfflinePlayer?
        get() = offlinePlayer ?: Bukkit.getOfflinePlayer(name).takeIf { it.isValid }

    fun matches(player: OfflinePlayer, allowNameMatch: Boolean = false): Boolean {
        return uuid?.let { it == player.uniqueId } ?: false
            || (allowNameMatch && name?.let { it == player.name } ?: false)
    }

    fun equals(other: ParcelOwner): Boolean {
        return if (hasUUID) other.hasUUID && uuid == other.uuid
        else !other.hasUUID && name == other.name
    }

    override fun equals(other: Any?): Boolean {
        return other is ParcelOwner && equals(other)
    }

    override fun hashCode(): Int {
        return if (hasUUID) uuid!!.hashCode() else name!!.hashCode()
    }

}