package io.dico.dicore.command.parameter.type;

import java.lang.annotation.Annotation;

/**
 * An interface for an object that stores parameter types by {@link ParameterKey} and finds appropriate types for {@link ParameterKey parameterKeys}
 */
public interface IParameterTypeSelector {

    <TReturn, TParamInfo> ParameterType<TReturn, TParamInfo> selectExact(ParameterKey key);

    //<TReturn, TParamInfo> ParameterType<TReturn, TParamInfo> selectExactOrSubclass(ParameterKey key);

    <TReturn, TParamInfo> ParameterType<TReturn, TParamInfo> selectAny(ParameterKey key);


    default <TReturn, TParamInfo> ParameterType<TReturn, TParamInfo> selectExact(Class<?> returnType) {
        return selectExact(returnType, null);
    }

    default <TReturn, TParamInfo> ParameterType<TReturn, TParamInfo> selectExact(Class<?> returnType, Class<? extends Annotation> annotationClass) {
        return selectExact(new ParameterKey(returnType, annotationClass));
    }

    /*
    default <TReturn, TParamInfo> ParameterType<TReturn, TParamInfo> selectExactOrSubclass(Class<?> returnType) {
        return selectExactOrSubclass(returnType, null);
    }
    
    default <TReturn, TParamInfo> ParameterType<TReturn, TParamInfo> selectExactOrSubclass(Class<?> returnType, Class<? extends Annotation> annotationClass) {
        return selectExactOrSubclass(new ParameterKey(returnType, annotationClass));
    }
    */
    default <TReturn, TParamInfo> ParameterType<TReturn, TParamInfo> selectAny(Class<?> returnType) {
        return selectAny(returnType, null);
    }

    default <TReturn, TParamInfo> ParameterType<TReturn, TParamInfo> selectAny(Class<?> returnType, Class<? extends Annotation> annotationClass) {
        return selectAny(new ParameterKey(returnType, annotationClass));
    }

    void addType(boolean infolessAlias, ParameterType<?, ?> type);

}
