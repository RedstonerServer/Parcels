package io.dico.parcels2

import io.dico.parcels2.util.Vec2i
import io.dico.parcels2.util.hasBuildAnywhere
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.joda.time.DateTime
import java.util.*

interface ParcelData : AddedData {
    var owner: ParcelOwner?
    val since: DateTime?

    fun canBuild(player: OfflinePlayer, checkAdmin: Boolean = true, checkGlobal: Boolean = true): Boolean

    var allowInteractInputs: Boolean
    var allowInteractInventory: Boolean

    fun isOwner(uuid: UUID): Boolean {
        return owner?.uuid == uuid
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
    val homeLocation get() = world.generator.getHomeLocation(this)
    private var blockVisitors = 0

    val infoString: String
        get() {
            return "$id; owned by ${owner?.let { it.name ?: Bukkit.getOfflinePlayer(it.uuid).name }}"
        }

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
    override fun canBuild(player: OfflinePlayer, checkAdmin: Boolean, checkGlobal: Boolean) = data.canBuild(player)

    override val since: DateTime? get() = data.since

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
            if (it) world.storage.setParcelPlayerStatus(this, uuid, status)
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

    var hasBlockVisitors: Boolean = false; private set
}

class ParcelDataHolder : AddedDataHolder(), ParcelData {
    override var owner: ParcelOwner? = null
    override var since: DateTime? = null
    override fun canBuild(player: OfflinePlayer, checkAdmin: Boolean, checkGlobal: Boolean) = isAllowed(player.uniqueId)
        || owner.let { it != null && it.matches(player, allowNameMatch = false) }
        || (checkAdmin && player is Player && player.hasBuildAnywhere)

    override var allowInteractInputs = true
    override var allowInteractInventory = true
}

