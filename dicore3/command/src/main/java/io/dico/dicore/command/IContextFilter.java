package io.dico.dicore.command;

import io.dico.dicore.exceptions.checkedfunctions.CheckedConsumer;
import io.dico.dicore.exceptions.checkedfunctions.CheckedRunnable;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public interface IContextFilter extends Comparable<IContextFilter> {

    /**
     * Filter the given context by this filter's criteria.
     * If the context does not match the criteria, an exception is thrown describing the problem.
     *
     * @param context the context to match
     * @throws CommandException if it doesn't match
     */
    void filterContext(ExecutionContext context) throws CommandException;

    /**
     * Filter an execution context for a direct or indirect sub command of the command that registered this filter.
     *
     * @param subContext the context for the execution
     * @param path       the path traversed from the command that registered this filter to the executed command
     */
    default void filterSubContext(ExecutionContext subContext, String... path) throws CommandException {
        filterContext(subContext);
    }

    /**
     * Get the priority of this context filter.
     * The priorities determine the order in which a command's context filters are executed.
     *
     * @return the priority
     */
    Priority getPriority();

    default boolean allowsContext(ExecutionContext context) {
        try {
            filterContext(context);
            return true;
        } catch (CommandException ex) {
            return false;
        }
    }

    /**
     * Used to sort filters in execution order. That is, filters are ordered by {@link #getPriority()}
     *
     * @param o compared filter
     * @return comparison value
     */
    @Override
    default int compareTo(@NotNull IContextFilter o) {
        return getPriority().compareTo(o.getPriority());
    }

    /*
    default boolean isInheritable() {
        return false;
    }

    default IContextFilter inherit(String... components) {
        if (!isInheritable()) {
            throw new IllegalStateException("This IContextFilter cannot be inherited");
        }

        return this;
    }*/

    /**
     * IContextFilter priorities. Executes from top to bottom.
     */
    enum Priority {
        /**
         * This priority should have checks on the sender type.
         * Any filters on this priority are tested before permissions are.
         * This is the highest priority.
         */
        VERY_EARLY, // sender type check

        /**
         * This priority is specific to permissions.
         */
        PERMISSION,

        /**
         * Early priority. Post permissions, pre parameter-parsing.
         */
        EARLY,

        /**
         * Normal priority. Post permissions, pre parameter-parsing.
         */
        NORMAL,

        /**
         * Late priority. Post permissions, pre parameter-parsing.
         */
        LATE,

        /**
         * Very late priority. Post permissions, pre parameter-parsing.
         */
        VERY_LATE,

        /**
         * Post parameters priority. Post permissions, post parameter-parsing.
         * This is the lowest priority.
         */
        POST_PARAMETERS;

        private IContextFilter inheritor;

        /**
         * Get the context filter that inherits context filters from the parent of the same priority.
         * If this filter is also present at the parent, it will do the same for the parent's parent, and so on.
         *
         * @return the inheritor
         */
        public IContextFilter getInheritor() {
            if (inheritor == null) {
                inheritor = InheritingContextFilter.inheritingPriority(this);
            }
            return inheritor;
        }

    }

    /**
     * Ensures that only {@link org.bukkit.entity.Player} type senders can execute the command.
     */
    IContextFilter PLAYER_ONLY = filterSender(Priority.VERY_EARLY, Validate::isPlayer);

    /**
     * Ensures that only {@link org.bukkit.command.ConsoleCommandSender} type senders can execute the command.
     */
    IContextFilter CONSOLE_ONLY = filterSender(Priority.VERY_EARLY, Validate::isConsole);

    /**
     * This filter is not working as intended.
     * <p>
     * There is supposed to be a permission filter that takes a base, and appends the command's address to the base, and checks that permission.
     */
    IContextFilter INHERIT_PERMISSIONS = Priority.PERMISSION.getInheritor();

    static IContextFilter fromCheckedRunnable(Priority priority, CheckedRunnable<? extends CommandException> runnable) {
        return new IContextFilter() {
            @Override
            public void filterContext(ExecutionContext context) throws CommandException {
                runnable.checkedRun();
            }

            @Override
            public Priority getPriority() {
                return priority;
            }
        };
    }

    static IContextFilter filterSender(Priority priority, CheckedConsumer<? super CommandSender, ? extends CommandException> consumer) {
        return new IContextFilter() {
            @Override
            public void filterContext(ExecutionContext context) throws CommandException {
                consumer.checkedAccept(context.getSender());
            }

            @Override
            public Priority getPriority() {
                return priority;
            }
        };
    }

    static IContextFilter permission(String permission) {
        return new PermissionContextFilter(permission);
    }

    static IContextFilter permission(String permission, String failMessage) {
        return new PermissionContextFilter(permission, failMessage);
    }

    static IContextFilter inheritablePermission(String permission) {
        return new PermissionContextFilter(permission, true);
    }

    /**
     * Produce an inheritable permission context filter.
     * A permission component is an element in {@code permission.split("\\.")}
     *
     * @param permission              The permission that is required for the command that this is directly assigned to
     * @param componentInsertionIndex the index where any sub-components are inserted. -1 for "at the end".
     * @param failMessage             the message to send if the permission is not met
     * @return the context filter
     * @throws IllegalArgumentException if componentInsertionIndex is out of range
     */
    static IContextFilter inheritablePermission(String permission, int componentInsertionIndex, String failMessage) {
        return new PermissionContextFilter(permission, componentInsertionIndex, failMessage);
    }

}

