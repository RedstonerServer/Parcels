package io.dico.dicore.command.predef;

import io.dico.dicore.command.CommandBuilder;
import io.dico.dicore.command.ExtendedCommand;
import io.dico.dicore.command.ICommandAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Marker class for commands that are generated. These commands can be replaced using methods in {@link CommandBuilder}
 */
public abstract class PredefinedCommand<T extends PredefinedCommand<T>> extends ExtendedCommand<T> {
    static final Map<String, Consumer<ICommandAddress>> predefinedCommandGenerators = new HashMap<>();

    /**
     * Get a predefined command
     *
     * @param name the name
     * @return the subscriber
     */
    public static Consumer<ICommandAddress> getPredefinedCommandGenerator(String name) {
        return predefinedCommandGenerators.get(name);
    }

    /**
     * Register a predefined command
     *
     * @param name     the name
     * @param consumer the generator which adds the child to the address
     * @return true if and only if the subscriber was registered (false if the name exists)
     */
    public static boolean registerPredefinedCommandGenerator(String name, Consumer<ICommandAddress> consumer) {
        return predefinedCommandGenerators.putIfAbsent(name, consumer) == null;
    }

    static {
        registerPredefinedCommandGenerator("help", HelpCommand::registerAsChild);
        //noinspection StaticInitializerReferencesSubClass
        registerPredefinedCommandGenerator("syntax", SyntaxCommand::registerAsChild);
    }

    public PredefinedCommand() {
    }

    public PredefinedCommand(boolean modifiable) {
        super(modifiable);
    }
}
