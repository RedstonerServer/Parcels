package io.dico.dicore.command.predef;

import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.IContextFilter;
import org.bukkit.command.CommandSender;

public class DefaultGroupCommand extends PredefinedCommand<DefaultGroupCommand> {
    private static final DefaultGroupCommand instance;
    private static final IContextFilter noArgumentFilter;

    public static DefaultGroupCommand getInstance() {
        return instance;
    }

    private DefaultGroupCommand(boolean modifiable) {
        addContextFilter(IContextFilter.INHERIT_PERMISSIONS);
        addContextFilter(noArgumentFilter);
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
        context.getAddress().getChatHandler().sendHelpMessage(sender, context, context.getAddress(), 1);
        return null;
    }

    static {
        noArgumentFilter = new IContextFilter() {
            @Override
            public void filterContext(ExecutionContext context) throws CommandException {
                if (context.getBuffer().hasNext()) {
                    throw new CommandException("No such command: /" + context.getAddress().getAddress()
                        + " " + context.getBuffer().next());
                }
            }

            @Override
            public Priority getPriority() {
                return Priority.EARLY;
            }
        };

        instance = new DefaultGroupCommand(false);
    }

}
