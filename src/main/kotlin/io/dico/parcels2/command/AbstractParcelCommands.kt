package io.dico.parcels2.command

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.ICommandReceiver
import io.dico.parcels2.PlayerProfile
import io.dico.parcels2.ParcelWorld
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.util.hasAdminManage
import io.dico.parcels2.util.parcelLimit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.reflect.Method

abstract class AbstractParcelCommands(val plugin: ParcelsPlugin) : ICommandReceiver.Factory {

    override fun getPlugin(): Plugin = plugin
    override fun getReceiver(context: ExecutionContext, target: Method, cmdName: String): ICommandReceiver {
        return getParcelCommandReceiver(plugin.parcelProvider, context, target, cmdName)
    }

    protected inline val worlds get() = plugin.parcelProvider

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

}

