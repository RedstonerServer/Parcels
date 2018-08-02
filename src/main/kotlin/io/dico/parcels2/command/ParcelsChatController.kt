package io.dico.parcels2.command

import io.dico.dicore.command.chat.AbstractChatController

class ParcelsChatController : AbstractChatController() {

    override fun filterMessage(message: String?): String {
        return "[Parcels] $message"
    }

}