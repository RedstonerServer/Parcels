package io.dico.dicore.command.registration;

import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.ICommandDispatcher;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * This class extends the bukkit's command class.
 * Instances are injected into the command map.
 */
public class BukkitCommand extends Command {
    private ICommandDispatcher dispatcher;
    private ICommandAddress origin;

    public BukkitCommand(ICommandAddress address) {
        super(validateTree(address).getNames().get(0), "", "", address.getNames().subList(1, address.getNames().size()));
        this.dispatcher = address.getDispatcherForTree();
        this.origin = address;

        setTimingsIfNecessary(this);
    }

    private static ICommandAddress validateTree(ICommandAddress tree) {
        if (!tree.hasParent()) {
            throw new IllegalArgumentException();
        }
        if (tree.getNames().isEmpty()) {
            throw new IllegalArgumentException();
        }
        return tree;
    }

    public ICommandAddress getOrigin() {
        return origin;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!dispatcher.dispatchCommand(sender, label, args)) {
            //System.out.println("failed to dispatch command");
            // target command not found, send a message in the future TODO
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        return this.tabComplete(sender, alias, args, null);
    }

    //@Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return dispatcher.getTabCompletions(sender, alias, location, args);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BukkitCommand that = (BukkitCommand) o;

        return getName().equals(that.getName()) && dispatcher == that.dispatcher;
    }

    @Override
    public int hashCode() {
        return dispatcher.hashCode() | getName().hashCode();
    }

    private static void setTimingsIfNecessary(Command object) {
        // with paper spigot, the timings are not set by super constructor but by CommandMap.register(), which is not invoked for this system
        // I use reflection so that the project does not require paper spigot to build
        try {
            // public field
            Field field = Command.class.getDeclaredField("timings");
            if (field.get(object) != null) return;
            Class<?> clazz = Class.forName("co.aikar.timings.TimingsManager");
            // public method
            Method method = clazz.getDeclaredMethod("getCommandTiming", String.class, Command.class);
            Object timings = method.invoke(null, "", object);
            field.set(object, timings);
        } catch (Throwable ignored) {
        }
    }
    
    /*
    public static void registerToMap(ICommandAddress tree, Map<String, Command> map) {
        BukkitCommand command = new BukkitCommand(tree);
        Iterator<String> iterator = tree.getNames().iterator();
        map.put(iterator.next(), command);
        while (iterator.hasNext()) {
            map.putIfAbsent(iterator.next(), command);
        }
    }
    
    public static void unregisterFromMap(ICommandAddress tree, Map<String, Command> map) {
        map.values().remove(new BukkitCommand(tree));
    }
    
    public static void registerChildrenToMap(ICommandAddress tree, Map<String, Command> map) {
        for (Map.Entry<String, ? extends ICommandAddress> entry : tree.getChildren().entrySet()) {
            ICommandAddress child = entry.getValue();
            registerToMap(child, map);
        }
    }
    
    public static void unregisterChildenFromMap(ICommandAddress tree, Map<String, Command> map) {
        for (Map.Entry<String, ? extends ICommandAddress> entry : tree.getChildren().entrySet()) {
            ICommandAddress child = entry.getValue();
            unregisterFromMap(child, map);
        }
    }
    */

}
