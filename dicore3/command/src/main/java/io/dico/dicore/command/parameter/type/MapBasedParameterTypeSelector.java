package io.dico.dicore.command.parameter.type;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * Map based implementation of {@link IParameterTypeSelector}
 */
public class MapBasedParameterTypeSelector implements IParameterTypeSelector {
    static final MapBasedParameterTypeSelector defaultSelector = new MapBasedParameterTypeSelector(false);
    private final Map<ParameterKey, ParameterType<?, ?>> parameterTypeMap;
    private final boolean useDefault;

    public MapBasedParameterTypeSelector(boolean useDefault) {
        this.parameterTypeMap = new HashMap<>();
        this.useDefault = useDefault;
    }

    @Override
    public <TReturn, TParamInfo> ParameterType<TReturn, TParamInfo> selectExact(ParameterKey key) {
        ParameterType<?, ?> out = parameterTypeMap.get(key);
        if (useDefault && out == null) {
            out = defaultSelector.selectExact(key);
        }
        if (out == null && key.getReturnType().isEnum()) {
            //noinspection unchecked
            out = new EnumParameterType(key.getReturnType());
            addType(false, out);
        }
        return cast(out);
    }

    @Override
    public <TReturn, TParamInfo> ParameterType<TReturn, TParamInfo> selectAny(ParameterKey key) {
        ParameterType<TReturn, TParamInfo> exact = selectExact(key);
        if (exact != null) {
            return exact;
        }

        if (key.getAnnotationClass() != null) {
            exact = selectExact(new ParameterKey(key.getReturnType()));
            if (exact != null) {
                return exact;
            }
        }

        Class<?> returnType = key.getReturnType();
        Class<? extends Annotation> annotationClass = key.getAnnotationClass();

        ParameterType<?, ?> out = selectByReturnType(parameterTypeMap, returnType, annotationClass, false);
        if (out == null && useDefault) {
            out = selectByReturnType(defaultSelector.parameterTypeMap, returnType, annotationClass, false);
        }
        if (out == null) {
            out = selectByReturnType(parameterTypeMap, returnType, annotationClass, true);
        }
        if (out == null && useDefault) {
            out = selectByReturnType(defaultSelector.parameterTypeMap, returnType, annotationClass, true);
        }
        return cast(out);
    }

    private static ParameterType<?, ?> selectByReturnType(Map<ParameterKey, ParameterType<?, ?>> map, Class<?> returnType,
                                                          Class<? extends Annotation> annotationClass, boolean allowSubclass) {
        ParameterType<?, ?> out = null;
        if (allowSubclass) {
            for (ParameterType<?, ?> type : map.values()) {
                if (returnType.isAssignableFrom(type.getReturnType())) {
                    if (annotationClass == type.getAnnotationClass()) {
                        out = type;
                        break;
                    }
                    if (out == null) {
                        out = type;
                    }
                }
            }
        } else {
            for (ParameterType<?, ?> type : map.values()) {
                if (returnType == type.getReturnType()) {
                    if (annotationClass == type.getAnnotationClass()) {
                        out = type;
                        break;
                    }
                    if (out == null) {
                        out = type;
                    }
                }
            }
        }
        return out;
    }

    private static <T> T cast(Object o) {
        //noinspection unchecked
        return (T) o;
    }

    @Override
    public void addType(boolean infolessAlias, ParameterType<?, ?> type) {
        parameterTypeMap.put(type.getTypeKey(), type);

        if (infolessAlias) {
            parameterTypeMap.putIfAbsent(type.getInfolessTypeKey(), type);
        }
    }

    static {
        // registers default parameter types
        ParameterTypes.clinit();
    }

}
