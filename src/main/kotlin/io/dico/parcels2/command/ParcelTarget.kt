package io.dico.parcels2.command

import io.dico.dicore.command.parameter.ArgumentBuffer
import io.dico.dicore.command.parameter.Parameter
import io.dico.dicore.command.parameter.type.ParameterConfig
import io.dico.dicore.command.parameter.type.ParameterType
import io.dico.parcels2.*
import io.dico.parcels2.storage.Storage
import io.dico.parcels2.util.Vec2i
import io.dico.parcels2.util.floor
import kotlinx.coroutines.experimental.Deferred
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

sealed class ParcelTarget(val world: ParcelWorld, val parsedKind: Int, val isDefault: Boolean) {

    abstract suspend fun getParcelSuspend(storage: Storage): Parcel?

    fun ParcelsPlugin.getParcelDeferred(): Deferred<Parcel?> = functionHelper.deferUndispatchedOnMainThread { getParcelSuspend(storage) }

    class ByID(world: ParcelWorld, val id: Vec2i?, parsedKind: Int, isDefault: Boolean) : ParcelTarget(world, parsedKind, isDefault) {
        override suspend fun getParcelSuspend(storage: Storage): Parcel? = getParcel()
        fun getParcel() = id?.let { world.getParcelById(it) }
        val isPath: Boolean get() = id == null
    }

    class ByOwner(world: ParcelWorld,
                  owner: PlayerProfile,
                  val index: Int,
                  parsedKind: Int,
                  isDefault: Boolean,
                  val onResolveFailure: (() -> Unit)? = null) : ParcelTarget(world, parsedKind, isDefault) {
        init {
            if (index < 0) throw IllegalArgumentException("Invalid parcel home index: $index")
        }

        var owner = owner; private set

        suspend fun resolveOwner(storage: Storage): Boolean {
            val owner = owner
            if (owner is PlayerProfile.Unresolved) {
                this.owner = owner.tryResolveSuspendedly(storage) ?: if (parsedKind and OWNER_FAKE != 0) PlayerProfile.Fake(owner.name)
                else run { onResolveFailure?.invoke(); return false }
            }
            return true
        }

        override suspend fun getParcelSuspend(storage: Storage): Parcel? {
            onResolveFailure?.let { resolveOwner(storage) }

            val ownedParcelsSerialized = storage.getOwnedParcels(owner).await()
            val ownedParcels = ownedParcelsSerialized
                .filter { it.worldId.equals(world.id) }
                .map { world.getParcelById(it.x, it.z) }

            return ownedParcels.getOrNull(index)
        }
    }

    annotation class Kind(val kind: Int)

    companion object Config : ParameterConfig<Kind, Int>(Kind::class.java) {
        override fun toParameterInfo(annotation: Kind): Int {
            return annotation.kind
        }

        const val ID = 1 // ID
        const val OWNER_REAL = 2 // an owner backed by a UUID
        const val OWNER_FAKE = 4 // an owner not backed by a UUID

        const val OWNER = OWNER_REAL or OWNER_FAKE // any owner
        const val ANY = ID or OWNER_REAL or OWNER_FAKE // any
        const val REAL = ID or OWNER_REAL // no owner not backed by a UUID

        const val DEFAULT_KIND = REAL

        const val PREFER_OWNED_FOR_DEFAULT = 8 // if the kind can be ID and OWNER_REAL, prefer OWNER_REAL for default
        // instead of parcel that the player is in
    }

    class PType(val parcelProvider: ParcelProvider) : ParameterType<ParcelTarget, Int>(ParcelTarget::class.java, ParcelTarget.Config) {

        override fun parse(parameter: Parameter<ParcelTarget, Int>, sender: CommandSender, buffer: ArgumentBuffer): ParcelTarget {
            var input = buffer.next()
            val worldString = input.substringBefore("->", missingDelimiterValue = "")
            input = input.substringAfter("->")

            val world = if (worldString.isEmpty()) {
                val player = requirePlayer(sender, parameter, "the world")
                parcelProvider.getWorld(player.world)
                    ?: invalidInput(parameter, "You cannot omit the world if you're not in a parcel world")
            } else {
                parcelProvider.getWorld(worldString) ?: invalidInput(parameter, "$worldString is not a parcel world")
            }

            val kind = parameter.paramInfo ?: DEFAULT_KIND
            if (input.contains(',')) {
                if (kind and ID == 0) invalidInput(parameter, "You must specify a parcel by OWNER, that is, an owner and index")
                return ByID(world, getId(parameter, input), kind, false)
            }

            if (kind and OWNER == 0) invalidInput(parameter, "You must specify a parcel by ID, that is, the x and z component separated by a comma")
            val (owner, index) = getHomeIndex(parameter, kind, sender, input)
            return ByOwner(world, owner, index, kind, false, onResolveFailure = { invalidInput(parameter, "The player $input does not exist") })
        }

        private fun getId(parameter: Parameter<*, *>, input: String): Vec2i {
            val x = input.substringBefore(',').run {
                toIntOrNull() ?: invalidInput(parameter, "ID(x) must be an integer, $this is not an integer")
            }
            val z = input.substringAfter(',').run {
                toIntOrNull() ?: invalidInput(parameter, "ID(z) must be an integer, $this is not an integer")
            }
            return Vec2i(x, z)
        }

        private fun getHomeIndex(parameter: Parameter<*, *>, kind: Int, sender: CommandSender, input: String): Pair<PlayerProfile, Int> {
            val splitIdx = input.indexOf(':')
            val ownerString: String
            val indexString: String

            if (splitIdx == -1) {
                // just the index.
                ownerString = ""
                indexString = input
            } else {
                ownerString = input.substring(0, splitIdx)
                indexString = input.substring(splitIdx + 1)
            }

            val owner = if (ownerString.isEmpty())
                PlayerProfile(requirePlayer(sender, parameter, "the player"))
            else
                PlayerProfile.byName(ownerString, allowReal = kind and OWNER_REAL != 0, allowFake = kind and OWNER_FAKE != 0)

            val index = if (indexString.isEmpty()) 0 else indexString.toIntOrNull()
                ?: invalidInput(parameter, "The home index must be an integer, $indexString is not an integer")

            return owner to index
        }

        private fun requirePlayer(sender: CommandSender, parameter: Parameter<*, *>, objName: String): Player {
            if (sender !is Player) invalidInput(parameter, "console cannot omit the $objName")
            return sender
        }

        override fun getDefaultValue(parameter: Parameter<ParcelTarget, Int>, sender: CommandSender, buffer: ArgumentBuffer): ParcelTarget? {
            val kind = parameter.paramInfo ?: DEFAULT_KIND
            val useLocation = when {
                kind and REAL == REAL -> kind and PREFER_OWNED_FOR_DEFAULT == 0
                kind and ID != 0 -> true
                kind and OWNER_REAL != 0 -> false
                else -> return null
            }

            val player = requirePlayer(sender, parameter, "the parcel")
            val world = parcelProvider.getWorld(player.world) ?: invalidInput(parameter, "You must be in a parcel world to omit the parcel")
            if (useLocation) {
                val id = player.location.let { world.getParcelIdAt(it.x.floor(), it.z.floor())?.pos }
                return ByID(world, id, kind, true)
            }

            return ByOwner(world, PlayerProfile(player), 0, kind, true)
        }
    }

}
