package io.dico.dicore.command.registration.reflect;

import io.dico.dicore.command.*;
import io.dico.dicore.command.annotation.*;
import io.dico.dicore.command.annotation.GroupMatchedCommands.GroupEntry;
import io.dico.dicore.command.parameter.IArgumentPreProcessor;
import io.dico.dicore.command.parameter.Parameter;
import io.dico.dicore.command.parameter.ParameterList;
import io.dico.dicore.command.parameter.type.IParameterTypeSelector;
import io.dico.dicore.command.parameter.type.MapBasedParameterTypeSelector;
import io.dico.dicore.command.parameter.type.ParameterType;
import io.dico.dicore.command.parameter.type.ParameterTypes;
import io.dico.dicore.command.predef.PredefinedCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Takes care of turning a reflection {@link Method} into a command and more.
 */
public class ReflectiveRegistration {
    /**
     * This object provides names of the parameters.
     * Oddly, the AnnotationParanamer extensions require a 'fallback' paranamer to function properly without
     * requiring ALL parameters to have that flag. This is weird because it should just use the AdaptiveParanamer on an upper level to
     * determine the name of each individual flag. Oddly this isn't how it works, so the fallback works the same way as the AdaptiveParanamer does.
     * It's just linked instead of using an array for that part. Then we can use an AdaptiveParanamer for the latest fallback, to get bytecode names
     * or, finally, to get the Jvm-provided parameter names.
     */
    //private static final Paranamer paranamer = new CachingParanamer(new BytecodeReadingParanamer());
    @SuppressWarnings("StatementWithEmptyBody")
    private static String[] lookupParameterNames(Method method, java.lang.reflect.Parameter[] parameters, int start) {
        int n = parameters.length;
        String[] out = new String[n - start];

        //String[] bytecode;
        //try {
        //    bytecode = paranamer.lookupParameterNames(method, false);
        //} catch (Exception ex) {
        //    bytecode = new String[0];
        //    System.err.println("ReflectiveRegistration.lookupParameterNames failed to read bytecode");
        //    //ex.printStackTrace();
        //}
        //int bn = bytecode.length;

        for (int i = start; i < n; i++) {
            java.lang.reflect.Parameter parameter = parameters[i];
            Flag flag = parameter.getAnnotation(Flag.class);
            NamedArg namedArg = parameter.getAnnotation(NamedArg.class);

            boolean isFlag = flag != null;
            String name;
            if (namedArg != null && !(name = namedArg.value()).isEmpty()) {
            } else if (isFlag && !(name = flag.value()).isEmpty()) {
                //} else if (i < bn && (name = bytecode[i]) != null && !name.isEmpty()) {
            } else {
                name = parameter.getName();
            }

            if (isFlag) {
                name = '-' + name;
            } else {
                int idx = 0;
                while (name.startsWith("-", idx)) {
                    idx++;
                }
                name = name.substring(idx);
            }

            out[i - start] = name;
        }

        return out;
    }

    public static void parseCommandGroup(ICommandAddress address, Class<?> clazz, Object instance) throws CommandParseException {
        parseCommandGroup(address, ParameterTypes.getSelector(), clazz, instance);
    }

