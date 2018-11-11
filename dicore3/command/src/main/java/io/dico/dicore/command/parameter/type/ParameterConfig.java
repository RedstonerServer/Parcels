package io.dico.dicore.command.parameter.type;

import io.dico.dicore.Reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

/**
 * This class serves the purpose of having annotated parameter configurations (such as ranges for number parameters).
 * Such configurations must be possible to obtain without using annotations, and as such, there should be a class conveying the information
 * that is separate from the annotation itself. This class acts as a bridge from the annotation to said class conveying the information.
 *
 * @param <TAnnotation> the annotation type for parameters
 * @param <TParamInfo>  the object type that holds the information required in memory
 */
public abstract class ParameterConfig<TAnnotation extends Annotation, TParamInfo> implements Comparable<ParameterConfig<?, ?>> {
    private final Class<TAnnotation> annotationClass;
    // protected final TParamInfo defaultValue;

    public ParameterConfig(Class<TAnnotation> annotationClass/*, TParamInfo defaultValue*/) {
        this.annotationClass = annotationClass;
        //this.defaultValue = defaultValue;
    }

    public final Class<TAnnotation> getAnnotationClass() {
        return annotationClass;
    }
    /*
    public TParamInfo getDefaultValue() {
        return defaultValue;
    }*/

    protected abstract TParamInfo toParameterInfo(TAnnotation annotation);

    public TParamInfo getParameterInfo(Annotation annotation) {
        //noinspection unchecked
        return toParameterInfo((TAnnotation) annotation);
    }

    public static <TAnnotation extends Annotation, TParamInfo> ParameterConfig<TAnnotation, TParamInfo>
    includeMemoryClass(Class<TAnnotation> annotationClass, Class<TParamInfo> memoryClass) {
        Constructor<TParamInfo> constructor;
        //TParamInfo defaultValue;
        try {
            constructor = memoryClass.getConstructor(annotationClass);
            //defaultValue = Reflection.getStaticFieldValue(annotationClass, "DEFAULT");
        } catch (NoSuchMethodException | IllegalArgumentException ex) {
            throw new IllegalArgumentException(ex);
        }
        /*
        if (defaultValue == null) try {
            defaultValue = memoryClass.newInstance();
        } catch (IllegalAccessException | InstantiationException ex) {
            throw new IllegalArgumentException("Failed to get a default value for the param info", ex);
        }*/

        return new ParameterConfig<TAnnotation, TParamInfo>(annotationClass/*, defaultValue*/) {

            @Override
            public TParamInfo toParameterInfo(TAnnotation annotation) {
                try {
                    return constructor.newInstance(annotation);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    public static <TAnnotation extends Annotation, TParamInfo> ParameterConfig<TAnnotation, TParamInfo> getMemoryClassFromField(Class<TAnnotation> annotationClass) {
        return ParameterConfig.includeMemoryClass(annotationClass, Reflection.getStaticFieldValue(annotationClass, "MEMORY_CLASS"));
    }

    @Override
    public int compareTo(ParameterConfig<?, ?> o) {
        return 0;
    }

}

