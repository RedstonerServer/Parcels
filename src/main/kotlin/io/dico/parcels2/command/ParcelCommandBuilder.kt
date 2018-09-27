package io.dico.parcels2.command

import io.dico.dicore.command.*
import io.dico.dicore.command.predef.DefaultGroupCommand
import io.dico.dicore.command.registration.reflect.ReflectiveRegistration
import io.dico.parcels2.Interactables
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.logger
import io.dico.parcels2.util.ext.hasPermAdminManage
import org.bukkit.command.CommandSender
import java.util.LinkedList
import java.util.Queue

@Suppress("UsePropertyAccessSyntax")
fun getParcelCommands(plugin: ParcelsPlugin): ICommandDispatcher = CommandBuilder().apply {
    val parcelsAddress = SpecialCommandAddress()

    setChatController(ParcelsChatController())
    addParameterType(false, ParcelParameterType(plugin.parcelProvider))
    addParameterType(false, ProfileParameterType())
    addParameterType(true, ParcelTarget.PType(plugin.parcelProvider, parcelsAddress))

    group(parcelsAddress, "parcel", "plot", "plots", "p") {
        addContextFilter(IContextFilter.inheritablePermission("parcels.command"))
        registerCommands(CommandsGeneral(plugin, parcelsAddress))
        registerCommands(CommandsPrivilegesLocal(plugin))

        group("option", "opt", "o") {
            setGroupDescription(
                "changes interaction options for this parcel",
                "Sets whether players who are not allowed to",
                "build here can interact with certain things."
            )

            group("interact", "i") {
                val command = ParcelOptionsInteractCommand(plugin)
                Interactables.classesById.forEach {
                    addSubCommand(it.name, command)
                }
            }
        }

        val adminPrivilegesGlobal = CommandsAdminPrivilegesGlobal(plugin)

        group("global", "g") {
            registerCommands(CommandsPrivilegesGlobal(plugin, adminVersion = adminPrivilegesGlobal))
        }

        group("admin", "a") {
            setCommand(AdminGroupCommand())
            registerCommands(CommandsAdmin(plugin))

            group("global", "g") {
                registerCommands(adminPrivilegesGlobal)
            }
        }

        if (!logger.isDebugEnabled) return@group

        group("debug", "d") {
            registerCommands(CommandsDebug(plugin))
        }
    }

    generateHelpAndSyntaxCommands(parcelsAddress)
}.getDispatcher()

private inline fun CommandBuilder.group(name: String, vararg aliases: String, config: CommandBuilder.() -> Unit) {
    group(name, *aliases)
    config()
    parent()
}

private inline fun CommandBuilder.group(address: ICommandAddress, name: String, vararg aliases: String, config: CommandBuilder.() -> Unit) {
    group(address, name, *aliases)
    config()
    parent()
}

private fun CommandBuilder.generateHelpAndSyntaxCommands(root: ICommandAddress): CommandBuilder {
    generateCommands(root, "help", "syntax")
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

class SpecialCommandAddress : ChildCommandAddress() {
    private val speciallyTreatedKeys = mutableListOf<String>()

    // Used to allow /p h:1 syntax, which is the same as what PlotMe uses.
    var speciallyParsedIndex: Int? = null; private set

    fun addSpeciallyTreatedKeys(vararg keys: String) {
        for (key in keys) {
            speciallyTreatedKeys.add(key + ":")
        }
    }

    @Throws(CommandException::class)
    override fun getChild(key: String, context: ExecutionContext): ChildCommandAddress? {
        speciallyParsedIndex = null

        for (specialKey in speciallyTreatedKeys) {
            if (key.startsWith(specialKey)) {
                val result = getChild(specialKey.substring(0, specialKey.length - 1))
                    ?: return null

                val text = key.substring(specialKey.length)
                val num = text.toIntOrNull() ?: throw CommandException("$text is not a number")
                speciallyParsedIndex = num

                return result
            }
        }

        return super.getChild(key)
    }

}

private class AdminGroupCommand : DefaultGroupCommand() {
    override fun isVisibleTo(sender: CommandSender) = sender.hasPermAdminManage
}
