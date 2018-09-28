package io.dico.dicore.command.chat;

import io.dico.dicore.Formatting;
import io.dico.dicore.command.EMessageType;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.chat.help.HelpPages;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AbstractChatHandler implements IChatHandler {
    private @NotNull HelpPages helpPages;

    public AbstractChatHandler(@NotNull HelpPages helpPages) {
        this.helpPages = helpPages;
    }

    public AbstractChatHandler() {
        this(HelpPages.newDefaultHelpPages());
    }

    @NotNull
    public HelpPages getHelpPages() {
        return helpPages;
    }

    public void setHelpPages(@NotNull HelpPages helpPages) {
        this.helpPages = helpPages;
    }

    @Override
    public Formatting getChatFormatForType(EMessageType type) {
        switch (type) {
            case EXCEPTION:
            case BAD_NEWS:
                return Formatting.RED;
            case INSTRUCTION:
            case NEUTRAL:
                return Formatting.GRAY;
            case CUSTOM:
                return Formatting.WHITE;
            case INFORMATIVE:
                return Formatting.AQUA;
            case RESULT:
            default:
            case GOOD_NEWS:
                return Formatting.GREEN;
            case WARNING:
                return Formatting.YELLOW;

            case DESCRIPTION:
                return Formatting.GREEN;
            case SYNTAX:
                return Formatting.AQUA;
            case HIGHLIGHT:
                return Formatting.RED;
            case SUBCOMMAND:
                return Formatting.GRAY;
            case NUMBER:
                return Formatting.YELLOW;
        }
    }

    @Override
    public String getMessagePrefixForType(EMessageType type) {
        return "";
    }

    protected String createMessage(EMessageType type, String message) {
        if (message == null || message.isEmpty()) return null;
        return getMessagePrefixForType(type) + getChatFormatForType(type) + message;
    }

    @Override
    public String createMessage(ExecutionContext context, EMessageType type, String message) {
        return createMessage(type, message);
    }

    @Override
    public String createMessage(CommandSender sender, EMessageType type, String message) {
        return createMessage(type, message);
    }

    @Override
    public String createHelpMessage(CommandSender sender, ExecutionContext context, ICommandAddress address, int page) {
        return helpPages.getHelpPage(sender, context, address, page);
    }

    @Override
    public String createSyntaxMessage(CommandSender sender, ExecutionContext context, ICommandAddress address) {
        return helpPages.getSyntax(sender, context, address);
    }

}
