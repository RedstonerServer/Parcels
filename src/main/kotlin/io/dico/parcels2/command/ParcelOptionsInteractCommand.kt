package io.dico.parcels2.command

import io.dico.dicore.command.Command
import io.dico.dicore.command.CommandException
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.IContextFilter
import io.dico.dicore.command.parameter.type.ParameterTypes
import io.dico.parcels2.Interactables
import io.dico.parcels2.ParcelProvider
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ParcelOptionsInteractCommand(val parcelProvider: ParcelProvider) : Command() {

    init {
        addContextFilter(IContextFilter.PLAYER_ONLY)
        addParameter("allowed", "allowed", ParameterTypes.BOOLEAN)
    }

    override fun execute(sender: CommandSender, context: ExecutionContext): String? {
        val parcel = parcelProvider.getParcelRequired(sender as Player, owner = true)
        val interactableClassName = context.address.mainKey
        val allowed: Boolean = context.get("allowed")
        val change = parcel.interactableConfig.setInteractable(Interactables[interactableClassName], allowed)

        return when {
            allowed && change -> "Other players can now interact with $interactableClassName"
            allowed && !change -> err("Other players could already interact with $interactableClassName")
            change -> "Other players can not interact with $interactableClassName anymore"
            else -> err("Other players were not allowed to interact with $interactableClassName")
        }
    }

}

private fun err(message: String): Nothing = throw CommandException(message)