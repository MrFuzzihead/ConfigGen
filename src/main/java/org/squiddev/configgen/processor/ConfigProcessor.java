package org.squiddev.configgen.processor;

import org.squiddev.configgen.Config;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

@SupportedAnnotationTypes({
	"org.squiddev.configgen.Config",
	"org.squiddev.configgen.DefaultBoolean",
	"org.squiddev.configgen.DefaultDouble",
	"org.squiddev.configgen.DefaultInt",
	"org.squiddev.configgen.DefaultString",
	"org.squiddev.configgen.Exclude",
	"org.squiddev.configgen.OnSync",
	"org.squiddev.configgen.Range",
	"org.squiddev.configgen.RequiresRestart",
})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ConfigProcessor extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
		boolean included = false;
		for (Element elem : roundEnvironment.getElementsAnnotatedWith(Config.class)) {
			if (elem.getKind() != ElementKind.CLASS) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated with @Config", elem);
				return true;
			}

			if (!included) {
				included = true;

				Filer filer = processingEnv.getFiler();
				try {
					InputStream input = getClass().getClassLoader().getResourceAsStream("org/squiddev/configgen/OptionParser.class");
					JavaFileObject object = filer.createClassFile("org.squiddev.configgen.OptionParser");
					copyStream(input, object.openOutputStream());
				} catch (IOException e) {
					processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error " + e.toString(), elem);
				}
			}

			try {
				ConfigClass config = new ConfigClass((TypeElement) elem, processingEnv);
				ForgeBuilder.generate(config, processingEnv);
				PropertyBuilder.generate(config, processingEnv);
			} catch (IOException e) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error " + e.toString(), elem);
			}
		}

		return true;
	}

	public static void copyStream(InputStream input, OutputStream output) throws IOException {
		byte[] buf = new byte[8192];
		while (true) {
			int r = input.read(buf);
			if (r == -1) {
				break;
			}
			output.write(buf, 0, r);
		}
	}
}
