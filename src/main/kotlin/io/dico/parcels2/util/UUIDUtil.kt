package io.dico.parcels2.util

import org.bukkit.Bukkit
import java.nio.ByteBuffer
import java.util.*

@Suppress("UsePropertyAccessSyntax")
fun getPlayerName(uuid: UUID?, ifUnknown: String? = null): String {
    return uuid?.let { Bukkit.getOfflinePlayer(uuid)?.takeIf { it.isOnline() || it.hasPlayedBefore() }?.name }
            ?: ifUnknown
            ?: ":unknown_name:"
}

fun UUID?.toByteArray(): ByteArray? = this?.let {
    ByteBuffer.allocate(16).apply {
        putLong(mostSignificantBits)
        putLong(leastSignificantBits)
    }.array()
}

fun ByteArray?.toUUID(): UUID? = this?.let {
    ByteBuffer.wrap(it).run { UUID(long, long) }
}