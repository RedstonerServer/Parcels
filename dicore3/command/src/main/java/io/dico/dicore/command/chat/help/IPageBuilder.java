package io.dico.dicore.command.chat.help;

import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import org.bukkit.permissions.Permissible;

import java.util.List;

public interface IPageBuilder {

    String getPage(List<IHelpTopic> helpTopics, IPageLayout pageLayout, ICommandAddress target, Permissible viewer, ExecutionContext context, int pageNum, int pageLen);

}
