package io.dico.dicore.command.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Desc {

    /**
     * Multiline description if {@link #shortVersion} is set.
     * Otherwise, this should be an array with one element (aka, you don't have to add array brackets).
     *
     * @return the multiline description.
     * @see CommandAnnotationUtils#getShortDescription(Desc)
     */
    String[] value();

    /**
     * Short description, use if {@link #value} is multi-line.
     * To get a short description from a {@link Desc}, you should use {@link CommandAnnotationUtils#getShortDescription(Desc)}
     *
     * @return short description
     * @see CommandAnnotationUtils#getShortDescription(Desc)
     */
    String shortVersion() default "";

}
