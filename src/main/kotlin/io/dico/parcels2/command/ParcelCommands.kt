package io.dico.parcels2.command

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.parcels2.ParcelOwner
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.util.parcelLimit
import io.dico.parcels2.util.uuid
import org.bukkit.entity.Player

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
                player.teleport(world.generator.getHomeLocation(parcel))
                "Enjoy your new parcel!"
            }
        }
    }

    @Cmd("info", aliases = ["i"])
    @Desc("Displays general information",
        "about the parcel you're on",
        shortVersion = "displays information about this parcel")
    fun cmdInfo(player: Player) = requireInParcel(player) { parcel.infoString }


}