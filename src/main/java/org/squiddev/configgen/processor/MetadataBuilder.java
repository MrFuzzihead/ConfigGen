package org.squiddev.configgen.processor;

import com.squareup.javapoet.*;
import org.squiddev.configgen.processor.TypeHelpers.IType;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class MetadataBuilder {
	private final String className;
	private final String packageName;
	private final ClassName propertyName;
	private final ClassName categoryName;
	private final TypeVariableName propertyVRep;

	private final List<Category> categories;

	private final Map<TypeMirror, TypeSpec> converters = new LinkedHashMap<TypeMirror, TypeSpec>();

	private MetadataBuilder(ConfigClass klass, ProcessingEnvironment env) {
		this.categories = klass.categories;
		String className = this.className = klass.type.getSimpleName() + "Metadata";
		String packageName = this.packageName = env.getElementUtils().getPackageOf(klass.type).getQualifiedName().toString();

		this.propertyName = ClassName.get(packageName, className, "Property");
		this.propertyVRep = TypeVariableName.get("R");
		this.categoryName = ClassName.get(packageName, className, "Category");
	}

	private JavaFile build() {
		TypeSpec propertyTy = TypeSpec
			.classBuilder("Property")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
			.addTypeVariable(propertyVRep)
			.addField(FieldSpec.builder(String.class, "name", Modifier.PUBLIC, Modifier.FINAL).build())
			.addField(FieldSpec.builder(String.class, "description", Modifier.PUBLIC, Modifier.FINAL).build())
			.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), propertyVRep), "type", Modifier.PUBLIC, Modifier.FINAL).build())
			.addField(FieldSpec.builder(propertyVRep, "defaultValue", Modifier.PUBLIC, Modifier.FINAL).build())
			.addField(FieldSpec.builder(java.lang.reflect.Field.class, "field", Modifier.PRIVATE, Modifier.FINAL).build())
			.addMethod(MethodSpec.constructorBuilder()
				.addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), propertyVRep), "type")
				.addParameter(String.class, "name")
				.addParameter(String.class, "description")
				.addParameter(Class.class, "category")
				.addParameter(propertyVRep, "defaultValue")
				.addStatement("this.$N = $N", "type", "type")
				.addStatement("this.$N = $N", "name", "name")
				.addStatement("this.$N = $N", "description", "description")
				.addStatement("this.$N = $N", "defaultValue", "defaultValue")
				.beginControlFlow("try")
				.addStatement("this.$N = $N.getField($N)", "field", "category", "name")
				.nextControlFlow("catch($T $N)", Exception.class, "e")
				.addStatement("throw new $T($N)", RuntimeException.class, "e")
				.endControlFlow()
				.build())
			.addMethod(MethodSpec.methodBuilder("convert")
				.addModifiers(Modifier.PUBLIC)
				.addParameter(propertyVRep, "value")
				.returns(Object.class)
				.addStatement("return $N", "value")
				.build())
			.addMethod(MethodSpec.methodBuilder("set")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addParameter(propertyVRep, "value")
				.returns(TypeName.VOID)
				.beginControlFlow("try")
				.addStatement("$N.set(null, convert($N))", "field", "value")
				.nextControlFlow("catch($T $N)", Exception.class, "e")
				.addStatement("throw new $T($N)", RuntimeException.class, "e")
				.endControlFlow()
				.build())
			.addMethod(MethodSpec.methodBuilder("toString")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.returns(String.class)
				.addStatement("return \"Property<\" + name + \":\" + type.getName() + \">\"")
				.build())
			.build();

		TypeSpec categoryTy = TypeSpec
			.classBuilder("Category")
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
			.addField(FieldSpec.builder(String.class, "name", Modifier.PUBLIC, Modifier.FINAL).build())
			.addField(FieldSpec.builder(String.class, "description", Modifier.PUBLIC, Modifier.FINAL).build())
			.addField(FieldSpec.builder(ArrayTypeName.of(propertyName), "properties", Modifier.PRIVATE, Modifier.FINAL).build())
			.addField(FieldSpec.builder(ArrayTypeName.of(categoryName), "children", Modifier.PRIVATE, Modifier.FINAL).build())
			.addMethod(MethodSpec.constructorBuilder()
				.addParameter(String.class, "name")
				.addParameter(String.class, "description")
				.addParameter(ArrayTypeName.of(propertyName), "properties")
				.addParameter(ArrayTypeName.of(categoryName), "children")
				.addStatement("this.$N = $N", "name", "name")
				.addStatement("this.$N = $N", "description", "description")
				.addStatement("this.$N = $N", "properties", "properties")
				.addStatement("this.$N = $N", "children", "children")
				.build())
			.addMethod(MethodSpec.methodBuilder("properties")
				.addModifiers(Modifier.PUBLIC)
				.returns(ParameterizedTypeName.get(ClassName.get(List.class), propertyName))
				.addStatement("return properties == null ? $T.<$T>emptyList() : $T.unmodifiableList($T.asList(properties))",
					Collections.class, propertyName, Collections.class, Arrays.class)
				.build())
			.addMethod(MethodSpec.methodBuilder("children")
				.addModifiers(Modifier.PUBLIC)
				.returns(ParameterizedTypeName.get(ClassName.get(List.class), categoryName))
				.addStatement("return children == null ? $T.<$T>emptyList() : $T.unmodifiableList($T.asList(children))",
					Collections.class, categoryName, Collections.class, Arrays.class)
				.build())
			.addMethod(MethodSpec.methodBuilder("toString")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.returns(String.class)
				.addStatement("return \"Category<\" + name + \">\"")
				.build())
			.build();

		CodeBlock.Builder intermediate = CodeBlock.builder();
		intermediate.add("new $T[] {\n$>", categoryName);
		for (Category category : categories) {
			generate(category, intermediate);
			intermediate.add(",\n");
		}
		intermediate.add("$<}");

		TypeSpec type = TypeSpec.classBuilder(className)
			.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
			.addType(propertyTy)
			.addType(categoryTy)
			.addTypes(converters.values())
			.addField(FieldSpec.builder(ArrayTypeName.of(categoryName), "categories")
				.addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
				.initializer(intermediate.build())
				.build())
			.addMethod(MethodSpec.methodBuilder("categories")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(ParameterizedTypeName.get(ClassName.get(List.class), categoryName))
				.addStatement("return $T.unmodifiableList($T.asList($N))", Collections.class, Arrays.class, "categories")
				.build())
			.build();

		return JavaFile
			.builder(packageName, type)
			.build();
	}

	public static void generate(ConfigClass klass, ProcessingEnvironment env) throws IOException {
		new MetadataBuilder(klass, env).build().writeTo(env.getFiler());

	}

	private void generate(Category category, CodeBlock.Builder block) {
		block.add("new $T($S, $S, ", categoryName, category.name, category.description);
		if (category.fields.size() == 0) {
			block.add("null");
		} else {
			block.add("new $T[] {\n$>", propertyName);
			for (Field field : category.fields) {
				if (field.type != null) {
					generate(field, block);
					block.add(",\n");
				}
			}
			block.add("$<}");
		}

		block.add(", ");

		if (category.children.size() == 0) {
			block.add("null");
		} else {
			block.add("new $T[] {\n$>", categoryName);
			for (Category child : category.children) {
				generate(child, block);
				block.add(",\n");
			}
			block.add("$<}");
		}

		block.add(")");
	}

	/**
	 * Generate field access
	 *
	 * @param block The writer to write to
	 */
	private void generate(Field field, CodeBlock.Builder block) {
		if (field.baseType.equals(field.type.getMirror())) {
			block.add("new $T<$T>($T.class, ", propertyName, TypeName.get(field.baseType).box(), field.baseType);
		} else {
			block.add("new $T(", getConverter(field.baseType, field.type));
		}
		block.add("$S, $S, $T.class, ", field.name, field.description, field.category.type);

		if (field.type.getType().isArray()) {
			// Generate the default array.
			String format = (field.type.getComponentType().getType() == TypeHelpers.Type.STRING ? "$S" : "$L") + ", ";
			block.add("new $T[]{", field.type.getComponentType().getMirror());
			int length = Array.getLength(field.defaultValue);
			for (int i = 0; i < length; i++) {
				block.add(format, Array.get(field.defaultValue, i));
			}
			block.add("}");
		} else {
			block.add(field.type.getType() == TypeHelpers.Type.STRING ? "$S" : "$L", field.defaultValue);
		}

		block.add(")");
	}

	private TypeName getConverter(TypeMirror repType, IType fieldType) {
		TypeMirror fieldMirror = fieldType.getMirror();
		TypeSpec spec = converters.get(fieldMirror);
		if (spec != null) return ClassName.get(packageName, className, spec.name);

		TypeSpec.Builder builder = TypeSpec.classBuilder("Property" + converters.size())
			.addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
			.superclass(ParameterizedTypeName.get(propertyName, TypeName.get(repType)))
			.addMethod(MethodSpec.constructorBuilder()
				.addParameter(String.class, "name")
				.addParameter(String.class, "description")
				.addParameter(Class.class, "category")
				.addParameter(TypeName.get(repType), "defaultValue")
				.addStatement("super($T.class, $N, $N, $N, $N)", repType, "name", "description", "category", "defaultValue")
				.build());

		MethodSpec.Builder convert = MethodSpec.methodBuilder("convert")
			.addAnnotation(Override.class)
			.addModifiers(Modifier.PUBLIC)
			.addParameter(TypeName.get(repType), "value")
			.returns(Object.class);

		if (fieldType.getType() == TypeHelpers.Type.GENERIC_ARRAY) {
			if (fieldType.throughConstructor()) {
				convert.addStatement("return new $T($N)", fieldMirror, "value");
			} else {
				convert.addStatement("$T $N = new $T()", fieldMirror, "converted", fieldMirror);
				convert.beginControlFlow("for($T $N : $N)", fieldType.getComponentType().getMirror(), "child", "value");
				convert.addStatement("$N.add($N)", "converted", "child");
				convert.endControlFlow();
				convert.addStatement("return $N", "converted");
			}
		} else {
			convert.addCode("return ($T)$N", fieldMirror, "value");
		}

		spec = builder.addMethod(convert.build()).build();
		converters.put(fieldMirror, spec);
		return ClassName.get(packageName, className, spec.name);
	}
}
