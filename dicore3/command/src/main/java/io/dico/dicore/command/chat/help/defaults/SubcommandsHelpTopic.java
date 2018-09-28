package io.dico.dicore.command.chat.help.defaults;

import io.dico.dicore.command.EMessageType;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.Formatting;
import io.dico.dicore.command.chat.help.IHelpComponent;
import io.dico.dicore.command.chat.help.IHelpTopic;
import io.dico.dicore.command.chat.help.SimpleHelpComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

import java.util.*;

public class SubcommandsHelpTopic implements IHelpTopic {

    @Override
    public List<IHelpComponent> getComponents(ICommandAddress target, Permissible viewer, ExecutionContext context, boolean isForPage) {
        Collection<String> mainKeys = target.getChildrenMainKeys();
        if (mainKeys.isEmpty()) {
            return Collections.emptyList();
        }

        List<IHelpComponent> result = new ArrayList<>();

        mainKeys = new ArrayList<>(target.getChildrenMainKeys());
        ((ArrayList<String>) mainKeys).sort(null);

        CommandSender sender = viewer instanceof CommandSender ? (CommandSender) viewer : context.getSender();
        for (String key : mainKeys) {
            ICommandAddress child = target.getChild(key);
            if ((child.hasChildren() || child.hasUserDeclaredCommand()) && child.getCommand().isVisibleTo(sender)) {
                result.add(getComponent(child, viewer, context));
            }
        }

        return result;
    }

    public IHelpComponent getComponent(ICommandAddress child, Permissible viewer, ExecutionContext context) {
        Formatting subcommand = colorOf(context, EMessageType.SUBCOMMAND);
        Formatting highlight = colorOf(context, EMessageType.HIGHLIGHT);

        String address = subcommand + "/" + child.getParent().getAddress() + ' ' + highlight + child.getMainKey();

        String description = child.hasCommand() ? child.getCommand().getShortDescription() : null;
        if (description != null) {
            Formatting descriptionFormat = colorOf(context, EMessageType.DESCRIPTION);
            return new SimpleHelpComponent(address, descriptionFormat + description);
        }

        return new SimpleHelpComponent(address);
    }

    private static Formatting colorOf(ExecutionContext context, EMessageType type) {
        return context.getAddress().getChatHandler().getChatFormatForType(type);
    }

}
