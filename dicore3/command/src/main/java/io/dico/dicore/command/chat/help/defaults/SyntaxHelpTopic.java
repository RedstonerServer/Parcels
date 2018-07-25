package io.dico.dicore.command.chat.help.defaults;

import io.dico.dicore.command.Command;
import io.dico.dicore.command.EMessageType;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.chat.Formatting;
import io.dico.dicore.command.chat.help.IHelpComponent;
import io.dico.dicore.command.chat.help.IHelpTopic;
import io.dico.dicore.command.chat.help.SimpleHelpComponent;
import io.dico.dicore.command.parameter.Parameter;
import io.dico.dicore.command.parameter.ParameterList;
import org.bukkit.permissions.Permissible;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SyntaxHelpTopic implements IHelpTopic {

    @Override
    public List<IHelpComponent> getComponents(ICommandAddress target, Permissible viewer, ExecutionContext context) {
        if (!target.hasCommand()) {
            return Collections.emptyList();
        }

        String line = context.getFormat(EMessageType.SYNTAX) + "Syntax: "
                + context.getFormat(EMessageType.INSTRUCTION) + target.getAddress()
                + ' ' + getShortSyntax(target, context);

        return Collections.singletonList(new SimpleHelpComponent(line));
    }

    private static String getShortSyntax(ICommandAddress target, ExecutionContext ctx) {
        StringBuilder syntax = new StringBuilder();
        if (target.hasCommand()) {
            Formatting syntaxColor = ctx.getFormat(EMessageType.SYNTAX);
            Formatting highlight = ctx.getFormat(EMessageType.HIGHLIGHT);
            syntax.append(syntaxColor);

            Command command = target.getCommand();
            ParameterList list = command.getParameterList();
            Parameter<?, ?> repeated = list.getRepeatedParameter();

            int requiredCount = list.getRequiredCount();
            List<Parameter<?, ?>> indexedParameters = list.getIndexedParameters();
            for (int i = 0, n = indexedParameters.size(); i < n; i++) {
                syntax.append(i < requiredCount ? " <" : " [");
                Parameter<?, ?> param = indexedParameters.get(i);
                syntax.append(param.getName());
                if (param == repeated) {
                    syntax.append(highlight).append("...").append(syntaxColor);
                }
                syntax.append(i < requiredCount ? '>' : ']');
            }

            Map<String, Parameter<?, ?>> parametersByName = list.getParametersByName();
            for (Parameter<?, ?> param : parametersByName.values()) {
                if (param.isFlag()) {
                    syntax.append(" [").append(param.getName());
                    if (param.expectsInput()) {
                        syntax.append(" <").append(param.getName()).append(">");
                    }
                    syntax.append(']');
                }
            }

        } else {
            syntax.append(' ');
        }
        return syntax.toString();
    }

}
