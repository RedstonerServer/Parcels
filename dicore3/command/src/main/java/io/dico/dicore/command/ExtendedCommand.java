package io.dico.dicore.command;

import io.dico.dicore.command.parameter.IArgumentPreProcessor;
import io.dico.dicore.command.parameter.Parameter;
import io.dico.dicore.command.parameter.type.ParameterType;

@SuppressWarnings("unchecked")
public abstract class ExtendedCommand<T extends ExtendedCommand<T>> extends Command {
    protected boolean modifiable;

    public ExtendedCommand() {
        this(true);
    }

    public ExtendedCommand(boolean modifiable) {
        this.modifiable = modifiable;
    }

    protected T newModifiableInstance() {
        return (T) this;
    }

    @Override
    public T addParameter(Parameter<?, ?> parameter) {
        return modifiable ? (T) super.addParameter(parameter) : newModifiableInstance().addParameter(parameter);
    }

    @Override
    public T addContextFilter(IContextFilter contextFilter) {
        return modifiable ? (T) super.addContextFilter(contextFilter) : newModifiableInstance().addContextFilter(contextFilter);
    }

    @Override
    public T removeContextFilter(IContextFilter contextFilter) {
        return modifiable ? (T) super.removeContextFilter(contextFilter) : newModifiableInstance().removeContextFilter(contextFilter);
    }

    @Override
    public T requiredParameters(int requiredParameters) {
        return modifiable ? (T) super.requiredParameters(requiredParameters) : newModifiableInstance().requiredParameters(requiredParameters);
    }

    @Override
    public T repeatFinalParameter() {
        return modifiable ? (T) super.repeatFinalParameter() : newModifiableInstance().repeatFinalParameter();
    }

    @Override
    public T setDescription(String... description) {
        return modifiable ? (T) super.setDescription(description) : newModifiableInstance().setDescription(description);
    }

    @Override
    public T setShortDescription(String shortDescription) {
        return modifiable ? (T) super.setShortDescription(shortDescription) : newModifiableInstance().setShortDescription(shortDescription);
    }

    /*
    @Override
    public T preprocessArguments(IArgumentPreProcessor processor) {
        return modifiable ? (T) super.preprocessArguments(processor) : newModifiableInstance().preprocessArguments(processor);
    }*/

}
