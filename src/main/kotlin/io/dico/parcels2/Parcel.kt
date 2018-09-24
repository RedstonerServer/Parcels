package io.dico.parcels2

import io.dico.parcels2.util.Vec2i
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
    val data: ParcelData
    val infoString: String
    val hasBlockVisitors: Boolean
    val globalPrivileges: GlobalPrivileges?

    override val keyOfOwner: PlayerProfile.Real?
        get() = owner as? PlayerProfile.Real

    fun copyDataIgnoringDatabase(data: ParcelData)

    fun copyData(data: ParcelData)

    fun dispose()

    suspend fun withBlockVisitorPermit(block: suspend () -> Unit)

    val homeLocation: Location get() = world.blockManager.getHomeLocation(id)
}

interface ParcelData : PrivilegesMinimal {
    var owner: PlayerProfile?
    val lastClaimTime: DateTime?
    var ownerSignOutdated: Boolean
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
    override var ownerSignOutdated = false

    //override fun canBuild(player: OfflinePlayer, checkAdmin: Boolean, checkGlobal: Boolean) =
    //    hasPrivilegeToBuild(player)
    //    || owner.let { it != null && it.matches(player, allowNameMatch = false) }
    //    || (checkAdmin && player is Player && player.hasPermBuildAnywhere)

    override var interactableConfig: InteractableConfiguration = BitmaskInteractableConfiguration()
}

