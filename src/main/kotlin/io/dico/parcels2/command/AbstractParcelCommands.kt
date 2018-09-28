package io.dico.parcels2.command

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.EMessageType
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.ICommandReceiver
import io.dico.parcels2.*
import io.dico.parcels2.PlayerProfile.Real
import io.dico.parcels2.PlayerProfile.Unresolved
import io.dico.parcels2.util.ext.hasPermAdminManage
import io.dico.parcels2.util.ext.parcelLimit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.reflect.Method

abstract class AbstractParcelCommands(val plugin: ParcelsPlugin) : ICommandReceiver.Factory {
    override fun getPlugin(): Plugin = plugin

    override fun getReceiver(context: ExecutionContext, target: Method, cmdName: String): ICommandReceiver {
        return getParcelCommandReceiver(plugin.parcelProvider, context, target, cmdName)
    }

    protected fun checkConnected(action: String) {
        if (!plugin.storage.isConnected) err("Parcels cannot $action right now because of a database error")
    }

    protected suspend fun checkParcelLimit(player: Player, world: ParcelWorld) {
        if (player.hasPermAdminManage) return
        val numOwnedParcels = plugin.storage.getOwnedParcels(PlayerProfile(player)).await()
            .filter { it.worldId.equals(world.id) }.size

        val limit = player.parcelLimit
        if (numOwnedParcels >= limit) {
            err("You have enough plots for now")
        }
    }

    protected suspend fun toPrivilegeKey(profile: PlayerProfile): PrivilegeKey = when (profile) {
        is Real -> profile
        is Unresolved -> profile.tryResolveSuspendedly(plugin.storage)
            ?: throw CommandException()
        else -> throw CommandException()
    }

    protected fun areYouSureMessage(context: ExecutionContext): String {
        val command = (context.route + context.original).joinToString(" ") + " -sure"
        return "Are you sure? You cannot undo this action!\n" +
            "Run \"/$command\" if you want to go through with this."
    }

    protected fun Job.reportProgressUpdates(context: ExecutionContext, action: String): Job =
        onProgressUpdate(1000, 1000) { progress, elapsedTime ->
            val alt = context.getFormat(EMessageType.NUMBER)
            val main = context.getFormat(EMessageType.INFORMATIVE)
            context.sendMessage(
                EMessageType.INFORMATIVE, false, "$action progress: $alt%.02f$main%%, $alt%.2f${main}s elapsed"
                    .format(progress * 100, elapsedTime / 1000.0)
            )
        }

    override fun getCoroutineContext() = plugin.coroutineContext
}

fun err(message: String): Nothing = throw CommandException(message)