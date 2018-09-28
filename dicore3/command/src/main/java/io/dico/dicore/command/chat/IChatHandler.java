package io.dico.dicore.command.chat;

import io.dico.dicore.Formatting;
import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.EMessageType;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import org.bukkit.command.CommandSender;

//TODO add methods to send JSON messages
public interface IChatHandler {

    default void sendMessage(ExecutionContext context, EMessageType type, String message) {
        message = createMessage(context, type, message);
        if (message != null) {
            context.getSender().sendMessage(message);
        }
    }

    default void sendMessage(CommandSender sender, EMessageType type, String message) {
        message = createMessage(sender, type, message);
        if (message != null) {
            sender.sendMessage(message);
        }
    }

    default void handleCommandException(CommandSender sender, ExecutionContext context, CommandException exception) {
        sendMessage(context, EMessageType.EXCEPTION, exception.getMessage());
    }

    default void handleException(CommandSender sender, ExecutionContext context, Throwable exception) {
        if (exception instanceof CommandException) {
            handleCommandException(sender, context, (CommandException) exception);
        } else {
            sendMessage(sender, EMessageType.EXCEPTION, "An internal error occurred whilst executing this command");
            exception.printStackTrace();
        }
    }

    default void sendHelpMessage(CommandSender sender, ExecutionContext context, ICommandAddress address, int page) {
        sender.sendMessage(createHelpMessage(sender, context, address, page));
    }

    default void sendSyntaxMessage(CommandSender sender, ExecutionContext context, ICommandAddress address) {
        sender.sendMessage(createSyntaxMessage(sender, context, address));
    }

    Formatting getChatFormatForType(EMessageType type);

    String getMessagePrefixForType(EMessageType type);

    String createMessage(ExecutionContext context, EMessageType type, String message);

    String createMessage(CommandSender sender, EMessageType type, String message);

    String createHelpMessage(CommandSender sender, ExecutionContext context, ICommandAddress address, int page);

    String createSyntaxMessage(CommandSender sender, ExecutionContext context, ICommandAddress address);

}
