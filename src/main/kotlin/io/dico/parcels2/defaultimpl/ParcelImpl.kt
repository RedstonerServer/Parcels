package io.dico.parcels2.defaultimpl

import io.dico.dicore.Formatting
import io.dico.parcels2.*
import io.dico.parcels2.Privilege.*
import io.dico.parcels2.util.Vec2i
import io.dico.parcels2.util.ext.alsoIfTrue
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.joda.time.DateTime
import java.util.concurrent.atomic.AtomicInteger

class ParcelImpl(
    override val world: ParcelWorld,
    override val x: Int,
    override val z: Int
) : Parcel, ParcelId {
    override val id: ParcelId = this
    override val pos get() = Vec2i(x, z)
    override var data: ParcelDataHolder = ParcelDataHolder(); private set
    override val infoString get() = ParcelInfoStringComputer.getInfoString(this)
    override val hasBlockVisitors get() = blockVisitors.get() > 0
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

    override val privilegeMap: PrivilegeMap get() = data.privilegeMap
    override fun getStoredPrivilege(key: PrivilegeKey) = data.getStoredPrivilege(key)

    override fun setStoredPrivilege(key: PrivilegeKey, privilege: Privilege): Boolean {
        return data.setStoredPrivilege(key, privilege).alsoIfTrue {
            world.storage.setLocalPrivilege(this, key, privilege)
        }
    }

    override fun privilege(player: OfflinePlayer, adminPerm: String): Privilege {
        val privilege = super.privilege(player, adminPerm)
        return if (privilege == DEFAULT) globalPrivileges?.privilege(player, adminPerm) ?: DEFAULT
        else privilege
    }

    override val globalPrivileges: GlobalPrivileges?
        get() = keyOfOwner?.let { world.globalPrivileges[it] }

    override val lastClaimTime: DateTime? get() = data.lastClaimTime

    override var ownerSignOutdated: Boolean
        get() = data.ownerSignOutdated
        set(value) {
            if (data.ownerSignOutdated != value) {
                world.storage.setParcelOwnerSignOutdated(this, value)
                data.ownerSignOutdated = value
            }
        }

    override var owner: PlayerProfile?
        get() = data.owner
        set(value) {
            if (data.owner != value) {
                world.storage.setParcelOwner(this, value)
                world.blockManager.setOwnerBlock(this, value)
                data.owner = value
            }
        }

    private fun updateInteractableConfigStorage() {
        world.storage.setParcelOptionsInteractConfig(this, data.interactableConfig)
    }

    private var _interactableConfig: InteractableConfiguration? = null
    override var interactableConfig: InteractableConfiguration
        get() {
            if (_interactableConfig == null) {
                _interactableConfig = object : InteractableConfiguration {
                    override fun isInteractable(material: Material): Boolean = data.interactableConfig.isInteractable(material)
                    override fun isInteractable(clazz: Interactables): Boolean = data.interactableConfig.isInteractable(clazz)
                    override fun isDefault(): Boolean = data.interactableConfig.isDefault()

                    override fun setInteractable(clazz: Interactables, interactable: Boolean): Boolean =
                        data.interactableConfig.setInteractable(clazz, interactable).alsoIfTrue { updateInteractableConfigStorage() }

                    override fun clear(): Boolean =
                        data.interactableConfig.clear().alsoIfTrue { updateInteractableConfigStorage() }
                }
            }
            return _interactableConfig!!
        }
        set(value) {
            if (data.interactableConfig.copyFrom(value)) {
                updateInteractableConfigStorage()
            }
        }

    private var blockVisitors = AtomicInteger(0)

    override suspend fun withBlockVisitorPermit(block: suspend () -> Unit) {
        try {
            blockVisitors.getAndIncrement()
            block()
        } finally {
            blockVisitors.getAndDecrement()
        }
    }

    override fun toString() = toStringExt()
}

private operator fun Formatting.plus(other: Formatting) = toString() + other
private operator fun Formatting.plus(other: String) = toString() + other

private object ParcelInfoStringComputer {
    val infoStringColor1 = Formatting.GREEN
    val infoStringColor2 = Formatting.AQUA

    private inline fun StringBuilder.appendField(field: StringBuilder.() -> Unit, value: StringBuilder.() -> Unit) {
        append(infoStringColor1)
        field()
        append(": ")
        append(infoStringColor2)
        value()
        append(' ')
    }

    private inline fun StringBuilder.appendField(name: String, value: StringBuilder.() -> Unit) {
        appendField({ append(name) }, value)
    }

    private inline fun StringBuilder.appendFieldWithCount(name: String, count: Int, value: StringBuilder.() -> Unit) {
        appendField({
            append(name)
            append('(')
            append(infoStringColor2)
            append(count)
            append(infoStringColor1)
            append(')')
        }, value)
    }

    private fun StringBuilder.appendAddedList(local: PrivilegeMap, global: PrivilegeMap, privilege: Privilege, fieldName: String) {
        // local takes precedence over global

        val localFiltered = local.filterValues { it.isDistanceGrEq(privilege) }
        // global keys are dropped here when merged with the local ones
        val all = localFiltered + global.filterValues { it.isDistanceGrEq(privilege) }
        if (all.isEmpty()) return

        appendFieldWithCount(fieldName, all.size) {
            val separator = "$infoStringColor1, $infoStringColor2"

            // first [localCount] entries are local
            val localCount = localFiltered.size
            val iterator = all.iterator()

            if (localCount != 0) {
                appendPrivilegeEntry(false, iterator.next().toPair())
                repeat(localCount - 1) {
                    append(separator)
                    appendPrivilegeEntry(false, iterator.next().toPair())
                }

            } else if (iterator.hasNext()) {
                // ensure there is never a leading or trailing separator
                appendPrivilegeEntry(true, iterator.next().toPair())
            }

            iterator.forEach { next ->
                append(separator)
                appendPrivilegeEntry(true, next.toPair())
            }
        }
    }

    private fun StringBuilder.appendPrivilegeEntry(global: Boolean, pair: Pair<PrivilegeKey, Privilege>) {
        val (key, priv) = pair

        // prefix. Maybe T should be M for mod or something. T means they have CAN_MANAGE privilege.
        append(
            when {
                global && priv == CAN_MANAGE -> "(GT)"
                global -> "(G)"
                priv == CAN_MANAGE -> "(T)"
                else -> ""
            }
        )

        append(key.notNullName)
    }

    fun getInfoString(parcel: Parcel): String = buildString {
        appendField("ID") {
            append(parcel.x)
            append(',')
            append(parcel.z)
        }

        val owner = parcel.owner
        appendField("Owner") {
            if (owner == null) {
                append(infoStringColor1)
                append("none")
            } else {
                append(owner.notNullName)
            }
        }

        // plotme appends biome here

        append('\n')

        val local = parcel.privilegeMap
        val global = parcel.globalPrivileges?.privilegeMap ?: emptyMap()
        appendAddedList(local, global, CAN_BUILD, "Allowed") // includes CAN_MANAGE privilege
        append('\n')
        appendAddedList(local, global, BANNED, "Banned")

        if (!parcel.interactableConfig.isDefault()) {
            val interactables = parcel.interactableConfig.interactableClasses
            appendFieldWithCount("Interactables", interactables.size) {
                interactables.asSequence().map { it.name }.joinTo(this)
            }
        }

    }
}