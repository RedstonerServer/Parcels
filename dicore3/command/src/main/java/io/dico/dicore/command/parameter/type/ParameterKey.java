package io.dico.dicore.command.parameter.type;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * More appropriate name: ParameterTypeKey
 */
public class ParameterKey {

    private final Class<?> returnType;
    private final Class<? extends Annotation> annotationClass;

    // just a marker, not used in equals or hashCode().
    // returnType is never primitive
    private boolean isPrimitive;

    public ParameterKey(Class<?> returnType) {
        this(returnType, null);
    }

    public ParameterKey(Class<?> returnType, Class<? extends Annotation> annotationClass) {
        boolean isPrimitive = returnType.isPrimitive();

        if (isPrimitive) {
            returnType = primitivesToWrappers.get(returnType);
        }

        this.returnType = Objects.requireNonNull(returnType);
        this.annotationClass = annotationClass;
        this.isPrimitive = isPrimitive;
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

    private static Class<?> getPrimitiveWrapperClass(Class<?> primitiveClass) {
        if (!primitiveClass.isPrimitive()) return null;
        switch (primitiveClass.getName()) {
            case "boolean":
                return Boolean.class;
            case "char":
                return Character.class;
            case "byte":
                return Byte.class;
            case "short":
                return Short.class;
            case "int":
                return Integer.class;
            case "float":
                return Float.class;
            case "long":
                return Long.class;
            case "double":
                return Double.class;
            case "void":
                return Void.class;
            default:
                throw new InternalError();
        }
    }

    private static final BiMap<Class<?>, Class<?>> primitivesToWrappers;
    static {
        HashBiMap<Class<?>, Class<?>> tmp = HashBiMap.create();
        tmp.put(Boolean.TYPE, Boolean.class);
        tmp.put(Character.TYPE, Character.class);
        tmp.put(Byte.TYPE, Byte.class);
        tmp.put(Short.TYPE, Short.class);
        tmp.put(Integer.TYPE, Integer.class);
        tmp.put(Float.TYPE, Float.class);
        tmp.put(Long.TYPE, Long.class);
        tmp.put(Double.TYPE, Double.class);
        tmp.put(Void.TYPE, Void.class);
        primitivesToWrappers = Maps.unmodifiableBiMap(tmp);
    }

}
