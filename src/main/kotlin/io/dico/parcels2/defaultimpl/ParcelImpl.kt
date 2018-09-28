package io.dico.parcels2.defaultimpl

import io.dico.parcels2.*
import io.dico.parcels2.Privilege.*
import io.dico.parcels2.util.ext.alsoIfTrue
import io.dico.parcels2.util.isServerThread
import io.dico.parcels2.util.math.Vec2i
import org.bukkit.Material
import org.joda.time.DateTime
import java.lang.IllegalStateException

class ParcelImpl (
    override val world: ParcelWorld,
    override val x: Int,
    override val z: Int
) : Parcel, ParcelId {
    override val id: ParcelId get() = this
    override val pos get() = Vec2i(x, z)
    override var data = ParcelDataHolder(); private set
    override val worldId: ParcelWorldId get() = world.id

    override fun copyData(newData: ParcelDataHolder, callerIsDatabase: Boolean) {
        if (callerIsDatabase) {
            data = newData
            return
        }

        val ownerChanged = owner != newData.owner
        data = newData

        if (ownerChanged) {
            updateOwnerSign(true, false, false)
        }

        world.storage.setParcelData(this, data)
    }

    override var owner: PlayerProfile?
        get() = data.owner
        set(value) {
            if (data.owner != value) {
                world.storage.setParcelOwner(this, value)
                data.owner = value
                updateOwnerSign(true, false, true)
            }
        }

    override val lastClaimTime: DateTime?
        get() = data.lastClaimTime

    override var isOwnerSignOutdated: Boolean
        get() = data.isOwnerSignOutdated
        set(value) {
            if (data.isOwnerSignOutdated != value) {
                world.storage.setParcelOwnerSignOutdated(this, value)
                data.isOwnerSignOutdated = value
            }
        }

    override fun updateOwnerSign(force: Boolean) {
        updateOwnerSign(false, force, true)
    }

    private fun updateOwnerSign(ownerChanged: Boolean, force: Boolean, updateDatabase: Boolean) {
        if (!ownerChanged && !isOwnerSignOutdated && !force) return

        val update = force || world.blockManager.isParcelInfoSectionLoaded(this)
        if (update) world.blockManager.updateParcelInfo(this, owner)

        if (updateDatabase) isOwnerSignOutdated = !update
        else data.isOwnerSignOutdated = !update
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

    override fun hasAnyDeclaredPrivileges() = data.hasAnyDeclaredPrivileges()

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

    override val hasBlockVisitors: Boolean
        get() = permit != null

    private var permit: Permit? = null

    fun acquireBlockVisitorPermit(with: Permit): Boolean {
        if (permit === with) return true
        if (permit != null) return false
        permit = with
        return true
    }

    fun releaseBlockVisitorPermit(with: Permit) {
        if (permit !== with) throw IllegalStateException()
        permit = null
    }

    /*
    private var blockVisitors = AtomicInteger(0)

    override suspend fun withBlockVisitorPermit(block: suspend () -> Unit) {
        try {
            blockVisitors.getAndIncrement()
            block()
        } finally {
            blockVisitors.getAndDecrement()
        }
    }*/

    override fun toString() = parcelIdToString()

    override val infoString: String
        get() = getInfoString()
}

private fun Parcel.getInfoString() = StringBuilder().apply {
    with(InfoBuilder) {
        appendField("ID") {
            append(x)
            append(',')
            append(z)
        }

        val owner = owner
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

        val local: RawPrivileges = data
        val global = globalPrivileges
        appendProfilesWithPrivilege("Allowed", local, global, CAN_BUILD) // includes CAN_MANAGE privilege
        append('\n')
        appendProfilesWithPrivilege("Banned", local, global, BANNED)

        if (!interactableConfig.isDefault()) {
            val interactables = interactableConfig.interactableClasses
            appendFieldWithCount("Interactables", interactables.size) {
                interactables.asSequence().map { it.name }.joinTo(this)
            }
        }
    }
}.toString()
