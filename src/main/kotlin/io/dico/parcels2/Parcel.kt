package io.dico.parcels2

import io.dico.parcels2.math.Vec2i
import io.dico.parcels2.util.getPlayerName
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class Parcel(val world: ParcelWorld,
             val pos: Vec2i,
             var data: ParcelData = ParcelData()) {



}

class ParcelData {
    val owner: ParcelOwner? = null
    val added = mutableMapOf<UUID, Boolean>()
}

data class ParcelOwner(val uuid: UUID? = null,
                       val name: String? = null) {

    init {
        uuid ?: name ?: throw IllegalArgumentException("uuid and/or name must be present")
    }

    val playerName get() = getPlayerName(uuid, name)

    @Suppress("DEPRECATION")
    val offlinePlayer get() = (uuid?.let { Bukkit.getOfflinePlayer(it) } ?: Bukkit.getOfflinePlayer(name))
            ?.takeIf { it.isOnline() || it.hasPlayedBefore() }

    fun matches(player: Player, allowNameMatch: Boolean = false): Boolean {
        return player.uniqueId == uuid || (allowNameMatch && player.name == name)
    }

}