package org.squiddev.configgen.data;

import com.squareup.javapoet.MethodSpec;
import org.squiddev.configgen.Exclude;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.io.StringWriter;
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
			}
		}
	}

	public void generate(MethodSpec.Builder spec, ProcessingEnvironment env) {
		for (Category child : children) {
			child.generate(spec, env);
		}
		for (Field field : fields) {
			field.generate(spec, env);
		}

		if (description != null) {
			spec.addStatement("$N.setCategoryComment($S, $S)", ConfigClass.CONFIG_NAME, name, description.trim());
		}
	}
}


