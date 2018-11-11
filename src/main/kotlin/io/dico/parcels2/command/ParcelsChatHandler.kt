package io.dico.parcels2.command

import io.dico.dicore.Formatting
import io.dico.dicore.command.EMessageType
import io.dico.dicore.command.ExecutionContext
import io.dico.dicore.command.chat.AbstractChatHandler
import io.dico.parcels2.util.ext.plus

class ParcelsChatHandler : AbstractChatHandler() {

    override fun getMessagePrefixForType(type: EMessageType?): String {
        return Formatting.RED + "[Parcels] "
    }

    override fun createMessage(context: ExecutionContext, type: EMessageType, message: String?): String? {
        if (message.isNullOrEmpty()) return null
        var result = getChatFormatForType(type) + message
        if (context.address.mainKey != "info") {
            result = getMessagePrefixForType(type) + result
        }
        return result
    }

}