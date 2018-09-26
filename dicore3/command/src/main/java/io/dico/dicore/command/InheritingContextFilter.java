package io.dico.dicore.command;

import java.util.List;

public abstract class InheritingContextFilter implements IContextFilter {
    private static final String[] emptyStringArray = new String[0];

    private static String[] addParent(String[] path, String parent) {
        String[] out = new String[path.length + 1];
        System.arraycopy(path, 0, out, 0, path.length);
        out[0] = parent;
        return out;
    }

    protected abstract boolean isInherited(IContextFilter filter);

    @Override
    public void filterContext(ExecutionContext context) throws CommandException {
        ICommandAddress address = context.getAddress();

        String[] traversedPath = emptyStringArray;
        do {
            traversedPath = addParent(traversedPath, address.getMainKey());
            address = address.getParent();

            if (address != null && address.hasCommand()) {
                boolean doBreak = true;

                Command command = address.getCommand();
                List<IContextFilter> contextFilterList = command.getContextFilters();
                for (IContextFilter filter : contextFilterList) {
                    if (isInherited(filter)) {
                        if (filter == this) {
                            // do the same for next parent
                            // this method is necessary to keep traversedPath information
                            doBreak = false;
                        } else {
                            filter.filterSubContext(context, traversedPath);
                        }
                    }
                }

                if (doBreak) {
                    break;
                }
            }
        } while (address != null);
    }

    static InheritingContextFilter inheritingPriority(Priority priority) {
        return new InheritingContextFilter() {
            @Override
            protected boolean isInherited(IContextFilter filter) {
                return filter.getPriority() == priority;
            }

            @Override
            public Priority getPriority() {
                return priority;
            }
        };
    }

}
