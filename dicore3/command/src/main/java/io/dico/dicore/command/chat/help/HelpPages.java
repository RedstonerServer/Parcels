package io.dico.dicore.command.chat.help;

import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.chat.help.defaults.*;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HelpPages {
    private @NotNull IPageBuilder pageBuilder;
    private @NotNull IPageLayout pageLayout;
    private int pageLength;
    private @NotNull List<IHelpTopic> helpTopics;
    private @NotNull IHelpTopic syntaxTopic;

    public HelpPages(@NotNull IPageBuilder pageBuilder, @NotNull IPageLayout pageLayout, int pageLength, @NotNull IHelpTopic syntaxTopic, @NotNull List<IHelpTopic> helpTopics) {
        this.pageBuilder = pageBuilder;
        this.pageLayout = pageLayout;
        this.pageLength = pageLength;
        this.syntaxTopic = syntaxTopic;
        this.helpTopics = helpTopics;
    }

    public HelpPages(IPageBuilder pageBuilder, IPageLayout pageLayout, int pageLength, IHelpTopic syntaxTopic, IHelpTopic... helpTopics) {
        this(pageBuilder, pageLayout, pageLength, syntaxTopic, new ArrayList<>(Arrays.asList(helpTopics)));
    }

    @SuppressWarnings("RedundantArrayCreation")
    public static HelpPages newDefaultHelpPages() {
        IHelpTopic syntaxTopic = new SyntaxHelpTopic();
        return new HelpPages(new DefaultPageBuilder(), new DefaultPageLayout(), 12,
            syntaxTopic, new IHelpTopic[]{new DescriptionHelpTopic(), syntaxTopic, new SubcommandsHelpTopic()});
    }

    public @NotNull IPageBuilder getPageBuilder() {
        return pageBuilder;
    }

    public void setPageBuilder(@NotNull IPageBuilder pageBuilder) {
        this.pageBuilder = pageBuilder;
    }

    public @NotNull IPageLayout getPageLayout() {
        return pageLayout;
    }

    public void setPageLayout(@NotNull IPageLayout pageLayout) {
        this.pageLayout = pageLayout;
    }

    public int getPageLength() {
        return pageLength;
    }

    public void setPageLength(int pageLength) {
        this.pageLength = pageLength;
    }

    public @NotNull IHelpTopic getSyntaxTopic() {
        return syntaxTopic;
    }

    public void setSyntaxTopic(@NotNull IHelpTopic syntaxTopic) {
        this.syntaxTopic = syntaxTopic;
    }

    @NotNull
    public List<IHelpTopic> getHelpTopics() {
        return helpTopics;
    }

    public void setHelpTopics(@NotNull List<IHelpTopic> helpTopics) {
        this.helpTopics = helpTopics;
    }

    public @NotNull String getHelpPage(Permissible viewer, ExecutionContext context, ICommandAddress address, int page) {
        return pageBuilder.getPage(helpTopics, pageLayout, address, viewer, context, page, pageLength);
    }

    public @NotNull String getSyntax(Permissible viewer, ExecutionContext context, ICommandAddress address) {
        List<IHelpComponent> components = syntaxTopic.getComponents(address, viewer, context, false);
        if (components.isEmpty()) {
            return getHelpPage(viewer, context, address, 1);
        }
        return DefaultPageBuilder.combine(components);
    }

}
