package io.dico.dicore.command;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public interface ICommandReceiver {

    interface Factory {

        ICommandReceiver getReceiver(ExecutionContext context, Method target, String cmdName);

        Plugin getPlugin();

        // type is CoroutineContext, but we avoid referring to Kotlin runtime here
        default Object getCoroutineContext() {
            return null;
        }

    }

}
