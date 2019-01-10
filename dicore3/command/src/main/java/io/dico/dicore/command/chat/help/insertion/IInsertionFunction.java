package io.dico.dicore.command.chat.help.insertion;

import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.chat.help.IHelpComponent;
import org.bukkit.permissions.Permissible;

import java.util.List;

public interface IInsertionFunction {

    int insertionIndex(List<IHelpComponent> current, ICommandAddress target, Permissible viewer, ExecutionContext context);

}
