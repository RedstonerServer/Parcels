package io.dico.parcels2.defaultimpl

import io.dico.dicore.Formatting
import io.dico.parcels2.*
import io.dico.parcels2.Privilege.*
import io.dico.parcels2.util.Vec2i
import io.dico.parcels2.util.ext.alsoIfTrue
import org.bukkit.Material
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


    override var owner: PlayerProfile?
        get() = data.owner
        set(value) {
            if (data.owner != value) {
                world.storage.setParcelOwner(this, value)
                world.blockManager.setOwnerBlock(this, value)
                data.owner = value
            }
        }

    override val lastClaimTime: DateTime?
        get() = data.lastClaimTime

    override var ownerSignOutdated: Boolean
        get() = data.ownerSignOutdated
        set(value) {
            if (data.ownerSignOutdated != value) {
                world.storage.setParcelOwnerSignOutdated(this, value)
                data.ownerSignOutdated = value
            }
        }


    override val privilegeMap: PrivilegeMap
        get() = data.privilegeMap

    override val globalPrivileges: GlobalPrivileges?
        get() = keyOfOwner?.let { world.globalPrivileges[it] }

    override fun getRawStoredPrivilege(key: PrivilegeKey) = data.getRawStoredPrivilege(key)

    override fun setRawStoredPrivilege(key: PrivilegeKey, privilege: Privilege) =
        data.setRawStoredPrivilege(key, privilege).alsoIfTrue {
            world.storage.setLocalPrivilege(this, key, privilege)
        }

    override fun getStoredPrivilege(key: PrivilegeKey): Privilege =
        super.getStoredPrivilege(key).takeIf { it != DEFAULT }
            ?: globalPrivileges?.getStoredPrivilege(key)
            ?: DEFAULT


    private var _interactableConfig: InteractableConfiguration? = null

    private fun updateInteractableConfigStorage() {
        world.storage.setParcelOptionsInteractConfig(this, data.interactableConfig)
    }

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

    private fun processPrivileges(local: RawPrivileges, global: RawPrivileges?,
                                  privilege: Privilege): Pair<LinkedHashMap<PrivilegeKey, Privilege>, Int> {
        val map = linkedMapOf<PrivilegeKey, Privilege>()
        local.privilegeOfStar.takeIf { it != DEFAULT }?.let { map[PlayerProfile.Star] = it }
        map.values.retainAll { it.isDistanceGrEq(privilege) }
        val localCount = map.size

        if (global != null) {
            global.privilegeMap.forEach {
                if (it.value.isDistanceGrEq(privilege))
                    map.putIfAbsent(it.key, it.value)
            }

            global.privilegeOfStar.takeIf { it != DEFAULT && it.isDistanceGrEq(privilege) }
                ?.let { map.putIfAbsent(PlayerProfile.Star, it) }
        }

        return map to localCount
    }

    private fun StringBuilder.appendAddedList(local: RawPrivileges, global: RawPrivileges?, privilege: Privilege, fieldName: String) {
        val (map, localCount) = processPrivileges(local, global, privilege)
        if (map.isEmpty()) return

        appendFieldWithCount(fieldName, map.size) {
            // first [localCount] entries are local
            val separator = "$infoStringColor1, $infoStringColor2"
            val iterator = map.iterator()

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

        append(key.notNullName)

        // suffix. Maybe T should be M for mod or something. T means they have CAN_MANAGE privilege.
        append(
            when {
                global && priv == CAN_MANAGE -> " (G) (T)"
                global -> " (G)"
                priv == CAN_MANAGE -> " (T)"
                else -> ""
            }
        )
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

        val local: RawPrivileges = parcel.data
        val global = parcel.globalPrivileges
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