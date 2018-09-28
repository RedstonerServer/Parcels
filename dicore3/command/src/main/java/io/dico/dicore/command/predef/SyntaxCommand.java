package io.dico.dicore.command.predef;

import io.dico.dicore.command.*;
import org.bukkit.command.CommandSender;

/**
 * The syntax command
 */
public class SyntaxCommand extends PredefinedCommand<SyntaxCommand> {
    public static final SyntaxCommand INSTANCE = new SyntaxCommand(false);

    private SyntaxCommand(boolean modifiable) {
        super(modifiable);
        setDescription("Describes how to use the command");
    }

    @Override
    protected SyntaxCommand newModifiableInstance() {
        return new SyntaxCommand(true);
    }

    @Override
    public String execute(CommandSender sender, ExecutionContext context) throws CommandException {
        context.getAddress().getChatHandler().sendSyntaxMessage(sender, context, context.getAddress().getParent());
        return null;
    }

    public static void registerAsChild(ICommandAddress address) {
        registerAsChild(address, "syntax");
    }

    public static void registerAsChild(ICommandAddress address, String main, String... aliases) {
        ((ModifiableCommandAddress) address).addChild(new ChildCommandAddress(INSTANCE, main, aliases));
    }

}
