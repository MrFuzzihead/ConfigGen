package org.squiddev.configgen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a property/category as requiring a restart
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface RequiresRestart {
	/**
	 * Requires a restart of the world
	 *
	 * @return If a reload of the world is required
	 */
	boolean world() default true;

	/**
	 * Requires a restart of game
	 *
	 * @return If a restart is required
	 */
	boolean mc() default true;
}
