package io.dico.dicore.command.annotation;

import io.dico.dicore.command.parameter.type.IParameterTypeSelector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that register a parameter type to the localized selector for use in reflective commands.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CmdParamType {

    /**
     * If this flag is set, the type is registered without its annotation type.
     * As a result, the {@link IParameterTypeSelector} is more likely to select it (faster).
     * This is irrelevant if there is no annotation type or param config.
     *
     * @return true if this parameter type should be registered without its annotation type too
     */
    boolean infolessAlias() default false;

}

