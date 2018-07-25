package io.dico.dicore.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define sub-groups of the group registered reflectively from all methods in a class.
 * <p>
 * Commands are selected for grouping by matching their method's names to a regular expression.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GroupMatchedCommands {

    @Retention(RetentionPolicy.RUNTIME)
    @interface GroupEntry {

        /**
         * Regular expression to match method names for this group
         * Must be non-empty
         *
         * @return the regular expression
         */
        String regex();

        /**
         * The name or main key of the sub-group or address
         * Must be non-empty
         *
         * @return the group name
         */
        String group();

        /**
         * The aliases for the sub-group
         *
         * @return the group aliases
         */
        String[] groupAliases() default {};

        /**
         * Generated (predefined) commands for the sub-group
         */
        String[] generatedCommands() default {};

        /**
         * @see Desc
         */
        String[] description() default {};

        /**
         * @see Desc
         */
        String shortDescription() default "";
    }

    /**
     * The defined groups.
     * If a method name matches the regex of multiple groups,
     * groups are prioritized by the order in which they appear in this array.
     *
     * @return the defined groups
     */
    GroupEntry[] value();

}
