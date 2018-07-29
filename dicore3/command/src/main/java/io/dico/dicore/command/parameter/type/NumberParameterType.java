package io.dico.dicore.command.parameter.type;

import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.annotation.Range;
import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.parameter.Parameter;
import org.bukkit.command.CommandSender;

/**
 * Abstraction for number parameter types which use {@link Range.Memory} as parameter info.
 *
 * @param <T> the Number subclass.
 */
public abstract class NumberParameterType<T extends Number> extends ParameterType<T, Range.Memory> {

    public NumberParameterType(Class<T> returnType) {
        super(returnType, Range.CONFIG);
    }

    protected abstract T parse(String input) throws NumberFormatException;

    protected abstract T select(Number number);

    @Override
    public T parse(Parameter<T, Range.Memory> parameter, CommandSender sender, ArgumentBuffer buffer) throws CommandException {
        //System.out.println("In NumberParameterType:parse() for class " + getReturnType().toGenericString());

        String input = buffer.next();
        if (input == null) {
            throw CommandException.missingArgument(parameter.getName());
        }

        T result;
        try {
            result = parse(input);
        } catch (Exception ex) {
            throw CommandException.invalidArgument(parameter.getName(), "a number");
        }

        Range.Memory memory = (Range.Memory) parameter.getParamInfo();
        if (memory != null) {
            memory.validate(result, "Argument " + parameter.getName() + " is out of range ["
                    + select(memory.min()) + ", " + select(memory.max()) + "]: " + result);
        }

        return result;
    }

    @Override
    public T getDefaultValue(Parameter<T, Range.Memory> parameter, CommandSender sender, ArgumentBuffer buffer) throws CommandException {
        Range.Memory memory = (Range.Memory) parameter.getParamInfo();
        if (memory != null) return select(memory.defaultValue());
        return !parameter.isPrimitive() ? null : select(0);
    }

}
