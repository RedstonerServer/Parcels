package io.dico.dicore.command.registration.reflect;

import io.dico.dicore.command.*;
import io.dico.dicore.command.annotation.Cmd;
import io.dico.dicore.command.annotation.GenerateCommands;
import io.dico.dicore.command.parameter.type.IParameterTypeSelector;
import org.bukkit.command.CommandSender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ReflectiveCommand extends Command {
    private final Cmd cmdAnnotation;
    private final Method method;
    private final Object instance;
    private String[] parameterOrder;
    private final int flags;

    ReflectiveCommand(IParameterTypeSelector selector, Method method, Object instance) throws CommandParseException {
        if (!method.isAnnotationPresent(Cmd.class)) {
            throw new CommandParseException("No @Cmd present for the method " + method.toGenericString());
        }
        cmdAnnotation = method.getAnnotation(Cmd.class);

        java.lang.reflect.Parameter[] parameters = method.getParameters();

        if (!method.isAccessible()) try {
            method.setAccessible(true);
        } catch (Exception ex) {
            throw new CommandParseException("Failed to make method accessible");
        }

        if (!Modifier.isStatic(method.getModifiers())) {
            if (instance == null) {
                try {
                    instance = method.getDeclaringClass().newInstance();
                } catch (Exception ex) {
                    throw new CommandParseException("No instance given for instance method, and failed to create new instance", ex);
                }
            } else if (!method.getDeclaringClass().isInstance(instance)) {
                throw new CommandParseException("Given instance is not an instance of the method's declaring class");
            }
        }

        this.method = method;
        this.instance = instance;
        this.flags = ReflectiveRegistration.parseCommandAttributes(selector, method, this, parameters);
    }

    public Method getMethod() {
        return method;
    }

    public Object getInstance() {
        return instance;
    }

    void setParameterOrder(String[] parameterOrder) {
        this.parameterOrder = parameterOrder;
    }

    ICommandAddress getAddress() {
        ChildCommandAddress result = new ChildCommandAddress();
        result.setCommand(this);

        Cmd cmd = cmdAnnotation;
        result.getNames().add(cmd.value());
        for (String alias : cmd.aliases()) {
            result.getNames().add(alias);
        }
        result.finalizeNames();

        GenerateCommands generateCommands = method.getAnnotation(GenerateCommands.class);
        if (generateCommands != null) {
            ReflectiveRegistration.generateCommands(result, generateCommands.value());
        }

        return result;
    }

    @Override
    public String execute(CommandSender sender, ExecutionContext context) throws CommandException {
        String[] parameterOrder = this.parameterOrder;
        int start = Integer.bitCount(flags);
        Object[] args = new Object[parameterOrder.length + start];

        int i = 0;
        if ((flags & 1) != 0) {
            try {
                args[i++] = ((ICommandReceiver.Factory) instance).getReceiver(context, method, cmdAnnotation.value());
            } catch (Exception ex) {
                handleException(ex);
                return null; // unreachable
            }
        }
        if ((flags & 2) != 0) {
            args[i++] = sender;
        }
        if ((flags & 4) != 0) {
            args[i++] = context;
        }

        for (int n = args.length; i < n; i++) {
            args[i] = context.get(parameterOrder[i - start]);
        }

        if (!isSuspendFunction()) {
            return callSynchronously(args);
        }

        return callAsCoroutine(context, args);
    }

    private boolean isSuspendFunction() {
        try {
            return KotlinReflectiveRegistrationKt.isSuspendFunction(method);
        } catch (Throwable ex) {
            return false;
        }
    }

    public String callSynchronously(Object[] args) throws CommandException {
        try {
            return getResult(method.invoke(instance, args), null);
        } catch (Exception ex) {
            return getResult(null, ex);
        }
    }

    public static String getResult(Object returned, Exception ex) throws CommandException {
        if (ex != null) {
            handleException(ex);
            return null; // unreachable
        }

        if (returned instanceof String) {
            return (String) returned;
        }
        if (returned instanceof CommandResult) {
            return ((CommandResult) returned).getMessage();
        }
        return null;
    }

    public static void handleException(Exception ex) throws CommandException {
        if (ex instanceof InvocationTargetException) {
            if (ex.getCause() instanceof CommandException) {
                throw (CommandException) ex.getCause();
            }

            ex.printStackTrace();
            throw new CommandException("An internal error occurred while executing this command.", ex);
        }
        if (ex instanceof CommandException) {
            throw (CommandException) ex;
        }
        ex.printStackTrace();
        throw new CommandException("An internal error occurred while executing this command.", ex);
    }

    private String callAsCoroutine(ExecutionContext context, Object[] args) {
        return KotlinReflectiveRegistrationKt.callAsCoroutine(this, (ICommandReceiver.Factory) instance, context, args);
    }

}
