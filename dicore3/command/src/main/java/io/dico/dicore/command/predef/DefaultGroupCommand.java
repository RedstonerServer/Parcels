package io.dico.dicore.command.predef;

import io.dico.dicore.command.Command;
import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.ExecutionContext;
import org.bukkit.command.CommandSender;

public class DefaultGroupCommand extends Command {
    private static final DefaultGroupCommand instance = new DefaultGroupCommand();

    public static DefaultGroupCommand getInstance() {
        return instance;
    }

    private DefaultGroupCommand() {

    }

    @Override public String execute(CommandSender sender, ExecutionContext context) throws CommandException {
        context.getAddress().getChatController().sendHelpMessage(sender, context, context.getAddress(), 1);
        return null;
    }

}
