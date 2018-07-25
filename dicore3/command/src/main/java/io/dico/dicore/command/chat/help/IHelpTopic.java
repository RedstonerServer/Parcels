package io.dico.dicore.command.chat.help;

import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import org.bukkit.permissions.Permissible;

import java.util.List;

public interface IHelpTopic {

    /**
     * Get the components of this help topic
     *
     * @param target  The address of the command to provide help about
     * @param viewer  The permissible that the page will be shown to (null -> choose a default set).
     * @param context Context of the command execution
     * @return a mutable list of components to include in the help pages
     */
    List<IHelpComponent> getComponents(ICommandAddress target, Permissible viewer, ExecutionContext context);


}
