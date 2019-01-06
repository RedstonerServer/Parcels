package io.dico.dicore.command;

import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.registration.BukkitCommand;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.*;

public class RootCommandAddress extends ModifiableCommandAddress implements ICommandDispatcher {
    @Deprecated
    public static final RootCommandAddress INSTANCE = new RootCommandAddress();

    public RootCommandAddress() {
    }

    @Override
    public Command getCommand() {
        return null;
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public List<String> getNames() {
        return Collections.emptyList();
    }

    @Override
    public ModifiableCommandAddress getParent() {
        return null;
    }

    @Override
    public String getMainKey() {
        return null;
    }

    @Override
    public String getAddress() {
        return "";
    }

    @Override
    public void registerToCommandMap(String fallbackPrefix, Map<String, org.bukkit.command.Command> map, EOverridePolicy overridePolicy) {
        Objects.requireNonNull(overridePolicy);
        //debugChildren(this);
        Map<String, ChildCommandAddress> children = this.children;
        Map<ChildCommandAddress, BukkitCommand> wrappers = new IdentityHashMap<>();

        for (ChildCommandAddress address : children.values()) {
            if (!wrappers.containsKey(address)) {
                wrappers.put(address, new BukkitCommand(address));
            }
        }

        for (Map.Entry<String, ChildCommandAddress> entry : children.entrySet()) {
            String key = entry.getKey();
            ChildCommandAddress address = entry.getValue();
            boolean override = overridePolicy == EOverridePolicy.OVERRIDE_ALL;
            if (!override && key.equals(address.getMainKey())) {
                override = overridePolicy == EOverridePolicy.MAIN_KEY_ONLY || overridePolicy == EOverridePolicy.MAIN_AND_FALLBACK;
            }

            registerMember(map, key, wrappers.get(address), override);

            if (fallbackPrefix != null) {
                key = fallbackPrefix + key;
                override = overridePolicy != EOverridePolicy.OVERRIDE_NONE && overridePolicy != EOverridePolicy.MAIN_KEY_ONLY;
                registerMember(map, key, wrappers.get(address), override);
            }
        }

    }

    private static void debugChildren(ModifiableCommandAddress address) {
        Collection<String> keys = address.getChildrenMainKeys();
        for (String key : keys) {
            ChildCommandAddress child = address.getChild(key);
            System.out.println(child.getAddress());
            debugChildren(child);
        }
    }

    private static void registerMember(Map<String, org.bukkit.command.Command> map,
                                       String key, org.bukkit.command.Command value, boolean override) {
        if (override) {
            map.put(key, value);
        } else {
            map.putIfAbsent(key, value);
        }
    }

    @Override
    public void unregisterFromCommandMap(Map<String, org.bukkit.command.Command> map) {
        Set<ICommandAddress> children = new HashSet<>(this.children.values());
        Iterator<Map.Entry<String, org.bukkit.command.Command>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, org.bukkit.command.Command> entry = iterator.next();
            org.bukkit.command.Command cmd = entry.getValue();
            if (cmd instanceof BukkitCommand && children.contains(((BukkitCommand) cmd).getOrigin())) {
                iterator.remove();
            }
        }
    }

    @Override
    public ModifiableCommandAddress getDeepChild(ArgumentBuffer buffer) {
        ModifiableCommandAddress cur = this;
        ChildCommandAddress child;
        while (buffer.hasNext()) {
            child = cur.getChild(buffer.next());
            if (child == null) {
                buffer.rewind();
                return cur;
            }

            cur = child;
        }
        return cur;
    }

    @Override
    public ModifiableCommandAddress getCommandTarget(CommandSender sender, ArgumentBuffer buffer) {
        ModifiableCommandAddress cur = this;
        ChildCommandAddress child;
        while (buffer.hasNext()) {
            child = cur.getChild(buffer.next());
            if (child == null
                    || (child.hasCommand() && !child.getCommand().isVisibleTo(sender))
                    || (cur.hasCommand() && cur.getCommand().takePrecedenceOverSubcommand(buffer.peekPrevious(), buffer.getUnaffectingCopy()))) {
                buffer.rewind();
                break;
            }

            cur = child;
        }

        return cur;
    }

