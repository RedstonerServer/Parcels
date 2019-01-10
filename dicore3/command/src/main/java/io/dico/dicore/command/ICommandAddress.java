package io.dico.dicore.command;

import io.dico.dicore.command.chat.IChatHandler;
import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.parameter.ParameterList;
import io.dico.dicore.command.predef.PredefinedCommand;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interface for an address of a command.
 * <p>
 * The address holds what the name and aliases of a command are.
 * The address also (optionally) holds a reference to a {@link Command}
 * <p>
 * One instance of {@link Command} can be held by multiple addresses,
 * because the address decides what the command's name and aliases are.
 * <p>
 * The address holds children by key in a map. This map's keys include aliases for its children.
 * This creates a tree of addresses. If a command is dispatches, the tree is traversed untill a command is found
 * and no children deeper down match the command (there are exceptions to the later as defined by
 * {@link Command#takePrecedenceOverSubcommand(String, ArgumentBuffer)}
 * and {@link Command#isVisibleTo(CommandSender)}
 */
public interface ICommandAddress {

    /**
     * @return true if this address has a parent.
     */
    boolean hasParent();

    /**
     * Get the parent of this address
     *
     * @return the parent of this address, or null if none exists.
     */
    ICommandAddress getParent();

    /**
     * @return true if this address has a command.
     */
    boolean hasCommand();

    /**
     * @return true if this address has a command that is not an instance of {@link PredefinedCommand}
     */
    boolean hasUserDeclaredCommand();

    /**
     * @return Get the command of this address, or null if none exists.
     */
    Command getCommand();

    /**
     * @return true if this address is an instance of {@link RootCommandAddress}
     */
    boolean isRoot();

    /**
     * @return the root address of the tree which this address resides in.
     */
    ICommandAddress getRoot();

    /**
     * A list of the names of this address, at the current level.
     * The first entry is the main key, the subsequent ones are aliases.
     * <p>
     * Untill an address is assigned a parent, this list is mutable.
     * <p>
     * If {@link #isRoot()}, this returns an immutable, empty list.
     *
     * @return the list of names.
     */
    List<String> getNames();

    /**
     * A list of the aliases of this address. That is, {@link #getNames()}
     * without the first entry.
     *
     * @return a list of aliases
     */
    List<String> getAliases();

    /**
     * @return The first element of {@link #getNames()}
     */
    String getMainKey();

    /**
     * Get the address of this command.
     * That is, the main keys of all commands leading up to this address, and this address itself, separated by a space.
     * In other words, the command without the / that is required to target the command at this address.
     *
     * @return the address of this command.
     */
    String getAddress();

    /**
     * Get the amount of addresses that separate this address from the root of the tree, + 1.
     * The root of the tree has a depth of 0. Each subsequent child has its depth incremented by 1.
     *
     * @return The depth of this address
     */
    int getDepth();

    /**
     * @return true if the depth of this address is larger than the argument.
     */
    boolean isDepthLargerThan(int depth);

    /**
     * @return true if this address has any children.
     */
    boolean hasChildren();

    /**
     * @return total number of children, not considering any aliases
     */
    int getNumberOfRealChildren();

    /**
     * Get an unmodifiable view of all main keys of the children of this address.
     *
     * @return the main keys
     */
    Collection<String> getChildrenMainKeys();

    /**
     * Get an unmodifiable view of the children of this address.
     * Values might be duplicated for aliases.
     *
     * <p>
     * To iterate children without duplicates, you can do something like this:
     * <pre>{@code
     *     for (String key : address.getChildrenMainKeys()) {
     *         ICommandAddress child = address.getChild(key);
     *         // do stuff with child
     *     }
     *     }</pre>
     * </p>
     *
     * @return the children of this address.
     */
    Map<String, ? extends ICommandAddress> getChildren();

    /**
     * Query for a child at the given key.
     *
     * @param key the key. The name or alias of a command.
     * @return the child, or null if it's not found
     */
    ICommandAddress getChild(String key);

    /**
     * Query for a child using the given buffer, with the given context for reference.
     * Can be used to override behaviour of the address tree.
     * <p>
     * The default implementation is as follows:
     * <pre>{@code
     * return buffer.hasNext() ? getChild(buffer.next()) : null;
     * }</pre>
     *
     * @param context context of a command being executed
     * @param buffer  the buffer. The name or alias of a command.
     * @return the child, or null if it's not found, altered freely by the implementation
     */
    ICommandAddress getChild(ExecutionContext context, ArgumentBuffer buffer) throws CommandException;

    /**
     * Get the command dispatcher for this tree
     *
     * @return the command dispatcher
     */
    ICommandDispatcher getDispatcherForTree();

    /**
     * @return The desired chathandler for use by commands at this address and any sub-addresses, if they define no explicit chat handler.
     */
    IChatHandler getChatHandler();

    /**
     * Returns if the command attached to this address should be treated as trailing.
     * A trailing command is executed whenever the address is scanned for children.
     * Its parameters are parsed and added to the context.
     *
     * @return true if the command attached to this address should be treated as trailing.
     */
    boolean isCommandTrailing();

    static ICommandAddress newChild() {
        return new ChildCommandAddress();
    }

    static ICommandAddress newChild(Command command) {
        return new ChildCommandAddress(command);
    }

    static ICommandAddress newRoot() {
        return new RootCommandAddress();
    }

}
