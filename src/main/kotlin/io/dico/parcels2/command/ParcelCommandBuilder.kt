package io.dico.parcels2.command

import io.dico.dicore.command.CommandBuilder
import io.dico.dicore.command.CommandException
import io.dico.dicore.command.ICommandDispatcher
import io.dico.dicore.command.parameter.ArgumentBuffer
import io.dico.dicore.command.parameter.IParameter
import io.dico.dicore.command.parameter.type.ParameterType
import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.Worlds
import io.dico.parcels2.debugging
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Suppress("UsePropertyAccessSyntax")
fun getParcelCommands(plugin: ParcelsPlugin): ICommandDispatcher {
    //@formatter:off
    return CommandBuilder()
        .addParameterType(false, ParcelParameterType(plugin.worlds))
        .group("parcel", "plot", "plots", "p")
            .registerCommands(ParcelCommands(plugin))
            .putDebugCommands(plugin)
            .parent()
        .getDispatcher()
    //@formatter:on
}

private fun CommandBuilder.putDebugCommands(plugin: ParcelsPlugin): CommandBuilder {
    if (!debugging) return this
    //@formatter:off
    return group("debug", "d")
        .registerCommands(DebugCommands(plugin))
        .parent()
    //@formatter:on
}

private val regex = Regex.fromLiteral("((.+)->)?([0-9]+):([0-9]+)")

private class ParcelParameterType(val worlds: Worlds) : ParameterType<Parcel, Unit>(Parcel::class.java) {

    private fun invalidInput(parameter: IParameter<*>, message: String): Nothing {
        throw CommandException("invalid input for ${parameter.name}: $message")
    }

    override fun parse(parameter: IParameter<Parcel>, sender: CommandSender, buffer: ArgumentBuffer): Parcel {
        val matchResult = regex.matchEntire(buffer.next())
            ?: invalidInput(parameter, "must match (w->)?a:b (/${regex.pattern}/)")

        val worldName = matchResult.groupValues[2]
            .takeUnless { it.isEmpty() }
            ?: (sender as? Player)?.world?.name
            ?: invalidInput(parameter, "console cannot omit the world name")

        val world = worlds.getWorld(worldName)
            ?: invalidInput(parameter, "$worldName is not a parcel world")

        val x = matchResult.groupValues[3].toIntOrNull()
            ?: invalidInput(parameter, "couldn't parse int")

        val z = matchResult.groupValues[4].toIntOrNull()
            ?: invalidInput(parameter, "couldn't parse int")

        return world.parcelByID(x, z)
            ?: invalidInput(parameter, "parcel id is out of range")
    }

}
