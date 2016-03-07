package org.squiddev.configgen.processor;

import com.squareup.javapoet.MethodSpec;
import org.squiddev.configgen.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.reflect.Array;

import static org.squiddev.configgen.processor.ConfigClass.CONFIG_NAME;
import static org.squiddev.configgen.processor.ConfigClass.LOOP_NAME;

public class Field {
	protected final VariableElement field;

	protected String name;
	protected String description;

	protected Object defaultValue;
	protected TypeHelpers.IType type;
	protected TypeMirror baseType;
	protected boolean requiresMcRestart = false;
	protected boolean requiresWorldRestart = false;

	protected Category category;

	public Field(VariableElement field, Category category, ProcessingEnvironment env) {
		this.field = field;
		this.category = category;

		name = field.getSimpleName().toString();
		String desc = env.getElementUtils().getDocComment(field);
		description = desc == null ? null : desc.trim();

		TypeHelpers.IType type = TypeHelpers.getType(field.asType(), env.getTypeUtils());
		String validate = TypeHelpers.validateType(type);
		if (validate == null) {
			defaultValue = type.extractValue(calculateDefault(type.getDefault()), type.getDefault());
			if (!TypeHelpers.isType(defaultValue.getClass(), type)) {
				env.getMessager().printMessage(
						Diagnostic.Kind.ERROR,
						"Unexpected default of type " + defaultValue.getClass() + ", wanted " + type.getMirror(),
						field
				);
			}

			this.type = type;
			baseType = type.getType() == TypeHelpers.Type.GENERIC_ARRAY ? env.getTypeUtils().getArrayType(type.getComponentType().getMirror()) : type.getMirror();
		} else {
			env.getMessager().printMessage(Diagnostic.Kind.ERROR, validate, field);

			defaultValue = null;
			this.type = null;
		}

		RequiresRestart restart = field.getAnnotation(RequiresRestart.class);
		if (restart != null) {
			requiresMcRestart = restart.mc();
			requiresWorldRestart = restart.world();
		}
	}

	/**
	 * Generate field access
	 *
	 * @param spec The writer to write to
	 */
	@SuppressWarnings("RedundantCast")
	public void generate(MethodSpec.Builder spec) {
		if (type == null) return;

		spec.addCode("$[");
		String propName = null;
		if (type.getType() == TypeHelpers.Type.GENERIC_ARRAY) {
			if (type.throughConstructor()) {
				spec.addCode("$T.$N = new $T(", category.type, name, type.getMirror());
			} else {
				propName = category.type.getQualifiedName().toString().replace('.', '_') + "_" + name;
				spec.addCode("$T $N = ", baseType, propName);
			}
		} else {
			spec.addCode("$T.$N = ", category.type, name);
		}

		spec.addCode("$N.get($S, $S, ", CONFIG_NAME, category.name, name);

		if (type.getType().isArray()) {
			// A horrible method to get the default
			String format = (type.getComponentType().getType() == TypeHelpers.Type.STRING ? "$S" : "$L") + ", ";

			spec.addCode("new $T[]{", type.getComponentType().getMirror());
			int length = Array.getLength(defaultValue);
			for (int i = 0; i < length; i++) {
				spec.addCode(format, Array.get(defaultValue, i));
			}
			spec.addCode("}");
		} else {
			spec.addCode(type.getType() == TypeHelpers.Type.STRING ? "$S" : "$L", defaultValue);
		}
		spec.addCode(", $S)\n", description);

		if (requiresWorldRestart) spec.addCode(".setRequiresWorldRestart($L)\n", true);
		if (requiresMcRestart) spec.addCode(".setRequiresMcRestart($L)\n", true);

		Range range = field.getAnnotation(Range.class);
		if (range != null) {
			if (type.getType() == TypeHelpers.Type.INT) {
				// We need the casts here to ensure that they are integers
				spec.addCode(".setMinValue($L)\n", (int) range.min());
				spec.addCode(".setMaxValue($L)\n", (int) range.max());
			} else {
				spec.addCode(".setMinValue($L)", range.min());
				spec.addCode(".setMaxValue($L)\n", range.max());
			}
		}

		if (category.root.languagePrefix != null) {
			spec.addCode(".setLanguageKey($S)\n", category.root.languagePrefix + category.name + "." + name);
		}

		spec.addCode(".$N()", "get" + type.accessName());
		if (type.getType() == TypeHelpers.Type.GENERIC_ARRAY && type.throughConstructor()) spec.addCode(")");
		spec.addCode(";\n$]");

		if (propName != null) {
			spec.addStatement("$T.$N = new $T()", category.type, name, type.getMirror());
			spec.beginControlFlow("for($T $N : $N)", type.getComponentType().getMirror(), LOOP_NAME, propName);
			spec.addStatement("$T.$N.add($N)", category.type, name, LOOP_NAME);
			spec.endControlFlow();
		}
	}

	protected Object calculateDefault(Object def) {
		DefaultBoolean dBoolean = field.getAnnotation(DefaultBoolean.class);
		if (dBoolean != null) return dBoolean.value();

		DefaultInt dInt = field.getAnnotation(DefaultInt.class);
		if (dInt != null) return dInt.value();

		DefaultDouble dDoub = field.getAnnotation(DefaultDouble.class);
		if (dDoub != null) return dDoub.value();

		DefaultString dString = field.getAnnotation(DefaultString.class);
		if (dString != null) return dString.value();

		return def;
	}
}

