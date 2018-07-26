package io.dico.dicore.command;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public interface ICommandReceiver {

    interface Factory {

        ICommandReceiver getReceiver(ExecutionContext context, Method target, String cmdName);

        Plugin getPlugin();

    }

}
