package org.squiddev.configgen.processor;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.squiddev.configgen.OptionParser;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.lang.reflect.Array;

/**
 * Builder for generating builders for default files
 */
public class PropertyBuilder {
	private static final String LOOP_NAME = "var";

	public static void generate(ConfigClass klass, ProcessingEnvironment env) throws IOException {
		MethodSpec.Builder init = MethodSpec.methodBuilder("init")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
			.returns(void.class);

		for (Category category : klass.categories) {
			generate(category, init, klass.propertyPrefix);
		}

		TypeSpec type = TypeSpec.classBuilder(klass.type.getSimpleName() + "PropertyLoader")
			.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
			.addMethod(init.build())
			.build();

		JavaFile
			.builder(env.getElementUtils().getPackageOf(klass.type).getQualifiedName().toString(), type)
			.build()
			.writeTo(env.getFiler());
	}

	private static void generate(Category category, MethodSpec.Builder spec, String root) {
		for (Category child : category.children) {
			generate(child, spec, root + "." + child.type.getSimpleName());
		}
		for (Field field : category.fields) {
			generate(field, spec, root);
		}
	}

	/**
	 * Generate field access
	 *
	 * @param spec The writer to write to
	 */
	private static void generate(Field field, MethodSpec.Builder spec, String root) {
		if (field.type == null) return;

		spec.addCode("$[");
		String propName = null;
		if (field.type.getType() == TypeHelpers.Type.GENERIC_ARRAY) {
			if (field.type.throughConstructor()) {
				spec.addCode("$T.$N = new $T(", field.category.type, field.name, field.type.getMirror());
			} else {
				propName = field.category.type.getQualifiedName().toString().replace('.', '_') + "_" + field.name;
				spec.addCode("$T $N = ", field.baseType, propName);
			}
		} else {
			spec.addCode("$T.$N = ", field.category.type, field.name);
		}

		spec.addCode("$T.$N($S, ", OptionParser.class, "get" + field.type.accessName(), root + "." + field.field.getSimpleName());

		if (field.type.getType().isArray()) {
			// A horrible method to get the default
			String format = (field.type.getComponentType().getType() == TypeHelpers.Type.STRING ? "$S" : "$L") + ", ";

			spec.addCode("new $T[]{", field.type.getComponentType().getMirror());
			int length = Array.getLength(field.defaultValue);
			for (int i = 0; i < length; i++) {
				spec.addCode(format, Array.get(field.defaultValue, i));
			}
			spec.addCode("}");
		} else {
			spec.addCode(field.type.getType() == TypeHelpers.Type.STRING ? "$S" : "$L", field.defaultValue);
		}

		spec.addCode(")");

		if (field.type.getType() == TypeHelpers.Type.GENERIC_ARRAY && field.type.throughConstructor()) {
			spec.addCode(")");
		}
		spec.addCode(";\n$]");

		if (propName != null) {
			spec.addStatement("$T.$N = new $T()", field.category.type, field.name, field.type.getMirror());
			spec.beginControlFlow("for($T $N : $N)", field.type.getComponentType().getMirror(), LOOP_NAME, propName);
			spec.addStatement("$T.$N.add($N)", field.category.type, field.name, LOOP_NAME);
			spec.endControlFlow();
		}
	}
}
