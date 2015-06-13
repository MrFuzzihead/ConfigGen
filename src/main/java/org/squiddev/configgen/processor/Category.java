package org.squiddev.configgen.processor;

import com.squareup.javapoet.MethodSpec;
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
	protected Category parent;

	protected List<Category> children = new ArrayList<Category>();
	protected List<Field> fields = new ArrayList<Field>();

	protected String description;
	protected boolean requiresMcRestart = false;
	protected boolean requiresWorldRestart = false;

	public Category(TypeElement type, Category parent, ConfigClass root, ProcessingEnvironment env) {
		this.type = type;
		this.parent = parent;
		this.root = root;

		name = (parent == null ? "" : parent.name + ".") + type.getSimpleName().toString();
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
		}
	}

	public void generate(MethodSpec.Builder spec) {
		for (Category child : children) {
			child.generate(spec);
		}
		for (Field field : fields) {
			field.generate(spec);
		}

		if (description != null || root.languagePrefix != null || requiresMcRestart || requiresWorldRestart) {
			spec.addCode("$[");
			spec.addCode("$N.getCategory($S)", ConfigClass.CONFIG_NAME, name);

			if (root.languagePrefix != null) spec.addCode("\n.setLanguageKey($S)", root.languagePrefix + name);
			if (requiresWorldRestart) spec.addCode("\n.setRequiresWorldRestart($L)", true);
			if (requiresMcRestart) spec.addCode("\n.setRequiresMcRestart($L)", true);

			// This doesn't return a ConfigCategory so has to be last
			if (description != null) spec.addCode("\n.setComment($S)", description.trim());

			spec.addCode(";\n$]");
		}
	}
}


