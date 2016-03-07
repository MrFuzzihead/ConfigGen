package org.squiddev.configgen.processor;

import org.squiddev.configgen.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

public class Field {
	protected final VariableElement field;

	public final String name;
	public final String description;

	public final Object defaultValue;
	public final TypeHelpers.IType type;
	public final TypeMirror baseType;
	public final boolean requiresMcRestart;
	public final boolean requiresWorldRestart;

	public final Category category;

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
			baseType = null;
		}

		RequiresRestart restart = field.getAnnotation(RequiresRestart.class);
		if (restart != null) {
			requiresMcRestart = restart.mc();
			requiresWorldRestart = restart.world();
		} else {
			requiresMcRestart = false;
			requiresWorldRestart = false;
		}
	}

	public Object calculateDefault(Object def) {
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

