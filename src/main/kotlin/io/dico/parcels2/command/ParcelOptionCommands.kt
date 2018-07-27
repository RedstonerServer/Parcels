package io.dico.parcels2.command

import io.dico.dicore.command.CommandBuilder
import io.dico.dicore.command.Validate
import io.dico.dicore.command.annotation.Cmd
import io.dico.dicore.command.annotation.Desc
import io.dico.dicore.command.annotation.RequireParameters
import io.dico.parcels2.Parcel
import io.dico.parcels2.ParcelsPlugin
import org.bukkit.entity.Player
import kotlin.reflect.KMutableProperty

class ParcelOptionCommands(plugin: ParcelsPlugin) : AbstractParcelCommands(plugin) {
    @Cmd("inputs")
    @Desc("Sets whether players who are not allowed to",
        "build here can use levers, buttons,",
        "pressure plates, tripwire or redstone ore",
        shortVersion = "allows using inputs")
    @RequireParameters(0)
    fun ParcelScope.cmdInputs(player: Player, enabled: Boolean?): Any? {
        return runOptionCommand(player, Parcel::allowInteractInputs, enabled, "using levers, buttons, etc.")
    }

    @Cmd("inventory")
    @Desc("Sets whether players who are not allowed to",
        "build here can interact with inventories",
        shortVersion = "allows editing inventories")
    fun ParcelScope.cmdInventory(player: Player, enabled: Boolean?): Any? {
        return runOptionCommand(player, Parcel::allowInteractInventory, enabled, "interaction with inventories")
    }

    private inline val Boolean.enabledWord get() = if (this) "enabled" else "disabled"
    private fun ParcelScope.runOptionCommand(player: Player,
                                             property: KMutableProperty<Boolean>,
                                             enabled: Boolean?,
                                             desc: String): Any? {
        checkConnected("have their options changed")
        val current = property.getter.call(parcel)
        if (enabled == null) {
            val word = if (current) "" else "not "
            return "This parcel does ${word}allow $desc"
        }

        checkCanManage(player, "change its options")
        Validate.isTrue(current != enabled, "That option was already ${enabled.enabledWord}")
        property.setter.call(parcel, enabled)
        return "That option is now ${enabled.enabledWord}"
    }

    companion object {
        private const val descShort = "changes interaction options for this parcel"
        private val desc = arrayOf("Sets whether players who are not allowed to", "build here can interact with certain things.")

        fun setGroupDescription(builder: CommandBuilder) {
            builder.setGroupDescription(descShort, *desc)
        }
    }

}