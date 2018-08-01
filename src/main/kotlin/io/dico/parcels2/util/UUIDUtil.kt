package io.dico.parcels2.util

import org.bukkit.Bukkit
import org.jetbrains.annotations.Contract
import java.nio.ByteBuffer
import java.util.*

@Suppress("UsePropertyAccessSyntax")
fun getPlayerNameOrDefault(uuid: UUID?, ifUnknown: String? = null): String {
    return uuid
        ?.let { getPlayerName(it) }
        ?: ifUnknown
        ?: ":unknown_name:"
}

fun getPlayerName(uuid: UUID): String? {
    return Bukkit.getOfflinePlayer(uuid)?.takeIf { it.isValid }?.name
}

fun UUID.toByteArray(): ByteArray =
    ByteBuffer.allocate(16).apply {
        putLong(mostSignificantBits)
        putLong(leastSignificantBits)
    }.array()

fun ByteArray.toUUID(): UUID = ByteBuffer.wrap(this).run { UUID(long, long) }
