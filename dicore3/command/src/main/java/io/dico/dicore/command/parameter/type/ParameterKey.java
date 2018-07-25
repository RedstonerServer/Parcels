package io.dico.dicore.command.parameter.type;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * More appropriate name: ParameterTypeKey
 */
public class ParameterKey {
    private final Class<?> returnType;
    private final Class<? extends Annotation> annotationClass;

    public ParameterKey(Class<?> returnType) {
        this(returnType, null);
    }

    public ParameterKey(Class<?> returnType, Class<? extends Annotation> annotationClass) {
        this.returnType = Objects.requireNonNull(returnType);
        this.annotationClass = annotationClass;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Class<? extends Annotation> getAnnotationClass() {
        return annotationClass;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof ParameterKey && equals((ParameterKey) o));
    }

    public boolean equals(ParameterKey that) {
        return returnType == that.returnType && annotationClass == that.annotationClass;
    }

    @Override
    public int hashCode() {
        int result = returnType.hashCode();
        result = 31 * result + (annotationClass != null ? annotationClass.hashCode() : 0);
        return result;
    }

}
