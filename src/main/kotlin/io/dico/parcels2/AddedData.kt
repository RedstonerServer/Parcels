package io.dico.parcels2

import io.dico.parcels2.AddedStatus.*
import org.bukkit.OfflinePlayer

enum class AddedStatus {
    DEFAULT, ALLOWED, BANNED;
}

typealias StatusKey = PlayerProfile.Real
typealias MutableAddedDataMap = MutableMap<StatusKey, AddedStatus>
typealias AddedDataMap = Map<StatusKey, AddedStatus>

@Suppress("FunctionName")
fun MutableAddedDataMap(): MutableAddedDataMap = hashMapOf()

interface AddedData {
    val addedMap: AddedDataMap
    var statusOfStar: AddedStatus

    fun getStatus(key: StatusKey): AddedStatus
    fun setStatus(key: StatusKey, status: AddedStatus): Boolean

    fun casStatus(key: StatusKey, expect: AddedStatus, status: AddedStatus): Boolean =
        getStatus(key) == expect && setStatus(key, status)

    fun isAllowed(key: StatusKey) = getStatus(key) == ALLOWED
    fun allow(key: StatusKey) = setStatus(key, ALLOWED)
    fun disallow(key: StatusKey) = casStatus(key, ALLOWED, DEFAULT)
    fun isBanned(key: StatusKey) = getStatus(key) == BANNED
    fun ban(key: StatusKey) = setStatus(key, BANNED)
    fun unban(key: StatusKey) = casStatus(key, BANNED, DEFAULT)

    fun isAllowed(player: OfflinePlayer) = isAllowed(player.statusKey)
    fun allow(player: OfflinePlayer) = allow(player.statusKey)
    fun disallow(player: OfflinePlayer) = disallow(player.statusKey)
    fun isBanned(player: OfflinePlayer) = isBanned(player.statusKey)
    fun ban(player: OfflinePlayer) = ban(player.statusKey)
    fun unban(player: OfflinePlayer) = unban(player.statusKey)
}

inline val OfflinePlayer.statusKey: StatusKey
    get() = PlayerProfile.nameless(this)

open class AddedDataHolder(override var addedMap: MutableAddedDataMap = MutableAddedDataMap()) : AddedData {
    override var statusOfStar: AddedStatus = DEFAULT

    override fun getStatus(key: StatusKey): AddedStatus = addedMap.getOrDefault(key, statusOfStar)

    override fun setStatus(key: StatusKey, status: AddedStatus): Boolean {
        return if (status == DEFAULT) addedMap.remove(key) != null
        else addedMap.put(key, status) != status
    }
}

interface GlobalAddedData : AddedData {
    val owner: PlayerProfile
}

interface GlobalAddedDataManager {
    operator fun get(owner: PlayerProfile): GlobalAddedData
}
