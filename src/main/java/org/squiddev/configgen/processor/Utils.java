package org.squiddev.configgen.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import java.util.Set;

public class Utils {
	/**
	 * Assert an element is public, static
	 *
	 * @param element The element to check
	 * @param env     The environment to produce errors in
	 */
	public static void checkUsable(Element element, ProcessingEnvironment env) {
		Set<Modifier> modifiers = element.getModifiers();

		if (!modifiers.contains(Modifier.PUBLIC)) {
			env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Expected public", element);
		}

		if (!modifiers.contains(Modifier.STATIC)) {
			env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Expected static", element);
		}
	}
}
