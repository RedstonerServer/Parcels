package io.dico.dicore.command.predef;

import io.dico.dicore.command.*;
import io.dico.dicore.command.annotation.Range;
import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.parameter.Parameter;
import io.dico.dicore.command.parameter.type.NumberParameterType;
import org.bukkit.command.CommandSender;

/**
 * The help command
 */
public class HelpCommand extends PredefinedCommand<HelpCommand> {
    private static final Parameter<Integer, Range.Memory> pageParameter;
    public static final HelpCommand INSTANCE;

    private HelpCommand(boolean modifiable) {
        super(modifiable);
        getParameterList().addParameter(pageParameter);
        getParameterList().setRequiredCount(0);
        setDescription("Shows this help page");
    }

    @Override
    protected HelpCommand newModifiableInstance() {
        return new HelpCommand(true);
    }

    @Override
    public String execute(CommandSender sender, ExecutionContext context) throws CommandException {
        ICommandAddress target = context.getAddress();
        if (context.getAddress().getCommand() == this) {
            target = target.getParent();
        }

        context.getAddress().getChatHandler().sendHelpMessage(sender, context, target, context.<Integer>get("page") - 1);
        return null;
    }

    public static void registerAsChild(ICommandAddress address) {
        registerAsChild(address, "help");
    }

    public static void registerAsChild(ICommandAddress address, String main, String... aliases) {
        ((ModifiableCommandAddress) address).addChild(new ChildCommandAddress(INSTANCE, main, aliases));
    }

    static {
        pageParameter = new Parameter<>("page", "the page number",
                new NumberParameterType<Integer>(Integer.TYPE) {
                    @Override
                    protected Integer parse(String input) throws NumberFormatException {
                        return Integer.parseInt(input);
                    }

                    @Override
                    protected Integer select(Number number) {
                        return number.intValue();
                    }

                    @Override
                    public Integer parseForContext(Parameter<Integer, Range.Memory> parameter, ExecutionContext context, ArgumentBuffer buffer) throws CommandException {
                        if (context.getAddress().getCommand() == null || context.getAddress().getCommand().getClass() != HelpCommand.class) {
                            // An address was executed with its help command as target
                            buffer.next();
                            return 1;
                        }
                        return parse(parameter, context.getSender(), buffer);
                    }
                },
                new Range.Memory(1, Integer.MAX_VALUE, 1));

        INSTANCE = new HelpCommand(false);
    }

}
