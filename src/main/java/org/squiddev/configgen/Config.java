package org.squiddev.configgen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The root config class.
 * Subclasses are considered categories
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Config {
	/**
	 * The prefix to use for language keys
	 *
	 * If left blank, language keys will not be set
	 *
	 * @return The language prefix
	 */
	String languagePrefix() default "";

	/**
	 * Prefix for properties
	 *
	 * @return The property prefix
	 */
	String propertyPrefix() default "";
}
