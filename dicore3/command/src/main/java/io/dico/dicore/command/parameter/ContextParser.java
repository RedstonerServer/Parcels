package io.dico.dicore.command.parameter;

import io.dico.dicore.command.CommandException;
import io.dico.dicore.command.ExecutionContext;

import java.lang.reflect.Array;
import java.util.*;

public class ContextParser {
    private final ExecutionContext m_context;
    private final ArgumentBuffer m_buffer;
    private final ParameterList m_paramList;
    private final Parameter<?, ?> m_repeatedParam;
    private final List<Parameter<?, ?>> m_indexedParams;
    private final int m_maxIndex;
    private final int m_requiredIndex;

    private Map<String, Object> m_valueMap = new HashMap<>();
    private Set<String> m_parsedKeys = new HashSet<>();
    private int m_completionCursor = -1;
    private Parameter<?, ?> m_completionTarget = null;

    public ContextParser(ExecutionContext context) {
        this.m_context = context;
        this.m_buffer = context.getProcessedBuffer();
        this.m_paramList = context.getParameterList();
        this.m_repeatedParam = m_paramList.getRepeatedParameter();
        this.m_indexedParams = m_paramList.getIndexedParameters();
        this.m_maxIndex = m_indexedParams.size() - 1;
        this.m_requiredIndex = m_paramList.getRequiredCount() - 1;
    }

    public ExecutionContext getContext() {
        return m_context;
    }

    public Map<String, Object> getValueMap() {
        return m_valueMap;
    }

    public Set<String> getParsedKeys() {
        return m_parsedKeys;
    }

    public void parse() throws CommandException {
        parseAllParameters();
    }

    public int getCompletionCursor() {
        if (!m_done) {
            throw new IllegalStateException();
        }
        return m_completionCursor;
    }

    public Parameter<?, ?> getCompletionTarget() {
        if (!m_done) {
            throw new IllegalStateException();
        }
        return m_completionTarget;
    }

    // ################################
    // # PARSING METHODS              #
    // ################################

    private boolean m_repeating = false;
    private boolean m_done = false;
    private int m_curParamIndex = -1;
    private Parameter<?, ?> m_curParam = null;
    private List<Object> m_curRepeatingList = null;

    private void parseAllParameters() throws CommandException {
        try {
            do {
                prepareStateToParseParam();
                if (m_done) break;
                parseCurParam();
            } while (!m_done);

        } finally {
            m_curParam = null;
            m_curRepeatingList = null;
            assignDefaultValuesToUncomputedParams();
            arrayifyRepeatedParamValue();
        }
    }

    private void prepareStateToParseParam() throws CommandException {

        boolean requireInput;
        if (identifyFlag()) {
            m_buffer.advance();
            prepareRepeatedParameterIfSet();
            requireInput = false;

        } else if (m_repeating) {
            m_curParam = m_repeatedParam;
            requireInput = false;

        } else if (m_curParamIndex < m_maxIndex) {
            m_curParamIndex++;
            m_curParam = m_indexedParams.get(m_curParamIndex);
            prepareRepeatedParameterIfSet();
            requireInput = m_curParamIndex <= m_requiredIndex;

        } else if (m_buffer.hasNext()) {
            throw new CommandException("Too many arguments for /" + m_context.getAddress().getAddress());

        } else {
            m_done = true;
            return;
        }

        if (!m_buffer.hasNext()) {
            if (requireInput) {
                reportParameterRequired(m_curParam);
            }

            if (m_repeating) {
                m_done = true;
            }
        }

    }

    private boolean identifyFlag() {
        String potentialFlag = m_buffer.peekNext();
        Parameter<?, ?> target;
        if (potentialFlag != null
                && potentialFlag.startsWith("-")
                && (target = m_paramList.getParameterByName(potentialFlag)) != null
                && target.isFlag()
                && !m_valueMap.containsKey(potentialFlag)

//              Disabled because it's checked by {@link Parameter#parse(ExecutionContext, ArgumentBuffer)}
//              && (target.getFlagPermission() == null || m_context.getSender().hasPermission(target.getFlagPermission()))
                ) {
            m_curParam = target;
            return true;
        }

        return false;
    }