    public static void parseCommandGroup(ICommandAddress address, IParameterTypeSelector selector, Class<?> clazz, Object instance) throws CommandParseException {
        boolean requireStatic = instance == null;
        if (!requireStatic && !clazz.isInstance(instance)) {
            throw new CommandParseException();
        }

        List<Method> methods = new LinkedList<>(Arrays.asList(clazz.getDeclaredMethods()));

        Iterator<Method> it = methods.iterator();
        for (Method method; it.hasNext(); ) {
            method = it.next();

            if (requireStatic && !Modifier.isStatic(method.getModifiers())) {
                it.remove();
                continue;
            }

            if (method.isAnnotationPresent(CmdParamType.class)) {
                it.remove();

                if (method.getReturnType() != ParameterType.class || method.getParameterCount() != 0) {
                    throw new CommandParseException("Invalid CmdParamType method: must return ParameterType and take no arguments");
                }

                ParameterType<?, ?> type;
                try {
                    Object inst = Modifier.isStatic(method.getModifiers()) ? null : instance;
                    type = (ParameterType<?, ?>) method.invoke(inst);
                    Objects.requireNonNull(type, "ParameterType returned is null");
                } catch (Exception ex) {
                    throw new CommandParseException("Error occurred whilst getting ParameterType from CmdParamType method '" + method.toGenericString() + "'", ex);
                }

                if (selector == ParameterTypes.getSelector()) {
                    selector = new MapBasedParameterTypeSelector(true);
                }

                selector.addType(method.getAnnotation(CmdParamType.class).infolessAlias(), type);
            }
        }

        GroupMatcherCache groupMatcherCache = new GroupMatcherCache(clazz, address);
        for (Method method : methods) {
            if (method.isAnnotationPresent(Cmd.class)) {
                ICommandAddress parsed = parseCommandMethod(selector, method, instance);
                groupMatcherCache.getGroupFor(method).addChild(parsed);
            }
        }

    }

    private static final class GroupMatcherCache {
        private ModifiableCommandAddress groupRootAddress;
        private GroupEntry[] matchEntries;
        private Pattern[] patterns;
        private ModifiableCommandAddress[] addresses;

        GroupMatcherCache(Class<?> clazz, ICommandAddress groupRootAddress) throws CommandParseException {
            this.groupRootAddress = (ModifiableCommandAddress) groupRootAddress;

            GroupMatchedCommands groupMatchedCommands = clazz.getAnnotation(GroupMatchedCommands.class);
            GroupEntry[] matchEntries = groupMatchedCommands == null ? new GroupEntry[0] : groupMatchedCommands.value();

            Pattern[] patterns = new Pattern[matchEntries.length];
            for (int i = 0; i < matchEntries.length; i++) {
                GroupEntry matchEntry = matchEntries[i];
                if (matchEntry.group().isEmpty() || matchEntry.regex().isEmpty()) {
                    throw new CommandParseException("Empty group or regex in GroupMatchedCommands entry");
                }
                try {
                    patterns[i] = Pattern.compile(matchEntry.regex());
                } catch (PatternSyntaxException ex) {
                    throw new CommandParseException(ex);
                }
            }

            this.matchEntries = matchEntries;
            this.patterns = patterns;
            this.addresses = new ModifiableCommandAddress[this.matchEntries.length];
        }

        ModifiableCommandAddress getGroupFor(Method method) {
            String name = method.getName();

            GroupEntry[] matchEntries = this.matchEntries;
            Pattern[] patterns = this.patterns;
            ModifiableCommandAddress[] addresses = this.addresses;

            for (int i = 0; i < matchEntries.length; i++) {
                GroupEntry matchEntry = matchEntries[i];
                if (patterns[i].matcher(name).matches()) {
                    if (addresses[i] == null) {
                        ChildCommandAddress placeholder = new ChildCommandAddress();
                        placeholder.setupAsPlaceholder(matchEntry.group(), matchEntry.groupAliases());
                        addresses[i] = placeholder;
                        groupRootAddress.addChild(placeholder);
                        generateCommands(placeholder, matchEntry.generatedCommands());
                        setDescription(placeholder, matchEntry.description(), matchEntry.shortDescription());
                    }
                    return addresses[i];
                }
            }

            return groupRootAddress;
        }

    }

    public static ICommandAddress parseCommandMethod(IParameterTypeSelector selector, Method method, Object instance) throws CommandParseException {
        return new ReflectiveCommand(selector, method, instance).getAddress();
    }

