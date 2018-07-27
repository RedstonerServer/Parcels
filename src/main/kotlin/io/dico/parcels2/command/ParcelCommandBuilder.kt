package io.dico.parcels2.command

import io.dico.dicore.command.CommandBuilder
import io.dico.dicore.command.ICommandDispatcher
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.logger

@Suppress("UsePropertyAccessSyntax")
fun getParcelCommands(plugin: ParcelsPlugin): ICommandDispatcher {
    //@formatter:off
    return CommandBuilder()
        .setChatController(ParcelsChatController())
        .addParameterType(false, ParcelParameterType(plugin.worlds))
        .addParameterType(true, ParcelHomeParameterType(plugin.worlds))

        .group("parcel", "plot", "plots", "p")
            .registerCommands(CommandsGeneral(plugin))
            .registerCommands(CommandsAddedStatus(plugin))

            .group("option")
                .apply { CommandsParcelOptions.setGroupDescription(this) }
                .registerCommands(CommandsParcelOptions(plugin))
                .parent()

            .group("admin", "a")
                .registerCommands(CommandsAdmin(plugin))
                .parent()

            .putDebugCommands(plugin)

            .parent()
        .getDispatcher()
    //@formatter:on
}

private fun CommandBuilder.putDebugCommands(plugin: ParcelsPlugin): CommandBuilder {
    if (!logger.isDebugEnabled) return this
    //@formatter:off
    return group("debug", "d")
        .registerCommands(CommandsDebug(plugin))
        .parent()
    //@formatter:on
}
