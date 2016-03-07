package org.squiddev.configgen.processor;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.minecraftforge.common.config.Configuration;
import org.squiddev.configgen.Range;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;

/**
 * Builder for generating {@link Configuration} readers
 */
public class ForgeBuilder {
	public static final String CONFIG_FIELD = "configuration";
	public static final String CONFIG_NAME = "config";
	public static final String LOOP_NAME = "var";

	public static void generate(ConfigClass klass, ProcessingEnvironment env) throws IOException {
		FieldSpec configuration = FieldSpec
			.builder(Configuration.class, CONFIG_FIELD, Modifier.PRIVATE, Modifier.STATIC)
			.build();

		MethodSpec.Builder sync = MethodSpec.methodBuilder("sync")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
			.returns(void.class)
			.addStatement("$T $N = $N", Configuration.class, CONFIG_NAME, CONFIG_FIELD);

		for (Category category : klass.categories) {
			generate(category, sync);
		}

		if (klass.sync != null) sync.addStatement("$T.$N()", klass.type, klass.sync.getSimpleName());
		sync.addStatement("$N.save()", CONFIG_NAME);


		MethodSpec init = MethodSpec.methodBuilder("init")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
			.addParameter(File.class, "file")
			.returns(void.class)
			.addStatement("configuration = new Configuration(file)")
			.addStatement("configuration.load()")
			.addStatement("sync()")
			.build();

		MethodSpec getConfiguration = MethodSpec.methodBuilder("getConfiguration")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
			.returns(Configuration.class)
			.addStatement("return configuration")
			.build();

		TypeSpec type = TypeSpec.classBuilder(klass.type.getSimpleName() + "Loader")
			.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
			.addField(configuration)
			.addMethod(sync.build())
			.addMethod(init)
			.addMethod(getConfiguration)
			.build();

		JavaFile
			.builder(env.getElementUtils().getPackageOf(klass.type).getQualifiedName().toString(), type)
			.build()
			.writeTo(env.getFiler());
	}

	public static void generate(Category category, MethodSpec.Builder spec) {
		for (Category child : category.children) {
			generate(child, spec);
		}
		for (Field field : category.fields) {
			generate(field, spec);
		}

		if (category.description != null || category.root.languagePrefix != null || category.requiresMcRestart || category.requiresWorldRestart) {
			spec.addCode("$[");
			spec.addCode("$N.getCategory($S)", CONFIG_NAME, category.name);

			if (category.root.languagePrefix != null) {
				spec.addCode("\n.setLanguageKey($S)", category.root.languagePrefix + category.name);
			}
			if (category.requiresWorldRestart) spec.addCode("\n.setRequiresWorldRestart($L)", true);
			if (category.requiresMcRestart) spec.addCode("\n.setRequiresMcRestart($L)", true);

			// This doesn't return a ConfigCategory so has to be last
			if (category.description != null) spec.addCode("\n.setComment($S)", category.description.trim());

			spec.addCode(";\n$]");
		}
	}

	/**
	 * Generate field access
	 *
	 * @param spec The writer to write to
	 */
	public static void generate(Field field, MethodSpec.Builder spec) {
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

		spec.addCode("$N.get($S, $S, ", CONFIG_NAME, field.category.name, field.name);

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
		spec.addCode(", $S)\n", field.description);

		if (field.requiresWorldRestart) spec.addCode(".setRequiresWorldRestart($L)\n", true);
		if (field.requiresMcRestart) spec.addCode(".setRequiresMcRestart($L)\n", true);

		Range range = field.field.getAnnotation(Range.class);
		if (range != null) {
			if (field.type.getType() == TypeHelpers.Type.INT) {
				// We need the casts here to ensure that they are integers
				spec.addCode(".setMinValue($L)\n", (int) range.min());
				spec.addCode(".setMaxValue($L)\n", (int) range.max());
			} else {
				spec.addCode(".setMinValue($L)", range.min());
				spec.addCode(".setMaxValue($L)\n", range.max());
			}
		}

		if (field.category.root.languagePrefix != null) {
			spec.addCode(".setLanguageKey($S)\n", field.category.root.languagePrefix + field.category.name + "." + field.name);
		}

		spec.addCode(".$N()", "get" + field.type.accessName());
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
