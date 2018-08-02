package io.dico.parcels2.defaultimpl

import io.dico.dicore.Formatting
import io.dico.parcels2.*
import io.dico.parcels2.util.Vec2i
import io.dico.parcels2.util.getPlayerName
import org.bukkit.OfflinePlayer
import org.joda.time.DateTime
import java.util.UUID
import kotlin.reflect.KProperty

class ParcelImpl(override val world: ParcelWorld,
                 override val x: Int,
                 override val z: Int) : Parcel, ParcelId {
    override val id: ParcelId = this
    override val pos get() = Vec2i(x, z)
    override var data: ParcelDataHolder = ParcelDataHolder(); private set
    override val infoString by ParcelInfoStringComputer
    override var hasBlockVisitors: Boolean = false; private set
    override val worldId: ParcelWorldId get() = world.id

    override fun copyDataIgnoringDatabase(data: ParcelData) {
        this.data = ((data as? Parcel)?.data ?: data) as ParcelDataHolder
    }

    override fun copyData(data: ParcelData) {
        copyDataIgnoringDatabase(data)
        world.storage.setParcelData(this, data)
    }

    override fun dispose() {
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
            append(parcel.x)
            append(',')
            append(parcel.z)
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
                appendField("inputs") { append(parcel.allowInteractInputs) }
                append(", ")
                appendField("inventory") { append(parcel.allowInteractInventory) }
                append(")")
            }
        }

    }
}