    static int parseCommandAttributes(IParameterTypeSelector selector, Method method, ReflectiveCommand command, java.lang.reflect.Parameter[] parameters) throws CommandParseException {
        ParameterList list = command.getParameterList();
        boolean hasReceiverParameter = false;
        boolean hasSenderParameter = false;
        int start = 0;
        Class<?> firstParameterType = null;
        Class<?> senderParameterType = null;

        if (parameters.length > start
            && command.getInstance() instanceof ICommandReceiver.Factory
            && ICommandReceiver.class.isAssignableFrom(firstParameterType = parameters[start].getType())) {
            hasReceiverParameter = true;
            start++;
        }

        if (parameters.length > start && CommandSender.class.isAssignableFrom(senderParameterType = parameters[start].getType())) {
            hasSenderParameter = true;
            start++;
        }

        boolean hasContextParameter = false;
        if (parameters.length > start && parameters[start].getType() == ExecutionContext.class) {
            hasContextParameter = true;
            start++;
        }

        String[] parameterNames = lookupParameterNames(method, parameters, start);
        for (int i = start, n = parameters.length; i < n; i++) {
            if (parameters[i].getType().getName().equals("kotlin.coroutines.Continuation")) {
                List<String> temp = new ArrayList<>(Arrays.asList(parameterNames));
                temp.remove(i - start);
                parameterNames = temp.toArray(new String[0]);
                continue;
            }
            Parameter<?, ?> parameter = parseParameter(selector, method, parameters[i], parameterNames[i - start]);
            list.addParameter(parameter);
        }
        command.setParameterOrder(parameterNames);

        RequirePermissions cmdPermissions = method.getAnnotation(RequirePermissions.class);
        if (cmdPermissions != null) {
            for (String permission : cmdPermissions.value()) {
                command.addContextFilter(IContextFilter.permission(permission));
            }

            if (cmdPermissions.inherit()) {
                command.addContextFilter(IContextFilter.INHERIT_PERMISSIONS);
            }
        } else {
            command.addContextFilter(IContextFilter.INHERIT_PERMISSIONS);
        }

        RequireParameters reqPar = method.getAnnotation(RequireParameters.class);
        if (reqPar != null) {
            list.setRequiredCount(reqPar.value() < 0 ? Integer.MAX_VALUE : reqPar.value());
        } else {
            list.setRequiredCount(list.getIndexedParameters().size());
        }

        PreprocessArgs preprocessArgs = method.getAnnotation(PreprocessArgs.class);
        if (preprocessArgs != null) {
            IArgumentPreProcessor preProcessor = IArgumentPreProcessor.mergeOnTokens(preprocessArgs.tokens(), preprocessArgs.escapeChar());
            list.setArgumentPreProcessor(preProcessor);
        }

        Desc desc = method.getAnnotation(Desc.class);
        if (desc != null) {
            String[] array = desc.value();
            if (array.length == 0) {
                command.setDescription(desc.shortVersion());
            } else {
                command.setDescription(array);
            }
        } else {
            command.setDescription();
        }

        if (hasSenderParameter && Player.class.isAssignableFrom(senderParameterType)) {
            command.addContextFilter(IContextFilter.PLAYER_ONLY);
        } else if (hasSenderParameter && ConsoleCommandSender.class.isAssignableFrom(senderParameterType)) {
            command.addContextFilter(IContextFilter.CONSOLE_ONLY);
        } else if (method.isAnnotationPresent(RequirePlayer.class)) {
            command.addContextFilter(IContextFilter.PLAYER_ONLY);
        } else if (method.isAnnotationPresent(RequireConsole.class)) {
            command.addContextFilter(IContextFilter.CONSOLE_ONLY);
        }

        list.setRepeatFinalParameter(parameters.length > start && parameters[parameters.length - 1].isVarArgs());
        list.setFinalParameterMayBeFlag(true);
        return (hasSenderParameter ? 2 : 0) | (hasContextParameter ? 4 : 0) | (hasReceiverParameter ? 1 : 0);
    }

