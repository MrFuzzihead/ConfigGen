package org.squiddev.configgen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify a range the value should take
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Range {

    int min() default Integer.MIN_VALUE;

    int max() default Integer.MAX_VALUE;
}
