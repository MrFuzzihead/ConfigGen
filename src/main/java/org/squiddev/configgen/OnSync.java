package org.squiddev.configgen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The method to call on sync
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.METHOD })
public @interface OnSync {}
