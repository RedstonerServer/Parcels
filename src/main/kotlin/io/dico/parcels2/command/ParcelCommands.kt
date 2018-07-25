package io.dico.parcels2.command

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.dicore.command.annotation.RequireParameters
import io.dico.parcels2.ParcelOwner
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.storage.getParcelBySerializedValue
import io.dico.parcels2.util.hasParcelHomeOthers
import io.dico.parcels2.util.parcelLimit
import io.dico.parcels2.util.uuid
import org.bukkit.entity.Player

@Suppress("unused")
class ParcelCommands(override val plugin: ParcelsPlugin) : HasWorlds, HasPlugin {
    override val worlds = plugin.worlds

    private fun error(message: String): Nothing {
        throw CommandException(message)
    }

    @Cmd("auto")
    @Desc("Finds the unclaimed parcel nearest to origin,",
        "and gives it to you",
        shortVersion = "sets you up with a fresh, unclaimed parcel")
    fun cmdAuto(player: Player, context: ExecutionContext) = requireInWorld(player) {
        delegateCommandAsync(context) {
            val numOwnedParcels = plugin.storage.getNumParcels(ParcelOwner(uuid = player.uuid)).await()

            awaitSynchronousTask {
                val limit = player.parcelLimit

                if (numOwnedParcels >= limit) {
                    error("You have enough plots for now")
                }

                val parcel = world.nextEmptyParcel()
                    ?: error("This world is full, please ask an admin to upsize it")
                parcel.owner = ParcelOwner(uuid = player.uuid)
                player.teleport(parcel.homeLocation)
                "Enjoy your new parcel!"
            }
        }
    }

    @Cmd("info", aliases = ["i"])
    @Desc("Displays general information",
        "about the parcel you're on",
        shortVersion = "displays information about this parcel")
    fun cmdInfo(player: Player) = requireInParcel(player) { parcel.infoString }

    @Cmd("home", aliases = ["h"])
    @Desc("Teleports you to your parcels,",
        "unless another player was specified.",
        "You can specify an index number if you have",
        "more than one parcel",
        shortVersion = "teleports you to parcels")
    @RequireParameters(0)
    fun cmdHome(player: Player, context: ExecutionContext, target: NamedParcelTarget) {
        if (player !== target.player && !player.hasParcelHomeOthers) {
            error("You do not have permission to teleport to other people's parcels")
        }

        return delegateCommandAsync(context) {
            val ownedParcelsResult = plugin.storage.getOwnedParcels(ParcelOwner(uuid = target.player.uuid)).await()
            awaitSynchronousTask {
                val uuid = target.player.uuid
                val ownedParcels = ownedParcelsResult
                    .map { worlds.getParcelBySerializedValue(it) }
                    .filter { it != null && it.world == target.world && it.owner?.uuid == uuid }

                val targetMatch = ownedParcels.getOrNull(target.index)
                    ?: error("The specified parcel could not be matched")

                player.teleport(targetMatch.homeLocation)
                ""
            }
        }
    }

    @Cmd("claim")
    @Desc("If this parcel is unowned, makes you the owner",
        shortVersion = "claims this parcel")
    fun cmdClaim(player: Player) {

    }




}