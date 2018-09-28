package io.dico.parcels2

import io.dico.parcels2.util.math.Vec2i
import io.dico.parcels2.util.math.Vec3i
import org.bukkit.Location
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
interface Parcel : ParcelData, Privileges {
    val id: ParcelId
    val world: ParcelWorld
    val pos: Vec2i
    val x: Int
    val z: Int
    val data: ParcelDataHolder
    val infoString: String
    val hasBlockVisitors: Boolean
    val globalPrivileges: GlobalPrivileges?

    override val keyOfOwner: PlayerProfile.Real?
        get() = owner as? PlayerProfile.Real

    fun copyData(newData: ParcelDataHolder, callerIsDatabase: Boolean = false)

    fun dispose() = copyData(ParcelDataHolder())

    fun updateOwnerSign(force: Boolean = false)

    val homeLocation: Location get() = world.blockManager.getHomeLocation(id)
}



interface ParcelData : RawPrivileges {
    var owner: PlayerProfile?
    val lastClaimTime: DateTime?
    var isOwnerSignOutdated: Boolean
    var interactableConfig: InteractableConfiguration

    //fun canBuild(player: OfflinePlayer, checkAdmin: Boolean = true, checkGlobal: Boolean = true): Boolean

    fun isOwner(uuid: UUID): Boolean {
        return owner?.uuid == uuid
    }

    fun isOwner(profile: PlayerProfile?): Boolean {
        return owner == profile
    }
}

class ParcelDataHolder(addedMap: MutablePrivilegeMap = mutableMapOf())
    : ParcelData, PrivilegesHolder(addedMap) {
    override var owner: PlayerProfile? = null
    override var lastClaimTime: DateTime? = null
    override var isOwnerSignOutdated = false
    override var interactableConfig: InteractableConfiguration = BitmaskInteractableConfiguration()
}

