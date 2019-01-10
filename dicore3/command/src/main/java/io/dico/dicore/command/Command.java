package io.dico.dicore.command;

import io.dico.dicore.command.IContextFilter.Priority;
import io.dico.dicore.command.parameter.ArgumentBuffer;
import io.dico.dicore.command.parameter.IArgumentPreProcessor;
import io.dico.dicore.command.parameter.Parameter;
import io.dico.dicore.command.parameter.ParameterList;
import io.dico.dicore.command.parameter.type.ParameterType;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class Command {
    private static final String[] EMPTY_DESCRIPTION = new String[0];
    private final ParameterList parameterList = new ParameterList();
    private final List<IContextFilter> contextFilters = new ArrayList<>(3);
    private String[] description = EMPTY_DESCRIPTION;
    private String shortDescription;

    public Command addParameter(Parameter<?, ?> parameter) {
        parameterList.addParameter(parameter);
        return this;
    }

    public <TType> Command addParameter(String name, String description, ParameterType<TType, Void> type) {
        return addParameter(new Parameter<>(name, description, type, null, false, null));
    }

    public <TType, TParamInfo> Command addParameter(String name, String description, ParameterType<TType, TParamInfo> type, TParamInfo paramInfo) {
        return addParameter(new Parameter<>(name, description, type, paramInfo, false, null));
    }

    public <TType> Command addFlag(String name, String description, ParameterType<TType, Void> type) {
        return addParameter(new Parameter<>('-' + name, description, type, null, true, null));
    }

    public <TType, TParamInfo> Command addFlag(String name, String description, ParameterType<TType, TParamInfo> type, TParamInfo paramInfo) {
        return addParameter(new Parameter<>('-' + name, description, type, paramInfo, true, null));
    }

    public <TType> Command addAuthorizedFlag(String name, String description, ParameterType<TType, Void> type, String permission) {
        return addParameter(new Parameter<>('-' + name, description, type, null, true, permission));
    }

    public <TType, TParamInfo> Command addAuthorizedFlag(String name, String description, ParameterType<TType, TParamInfo> type, TParamInfo paramInfo, String permission) {
        return addParameter(new Parameter<>('-' + name, description, type, paramInfo, true, permission));
    }

    public Command requiredParameters(int requiredParameters) {
        parameterList.setRequiredCount(requiredParameters);
        return this;
    }

    public Command repeatFinalParameter() {
        parameterList.setRepeatFinalParameter(true);
        return this;
    }

    public Command setDescription(String... description) {
        this.description = Objects.requireNonNull(description);
        return this;
    }

    public Command setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
        return this;
    }

    /*
    public Command preprocessArguments(IArgumentPreProcessor processor) {
        parameterList.setArgumentPreProcessor(processor);
        return this;
    }*/

    public final ParameterList getParameterList() {
        return parameterList;
    }

    public final String[] getDescription() {
        return description.length == 0 ? description : description.clone();
    }

    public String getShortDescription() {
        return shortDescription;
    }

    /**
     * ---- CONTEXT FILTERS ----
     * Filter the contexts. For example, if the sender must be a player but it's the console,
     * throw a CommandException describing the problem.
     */
    private transient int postParameterFilterCount = 0;

    public Command addContextFilter(IContextFilter contextFilter) {
        Objects.requireNonNull(contextFilter);
        if (!contextFilters.contains(contextFilter)) {
            contextFilters.add(contextFilter);
            contextFilters.sort(null);

            if (contextFilter.getPriority().compareTo(Priority.POST_PARAMETERS) >= 0) {
                postParameterFilterCount++;
            }
        }
        return this;
    }

    public List<IContextFilter> getContextFilters() {
        return Collections.unmodifiableList(contextFilters);
    }

    public Command removeContextFilter(IContextFilter contextFilter) {
        boolean ret = contextFilters.remove(contextFilter);
        if (ret) {
            if (contextFilter.getPriority().compareTo(Priority.POST_PARAMETERS) >= 0) {
                postParameterFilterCount--;
            }
        }
        return this;
    }

    // ---- CONTROL FLOW IN COMMAND TREES ----

    public boolean isVisibleTo(CommandSender sender) {
        return true;
    }

    public boolean takePrecedenceOverSubcommand(String subCommand, ArgumentBuffer buffer) {
        return false;
    }

    // ---- EXECUTION ----

    public void initializeAndFilterContext(ExecutionContext context) throws CommandException {
        int i, n;
        for (i = 0, n = contextFilters.size() - postParameterFilterCount; i < n; i++) {
            contextFilters.get(i).filterContext(context);
        }

        context.parse(parameterList);

        if (!context.isTabComplete()) {
            for (n = contextFilters.size(); i < n; i++) {
                contextFilters.get(i).filterContext(context);
            }
        }
    }

    public abstract String execute(CommandSender sender, ExecutionContext context) throws CommandException;

    public List<String> tabComplete(CommandSender sender, ExecutionContext context, Location location) {
        return context.getSuggestedCompletions(location);
    }

}
