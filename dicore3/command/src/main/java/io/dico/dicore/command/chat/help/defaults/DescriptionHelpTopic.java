package io.dico.dicore.command.chat.help.defaults;

import io.dico.dicore.command.Command;
import io.dico.dicore.command.EMessageType;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.chat.Formatting;
import io.dico.dicore.command.chat.help.IHelpComponent;
import io.dico.dicore.command.chat.help.IHelpTopic;
import io.dico.dicore.command.chat.help.SimpleHelpComponent;
import org.bukkit.permissions.Permissible;

import java.util.ArrayList;
import java.util.List;

public class DescriptionHelpTopic implements IHelpTopic {

    @Override
    public List<IHelpComponent> getComponents(ICommandAddress target, Permissible viewer, ExecutionContext context, boolean isForPage) {
        List<IHelpComponent> out = new ArrayList<>();
        Formatting format = context.getFormat(EMessageType.DESCRIPTION);

        if (!target.hasCommand()) {
            return out;
        }
        Command command = target.getCommand();
        String[] description = command.getDescription();
        if (description.length == 0) {
            String shortDescription = command.getShortDescription();
            if (shortDescription == null) {
                return out;
            }

            description = new String[]{shortDescription};
        }

        for (int i = 0; i < description.length; i++) {
            description[i] = format + description[i];
        }

        out.add(new SimpleHelpComponent(description));
        return out;
    }

}
