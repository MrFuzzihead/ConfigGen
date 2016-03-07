package org.squiddev.configgen.processor;

import com.squareup.javapoet.MethodSpec;
import org.squiddev.configgen.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.lang.reflect.Array;

import static org.squiddev.configgen.processor.ConfigClass.*;

public class Field {
	protected final VariableElement field;

	protected final String name;
	protected final String description;

	protected final Object defaultValue;
	protected final TypeHelpers.IType type;

	protected final Category category;

	public Field(VariableElement field, Category category, ProcessingEnvironment env) {
		this.field = field;
		this.category = category;

		name = field.getSimpleName().toString();
		String desc = env.getElementUtils().getDocComment(field);
		description = desc == null ? null : desc.trim();

		TypeHelpers.IType type = TypeHelpers.getType(field.asType(), env.getTypeUtils());
		String validate = TypeHelpers.validateType(type);
		if (validate == null) {
			defaultValue = type.extractValue(calculateDefault(), type.getType().getDefault());
			if (!TypeHelpers.isType(defaultValue.getClass(), type)) {
				env.getMessager().printMessage(
						Diagnostic.Kind.ERROR,
						"Unexpected default of type " + defaultValue.getClass() + ", wanted " + type.getMirror(),
						field
				);
			}

			this.type = type;
		} else {
			env.getMessager().printMessage(Diagnostic.Kind.ERROR, validate, field);

			defaultValue = null;
			this.type = null;
		}
	}

	/**
	 * Generate field access
	 *
	 * @param spec The writer to write to
	 */
	public void generate(MethodSpec.Builder spec) {
		if (type == null) return;

		spec.addCode("$[$N = $N.get($S, $S, ", PROPERTY_NAME, CONFIG_NAME, category.name, name);

		if (type.getType() == TypeHelpers.Type.ARRAY || type.getType() == TypeHelpers.Type.GENERIC_ARRAY) {
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
		spec.addCode(", $S);\n$]", description);

		RequiresRestart restart = field.getAnnotation(RequiresRestart.class);
		if (restart != null) {
			if (restart.world()) spec.addStatement("$N.setRequiresWorldRestart($L)", PROPERTY_NAME, true);
			if (restart.mc()) spec.addStatement("$N.setRequiresMcRestart($L)", PROPERTY_NAME, true);
		}

		Range range = field.getAnnotation(Range.class);
		if (range != null) {
			spec.addStatement("$N.setMinValue($L)", PROPERTY_NAME, range.min());
			spec.addStatement("$N.setMaxValue($L)", PROPERTY_NAME, range.max());
		}

		if (type.getType() == TypeHelpers.Type.GENERIC_ARRAY) {
			if (type.throughConstructor()) {
				spec.addStatement("$T.$N = new $T($N.$N())", category.type, name, type.getMirror(), PROPERTY_NAME, "get" + type.accessName());
			} else {
				spec.beginControlFlow("for($T $N : $N.$N())", type.getComponentType().getMirror(), LOOP_NAME, PROPERTY_NAME, "get" + type.accessName());
				spec.addStatement("$T.$N.add($N)", category.type, name, LOOP_NAME);
				spec.endControlFlow();
			}
		} else {
			spec.addStatement("$T.$N = $N.$N()", category.type, name, PROPERTY_NAME, "get" + type.accessName());
		}
	}

	protected Object calculateDefault() {
		DefaultBoolean dBoolean = field.getAnnotation(DefaultBoolean.class);
		if (dBoolean != null) return dBoolean.value();

		DefaultInt dInt = field.getAnnotation(DefaultInt.class);
		if (dInt != null) return dInt.value();

		DefaultDouble dDoub = field.getAnnotation(DefaultDouble.class);
		if (dDoub != null) return dDoub.value();

		DefaultString dString = field.getAnnotation(DefaultString.class);
		if (dString != null) return dString.value();

		return null;
	}
}

