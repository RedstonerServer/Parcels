package io.dico.parcels2

import io.dico.parcels2.util.Vec2i
import io.dico.parcels2.util.hasBuildAnywhere
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.joda.time.DateTime
import java.util.UUID

/**
 * Parcel implementation of ParcelData will update the database when changes are made.
 * To change the data without updating the database, defer to the data delegate instance.
 *
 * This should be used for example in database query callbacks.
 * However, this implementation is intentionally not thread-safe.
 * Therefore, database query callbacks should schedule their updates using the bukkit scheduler.
 */
interface Parcel : ParcelData {
    val id: ParcelId
    val world: ParcelWorld
    val pos: Vec2i
    val x: Int
    val z: Int
    val data: ParcelData
    val infoString: String
    val hasBlockVisitors: Boolean

    fun copyDataIgnoringDatabase(data: ParcelData)

    fun copyData(data: ParcelData)

    fun dispose()
}

interface ParcelData : AddedData {
    var owner: PlayerProfile?
    val since: DateTime?

    fun canBuild(player: OfflinePlayer, checkAdmin: Boolean = true, checkGlobal: Boolean = true): Boolean

    var allowInteractInputs: Boolean
    var allowInteractInventory: Boolean

    fun isOwner(uuid: UUID): Boolean {
        return owner?.uuid == uuid
    }
}

class ParcelDataHolder(addedMap: MutableAddedDataMap = mutableMapOf()) : AddedDataHolder(addedMap), ParcelData {

    override var owner: PlayerProfile? = null
    override var since: DateTime? = null
    override fun canBuild(player: OfflinePlayer, checkAdmin: Boolean, checkGlobal: Boolean) = isAllowed(player.statusKey)
        || owner.let { it != null && it.matches(player, allowNameMatch = false) }
        || (checkAdmin && player is Player && player.hasBuildAnywhere)

    override var allowInteractInputs = true
    override var allowInteractInventory = true
}

