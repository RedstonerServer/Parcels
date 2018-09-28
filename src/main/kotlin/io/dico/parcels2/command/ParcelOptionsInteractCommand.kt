package io.dico.parcels2.command

import io.dico.dicore.command.*
import io.dico.dicore.command.parameter.type.ParameterTypes
import io.dico.parcels2.Interactables
import io.dico.parcels2.ParcelsPlugin
import io.dico.parcels2.Privilege
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ParcelOptionsInteractCommand(val plugin: ParcelsPlugin) : Command() {

    init {
        setShortDescription("View and/or change the setting")
        setDescription(shortDescription)
        addContextFilter(IContextFilter.PLAYER_ONLY)
        addContextFilter(IContextFilter.INHERIT_PERMISSIONS)
        addParameter("allowed", "new setting", ParameterTypes.BOOLEAN)
        requiredParameters(0)
    }

    override fun execute(sender: CommandSender, context: ExecutionContext): String? {
        if (!plugin.storage.isConnected) err("Parcels cannot have their options changed right now because of a database error")

        val interactableClass = Interactables[context.address.mainKey]
        val allowed: Boolean? = context.get("allowed")

        val parcel = plugin.parcelProvider.getParcelRequired(sender as Player,
            if (allowed == null) Privilege.DEFAULT else Privilege.CAN_MANAGE)

        if (allowed == null) {
            val setting = parcel.interactableConfig.isInteractable(interactableClass)
            val default = setting == interactableClass.interactableByDefault

            val canColor = context.address.chatHandler.getChatFormatForType(EMessageType.BAD_NEWS)
            val cannotColor = context.address.chatHandler.getChatFormatForType(EMessageType.GOOD_NEWS)
            val resetColor = context.address.chatHandler.getChatFormatForType(EMessageType.RESULT)

            val settingString = (if (setting) "${canColor}can" else "${cannotColor}cannot") + resetColor
            val defaultString = if (default) " (default)" else ""

            return "Players $settingString interact with ${interactableClass.name} on this parcel$defaultString"
        }

        val change = parcel.interactableConfig.setInteractable(interactableClass, allowed)

        val interactableClassName = interactableClass.name
        return when {
            allowed && change -> "Other players can now interact with $interactableClassName"
            allowed && !change -> err("Other players could already interact with $interactableClassName")
            change -> "Other players can not interact with $interactableClassName anymore"
            else -> err("Other players were not allowed to interact with $interactableClassName")
        }
    }

}
