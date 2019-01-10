package io.dico.dicore.command;

import io.dico.dicore.exceptions.checkedfunctions.CheckedBiFunction;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class LambdaCommand extends ExtendedCommand<LambdaCommand> {
    private CheckedBiFunction<CommandSender, ExecutionContext, String, CommandException> executor;
    private BiFunction<CommandSender, ExecutionContext, List<String>> completer;

    public LambdaCommand executor(CheckedBiFunction<CommandSender, ExecutionContext, String, CommandException> executor) {
        this.executor = Objects.requireNonNull(executor);
        return this;
    }

    public LambdaCommand completer(BiFunction<CommandSender, ExecutionContext, List<String>> completer) {
        this.completer = Objects.requireNonNull(completer);
        return this;
    }

    @Override
    public String execute(CommandSender sender, ExecutionContext context) throws CommandException {
        return executor.checkedApply(sender, context);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, ExecutionContext context, Location location) {
        return completer == null ? super.tabComplete(sender, context, location) : completer.apply(sender, context);
    }

}
