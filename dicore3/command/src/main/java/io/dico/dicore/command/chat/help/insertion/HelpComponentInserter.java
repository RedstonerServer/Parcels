package io.dico.dicore.command.chat.help.insertion;

import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.chat.help.HelpTopicModifier;
import io.dico.dicore.command.chat.help.IHelpComponent;
import io.dico.dicore.command.chat.help.IHelpTopic;
import org.bukkit.permissions.Permissible;

import java.util.ArrayList;
import java.util.List;

public class HelpComponentInserter extends HelpTopicModifier {
    private List<IInsertion> insertions = new ArrayList<>();

    public HelpComponentInserter(IHelpTopic delegate) {
        super(delegate);
    }

    @Override
    protected List<IHelpComponent> modify(List<IHelpComponent> components, ICommandAddress target, Permissible viewer, ExecutionContext context) {
        // int componentCount = components.size();

        for (int i = insertions.size() - 1; i >= 0; i--) {
            IInsertion insertion = insertions.get(i);
            int idx = insertion.insertionIndex(components, target, viewer, context);
            List<IHelpComponent> inserted = insertion.getComponents(target, viewer, context, true);
            components.addAll(idx, inserted);
        }

        return components;
    }

    public HelpComponentInserter insert(IInsertionFunction insertionFunction, IHelpTopic helpTopic) {
        return insert(Insertions.combine(helpTopic, insertionFunction));
    }

    public HelpComponentInserter insert(IInsertion insertion) {
        insertions.add(insertion);
        return this;
    }

}
