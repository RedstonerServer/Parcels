package io.dico.dicore.command.chat;

import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.EMessageType;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import org.bukkit.command.CommandSender;

public class AbstractChatController implements IChatController {

    @Override
    public void sendMessage(ExecutionContext context, EMessageType type, String message) {
        sendMessage(context.getSender(), type, message);
    }

    @Override
    public void sendMessage(CommandSender sender, EMessageType type, String message) {
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(getMessagePrefixForType(type) + getChatFormatForType(type) + message);
        }
    }

    @Override
    public void handleCommandException(CommandSender sender, ExecutionContext context, CommandException exception) {
        sendMessage(sender, EMessageType.EXCEPTION, exception.getMessage());
    }

    @Override
    public void handleException(CommandSender sender, ExecutionContext context, Throwable exception) {
        if (exception instanceof CommandException) {
            handleCommandException(sender, context, (CommandException) exception);
        } else {
            sendMessage(sender, EMessageType.EXCEPTION, "An internal error occurred whilst executing this command");
            exception.printStackTrace();
        }
    }

    @Override
    public void sendHelpMessage(CommandSender sender, ExecutionContext context, ICommandAddress address, int page) {
        sendMessage(sender, EMessageType.INSTRUCTION, HelpCache.getHelpCache(address).getHelpPage(page));
    }

    @Override
    public void sendSyntaxMessage(CommandSender sender, ExecutionContext context, ICommandAddress address) {
        sendMessage(sender, EMessageType.INSTRUCTION, HelpCache.getHelpCache(address).getSyntax());
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
                return Formatting.BLUE;
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

}
