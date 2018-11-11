package io.dico.dicore.command.parameter.type;

import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.parameter.Parameter;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class EnumParameterType<E extends Enum> extends SimpleParameterType<E, Void> {
    private final E[] universe;

    public EnumParameterType(Class<E> returnType) {
        super(returnType);
        universe = returnType.getEnumConstants();
        if (universe == null) {
            throw new IllegalArgumentException("returnType must be an enum");
        }
    }

    @Override
    protected E parse(Parameter<E, Void> parameter, CommandSender sender, String input) throws CommandException {
        for (E constant : universe) {
            if (constant.name().equalsIgnoreCase(input)) {
                return constant;
            }
        }

        throw CommandException.invalidArgument(parameter.getName(), "the enum value does not exist");
    }

    @Override
    public List<String> complete(Parameter<E, Void> parameter, CommandSender sender, Location location, ArgumentBuffer buffer) {
        String input = buffer.next().toUpperCase();
        List<String> result = new ArrayList<>();
        for (E constant : universe) {
            if (constant.name().toUpperCase().startsWith(input.toUpperCase())) {
                result.add(constant.name().toLowerCase());
            }
        }
        return result;
    }

}
