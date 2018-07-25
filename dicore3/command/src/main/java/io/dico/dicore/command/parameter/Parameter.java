package io.dico.dicore.command.parameter;

import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.Validate;
import io.dico.dicore.command.annotation.Range;
import io.dico.dicore.command.parameter.type.ParameterType;
import org.bukkit.Location;

import java.util.List;
import java.util.Objects;

/**
 * IParameter object.
 *
 * @param <TResult>    the parameter's type
 * @param <TParamInfo> the parameter info object. Example: {@link Range.Memory}
 */
public class Parameter<TResult, TParamInfo> {
    private final String name;
    private final String description;
    private final ParameterType<TResult, TParamInfo> parameterType;
    private final TParamInfo paramInfo;
    private final boolean flag;
    private final String flagPermission;

    public Parameter(String name, String description, ParameterType<TResult, TParamInfo> parameterType, TParamInfo paramInfo) {
        this(name, description, parameterType, paramInfo, false, null);
    }

    public Parameter(String name, String description, ParameterType<TResult, TParamInfo> parameterType, TParamInfo paramInfo, boolean flag, String flagPermission) {
        this.name = Objects.requireNonNull(name);
        this.description = description == null ? "" : description;
        this.parameterType = flag ? parameterType.asFlagParameter() : parameterType;
        /*
        if (paramInfo == null && parameterType.getParameterConfig() != null) {
            paramInfo = parameterType.getParameterConfig().getDefaultValue();
        }
        */
        this.paramInfo = paramInfo;

        this.flag = flag;
        this.flagPermission = flagPermission;

        if (flag && !name.startsWith("-")) {
            throw new IllegalArgumentException("Flag parameter's name must start with -");
        } else if (!flag && name.startsWith("-")) {
            throw new IllegalArgumentException("Non-flag parameter's name may not start with -");
        }
    }

    public static <TResult> Parameter<TResult, ?> newParameter(String name, String description, ParameterType<TResult, ?> type) {
        return new Parameter<>(name, description, type, null);
    }

    public static <TResult, TParamInfo> Parameter<TResult, TParamInfo> newParameter(String name, String description, ParameterType<TResult, TParamInfo> type, TParamInfo info) {
        return new Parameter<>(name, description, type, info);
    }

    public static <TResult, TParamInfo> Parameter<TResult, TParamInfo> newParameter(String name, String description, ParameterType<TResult, TParamInfo> parameterType, TParamInfo paramInfo, boolean flag, String flagPermission) {
        return new Parameter<>(name, description, parameterType, paramInfo, flag, flagPermission);
    }

    public TResult parse(ExecutionContext context, ArgumentBuffer buffer) throws CommandException {
        if (getFlagPermission() != null) {
            Validate.isAuthorized(context.getSender(), getFlagPermission(), "You do not have permission to use the flag " + name);
        }
        return checkAllowed(context, parameterType.parseForContext(this, context, buffer));
    }

    public TResult getDefaultValue(ExecutionContext context, ArgumentBuffer buffer) throws CommandException {
        return parameterType.getDefaultValueForContext(this, context, buffer);
    }

    public List<String> complete(ExecutionContext context, Location location, ArgumentBuffer buffer) {
        return parameterType.completeForContext(this, context, location, buffer);
    }

    public TResult checkAllowed(ExecutionContext context, TResult result) throws CommandException {
        return result;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ParameterType<TResult, TParamInfo> getType() {
        return parameterType;
    }

    public TParamInfo getParamInfo() {
        return paramInfo;
    }

    public boolean isFlag() {
        return flag;
    }

    // override with false for the flag parameter that simply must be present
    public boolean expectsInput() {
        return parameterType.getExpectedAmountOfConsumedArguments() > 0;
    }

    public String getFlagPermission() {
        return flag ? flagPermission : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Parameter)) return false;
        /*
        IParameter<?, ?> parameter = (IParameter<?, ?>) o;
        
        return name.equals(parameter.name);
        */
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
