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
            .registerCommands(ParcelCommands(plugin))
            .registerCommands(ParcelAddCommands(plugin))

            .group("option")
                .apply { ParcelOptionCommands.setGroupDescription(this) }
                .registerCommands(ParcelOptionCommands(plugin))
                .parent()

            .group("admin", "a")
                .registerCommands(ParcelAdminCommands(plugin))
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
        .registerCommands(DebugCommands(plugin))
        .parent()
    //@formatter:on
}
