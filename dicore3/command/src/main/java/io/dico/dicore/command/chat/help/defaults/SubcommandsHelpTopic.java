package io.dico.dicore.command.chat.help.defaults;

import io.dico.dicore.command.EMessageType;
import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.chat.Formatting;
import io.dico.dicore.command.chat.help.IHelpComponent;
import io.dico.dicore.command.chat.help.IHelpTopic;
import io.dico.dicore.command.chat.help.SimpleHelpComponent;
import io.dico.dicore.command.predef.PredefinedCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SubcommandsHelpTopic implements IHelpTopic {

    @Override
    public List<IHelpComponent> getComponents(ICommandAddress target, Permissible viewer, ExecutionContext context) {
        List<IHelpComponent> out = new ArrayList<>();
        Map<String, ? extends ICommandAddress> children = target.getChildren();
        if (children.isEmpty()) {
            //System.out.println("No subcommands");
            return out;
        }

        CommandSender sender = viewer instanceof CommandSender ? (CommandSender) viewer : context.getSender();
        children.values().stream().distinct().forEach(child -> {
            if ((!child.hasCommand() || child.getCommand().isVisibleTo(sender)) && !(child instanceof PredefinedCommand)) {
                out.add(getComponent(child, viewer, context));
            }
        });

        return out;
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
        return context.getAddress().getChatController().getChatFormatForType(type);
    }

}
