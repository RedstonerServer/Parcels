package io.dico.dicore.command.chat;

import io.dico.dicore.Formatting;
import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.EMessageType;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import org.bukkit.command.CommandSender;

//TODO add methods to send JSON messages
public interface IChatController {

    void sendMessage(ExecutionContext context, EMessageType type, String message);

    void sendMessage(CommandSender sender, EMessageType type, String message);

    void handleCommandException(CommandSender sender, ExecutionContext context, CommandException exception);

    void handleException(CommandSender sender, ExecutionContext context, Throwable exception);

    void sendHelpMessage(CommandSender sender, ExecutionContext context, ICommandAddress address, int page);

    void sendSyntaxMessage(CommandSender sender, ExecutionContext context, ICommandAddress address);

    Formatting getChatFormatForType(EMessageType type);

    String getMessagePrefixForType(EMessageType type);

    String filterMessage(String message);

}
