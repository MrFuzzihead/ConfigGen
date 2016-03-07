package org.squiddev.configgen.processor;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.minecraftforge.common.config.Configuration;
import org.squiddev.configgen.Config;
import org.squiddev.configgen.OnSync;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The root config class
 */
public class ConfigClass {
	public static final String CONFIG_FIELD = "configuration";
	public static final String CONFIG_NAME = "config";
	public static final String PROPERTY_NAME = "property";
	public static final String LOOP_NAME = "var";

	protected final TypeElement type;

	public final String languagePrefix;

	protected final List<Category> categories = new ArrayList<Category>();
	protected ExecutableElement sync;

	public ConfigClass(TypeElement type, ProcessingEnvironment env) {
		this.type = type;

		Config config = type.getAnnotation(Config.class);
		languagePrefix = config.languagePrefix() == null || config.languagePrefix().isEmpty() ? null : config.languagePrefix();

		for (Element element : type.getEnclosedElements()) {
			switch (element.getKind()) {
				case CLASS:
					Utils.checkUsable(element, env);
					categories.add(new Category((TypeElement) element, null, this, env));
					break;
				case METHOD:
					if (element.getAnnotation(OnSync.class) != null) {
						Utils.checkUsable(element, env);
						if (sync != null) {
							env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot have multiple @OnSync instances", element);
						}
						sync = (ExecutableElement) element;
					}
				default:
					break;
			}
		}
	}

	public void generate(ProcessingEnvironment env) throws IOException {
		FieldSpec configuration = FieldSpec
				.builder(Configuration.class, CONFIG_FIELD, Modifier.PRIVATE, Modifier.STATIC)
				.build();

		MethodSpec.Builder sync = MethodSpec.methodBuilder("sync")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(void.class)
				.addStatement("$T $N = $N", Configuration.class, CONFIG_NAME, CONFIG_FIELD);

		for (Category category : categories) {
			category.generate(sync);
		}

		if (this.sync != null) sync.addStatement("$T.$N()", type, this.sync.getSimpleName());
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

		TypeSpec klass = TypeSpec.classBuilder(type.getSimpleName() + "Loader")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addField(configuration)
				.addMethod(sync.build())
				.addMethod(init)
				.addMethod(getConfiguration)
				.build();

		JavaFile
				.builder(env.getElementUtils().getPackageOf(type).getQualifiedName().toString(), klass)
				.build()
				.writeTo(env.getFiler());
	}
}