    private void prepareRepeatedParameterIfSet() throws CommandException {
        if (m_curParam != null && m_curParam == m_repeatedParam) {

            if (m_curParam.isFlag() && m_curParamIndex < m_requiredIndex) {
                Parameter<?, ?> requiredParam = m_indexedParams.get(m_curParamIndex + 1);
                reportParameterRequired(requiredParam);
            }

            m_curRepeatingList = new ArrayList<>();
            assignValue(m_curRepeatingList);
            m_repeating = true;
        }
    }

    private void reportParameterRequired(Parameter<?, ?> param) throws CommandException {
        throw new CommandException("The argument '" + param.getName() + "' is required");
    }

    private void parseCurParam() throws CommandException {
        if (!m_buffer.hasNext() && !m_curParam.isFlag()) {
            assignDefaultValue();
            return;
        }

        int cursorStart = m_buffer.getCursor();

        if (m_context.isTabComplete() && "".equals(m_buffer.peekNext())) {
            assignAsCompletionTarget(cursorStart);
            return;
        }

        Object parseResult;
        try {
            parseResult = m_curParam.parse(m_context, m_buffer);
        } catch (CommandException e) {
            assignAsCompletionTarget(cursorStart);
            throw e;
        }

        assignValue(parseResult);
        m_parsedKeys.add(m_curParam.getName());
    }

    private void assignDefaultValue() throws CommandException {
        assignValue(m_curParam.getDefaultValue(m_context, m_buffer));
    }

    private void assignAsCompletionTarget(int cursor) {
        m_completionCursor = cursor;
        m_completionTarget = m_curParam;
        m_done = true;
    }

    private void assignValue(Object value) {
        if (m_repeating) {
            m_curRepeatingList.add(value);
        } else {
            m_valueMap.put(m_curParam.getName(), value);
        }
    }

    private void assignDefaultValuesToUncomputedParams() throws CommandException {
        // add default values for unset parameters
        for (Map.Entry<String, Parameter<?, ?>> entry : m_paramList.getParametersByName().entrySet()) {
            String name = entry.getKey();
            if (!m_valueMap.containsKey(name)) {
                if (m_repeatedParam == entry.getValue()) {
                    // below value will be turned into an array later
                    m_valueMap.put(name, Collections.emptyList());
                } else {
                    m_valueMap.put(name, entry.getValue().getDefaultValue(m_context, m_buffer));
                }
            }
        }
    }

    private void arrayifyRepeatedParamValue() {
        if (m_repeatedParam != null) {
            m_valueMap.computeIfPresent(m_repeatedParam.getName(), (k, v) -> {
                List list = (List) v;
                Class<?> returnType = m_repeatedParam.getType().getReturnType();
                Object array = Array.newInstance(returnType, list.size());
                ArraySetter setter = ArraySetter.getSetter(returnType);
                for (int i = 0, n = list.size(); i < n; i++) {
                    setter.set(array, i, list.get(i));
                }

                return array;
            });
        }
    }

    private interface ArraySetter {
        void set(Object array, int index, Object value);

        static ArraySetter getSetter(Class<?> clazz) {
            if (!clazz.isPrimitive()) {
                return (array, index, value) -> ((Object[]) array)[index] = value;
            }

            switch (clazz.getSimpleName()) {
                case "boolean":
                    return (array, index, value) -> ((boolean[]) array)[index] = (boolean) value;
                case "int":
                    return (array, index, value) -> ((int[]) array)[index] = (int) value;
                case "double":
                    return (array, index, value) -> ((double[]) array)[index] = (double) value;
                case "long":
                    return (array, index, value) -> ((long[]) array)[index] = (long) value;
                case "short":
                    return (array, index, value) -> ((short[]) array)[index] = (short) value;
                case "byte":
                    return (array, index, value) -> ((byte[]) array)[index] = (byte) value;
                case "float":
                    return (array, index, value) -> ((float[]) array)[index] = (float) value;
                case "char":
                    return (array, index, value) -> ((char[]) array)[index] = (char) value;
                case "void":
                default:
                    throw new InternalError("This should not happen");
            }
        }
    }

}
