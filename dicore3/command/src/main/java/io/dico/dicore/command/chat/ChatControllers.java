package io.dico.dicore.command.chat;

import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.chat.help.IHelpComponent;
import io.dico.dicore.command.chat.help.IHelpTopic;
import io.dico.dicore.command.chat.help.IPageBuilder;
import io.dico.dicore.command.chat.help.IPageLayout;
import io.dico.dicore.command.chat.help.defaults.*;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * Static factory methods for {@link IChatController}
 */
public class ChatControllers {
    private static final IChatController defaultChat;

    private ChatControllers() {

    }

    public static IChatController defaultChat() {
        return defaultChat;
    }

    static {
        defaultChat = new AbstractChatController() {
            IPageBuilder pageBuilder = new DefaultPageBuilder();
            IPageLayout pageLayout = new DefaultPageLayout();
            List<IHelpTopic> topics = Arrays.asList(new DescriptionHelpTopic(), new SyntaxHelpTopic(), new SubcommandsHelpTopic());

            @Override
            public void sendHelpMessage(CommandSender sender, ExecutionContext context, ICommandAddress address, int page) {
                sender.sendMessage(pageBuilder.getPage(topics, pageLayout, address, sender, context, page, 12));
            }

            @Override
            public void sendSyntaxMessage(CommandSender sender, ExecutionContext context, ICommandAddress address) {
                List<IHelpComponent> components = topics.get(1).getComponents(address, sender, context);
                if (components.isEmpty()) {
                    sendHelpMessage(sender, context, address, 1);
                } else {
                    sender.sendMessage(DefaultPageBuilder.combine(components));
                }
            }

        };
    }
}
