package io.dico.dicore.command.chat.help.insertion;

import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.chat.help.IHelpComponent;
import io.dico.dicore.command.chat.help.IHelpTopic;
import org.bukkit.permissions.Permissible;

import java.util.List;

public class Insertions {

    private Insertions() {

    }

    public static IInsertion combine(IHelpTopic topic, IInsertionFunction function) {
        return new IInsertion() {
            @Override
            public List<IHelpComponent> getComponents(ICommandAddress target, Permissible viewer, ExecutionContext context, boolean isForPage) {
                return topic.getComponents(target, viewer, context, true);
            }

            @Override
            public int insertionIndex(List<IHelpComponent> current, ICommandAddress target, Permissible viewer, ExecutionContext context) {
                return function.insertionIndex(current, target, viewer, context);
            }
        };
    }

}