    public static int parseCommandAttributes(IParameterTypeSelector selector, Method method, ReflectiveCommand command) throws CommandParseException {
        return parseCommandAttributes(selector, method, command, method.getParameters());
    }

    public static Parameter<?, ?> parseParameter(IParameterTypeSelector selector, Method method, java.lang.reflect.Parameter parameter, String name) throws CommandParseException {
        Class<?> type = parameter.getType();
        if (parameter.isVarArgs()) {
            type = type.getComponentType();
        }

        Annotation[] annotations = parameter.getAnnotations();
        Flag flag = null;
        Annotation typeAnnotation = null;
        Desc desc = null;

        for (Annotation annotation : annotations) {
            //noinspection StatementWithEmptyBody
            if (annotation instanceof NamedArg) {
                // do nothing
            } else if (annotation instanceof Flag) {
                if (flag != null) {
                    throw new CommandParseException("Multiple flags for the same parameter");
                }
                flag = (Flag) annotation;
            } else if (annotation instanceof Desc) {
                if (desc != null) {
                    throw new CommandParseException("Multiple descriptions for the same parameter");
                }
                desc = (Desc) annotation;
            } else {
                if (typeAnnotation != null) {
                    throw new CommandParseException("Multiple parameter type annotations for the same parameter");
                }
                typeAnnotation = annotation;
            }
        }

        if (flag == null && name.startsWith("-")) {
            throw new CommandParseException("Non-flag parameter's name starts with -");
        } else if (flag != null && !name.startsWith("-")) {
            throw new CommandParseException("Flag parameter's name doesn't start with -");
        }

        ParameterType<Object, Object> parameterType = selector.selectAny(type, typeAnnotation == null ? null : typeAnnotation.getClass());
        if (parameterType == null) {
            throw new CommandParseException("IParameter type not found for parameter " + name + " in method " + method.toString());
        }

        Object parameterInfo;
        if (typeAnnotation == null) {
            parameterInfo = null;
        } else try {
            parameterInfo = parameterType.getParameterConfig() == null ? null : parameterType.getParameterConfig().getParameterInfo(typeAnnotation);
        } catch (Exception ex) {
            throw new CommandParseException("Invalid parameter config", ex);
        }

        String descString = desc == null ? null : CommandAnnotationUtils.getShortDescription(desc);

        try {
            //noinspection unchecked
            String flagPermission = flag == null ? null : flag.permission();
            return new Parameter<>(name, descString, parameterType, parameterInfo, type.isPrimitive(), name.startsWith("-"), flagPermission);
        } catch (Exception ex) {
            throw new CommandParseException("Invalid parameter", ex);
        }
    }

    public static void generateCommands(ICommandAddress address, String[] input) {
        for (String value : input) {
            Consumer<ICommandAddress> consumer = PredefinedCommand.getPredefinedCommandGenerator(value);
            if (consumer == null) {
                System.out.println("[Command Warning] generated command '" + value + "' could not be found");
            } else {
                consumer.accept(address);
            }
        }
    }
    
    /*
    Desired format
    
    @Cmd({"tp", "tpto"})
    @RequirePermissions("teleport.self")
    public (static) String|void|CommandResult onCommand(Player sender, Player target, @Flag("force", permission = "teleport.self.force") boolean force) {
        Validate.isTrue(force || !hasTpToggledOff(target), "Target has teleportation disabled. Use -force to ignore");
        sender.teleport(target);
        //return
    }
    
    parser needs to:
    - see the @Cmd and create a CommandTree for it
    - see that it must be a Player executing the command
    - add an indexed IParameter for a Player type
    - add a flag parameter named force, that consumes no arguments.
    - see that setting the force flag requires a permission
     */

    private static void setDescription(ICommandAddress address, String[] array, String shortVersion) {
        if (!address.hasCommand()) {
            return;
        }

        if (array.length == 0) {
            address.getCommand().setDescription(shortVersion);
        } else {
            address.getCommand().setDescription(array);
        }

    }

}
