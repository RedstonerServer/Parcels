package io.dico.parcels2

import io.dico.parcels2.math.Vec2i
import io.dico.parcels2.util.getPlayerName
import io.dico.parcels2.util.hasBuildAnywhere
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

interface ParcelData {
    var owner: ParcelOwner?
    val added: Map<UUID, AddedStatus>

    fun getAddedStatus(uuid: UUID): AddedStatus
    fun setAddedStatus(uuid: UUID, status: AddedStatus): Boolean
    fun isBanned(uuid: UUID): Boolean
    fun isAllowed(uuid: UUID): Boolean
    fun canBuild(player: Player): Boolean

    var allowInteractInputs: Boolean
    var allowInteractInventory: Boolean

    fun isOwner(uuid: UUID): Boolean {
        return owner?.uuid == uuid
    }

    val infoString: String
        get() {
            TODO()
        }
}

/**
 * Parcel implementation of ParcelData will update the database when changes are made.
 * To change the data without updating the database, defer to the data delegate instance.
 *
 * This should be used for example in database query callbacks.
 * However, this implementation is intentionally not thread-safe.
 * Therefore, database query callbacks should schedule their updates using the bukkit scheduler.
 */
class Parcel(val world: ParcelWorld, val pos: Vec2i) : ParcelData {
    val id get() = "${pos.x}:${pos.z}"


    var data: ParcelData = ParcelDataHolder(); private set

    fun copyDataIgnoringDatabase(data: ParcelData) {
        this.data = data
    }

    fun copyData(data: ParcelData) {
        world.storage.setParcelData(this, data)
        this.data = data
    }

    override val added: Map<UUID, AddedStatus> get() = data.added
    override fun getAddedStatus(uuid: UUID) = data.getAddedStatus(uuid)
    override fun isBanned(uuid: UUID) = data.isBanned(uuid)
    override fun isAllowed(uuid: UUID) = data.isAllowed(uuid)
    override fun canBuild(player: Player) = data.canBuild(player)

    override var owner: ParcelOwner?
        get() = data.owner
        set(value) {
            if (data.owner != value) {
                world.storage.setParcelOwner(this, value)
                data.owner = value
            }
        }

    override fun setAddedStatus(uuid: UUID, status: AddedStatus): Boolean {
        return data.setAddedStatus(uuid, status).also {
            if (it) world.storage.setParcelPlayerState(this, uuid, status.asBoolean)
        }
    }

    override var allowInteractInputs: Boolean
        get() = data.allowInteractInputs
        set(value) {
            if (data.allowInteractInputs == value) return
            world.storage.setParcelAllowsInteractInputs(this, value)
            data.allowInteractInputs = value
        }

    override var allowInteractInventory: Boolean
        get() = data.allowInteractInventory
        set(value) {
            if (data.allowInteractInventory == value) return
            world.storage.setParcelAllowsInteractInventory(this, value)
            data.allowInteractInventory = value
        }
}

class ParcelDataHolder : ParcelData {
    override var added = mutableMapOf<UUID, AddedStatus>()
    override var owner: ParcelOwner? = null

    override fun getAddedStatus(uuid: UUID): AddedStatus = added.getOrDefault(uuid, AddedStatus.DEFAULT)
    override fun setAddedStatus(uuid: UUID, status: AddedStatus): Boolean = status.takeIf { it != AddedStatus.DEFAULT }
        ?.let { added.put(uuid, it) != it }
        ?: added.remove(uuid) != null

    override fun isBanned(uuid: UUID) = getAddedStatus(uuid) == AddedStatus.BANNED
    override fun isAllowed(uuid: UUID) = getAddedStatus(uuid) == AddedStatus.ALLOWED
    override fun canBuild(player: Player) = isAllowed(player.uniqueId)
        || owner?.matches(player, allowNameMatch = false) ?: false
        || player.hasBuildAnywhere

    override var allowInteractInputs = true
    override var allowInteractInventory = true
}

enum class AddedStatus {
    DEFAULT,
    ALLOWED,
    BANNED;

    val asBoolean
        get() = when (this) {
            DEFAULT -> null
            ALLOWED -> true
            BANNED -> false
        }
}

@Suppress("UsePropertyAccessSyntax")
class ParcelOwner(val uuid: UUID? = null,
                  name: String? = null) {

    companion object {
        fun create(uuid: UUID?, name: String?): ParcelOwner? {
            return uuid?.let { ParcelOwner(uuid, name) }
                ?: name?.let { ParcelOwner(uuid, name) }
        }
    }

    val name: String?

    init {
        uuid ?: name ?: throw IllegalArgumentException("uuid and/or name must be present")

        if (name != null) this.name = name
        else {
            val offlinePlayer = Bukkit.getOfflinePlayer(uuid).takeIf { it.isOnline() || it.hasPlayedBefore() }
            this.name = offlinePlayer?.name
        }
    }

    val playerName get() = getPlayerName(uuid, name)

    fun matches(player: Player, allowNameMatch: Boolean = false): Boolean {
        return uuid?.let { it == player.uniqueId } ?: false
            || (allowNameMatch && name?.let { it == player.name } ?: false)
    }

    val onlinePlayer: Player? get() = uuid?.let { Bukkit.getPlayer(uuid) }
    val onlinePlayerAllowingNameMatch: Player? get() = onlinePlayer ?: name?.let { Bukkit.getPlayer(name) }

    @Suppress("DEPRECATION")
    val offlinePlayer
        get() = (uuid?.let { Bukkit.getOfflinePlayer(it) } ?: Bukkit.getOfflinePlayer(name))
            ?.takeIf { it.isOnline() || it.hasPlayedBefore() }
}
