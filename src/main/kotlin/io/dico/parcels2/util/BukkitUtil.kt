package io.dico.parcels2.util

import io.dico.parcels2.util.ext.isValid
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.UUID

fun getPlayerName(uuid: UUID): String? = getOfflinePlayer(uuid)?.name

fun getOfflinePlayer(uuid: UUID): OfflinePlayer? = Bukkit.getOfflinePlayer(uuid).takeIf { it.isValid }

fun getOfflinePlayer(name: String): OfflinePlayer? = Bukkit.getOfflinePlayer(name).takeIf { it.isValid }
