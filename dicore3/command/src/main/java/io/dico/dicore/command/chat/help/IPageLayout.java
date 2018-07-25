package io.dico.dicore.command.chat.help;

import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import org.bukkit.permissions.Permissible;

public interface IPageLayout {

    /**
     * Get the page borders for a help page
     *
     * @param target  the address that help is displayed for
     * @param viewer  the viewer of the help page, or null if irrelevant
     * @param context the context of the execution
     * @param pageNum the page number as displayed in the help page (so it's 1-bound and not 0-bound)
     * @return the page borders.
     */
    PageBorders getPageBorders(ICommandAddress target, Permissible viewer, ExecutionContext context, int pageNum);

}
