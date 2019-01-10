package io.dico.parcels2.util

import io.dico.parcels2.util.ext.isValid
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.lang.IllegalArgumentException
import java.util.UUID

fun getPlayerName(uuid: UUID): String? = getOfflinePlayer(uuid)?.name

fun getOfflinePlayer(uuid: UUID): OfflinePlayer? = Bukkit.getOfflinePlayer(uuid).takeIf { it.isValid }

fun getOfflinePlayer(name: String): OfflinePlayer? = Bukkit.getOfflinePlayer(name).takeIf { it.isValid }

fun isServerThread(): Boolean = Thread.currentThread().name == "Server thread"

fun isPlayerNameValid(name: String): Boolean =
    name.length in 3..16
    && name.find { it !in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_" } == null

fun checkPlayerNameValid(name: String) {
    if (!isPlayerNameValid(name)) throw IllegalArgumentException("Invalid player name: $name")
}
