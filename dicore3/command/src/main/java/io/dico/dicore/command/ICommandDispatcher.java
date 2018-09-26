package io.dico.dicore.command;

import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.registration.CommandMap;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public interface ICommandDispatcher {

    /**
     * Get a potentially indirect child of the root of this dispatcher
     *
     * @param buffer the argument buffer with the subsequent keys to traverse. Any keys beyond the first that isn't found are ignored.
     * @return the child, or this same instance of no child is found.
     */
    ICommandAddress getDeepChild(ArgumentBuffer buffer);

    /**
     * Similar to {@link #getDeepChild(ArgumentBuffer)},
     * but this method incorporates checks on the command of traversed children:
     * {@link Command#isVisibleTo(CommandSender)}
     * and {@link Command#takePrecedenceOverSubcommand(String, ArgumentBuffer)}
     * <p>
     * The target of a command is never null, however, the same instance might be returned, and the returned address might not hold a command.
     *
     * @param sender the sender of the command
     * @param buffer the command itself as a buffer.
     * @return the address that is the target of the command.
     */
    @Deprecated
    ICommandAddress getCommandTarget(CommandSender sender, ArgumentBuffer buffer);

    /**
     * Similar to {@link #getDeepChild(ArgumentBuffer)},
     * but this method incorporates checks on the command of traversed children:
     * {@link Command#isVisibleTo(CommandSender)}
     * and {@link Command#takePrecedenceOverSubcommand(String, ArgumentBuffer)}
     * <p>
     * The target of a command is never null, however, the same instance might be returned, and the returned address might not hold a command.
     *
     * @param context the context of the command. The context must not have its address set.
     * @param buffer the command itself as a buffer.
     * @return the address that is the target of the command.
     */
    ICommandAddress getCommandTarget(ExecutionContext context, ArgumentBuffer buffer) throws CommandException;

    /**
     * dispatch the command
     *
     * @param sender  the sender
     * @param command the command
     * @return true if a command has executed
     */
    boolean dispatchCommand(CommandSender sender, String[] command);

    /**
     * dispatch the command
     *
     * @param sender    the sender
     * @param usedLabel the label (word after the /)
     * @param args      the arguments
     * @return true if a command has executed
     */
    boolean dispatchCommand(CommandSender sender, String usedLabel, String[] args);

    /**
     * dispatch the command
     *
     * @param sender the sender
     * @param buffer the command
     * @return true if a command has executed
     */
    boolean dispatchCommand(CommandSender sender, ArgumentBuffer buffer);

    /**
     * suggest tab completions
     *
     * @param sender the sender as passed to {@link org.bukkit.command.Command#tabComplete(CommandSender, String, String[], Location)}
     * @param location the location as passed to {@link org.bukkit.command.Command#tabComplete(CommandSender, String, String[], Location)}
     * @param args the arguments as passed to {@link org.bukkit.command.Command#tabComplete(CommandSender, String, String[], Location)}
     *             args must be sanitized such that it contains no empty elements, particularly at the last index.
     * @return tab completions
     */
    List<String> getTabCompletions(CommandSender sender, Location location, String[] args);

    /**
     * suggest tab completions
     *
     * @param sender the sender as passed to {@link org.bukkit.command.Command#tabComplete(CommandSender, String, String[], Location)}
     * @param usedLabel the label as passed to {@link org.bukkit.command.Command#tabComplete(CommandSender, String, String[], Location)}
     * @param location the location as passed to {@link org.bukkit.command.Command#tabComplete(CommandSender, String, String[], Location)}
     * @param args the arguments as passed to {@link org.bukkit.command.Command#tabComplete(CommandSender, String, String[], Location)}
     * @return tab completions
     */
    List<String> getTabCompletions(CommandSender sender, String usedLabel, Location location, String[] args);

    /**
     * suggest tab completions
     *
     * @param sender the sender as passed to {@link org.bukkit.command.Command#tabComplete(CommandSender, String, String[], Location)}
     * @param location the location as passed to {@link org.bukkit.command.Command#tabComplete(CommandSender, String, String[], Location)}
     * @param buffer the arguments as a buffer
     * @return tab completions
     */
    List<String> getTabCompletions(CommandSender sender, Location location, ArgumentBuffer buffer);

    /**
     * Register this dispatcher's commands to the command map
     *
     * @throws UnsupportedOperationException if this dispatcher is not the root of the tree
     */
    default void registerToCommandMap() {
        registerToCommandMap(null, CommandMap.getCommandMap(), EOverridePolicy.OVERRIDE_ALL);
    }

    /**
     * Register this dispatcher's commands to the command map
     *
     * @param fallbackPrefix the fallback prefix to use, null if none
     * @param overridePolicy the override policy
     * @throws UnsupportedOperationException if this dispatcher is not the root of the tree
     */
    default void registerToCommandMap(String fallbackPrefix, EOverridePolicy overridePolicy) {
        registerToCommandMap(fallbackPrefix, CommandMap.getCommandMap(), overridePolicy);
    }

    /**
     * Register this dispatcher's commands to the command map
     *
     * @param fallbackPrefix the fallback prefix to use, null if none
     * @param map            the command map
     * @param overridePolicy the override policy
     * @throws UnsupportedOperationException if this dispatcher is not the root of the tree
     */
    void registerToCommandMap(String fallbackPrefix, Map<String, org.bukkit.command.Command> map, EOverridePolicy overridePolicy);

    default void unregisterFromCommandMap() {
        unregisterFromCommandMap(CommandMap.getCommandMap());
    }

    void unregisterFromCommandMap(Map<String, org.bukkit.command.Command> map);

}
