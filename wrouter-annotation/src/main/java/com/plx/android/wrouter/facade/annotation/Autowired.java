package com.plx.android.wrouter.facade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for field, which need autowired.
 *
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface Autowired {

    // Mark param's name or service name.
    String name() default "";

    // If required, app will be crash when value is null.
    // Primitive type wont be check!
    boolean required() default false;

    // Description of the field
    String desc() default "";
}
