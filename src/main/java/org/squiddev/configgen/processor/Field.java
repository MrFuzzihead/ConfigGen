package org.squiddev.configgen.processor;

import com.squareup.javapoet.MethodSpec;
import org.squiddev.configgen.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.lang.reflect.Array;

import static org.squiddev.configgen.processor.ConfigClass.CONFIG_NAME;
import static org.squiddev.configgen.processor.ConfigClass.PROPERTY_NAME;

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


		TypeHelpers.IType type = TypeHelpers.getType(field.asType());
		String validate = TypeHelpers.validateType(type);
		if (validate == null) {
			defaultValue = type.extractValue(calculateDefault(), type.getType().getDefault());
			if (!TypeHelpers.instanceOf(type.getTypeClass(), defaultValue)) {
				env.getMessager().printMessage(
					Diagnostic.Kind.ERROR,
					"Unexpected default of type " + defaultValue.getClass() + ", wanted " + type.getTypeClass(),
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
	 */
	public void generate(MethodSpec.Builder spec) {
		if (type == null) return;

		spec.addCode("$[$N = $N.get($S, $S, ", PROPERTY_NAME, CONFIG_NAME, category.name, name);

		if (type.getTypeClass().isArray()) {
			// A horrible method to get the default
			String format = (((TypeHelpers.ArrayType) type).getComponentType().getType() == TypeHelpers.Type.STRING ? "$S" : "$L") + ", ";

			spec.addCode("new $T{", type.getTypeClass());
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

		spec.addStatement("$T.$N = $N.$N()", category.type, name, PROPERTY_NAME, "get" + type.accessName());
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

		return type.getType().getDefault();
	}
}

