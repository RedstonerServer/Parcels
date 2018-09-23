package io.dico.parcels2.util

import io.dico.parcels2.util.ext.isValid
import org.bukkit.Bukkit
import java.nio.ByteBuffer
import java.util.UUID

const val PLAYER_NAME_PLACEHOLDER = ":unknown_name:"

fun getPlayerName(uuid: UUID): String? {
    return Bukkit.getOfflinePlayer(uuid)?.takeIf { it.isValid }?.name
}


