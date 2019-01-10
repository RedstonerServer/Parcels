package io.dico.dicore.command;

import io.dico.dicore.command.chat.ChatHandlers;
import io.dico.dicore.command.chat.IChatHandler;
import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.parameter.ParameterList;
import io.dico.dicore.command.predef.DefaultGroupCommand;
import io.dico.dicore.command.predef.HelpCommand;
import io.dico.dicore.command.predef.PredefinedCommand;

import java.util.*;

public abstract class ModifiableCommandAddress implements ICommandAddress {
    Map<String, ChildCommandAddress> children;
    Collection<String> childrenMainKeys = Collections.emptyList();

    // the chat handler as configured by the programmer
    IChatHandler chatHandler;
    // cache for the algorithm that finds the first chat handler going up the tree
    transient IChatHandler cachedChatHandlerFallback;

    ModifiableCommandAddress helpChild;

    public ModifiableCommandAddress() {
        this.children = new LinkedHashMap<>(4);
    }

    @Override
    public boolean hasParent() {
        return getParent() != null;
    }

    @Override
    public boolean hasCommand() {
        return getCommand() != null;
    }

    @Override
    public boolean hasUserDeclaredCommand() {
        Command command = getCommand();
        return command != null && !(command instanceof PredefinedCommand);
    }

