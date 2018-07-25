package io.dico.dicore.command.registration.reflect;

import io.dico.dicore.command.*;
import io.dico.dicore.command.annotation.Cmd;
import io.dico.dicore.command.annotation.GenerateCommands;
import io.dico.dicore.command.parameter.type.IParameterTypeSelector;
import org.bukkit.command.CommandSender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class ReflectiveCommand extends Command {
    private final Method method;
    private final Object instance;
    private String[] parameterOrder;
    private final int flags;

    ReflectiveCommand(IParameterTypeSelector selector, Method method, Object instance) throws CommandParseException {
        if (!method.isAnnotationPresent(Cmd.class)) {
            throw new CommandParseException("No @Cmd present for the method " + method.toGenericString());
        }

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

    void setParameterOrder(String[] parameterOrder) {
        this.parameterOrder = parameterOrder;
    }

    ICommandAddress getAddress() {
        ChildCommandAddress result = new ChildCommandAddress();
        result.setCommand(this);

        Cmd cmd = method.getAnnotation(Cmd.class);
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
        //System.out.println("In ReflectiveCommand.execute()");

        String[] parameterOrder = this.parameterOrder;
        int start = Integer.bitCount(flags);
        //System.out.println("start = " + start);
        Object[] args = new Object[parameterOrder.length + start];

        int i = 0;
        if ((flags & 1) != 0) {
            args[i++] = sender;
        }
        if ((flags & 2) != 0) {
            args[i++] = context;
        }
        //System.out.println("i = " + i);
        //System.out.println("parameterOrder = " + Arrays.toString(parameterOrder));

        for (int n = args.length; i < n; i++) {
            //System.out.println("n = " + n);
            args[i] = context.get(parameterOrder[i - start]);
            //System.out.println("context.get(parameterOrder[i - start]) = " + context.get(parameterOrder[i - start]));
            //System.out.println("context.get(parameterOrder[i - start]).getClass() = " + context.get(parameterOrder[i - start]).getClass());
        }

        //System.out.println("args = " + Arrays.toString(args));

        Object result;
        try {
            result = method.invoke(instance, args);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof CommandException) {
                throw (CommandException) ex.getCause();
            }

            ex.printStackTrace();
            throw new CommandException("An internal error occurred while executing this command.", ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new CommandException("An internal error occurred while executing this command.", ex);
        }

        if (result instanceof String) {
            return (String) result;
        }
        if (result instanceof CommandResult) {
            return ((CommandResult) result).getMessage();
        }
        return null;
    }

}
