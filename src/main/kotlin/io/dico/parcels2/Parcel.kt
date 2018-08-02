package io.dico.parcels2

import io.dico.dicore.Formatting
import io.dico.parcels2.util.Vec2i
import io.dico.parcels2.util.getPlayerName
import io.dico.parcels2.util.hasBuildAnywhere
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.joda.time.DateTime
import java.util.*
import kotlin.reflect.KProperty

/**
 * Parcel implementation of ParcelData will update the database when changes are made.
 * To change the data without updating the database, defer to the data delegate instance.
 *
 * This should be used for example in database query callbacks.
 * However, this implementation is intentionally not thread-safe.
 * Therefore, database query callbacks should schedule their updates using the bukkit scheduler.
 */
class Parcel(val world: ParcelWorld, val pos: Vec2i) : ParcelData {
    var data: ParcelData = ParcelDataHolder(); private set

    val id get() = "${pos.x}:${pos.z}"
    val homeLocation get() = world.generator.getHomeLocation(this)

    val infoString by ParcelInfoStringComputer

    fun copyDataIgnoringDatabase(data: ParcelData) {
        this.data = data
    }

    fun copyData(data: ParcelData) {
        copyDataIgnoringDatabase(data)
        world.storage.setParcelData(this, data)
    }

    fun dispose() {
        copyDataIgnoringDatabase(ParcelDataHolder())
        world.storage.setParcelData(this, null)
    }

    override val addedMap: Map<UUID, AddedStatus> get() = data.addedMap
    override fun getAddedStatus(uuid: UUID) = data.getAddedStatus(uuid)
    override fun isBanned(uuid: UUID) = data.isBanned(uuid)
    override fun isAllowed(uuid: UUID) = data.isAllowed(uuid)
    override fun canBuild(player: OfflinePlayer, checkAdmin: Boolean, checkGlobal: Boolean): Boolean {
        return (data.canBuild(player, checkAdmin, false))
            || checkGlobal && world.globalAddedData[owner ?: return false].isAllowed(player)
    }

    val globalAddedMap: Map<UUID, AddedStatus>? get() = owner?.let { world.globalAddedData[it].addedMap }

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

class ParcelDataHolder : AddedDataHolder(), ParcelData {

    override var owner: ParcelOwner? = null
    override var since: DateTime? = null
    override fun canBuild(player: OfflinePlayer, checkAdmin: Boolean, checkGlobal: Boolean) = isAllowed(player.uniqueId)
        || owner.let { it != null && it.matches(player, allowNameMatch = false) }
        || (checkAdmin && player is Player && player.hasBuildAnywhere)

    override var allowInteractInputs = true
    override var allowInteractInventory = true
}

private object ParcelInfoStringComputer {
    val infoStringColor1 = Formatting.GREEN
    val infoStringColor2 = Formatting.AQUA

    private inline fun StringBuilder.appendField(name: String, value: StringBuilder.() -> Unit) {
        append(infoStringColor1)
        append(name)
        append(": ")
        append(infoStringColor2)
        value()
        append(' ')
    }

    operator fun getValue(parcel: Parcel, property: KProperty<*>): String = buildString {
        appendField("ID") {
            append(parcel.pos.x)
            append(':')
            append(parcel.pos.z)
        }

        appendField("Owner") {
            val owner = parcel.owner
            if (owner == null) {
                append(infoStringColor1)
                append("none")
            } else {
                append(owner.notNullName)
            }
        }

        // plotme appends biome here

        append('\n')

        val allowedMap = parcel.addedMap.filterValues { it.isAllowed }
        if (allowedMap.isNotEmpty()) appendField("Allowed") {
            allowedMap.keys.map(::getPlayerName).joinTo(this)
        }

        val bannedMap = parcel.addedMap.filterValues { it.isBanned }
        if (bannedMap.isNotEmpty()) appendField("Banned") {
            bannedMap.keys.map(::getPlayerName).joinTo(this)
        }

        if (!parcel.allowInteractInputs || !parcel.allowInteractInventory) {
            appendField("Options") {
                append("(")
                appendField("inputs") { append(parcel.allowInteractInputs)}
                append(", ")
                appendField("inventory") { append(parcel.allowInteractInventory) }
                append(")")
            }
        }

    }
}