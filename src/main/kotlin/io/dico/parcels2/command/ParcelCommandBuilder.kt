package io.dico.parcels2.command

import io.dico.dicore.command.CommandBuilder
import io.dico.dicore.command.ICommandAddress
import io.dico.dicore.command.ICommandDispatcher
import io.dico.dicore.command.predef.PredefinedCommand
import io.dico.dicore.command.registration.reflect.ReflectiveRegistration
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.logger
import java.util.*

@Suppress("UsePropertyAccessSyntax")
fun getParcelCommands(plugin: ParcelsPlugin): ICommandDispatcher {
    //@formatter:off
    return CommandBuilder()
        .setChatController(ParcelsChatController())
        .addParameterType(false, ParcelParameterType(plugin.worlds))
        .addParameterType(true, ParcelTarget.PType(plugin.worlds))

        .group("parcel", "plot", "plots", "p")
            .registerCommands(CommandsGeneral(plugin))
            .registerCommands(CommandsAddedStatus(plugin))

            .group("option")
                //.apply { CommandsParcelOptions.setGroupDescription(this) }
                .registerCommands(CommandsParcelOptions(plugin))
                .parent()

            .group("admin", "a")
                .registerCommands(CommandsAdmin(plugin))
                .parent()

            .putDebugCommands(plugin)

            .parent()
        .generateHelpAndSyntaxCommands()
        .getDispatcher()
    //@formatter:on
}

private fun CommandBuilder.putDebugCommands(plugin: ParcelsPlugin): CommandBuilder {
    //if (!logger.isDebugEnabled) return this
    //@formatter:off
    return group("debug", "d")
        .registerCommands(CommandsDebug(plugin))
        .parent()
    //@formatter:on
}

private fun CommandBuilder.generateHelpAndSyntaxCommands(): CommandBuilder {
    generateCommands(dispatcher as ICommandAddress, "help", "syntax")
    return this
}

private fun generateCommands(address: ICommandAddress, vararg names: String) {
    val addresses: Queue<ICommandAddress> = LinkedList()
    addresses.offer(address)

    while (addresses.isNotEmpty()) {
        val cur = addresses.poll()
        addresses.addAll(cur.children.values.distinct())
        if (cur.hasCommand()) {
            ReflectiveRegistration.generateCommands(cur, names)
        }
    }
}
