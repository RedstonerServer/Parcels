package io.dico.dicore.command.annotation;

import io.dico.dicore.command.IContextFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequirePermissions {

    /**
     * Any permissions that must be present on the sender
     *
     * @return an array of permission nodes
     */
    String[] value();

    /**
     * Whether permissions should (also) be inherited from the parent.
     * This uses {@link IContextFilter#INHERIT_PERMISSIONS}
     * This is true by default.
     *
     * @return true if permissions should be inherited.
     */
    boolean inherit() default true;

}
