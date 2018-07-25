package io.dico.dicore.command.registration;

import io.dico.dicore.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.SimplePluginManager;

import java.util.*;

/**
 * Provides access to bukkit's {@code Map<String, org.bukkit.command.Command>} command map.
 */
@SuppressWarnings("ConstantConditions")
public class CommandMap {
    private static final Map<String, Command> commandMap = findCommandMap();

    private CommandMap() {

    }

    public static Map<String, Command> getCommandMap() {
        return Objects.requireNonNull(commandMap);
    }

    public static boolean isAvailable() {
        return commandMap != null;
    }

    public static Command get(String key) {
        return commandMap.get(key);
    }

    public static void put(String key, Command command) {
        commandMap.put(key, command);
    }

    public static Collection<String> replace(Command command, Command replacement) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Command> entry : commandMap.entrySet()) {
            if (entry.getValue() == command) {
                entry.setValue(replacement);
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static Map<String, Command> findCommandMap() {
        try {
            return Reflection.getFieldValue(SimpleCommandMap.class, "knownCommands",
                    Reflection.getFieldValue(SimplePluginManager.class, "commandMap", Bukkit.getPluginManager()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
