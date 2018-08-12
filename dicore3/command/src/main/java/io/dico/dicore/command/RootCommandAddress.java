package io.dico.dicore.command;

import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.predef.DefaultGroupCommand;
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
        for (ModifiableCommandAddress child : new HashSet<ModifiableCommandAddress>(address.getChildren().values())) {
            System.out.println(child.getAddress());
            debugChildren(child);
        }
    }

    private static void registerMember(Map<String, org.bukkit.command.Command> map, String key, org.bukkit.command.Command value, boolean override) {
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
        //System.out.println("Buffer cursor upon getCommandTarget: " + buffer.getCursor());

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

        /*
        if (!cur.hasCommand() && cur.hasHelpCommand()) {
            cur = cur.getHelpCommand();
        } else {
            while (!cur.hasCommand() && cur.hasParent()) {
                cur = cur.getParent();
                buffer.rewind();
            }
        }
        */

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
        ModifiableCommandAddress targetAddress = getCommandTarget(sender, buffer);
        Command target = targetAddress.getCommand();

        if (target == null || target instanceof DefaultGroupCommand) {
            if (targetAddress.hasHelpCommand()) {
                target = targetAddress.getHelpCommand().getCommand();
            } else if (target == null){
                return false;
            }
        }

        target.execute(sender, targetAddress, buffer);
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
        ICommandAddress target = getCommandTarget(sender, buffer);
        List<String> out = target.hasCommand() ? target.getCommand().tabComplete(sender, target, location, buffer.getUnaffectingCopy()) : Collections.emptyList();

        int cursor = buffer.getCursor();
        String input;
        if (cursor >= buffer.size()) {
            input = "";
        } else {
            input = buffer.get(cursor).toLowerCase();
        }

        boolean wrapped = false;
        for (String child : target.getChildren().keySet()) {
            if (child.toLowerCase().startsWith(input)) {
                if (!wrapped) {
                    out = new ArrayList<>(out);
                    wrapped = true;
                }
                out.add(child);
            }
        }

        return out;
    }
}
