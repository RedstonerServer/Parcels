package io.dico.parcels2

import io.dico.parcels2.math.Vec2i
import io.dico.parcels2.util.getPlayerName
import io.dico.parcels2.util.hasBuildAnywhere
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class Parcel(val world: ParcelWorld,
             val pos: Vec2i,
             var data: ParcelData = ParcelData()) {

    val id get() = "${pos.x}:${pos.z}"

}

class ParcelData {
    private val added = mutableMapOf<UUID, AddedStatus>()
    var owner: ParcelOwner? = null

    fun setAddedStatus(uuid: UUID): AddedStatus = added.getOrDefault(uuid, AddedStatus.DEFAULT)
    fun setAddedStatus(uuid: UUID, state: AddedStatus) = state.takeIf { it != AddedStatus.DEFAULT }?.let { added[uuid] = it }
            ?: added.remove(uuid)

    fun isBanned(uuid: UUID) = setAddedStatus(uuid) == AddedStatus.BANNED
    fun isAllowed(uuid: UUID) = setAddedStatus(uuid) == AddedStatus.ALLOWED
    fun canBuild(player: Player) = isAllowed(player.uniqueId)
            || owner?.matches(player, allowNameMatch = false) ?: false
            || player.hasBuildAnywhere
}

enum class AddedStatus {
    DEFAULT,
    ALLOWED,
    BANNED
}

data class ParcelOwner(val uuid: UUID? = null,
                       val name: String? = null) {

    init {
        uuid ?: name ?: throw IllegalArgumentException("uuid and/or name must be present")
    }

    val playerName get() = getPlayerName(uuid, name)

    @Suppress("DEPRECATION")
    val offlinePlayer
        get() = (uuid?.let { Bukkit.getOfflinePlayer(it) } ?: Bukkit.getOfflinePlayer(name))
                ?.takeIf { it.isOnline() || it.hasPlayedBefore() }

    fun matches(player: Player, allowNameMatch: Boolean = false): Boolean {
        return uuid?.let { it == player.uniqueId } ?: false
                || (allowNameMatch && name?.let { it == player.name } ?: false)
    }

}