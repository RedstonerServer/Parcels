package io.dico.dicore.command.registration.reflect;

import io.dico.dicore.command.*;
import io.dico.dicore.command.annotation.Cmd;
import io.dico.dicore.command.annotation.GenerateCommands;
import io.dico.dicore.command.parameter.type.IParameterTypeSelector;
import io.dico.dicore.exceptions.checkedfunctions.CheckedSupplier;
import kotlin.coroutines.CoroutineContext;
import org.bukkit.command.CommandSender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ReflectiveCommand extends Command {
    private final Cmd cmdAnnotation;
    private final Method method;
    private final Object instance;
    private String[] parameterOrder;
    private final int callFlags;

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
        this.callFlags = ReflectiveRegistration.parseCommandAttributes(selector, method, this, parameters);
    }

    public Method getMethod() {
        return method;
    }

    public Object getInstance() {
        return instance;
    }

    public String getCmdName() {
        return cmdAnnotation.value();
    }

    public int getCallFlags() {
        return callFlags;
    }

    void setParameterOrder(String[] parameterOrder) {
        this.parameterOrder = parameterOrder;
    }

    public int getParameterNum() {
        return parameterOrder.length;
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

        CheckedSupplier<Object, CommandException> receiverFunction = () -> {
            try {
                return ((ICommandInterceptor) instance).getReceiver(context, method, getCmdName());
            } catch (Exception ex) {
                handleException(ex);
                return null; // unreachable
            }
        };

        Object[] callArgs = ReflectiveCallFlags.getCallArgs(callFlags, context, parameterOrder, receiverFunction);

        if (ReflectiveCallFlags.hasCallArg(callFlags, ReflectiveCallFlags.CONTINUATION_BIT)) {
            // If it has a continuation, call as coroutine
            return callAsCoroutine(context, callArgs);
        }

        return callSynchronously(callArgs);
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

    private String callAsCoroutine(ExecutionContext executionContext, Object[] args) throws CommandException {
        ICommandInterceptor factory = (ICommandInterceptor) instance;
        CoroutineContext coroutineContext = (CoroutineContext) factory.getCoroutineContext(executionContext, method, getCmdName());
        int continuationIndex = ReflectiveCallFlags.getCallArgIndex(callFlags, ReflectiveCallFlags.CONTINUATION_BIT, parameterOrder.length);
        return KotlinReflectiveRegistrationKt.callCommandAsCoroutine(executionContext, coroutineContext, continuationIndex, method, instance, args);
    }

}
