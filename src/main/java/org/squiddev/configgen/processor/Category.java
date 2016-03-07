package org.squiddev.configgen.processor;

import org.squiddev.configgen.Exclude;
import org.squiddev.configgen.RequiresRestart;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;

public class Category {
	public final TypeElement type;

	public final String name;
	public final ConfigClass root;
	public final Category parent;

	public final List<Category> children = new ArrayList<Category>();
	public final List<Field> fields = new ArrayList<Field>();

	public final String description;
	public final boolean requiresMcRestart;
	public final boolean requiresWorldRestart;

	public Category(TypeElement type, Category parent, ConfigClass root, ProcessingEnvironment env) {
		this.type = type;
		this.parent = parent;
		this.root = root;

		name = ((parent == null ? "" : parent.name + ".") + type.getSimpleName().toString()).toLowerCase();
		description = env.getElementUtils().getDocComment(type);

		for (Element element : type.getEnclosedElements()) {
			switch (element.getKind()) {
				case FIELD:
					if (!element.getModifiers().contains(Modifier.FINAL) && element.getAnnotation(Exclude.class) == null) {
						Utils.checkUsable(element, env);
						fields.add(new Field((VariableElement) element, this, env));
					}
					break;
				case CLASS:
					if (element.getAnnotation(Exclude.class) == null) {
						Utils.checkUsable(element, env);
						children.add(new Category((TypeElement) element, this, root, env));
					}
					break;
				default:
					break;
			}
		}

		RequiresRestart restart = type.getAnnotation(RequiresRestart.class);
		if (restart != null) {
			requiresMcRestart = restart.mc();
			requiresWorldRestart = restart.world();
		} else {
			requiresMcRestart = false;
			requiresWorldRestart = false;
		}
	}


}


