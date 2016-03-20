package org.squiddev.configgen.processor;

import org.squiddev.configgen.Config;
import org.squiddev.configgen.OnSync;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

/**
 * The root config class
 */
public class ConfigClass {
	public final TypeElement type;

	public final String languagePrefix;
	public final String propertyPrefix;

	public final List<Category> categories = new ArrayList<Category>();
	public ExecutableElement sync;

	public ConfigClass(TypeElement type, ProcessingEnvironment env) {
		this.type = type;

		Config config = type.getAnnotation(Config.class);
		languagePrefix = config.languagePrefix().isEmpty() ? null : config.languagePrefix();
		propertyPrefix = config.propertyPrefix().isEmpty() ? type.getSimpleName().toString() : config.propertyPrefix();

		for (Element element : type.getEnclosedElements()) {
			switch (element.getKind()) {
				case CLASS:
					Utils.checkUsable(element, env);
					categories.add(new Category((TypeElement) element, null, this, env));
					break;
				case METHOD:
					if (element.getAnnotation(OnSync.class) != null) {
						Utils.checkUsable(element, env);
						if (sync != null) {
							env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot have multiple @OnSync instances", element);
						}
						sync = (ExecutableElement) element;
					}
				default:
					break;
			}
		}
	}


}
