package io.dico.parcels2

import io.dico.parcels2.util.uuid
import org.bukkit.OfflinePlayer
import java.util.*

interface AddedData {
    val added: Map<UUID, AddedStatus>

    fun getAddedStatus(uuid: UUID): AddedStatus
    fun setAddedStatus(uuid: UUID, status: AddedStatus): Boolean

    fun compareAndSetAddedStatus(uuid: UUID, expect: AddedStatus, status: AddedStatus): Boolean =
        (getAddedStatus(uuid) == expect).also { if (it) setAddedStatus(uuid, status) }

    fun isAllowed(uuid: UUID) = getAddedStatus(uuid) == AddedStatus.ALLOWED
    fun allow(uuid: UUID) = setAddedStatus(uuid, AddedStatus.ALLOWED)
    fun disallow(uuid: UUID) = compareAndSetAddedStatus(uuid, AddedStatus.ALLOWED, AddedStatus.DEFAULT)
    fun isBanned(uuid: UUID) = getAddedStatus(uuid) == AddedStatus.BANNED
    fun ban(uuid: UUID) = setAddedStatus(uuid, AddedStatus.BANNED)
    fun unban(uuid: UUID) = compareAndSetAddedStatus(uuid, AddedStatus.BANNED, AddedStatus.DEFAULT)

    fun isAllowed(player: OfflinePlayer) = isAllowed(player.uuid)
    fun allow(player: OfflinePlayer) = allow(player.uuid)
    fun disallow(player: OfflinePlayer) = disallow(player.uuid)
    fun isBanned(player: OfflinePlayer) = isBanned(player.uuid)
    fun ban(player: OfflinePlayer) = ban(player.uuid)
    fun unban(player: OfflinePlayer) = unban(player.uuid)
}

open class AddedDataHolder(override var added: MutableMap<UUID, AddedStatus>
                           = mutableMapOf<UUID, AddedStatus>()) : AddedData {
    override fun getAddedStatus(uuid: UUID): AddedStatus = added.getOrDefault(uuid, AddedStatus.DEFAULT)
    override fun setAddedStatus(uuid: UUID, status: AddedStatus): Boolean = status.takeIf { it != AddedStatus.DEFAULT }
        ?.let { added.put(uuid, it) != it }
        ?: added.remove(uuid) != null
}

enum class AddedStatus {
    DEFAULT,
    ALLOWED,
    BANNED;

    val isDefault get() = this == DEFAULT
    val isAllowed get() = this == ALLOWED
    val isBanned get() = this == BANNED
}