    @Override
    public ModifiableCommandAddress getCommandTarget(ExecutionContext context, ArgumentBuffer buffer) throws CommandException {
        CommandSender sender = context.getSender();
        ModifiableCommandAddress cur = this;
        ChildCommandAddress child;
        while (buffer.hasNext()) {
            int cursor = buffer.getCursor();

            child = cur.getChild(context, buffer);

            if (child == null
                || (context.isTabComplete() && !buffer.hasNext())
                || (child.hasCommand() && !child.getCommand().isVisibleTo(sender))
                || (cur.hasCommand() && cur.getCommand().takePrecedenceOverSubcommand(buffer.peekPrevious(), buffer.getUnaffectingCopy()))) {
                buffer.setCursor(cursor);
                break;
            }

            cur = child;

            context.setAddress(child);
            if (child.hasCommand() && child.isCommandTrailing()) {
                child.getCommand().initializeAndFilterContext(context);
                child.getCommand().execute(context.getSender(), context);
            }
        }

        return cur;
    }

    @Override
    public boolean dispatchCommand(CommandSender sender, String[] command) {
        return dispatchCommand(sender, new ArgumentBuffer(command));
    }

    @Override
    public boolean dispatchCommand(CommandSender sender, String usedLabel, String[] args) {
        return dispatchCommand(sender, new ArgumentBuffer(usedLabel, args));
    }

    @Override
    public boolean dispatchCommand(CommandSender sender, ArgumentBuffer buffer) {
        ExecutionContext context = new ExecutionContext(sender, buffer, false);

        ModifiableCommandAddress targetAddress = null;

        try {
            targetAddress = getCommandTarget(context, buffer);
            Command target = targetAddress.getCommand();

            if (target == null) {
                if (targetAddress.hasHelpCommand()) {
                    target = targetAddress.getHelpCommand().getCommand();
                } else {
                    return false;
                }
            }

            context.setCommand(target);

            if (!targetAddress.isCommandTrailing()) {
                target.initializeAndFilterContext(context);
                String message = target.execute(sender, context);
                if (message != null && !message.isEmpty()) {
                    context.sendMessage(EMessageType.RESULT, message);
                }
            }

        } catch (Throwable t) {
            if (targetAddress == null) {
                targetAddress = this;
            }
            targetAddress.getChatHandler().handleException(sender, context, t);
        }

        return true;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, Location location, String[] args) {
        return getTabCompletions(sender, location, new ArgumentBuffer(args));
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String usedLabel, Location location, String[] args) {
        return getTabCompletions(sender, location, new ArgumentBuffer(usedLabel, args));
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, Location location, ArgumentBuffer buffer) {
        ExecutionContext context = new ExecutionContext(sender, buffer, true);
        long start = System.currentTimeMillis();

        try {
            ICommandAddress target = getCommandTarget(context, buffer);

            List<String> out = Collections.emptyList();
            /*if (target.hasCommand()) {
                context.setCommand(target.getCommand());
                target.getCommand().initializeAndFilterContext(context);
                out = target.getCommand().tabComplete(sender, context, location);
            } else {
                out = Collections.emptyList();
            }*/

            int cursor = buffer.getCursor();
            String input;
            if (cursor >= buffer.size()) {
                input = "";
            } else {
                input = buffer.get(cursor).toLowerCase();
            }

            boolean wrapped = false;
            for (String child : target.getChildrenMainKeys()) {
                if (child.toLowerCase().startsWith(input)) {
                    if (!wrapped) {
                        out = new ArrayList<>(out);
                        wrapped = true;
                    }
                    out.add(child);
                }
            }

            return out;

        } catch (CommandException ex) {
            return Collections.emptyList();
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration > 2) {
                System.out.println(String.format("Complete took %.3f seconds", duration / 1000.0));
            }
        }

    }
}
