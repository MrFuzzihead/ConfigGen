package org.squiddev.configgen.data;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import java.util.Set;

public class Utils {
	/**
	 * Assert an element is public, static and not abstract
	 */
	public static void checkUsable(Element element, ProcessingEnvironment env) {
		Set<Modifier> modifiers = element.getModifiers();

		if (!modifiers.contains(Modifier.PUBLIC)) {
			env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Expected public", element);
		}

		if (!modifiers.contains(Modifier.STATIC)) {
			env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Expected static", element);
		}

		if (modifiers.contains(Modifier.ABSTRACT)) {
			env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unexpected abstract", element);
		}

		if (modifiers.contains(Modifier.FINAL)) {
			env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unexpected final", element);
		}
	}

	public static void dump(Object obj, ProcessingEnvironment env) {
		env.getMessager().printMessage(Diagnostic.Kind.WARNING, dump(obj));
	}

	public static void dump(Object obj, ProcessingEnvironment env, int level) {
		env.getMessager().printMessage(Diagnostic.Kind.WARNING, dump(obj, level));
	}

	public static String dump(Object obj) {
		return dump(obj, 1);
	}

	public static String dump(Object obj, int level) {
		Class<?> klass = obj.getClass();

		StringBuilder builder = new StringBuilder().append(obj).append("\n");
		for (java.lang.reflect.Field field : klass.getFields()) {
			field.setAccessible(true);

			builder.append(new String(new char[level - 1]).replace('\0', '\t'));
			try {
				builder.append(field).append(": ");

				if (field.getType().isPrimitive() || level <= 1) {
					builder.append(field.get(obj));
				} else {
					builder.append(dump(field.get(obj), level - 1));
				}
			} catch (Exception e) {
				builder.append("Err: ").append(e.toString());
			}
			builder.append("\n");
		}

		return builder.toString();
	}
}
