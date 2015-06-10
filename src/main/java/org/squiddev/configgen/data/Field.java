package org.squiddev.configgen.data;

import com.squareup.javapoet.MethodSpec;
import net.minecraftforge.common.config.Configuration;
import org.squiddev.configgen.Range;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

public class Field {
	protected final VariableElement field;

	protected final String name;
	protected final String description;

	protected final Category category;

	public Field(VariableElement field, Category category, ProcessingEnvironment env) {
		this.field = field;
		this.category = category;

		name = field.getSimpleName().toString();
		String desc = env.getElementUtils().getDocComment(field);
		description = desc == null ? null : desc.trim();
	}

	/**
	 * Generate field access
	 * TODO: Use {@link Configuration#get(String, String, boolean, String)} style members instead
	 */
	public void generate(MethodSpec.Builder spec, ProcessingEnvironment env) {
		TypeHelpers.IType type = TypeHelpers.getType(field.asType());
		// Utils.dump(field, env);
		switch (type.getType()) {
			case BOOLEAN:
				spec.addStatement(
					"$T.$N = $N.getBoolean($S, $S, $L, $S)",
					category.type, name, ConfigClass.CONFIG_NAME, name, category.name,
					true,
					description
				);
				break;
			case INT:
				int min = 0, max = Integer.MAX_VALUE;
				Range range = field.getAnnotation(Range.class);
				if (range != null) {
					min = range.min();
					max = range.max();
				}

				spec.addStatement(
					"$T.$N = $N.getInt($S, $S, $L, $L, $L, $S)",
					category.type, name, ConfigClass.CONFIG_NAME, name, category.name,
					0, min, max,
					description
				);
				break;
			case ARRAY: {
				if (((TypeHelpers.ArrayType) type).getComponentType().getType() == TypeHelpers.Type.STRING) {
					spec.addStatement(
						"$T.$N = $N.getStringList($S, $S, $L, $S)",
						category.type, name, ConfigClass.CONFIG_NAME, name, category.name,
						null,
						description
					);
					break;
				}
				unexpected(env);
			}
			case STRING: {
				spec.addStatement(
					"$T.$N = $N.getString($S, $S, $S, $S)",
					category.type, name, ConfigClass.CONFIG_NAME, name, category.name,
					null,
					description
				);
				break;
			}

			default:
				unexpected(env);
		}
	}

	protected void unexpected(ProcessingEnvironment env) {
		env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unexpected type " + field.asType(), field);
	}
}
