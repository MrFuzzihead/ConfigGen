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
	protected final String description;

	protected final Category parent;

	protected List<Category> children = new ArrayList<Category>();
	protected List<Field> fields = new ArrayList<Field>();

	public Category(TypeElement type, Category parent, ProcessingEnvironment env) {
		this.type = type;
		this.parent = parent;

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
					Utils.checkUsable(element, env);
					children.add(new Category((TypeElement) element, this, env));
					break;
				default:
					break;
			}
		}
	}

	public void generate(MethodSpec.Builder spec) {
		for (Category child : children) {
			child.generate(spec);
		}
		for (Field field : fields) {
			field.generate(spec);
		}

		if (description != null) {
			spec.addStatement("$N.setCategoryComment($S, $S)", ConfigClass.CONFIG_NAME, name, description.trim());
		}

		RequiresRestart restart = type.getAnnotation(RequiresRestart.class);
		if (restart != null) {
			if (restart.world()) {
				spec.addStatement("$N.setCategoryRequiresWorldRestart($S, $L)", ConfigClass.CONFIG_NAME, name, true);
			}
			if (restart.mc()) {
				spec.addStatement("$N.setCategoryRequiresMcRestart($S, $L)", ConfigClass.CONFIG_NAME, name, true);
			}
		}


	}
}


