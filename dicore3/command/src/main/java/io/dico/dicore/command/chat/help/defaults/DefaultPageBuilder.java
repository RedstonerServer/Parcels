package io.dico.dicore.command.chat.help.defaults;

import io.dico.dicore.command.ExecutionContext;
import io.dico.dicore.command.ICommandAddress;
import io.dico.dicore.command.chat.help.*;
import org.bukkit.permissions.Permissible;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class DefaultPageBuilder implements IPageBuilder {

    @Override
    public String getPage(List<IHelpTopic> helpTopics, IPageLayout pageLayout, ICommandAddress target,
                          Permissible viewer, ExecutionContext context, int pageNum, int pageLen) {
        if (pageLen <= 0 || pageNum < 0) {
            throw new IllegalArgumentException();
        }

        List<IHelpComponent> components = new LinkedList<>();
        for (IHelpTopic topic : helpTopics) {
            components.addAll(topic.getComponents(target, viewer, context, true));
        }

        PageBorders pageBorders = null;
        int componentStartIdx = -1;
        int componentEndIdx = -1;
        int totalPageCount = 0;
        int curPageLines = 0;

        ListIterator<IHelpComponent> iterator = components.listIterator();

        while (iterator.hasNext()) {
            if (curPageLines == 0) {

                if (pageBorders != null) {
                    iterator.add(pageBorders.getFooter());
                }

                if (pageNum == totalPageCount) {
                    componentStartIdx = iterator.nextIndex();
                } else if (pageNum + 1 == totalPageCount) {
                    componentEndIdx = iterator.nextIndex();
                }

                pageBorders = pageLayout.getPageBorders(target, viewer, context, totalPageCount + 1);

                if (pageBorders != null) {
                    iterator.add(pageBorders.getHeader());
                    iterator.previous();

                    curPageLines += pageBorders.getFooter().lineCount();
                }

                totalPageCount++;
            }

            IHelpComponent component = iterator.next();
            int lineCount = component.lineCount();
            curPageLines += lineCount;

            if (curPageLines >= pageLen) {
                curPageLines = 0;
            }
        }

        if (componentStartIdx == -1) {
            // page does not exist
            return "";
        }

        if (componentEndIdx == -1) {
            componentEndIdx = components.size();
        }

        StringBuilder sb = new StringBuilder();
        iterator = components.listIterator(componentStartIdx);
        int count = componentEndIdx - componentStartIdx;
        boolean first = true;

        while (count-- > 0) {
            IHelpComponent component = iterator.next();
            if (component instanceof IPageBorder) {
                ((IPageBorder) component).setPageCount(totalPageCount);
            }
            if (first) {
                first = false;
            } else {
                sb.append('\n');
            }
            component.appendTo(sb);

        }

        return sb.toString();
    }

    public static String combine(List<IHelpComponent> components) {
        StringBuilder rv = new StringBuilder();

        Iterator<IHelpComponent> iterator = components.iterator();
        if (iterator.hasNext()) {
            iterator.next().appendTo(rv);
        }
        while (iterator.hasNext()) {
            rv.append('\n');
            iterator.next().appendTo(rv);
        }

        return rv.toString();
    }

}
