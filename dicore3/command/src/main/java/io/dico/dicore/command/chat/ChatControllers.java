package io.dico.dicore.command.chat;

/**
 * Static factory methods for {@link IChatController}
 */
public class ChatControllers {
    private static final IChatController defaultChat;

    private ChatControllers() {

    }

    public static IChatController defaultChat() {
        return defaultChat;
    }

    static {
        defaultChat = new AbstractChatController();
    }
}