    @Override
    public Command getCommand() {
        return null;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public List<String> getNames() {
        return null;
    }

    @Override
    public List<String> getAliases() {
        List<String> names = getNames();
        if (names == null) {
            return null;
        }
        if (names.isEmpty()) {
            return Collections.emptyList();
        }
        return names.subList(1, names.size());
    }

    @Override
    public String getMainKey() {
        return null;
    }

    public void setCommand(Command command) {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract ModifiableCommandAddress getParent();

    @Override
    public RootCommandAddress getRoot() {
        ModifiableCommandAddress out = this;
        while (out.hasParent()) {
            out = out.getParent();
        }
        return out.isRoot() ? (RootCommandAddress) out : null;
    }

    @Override
    public int getDepth() {
        int depth = 0;
        ICommandAddress address = this;
        while (address.hasParent()) {
            address = address.getParent();
            depth++;
        }
        return depth;
    }

    @Override
    public boolean isDepthLargerThan(int value) {
        int depth = 0;
        ICommandAddress address = this;
        do {
            if (depth > value) {
                return true;
            }

            address = address.getParent();
            depth++;
        } while (address != null);
        return false;
    }

    @Override
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @Override
    public int getNumberOfRealChildren() {
        return childrenMainKeys.size();
    }

    @Override
    public Collection<String> getChildrenMainKeys() {
        return Collections.unmodifiableCollection(childrenMainKeys);
    }

    @Override
    public Map<String, ? extends ModifiableCommandAddress> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    @Override
    public ChildCommandAddress getChild(String key) {
        return children.get(key);
    }

    @Override
    public ChildCommandAddress getChild(ExecutionContext context, ArgumentBuffer buffer) throws CommandException {
        return buffer.hasNext() ? getChild(buffer.next()) : null;
    }

    public void addChild(ICommandAddress child) {
        if (!(child instanceof ChildCommandAddress)) {
            throw new IllegalArgumentException("Argument must be a ChildCommandAddress");
        }

        ChildCommandAddress mChild = (ChildCommandAddress) child;
        if (mChild.parent != null) {
            throw new IllegalArgumentException("Argument already has a parent");
        }

        if (mChild.names.isEmpty()) {
            throw new IllegalArgumentException("Argument must have names");
        }

        Iterator<String> names = mChild.modifiableNamesIterator();
        String mainKey = names.next();

        if (!childrenMainKeys.contains(mainKey)) {
            if (!(childrenMainKeys instanceof ArrayList)) {
                childrenMainKeys = new ArrayList<>();
            }
            childrenMainKeys.add(mainKey);
        }

        children.put(mainKey, mChild);

        while (names.hasNext()) {
            String name = names.next();
            if (children.putIfAbsent(name, mChild) != null) {
                names.remove();
            }
        }

        mChild.setParent(this);

        if (mChild.hasCommand() && mChild.getCommand() instanceof HelpCommand) {
            helpChild = mChild;
        }
    }

    public void removeChildren(boolean removeAliases, String... keys) {
        if (keys.length == 0) {
            throw new IllegalArgumentException("keys is empty");
        }

        for (String key : keys) {
            ChildCommandAddress keyTarget = getChild(key);
            if (keyTarget == null) {
                continue;
            }

            if (removeAliases) {
                Iterator<String> iterator = keyTarget.namesModifiable.iterator();
                boolean first = true;
                while (iterator.hasNext()) {
                    String alias = iterator.next();
                    ChildCommandAddress aliasTarget = getChild(key);
                    if (aliasTarget == keyTarget) {
                        if (first) {
                            childrenMainKeys.remove(alias);
                        }
                        children.remove(alias);
                    }
                    iterator.remove();
                    first = false;
                }

            } else {
                if (key.equals(keyTarget.getMainKey())) {
                    childrenMainKeys.remove(key);
                }

                children.remove(key);
                keyTarget.namesModifiable.remove(key);
            }
        }
    }

    public boolean hasHelpCommand() {
        return helpChild != null;
    }

    public ModifiableCommandAddress getHelpCommand() {
        return helpChild;
    }

    @Override
    public IChatHandler getChatHandler() {
        if (cachedChatHandlerFallback == null) {
            if (chatHandler != null) {
                cachedChatHandlerFallback = chatHandler;
            } else if (!hasParent()) {
                cachedChatHandlerFallback = ChatHandlers.defaultChat();
            } else {
                cachedChatHandlerFallback = getParent().getChatHandler();
            }
        }
        return cachedChatHandlerFallback;
    }

    public void setChatHandler(IChatHandler chatHandler) {
        this.chatHandler = chatHandler;
        resetChatHandlerCache(new HashSet<>());
    }

    void resetChatHandlerCache(Set<ModifiableCommandAddress> dejaVu) {
        if (dejaVu.add(this)) {
            cachedChatHandlerFallback = chatHandler;
            for (ChildCommandAddress address : children.values()) {
                if (address.chatHandler == null) {
                    address.resetChatHandlerCache(dejaVu);
                }
            }
        }
    }

    @Override
    public ICommandDispatcher getDispatcherForTree() {
        return getRoot();
    }

    @Override
    public boolean isCommandTrailing() {
        return false;
    }

    public void setCommandTrailing(boolean trailing) {
        throw new UnsupportedOperationException();
    }

    void appendDebugInformation(StringBuilder target, String linePrefix, Set<ICommandAddress> seen) {
        target.append('\n').append(linePrefix);
        if (!seen.add(this)) {
            target.append("<duplicate of address '").append(getAddress()).append("'>");
            return;
        }

        if (this instanceof ChildCommandAddress) {
            List<String> namesModifiable = ((ChildCommandAddress) this).namesModifiable;
            if (namesModifiable.isEmpty()) {
                target.append("<no key>");
            } else {
                Iterator<String> keys = namesModifiable.iterator();
                target.append(keys.next()).append(' ');
                if (keys.hasNext()) {
                    target.append('(').append(keys.next());
                    while (keys.hasNext()) {
                        target.append(" ,").append(keys.next());
                    }
                    target.append(") ");
                }
            }
        } else {
            target.append("<root> ");
        }

        String commandClass = hasCommand() ? getCommand().getClass().getCanonicalName() : "<no command>";
        target.append(commandClass);

        for (ChildCommandAddress child : new HashSet<>(children.values())) {
            child.appendDebugInformation(target, linePrefix + "  ", seen);
        }
    }

}
