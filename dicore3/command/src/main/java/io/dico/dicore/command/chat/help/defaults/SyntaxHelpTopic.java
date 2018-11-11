package io.dico.dicore.command.chat.help.defaults;

import io.dico.dicore.command.Command;
import io.dico.dicore.command.EMessageType;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.Formatting;
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
    public List<IHelpComponent> getComponents(ICommandAddress target, Permissible viewer, ExecutionContext context, boolean isForPage) {
        if (!target.hasCommand()) {
            return Collections.emptyList();
        }

        if (target.hasChildren()) {
            if (!isForPage) {
                // HelpPages will send help instead of syntax, which might in turn include syntax as well.
                return Collections.emptyList();
            }

            if (!target.hasUserDeclaredCommand() && !target.getCommand().getParameterList().hasAnyParameters()) {
                // no point adding syntax at all
                return Collections.emptyList();
            }
        }

        StringBuilder line = new StringBuilder();
        if (isForPage)
            line.append(context.getFormat(EMessageType.SYNTAX))
                .append("Syntax: ");

        line.append('/')
            .append(context.getFormat(EMessageType.INSTRUCTION))
            .append(target.getAddress())
            .append(' ');

        addShortSyntax(line, target, context);

        return Collections.singletonList(new SimpleHelpComponent(line.toString()));
    }

    private static void addShortSyntax(StringBuilder builder, ICommandAddress address, ExecutionContext ctx) {
        if (address.hasCommand()) {
            Formatting syntaxColor = ctx.getFormat(EMessageType.SYNTAX);
            Formatting highlight = ctx.getFormat(EMessageType.HIGHLIGHT);
            builder.append(syntaxColor);

            Command command = address.getCommand();
            ParameterList list = command.getParameterList();
            Parameter<?, ?> repeated = list.getRepeatedParameter();

            int requiredCount = list.getRequiredCount();
            List<Parameter<?, ?>> indexedParameters = list.getIndexedParameters();
            for (int i = 0, n = indexedParameters.size(); i < n; i++) {
                builder.append(i < requiredCount ? " <" : " [");
                Parameter<?, ?> param = indexedParameters.get(i);
                builder.append(param.getName());
                if (param == repeated) {
                    builder.append(highlight).append("...").append(syntaxColor);
                }
                builder.append(i < requiredCount ? '>' : ']');
            }

            Map<String, Parameter<?, ?>> parametersByName = list.getParametersByName();
            for (Parameter<?, ?> param : parametersByName.values()) {
                if (param.isFlag()) {
                    builder.append(" [").append(param.getName());
                    if (param.expectsInput()) {
                        builder.append(" <").append(param.getName()).append(">");
                    }
                    builder.append(']');
                }
            }

        } else {
            builder.append(' ');
        }
    }

}
