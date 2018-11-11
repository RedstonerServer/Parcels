package io.dico.dicore.command;

import java.util.List;
import java.util.Objects;

public class PermissionContextFilter implements IContextFilter {
    private String permission;
    private String[] permissionComponents;
    private int componentInsertionIndex;
    private String failMessage;

    public PermissionContextFilter(String permission) {
        this.permission = Objects.requireNonNull(permission);
    }

    public PermissionContextFilter(String permission, String failMessage) {
        this(permission);
        this.failMessage = failMessage;
    }

    public PermissionContextFilter(String permission, boolean inheritable) {
        this(permission, null, inheritable);
    }

    public PermissionContextFilter(String permission, String failMessage, boolean inheritable) {
        this(permission, failMessage);
        if (inheritable) {
            setupInheritability(-1);
        }
    }

    public PermissionContextFilter(String permission, int componentInsertionIndex, String failMessage) {
        this(permission, failMessage);
        setupInheritability(componentInsertionIndex);
    }

    private void setupInheritability(int componentInsertionIndex) {
        this.permissionComponents = permission.split("\\.");
        this.componentInsertionIndex = componentInsertionIndex < 0 ? permissionComponents.length : componentInsertionIndex;
        if (componentInsertionIndex > permissionComponents.length) throw new IllegalArgumentException();
    }

    private void doFilter(ExecutionContext context, String permission) throws CommandException {
        if (failMessage != null) {
            Validate.isAuthorized(context.getSender(), permission, failMessage);
        } else {
            Validate.isAuthorized(context.getSender(), permission);
        }
    }

    @Override
    public void filterContext(ExecutionContext context) throws CommandException {
        doFilter(context, permission);
    }

    public String getInheritedPermission(String[] components) {
        int insertedAmount = components.length;
        String[] currentComponents = permissionComponents;
        int currentAmount = currentComponents.length;
        String[] targetArray = new String[currentAmount + insertedAmount];

        int insertionIndex;
        //int newInsertionIndex;
        if (componentInsertionIndex == -1) {
            insertionIndex = currentAmount;
            //newInsertionIndex = -1;
        } else {
            insertionIndex = componentInsertionIndex;
            //newInsertionIndex = insertionIndex + insertedAmount;
        }

        // copy the current components up to insertionIndex
        System.arraycopy(currentComponents, 0, targetArray, 0, insertionIndex);
        // copy the new components into the array at insertionIndex
        System.arraycopy(components, 0, targetArray, insertionIndex, insertedAmount);
        // copy the current components from insertionIndex + inserted amount
        System.arraycopy(currentComponents, insertionIndex, targetArray, insertionIndex + insertedAmount, currentAmount - insertionIndex);

        return String.join(".", targetArray);
    }

    @Override
    public void filterSubContext(ExecutionContext subContext, String... path) throws CommandException {
        if (isInheritable()) {
            doFilter(subContext, getInheritedPermission(path));
        }
    }

    @Override
    public Priority getPriority() {
        return Priority.PERMISSION;
    }

    public boolean isInheritable() {
        return permissionComponents != null;
    }

    public String getPermission() {
        return permission;
    }

    public int getComponentInsertionIndex() {
        return componentInsertionIndex;
    }

    public String getFailMessage() {
        return failMessage;
    }

    /*
    private fun getPermissionsOf(address: ICommandAddress) = getPermissionsOf(address, emptyArray(), mutableListOf())

    private fun getPermissionsOf(address: ICommandAddress, path: Array<String>, result: MutableList<String>): List<String> {
        val command = address.command ?: return result

        var inherited = false
        for (filter in command.contextFilters) {
            when (filter) {
                is PermissionContextFilter -> {
                    if (path.isEmpty()) result.add(filter.permission)
                    else if (filter.isInheritable) result.add(filter.getInheritedPermission(path))
                }
                is InheritingContextFilter -> {
                    if (filter.priority == PERMISSION && address.hasParent() && !inherited) {
                        inherited = true
                        getPermissionsOf(address.parent, arrayOf(address.mainKey, *path), result)
                    }
                }
            }
        }

        return result
    }
     */

}
