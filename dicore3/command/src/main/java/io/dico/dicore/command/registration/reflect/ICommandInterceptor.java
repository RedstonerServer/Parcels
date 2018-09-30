package io.dico.dicore.command.registration.reflect;

import io.dico.dicore.command.ExecutionContext;

import java.lang.reflect.Method;

public interface ICommandInterceptor {

    /**
     * Get the receiver of the command, if applicable.
     * A command has a receiver if its first parameter implements {@link ICommandReceiver}
     * and its instance object implements this interface.
     *
     * @param context the context of execution
     * @param target the method of the command
     * @param cmdName the name of the command
     * @return the receiver
     */
    default ICommandReceiver getReceiver(ExecutionContext context, Method target, String cmdName) {
        return null;
    }

    /**
     * If applicable, get the coroutine context to use in suspend functions (Kotlin only).
     * The return type is object to avoid depending on the kotlin runtime.
     *
     * @param context the context of execution
     * @param target the method of the command
     * @param cmdName the name of the command
     * @return the coroutine context
     */
    default Object getCoroutineContext(ExecutionContext context, Method target, String cmdName) {
        return null;
    }

}
