package io.dico.parcels2.command

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.EMessageType
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.ICommandReceiver
import io.dico.parcels2.ParcelWorld
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.PlayerProfile
import io.dico.parcels2.util.ext.hasAdminManage
import io.dico.parcels2.util.ext.parcelLimit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.reflect.Method

abstract class AbstractParcelCommands(val plugin: ParcelsPlugin) : ICommandReceiver.Factory {
    override fun getPlugin(): Plugin = plugin

    override fun getReceiver(context: ExecutionContext, target: Method, cmdName: String): ICommandReceiver {
        return getParcelCommandReceiver(plugin.parcelProvider, context, target, cmdName)
    }

    protected fun error(message: String): Nothing {
        throw CommandException(message)
    }

    protected fun checkConnected(action: String) {
        if (!plugin.storage.isConnected) error("Parcels cannot $action right now because of a database error")
    }

    protected suspend fun checkParcelLimit(player: Player, world: ParcelWorld) {
        if (player.hasAdminManage) return
        val numOwnedParcels = plugin.storage.getOwnedParcels(PlayerProfile(player)).await()
            .filter { it.worldId.equals(world.id) }.size

        val limit = player.parcelLimit
        if (numOwnedParcels >= limit) {
            error("You have enough plots for now")
        }
    }

    protected fun areYouSureMessage(context: ExecutionContext) = "Are you sure? You cannot undo this action!\n" +
        "Run \"/${context.route.joinToString(" ")} -sure\" if you want to go through with this."

    protected fun ParcelScope.clearWithProgressUpdates(context: ExecutionContext, action: String) {
        world.blockManager.clearParcel(parcel.id)
            .onProgressUpdate(1000, 1000) { progress, elapsedTime ->
                val alt = context.getFormat(EMessageType.NUMBER)
                val main = context.getFormat(EMessageType.INFORMATIVE)
                context.sendMessage(
                    EMessageType.INFORMATIVE, false, "$action progress: $alt%.02f$main%%, $alt%.2f${main}s elapsed"
                        .format(progress * 100, elapsedTime / 1000.0)
                )
            }
    }

    override fun getCoroutineContext() = plugin.coroutineContext
}

