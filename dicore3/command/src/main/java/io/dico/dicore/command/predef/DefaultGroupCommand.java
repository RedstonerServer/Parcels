package io.dico.dicore.command.predef;

import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.IContextFilter;
import org.bukkit.command.CommandSender;

public class DefaultGroupCommand extends PredefinedCommand<DefaultGroupCommand> {
    private static final DefaultGroupCommand instance = new DefaultGroupCommand(false);

    public static DefaultGroupCommand getInstance() {
        return instance;
    }

    private DefaultGroupCommand(boolean modifiable) {
        addContextFilter(IContextFilter.INHERIT_PERMISSIONS);
        this.modifiable = modifiable;
    }

    public DefaultGroupCommand() {
        this(true);
    }

    @Override
    protected DefaultGroupCommand newModifiableInstance() {
        return new DefaultGroupCommand(true);
    }

    @Override
    public String execute(CommandSender sender, ExecutionContext context) throws CommandException {
        context.getAddress().getChatController().sendHelpMessage(sender, context, context.getAddress(), 1);
        return null;
    }

}
