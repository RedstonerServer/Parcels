package io.dico.dicore.command.chat;

/**
 * Static factory methods for {@link IChatHandler}
 */
public class ChatHandlers {
    private static final IChatHandler defaultChat;

    private ChatHandlers() {

    }

    public static IChatHandler defaultChat() {
        return defaultChat;
    }

    static {
        defaultChat = new AbstractChatHandler();
    }
}
