package io.dico.parcels2.command

import io.dico.dicore.command.CommandBuilder
import io.dico.dicore.command.CommandException
import io.dico.dicore.command.ICommandDispatcher
import io.dico.dicore.command.parameter.ArgumentBuffer
import io.dico.dicore.command.parameter.IParameter
import io.dico.dicore.command.parameter.type.ParameterType
import io.dico.parcels2.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Suppress("UsePropertyAccessSyntax")
fun getParcelCommands(plugin: ParcelsPlugin): ICommandDispatcher {
    //@formatter:off
    return CommandBuilder()
        .addParameterType(false, ParcelParameterType(plugin.worlds))
        .addParameterType(false, ParcelHomeParameterType(plugin.worlds))
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

private fun invalidInput(parameter: IParameter<*>, message: String): Nothing {
    throw CommandException("invalid input for ${parameter.name}: $message")
}

private fun Worlds.getTargetWorld(input: String?, sender: CommandSender, parameter: IParameter<*>): ParcelWorld {
    val worldName = input
        ?.takeUnless { it.isEmpty() }
        ?: (sender as? Player)?.world?.name
        ?: invalidInput(parameter, "console cannot omit the world name")

    return getWorld(worldName)
        ?: invalidInput(parameter, "$worldName is not a parcel world")
}

private class ParcelParameterType(val worlds: Worlds) : ParameterType<Parcel, Unit>(Parcel::class.java) {
    val regex = Regex.fromLiteral("((.+)->)?([0-9]+):([0-9]+)")

    override fun parse(parameter: IParameter<Parcel>, sender: CommandSender, buffer: ArgumentBuffer): Parcel {
        val matchResult = regex.matchEntire(buffer.next())
            ?: invalidInput(parameter, "must match (w->)?a:b (/${regex.pattern}/)")

        val world = worlds.getTargetWorld(matchResult.groupValues[2], sender, parameter)

        val x = matchResult.groupValues[3].toIntOrNull()
            ?: invalidInput(parameter, "couldn't parse int")

        val z = matchResult.groupValues[4].toIntOrNull()
            ?: invalidInput(parameter, "couldn't parse int")

        return world.parcelByID(x, z)
            ?: invalidInput(parameter, "parcel id is out of range")
    }

}

class NamedParcelTarget(val world: ParcelWorld, val player: OfflinePlayer, val index: Int)

private class ParcelHomeParameterType(val worlds: Worlds) : ParameterType<NamedParcelTarget, Unit>(NamedParcelTarget::class.java) {
    val regex = Regex.fromLiteral("((.+)->)?(.+)|((.+):([0-9]+))")

    private fun requirePlayer(sender: CommandSender, parameter: IParameter<*>): Player {
        if (sender !is Player) invalidInput(parameter, "console cannot omit the player name")
        return sender
    }

    @Suppress("UsePropertyAccessSyntax")
    private fun getOfflinePlayer(input: String, parameter: IParameter<*>) = Bukkit.getOfflinePlayer(input)
        ?.takeIf { it.isOnline() || it.hasPlayedBefore() }
        ?: invalidInput(parameter, "do not know who $input is")

    override fun parse(parameter: IParameter<NamedParcelTarget>, sender: CommandSender, buffer: ArgumentBuffer): NamedParcelTarget {
        val matchResult = regex.matchEntire(buffer.next())
            ?: invalidInput(parameter, "must be a player, index, or player:index (/${regex.pattern}/)")

        val world = worlds.getTargetWorld(matchResult.groupValues[2], sender, parameter)

        matchResult.groupValues[3].takeUnless { it.isEmpty() }?.let {
            // first group was matched, it's a player or an int
            it.toIntOrNull()?.let {
                requirePlayer(sender, parameter)
                return NamedParcelTarget(world, sender as Player, it)
            }

            return NamedParcelTarget(world, getOfflinePlayer(it, parameter), 0)
        }

        val player = getOfflinePlayer(matchResult.groupValues[5], parameter)
        val index = matchResult.groupValues[6].toIntOrNull()
            ?: invalidInput(parameter, "couldn't parse int")

        return NamedParcelTarget(world, player, index)
    }

    override fun getDefaultValue(parameter: IParameter<NamedParcelTarget>, sender: CommandSender, buffer: ArgumentBuffer): NamedParcelTarget {
        val world = worlds.getTargetWorld(null, sender, parameter)
        val player = requirePlayer(sender, parameter)
        return NamedParcelTarget(world, player, 0)
    }

}
