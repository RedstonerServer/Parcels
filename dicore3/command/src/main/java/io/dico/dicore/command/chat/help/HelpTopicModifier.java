package io.dico.dicore.command.chat.help;

import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import org.bukkit.permissions.Permissible;

import java.util.List;
import java.util.Objects;

public abstract class HelpTopicModifier implements IHelpTopic {
    private final IHelpTopic delegate;

    public HelpTopicModifier(IHelpTopic delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public List<IHelpComponent> getComponents(ICommandAddress target, Permissible viewer, ExecutionContext context, boolean isForPage) {
        return modify(delegate.getComponents(target, viewer, context, true), target, viewer, context);
    }

    protected abstract List<IHelpComponent> modify(List<IHelpComponent> components, ICommandAddress target, Permissible viewer, ExecutionContext context);

}
