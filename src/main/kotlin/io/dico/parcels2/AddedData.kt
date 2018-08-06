package io.dico.parcels2

import org.bukkit.OfflinePlayer

typealias StatusKey = PlayerProfile.Real
typealias MutableAddedDataMap = MutableMap<StatusKey, AddedStatus>
typealias AddedDataMap = Map<StatusKey, AddedStatus>

@Suppress("FunctionName")
fun MutableAddedDataMap(): MutableAddedDataMap = hashMapOf()

interface AddedData {
    val addedMap: AddedDataMap
    var addedStatusOfStar: AddedStatus

    fun getAddedStatus(key: StatusKey): AddedStatus
    fun setAddedStatus(key: StatusKey, status: AddedStatus): Boolean

    fun compareAndSetAddedStatus(key: StatusKey, expect: AddedStatus, status: AddedStatus): Boolean =
        getAddedStatus(key) == expect && setAddedStatus(key, status)

    fun isAllowed(key: StatusKey) = getAddedStatus(key) == AddedStatus.ALLOWED
    fun allow(key: StatusKey) = setAddedStatus(key, AddedStatus.ALLOWED)
    fun disallow(key: StatusKey) = compareAndSetAddedStatus(key, AddedStatus.ALLOWED, AddedStatus.DEFAULT)
    fun isBanned(key: StatusKey) = getAddedStatus(key) == AddedStatus.BANNED
    fun ban(key: StatusKey) = setAddedStatus(key, AddedStatus.BANNED)
    fun unban(key: StatusKey) = compareAndSetAddedStatus(key, AddedStatus.BANNED, AddedStatus.DEFAULT)

    fun isAllowed(player: OfflinePlayer) = isAllowed(player.statusKey)
    fun allow(player: OfflinePlayer) = allow(player.statusKey)
    fun disallow(player: OfflinePlayer) = disallow(player.statusKey)
    fun isBanned(player: OfflinePlayer) = isBanned(player.statusKey)
    fun ban(player: OfflinePlayer) = ban(player.statusKey)
    fun unban(player: OfflinePlayer) = unban(player.statusKey)
}

inline val OfflinePlayer.statusKey get() = PlayerProfile.nameless(this)

open class AddedDataHolder(override var addedMap: MutableAddedDataMap = MutableAddedDataMap()) : AddedData {
    override var addedStatusOfStar: AddedStatus = AddedStatus.DEFAULT

    override fun getAddedStatus(key: StatusKey): AddedStatus = addedMap.getOrDefault(key, addedStatusOfStar)

    override fun setAddedStatus(key: StatusKey, status: AddedStatus): Boolean {
        return if (status.isDefault) addedMap.remove(key) != null
        else addedMap.put(key, status) != status
    }
}

enum class AddedStatus {
    DEFAULT,
    ALLOWED,
    BANNED;

    inline val isDefault get() = this == DEFAULT
    inline val isAllowed get() = this == ALLOWED
    inline val isBanned get() = this == BANNED
}

interface GlobalAddedData : AddedData {
    val owner: PlayerProfile
}

interface GlobalAddedDataManager {
    operator fun get(owner: PlayerProfile): GlobalAddedData
}
