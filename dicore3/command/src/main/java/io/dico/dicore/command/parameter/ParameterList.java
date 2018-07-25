package io.dico.dicore.command.parameter;

import java.util.*;

/**
 * IParameter definition for a command
 */
public class ParameterList {
    private List<Parameter<?, ?>> indexedParameters;
    private Map<String, Parameter<?, ?>> byName;
    private IArgumentPreProcessor argumentPreProcessor = IArgumentPreProcessor.NONE;
    private int requiredCount = -1;
    private boolean repeatFinalParameter;

    // if the final parameter is repeated and the command is implemented through reflection,
    // the repeated parameter is simply the last parameter of the method, rather than the last
    // indexed parameter. This might be a flag. As such, this field exists to ensure the correct
    // parameter is taken for repeating
    private boolean finalParameterMayBeFlag;

    public ParameterList() {
        this.indexedParameters = new ArrayList<>();
        this.byName = new LinkedHashMap<>();
        this.repeatFinalParameter = false;
    }

    public IArgumentPreProcessor getArgumentPreProcessor() {
        return argumentPreProcessor;
    }

    public ParameterList setArgumentPreProcessor(IArgumentPreProcessor argumentPreProcessor) {
        this.argumentPreProcessor = argumentPreProcessor == null ? IArgumentPreProcessor.NONE : argumentPreProcessor;
        return this;
    }

    public boolean repeatFinalParameter() {
        return repeatFinalParameter;
    }

    public ParameterList setRepeatFinalParameter(boolean repeatFinalParameter) {
        this.repeatFinalParameter = repeatFinalParameter;
        return this;
    }

    public boolean finalParameterMayBeFlag() {
        return finalParameterMayBeFlag;
    }

    public ParameterList setFinalParameterMayBeFlag(boolean finalParameterMayBeFlag) {
        this.finalParameterMayBeFlag = finalParameterMayBeFlag;
        return this;
    }

    public int getRequiredCount() {
        return requiredCount == -1 ? indexedParameters.size() : requiredCount;
    }

    public ParameterList setRequiredCount(int requiredCount) {
        this.requiredCount = requiredCount;
        return this;
    }

    public List<Parameter<?, ?>> getIndexedParameters() {
        return Collections.unmodifiableList(indexedParameters);
    }

    public Parameter<?, ?> getParameterByName(String name) {
        return byName.get(name);
    }

    public String getIndexedParameterName(int index) {
        return indexedParameters.get(index).getName();
    }

    public Map<String, Parameter<?, ?>> getParametersByName() {
        return Collections.unmodifiableMap(byName);
    }

    /**
     * Add the given parameter to the end of this parameter list
     * Can be a flag
     *
     * @param parameter the parameter
     * @return this
     */
    public ParameterList addParameter(Parameter<?, ?> parameter) {
        return addParameter(-1, parameter);
    }

    /**
     * Add the given parameter to this parameter list
     * If the parameter is a flag, the index is ignored
     *
     * @param index     parameter index number, -1 if end
     * @param parameter the parameter
     * @return this
     * @throws NullPointerException if parameter is null
     */
    public ParameterList addParameter(int index, Parameter<?, ?> parameter) {
        //System.out.println("Added parameter " + parameter.getName() + ", flag: " + parameter.isFlag());
        byName.put(parameter.getName(), parameter);
        if (!parameter.isFlag()) {
            indexedParameters.add(index == -1 ? indexedParameters.size() : index, parameter);
        }
        return this;
    }

    public Parameter<?, ?> getRepeatedParameter() {
        if (!repeatFinalParameter) {
            return null;
        }
        if (finalParameterMayBeFlag) {
            Iterator<Parameter<?, ?>> iterator = byName.values().iterator();
            Parameter<?, ?> result = null;
            while (iterator.hasNext()) {
                result = iterator.next();
            }
            return result;
        }

        if (indexedParameters.isEmpty()) {
            return null;
        }

        return indexedParameters.get(indexedParameters.size() - 1);
    }

}
