package org.squiddev.configgen.processor;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.squiddev.configgen.Config;

@SupportedAnnotationTypes({ "org.squiddev.configgen.Config", "org.squiddev.configgen.DefaultBoolean",
    "org.squiddev.configgen.DefaultDouble", "org.squiddev.configgen.DefaultInt", "org.squiddev.configgen.DefaultString",
    "org.squiddev.configgen.Exclude", "org.squiddev.configgen.OnSync", "org.squiddev.configgen.Range",
    "org.squiddev.configgen.RequiresRestart", })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ConfigProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (Element elem : roundEnvironment.getElementsAnnotatedWith(Config.class)) {
            if (elem.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated with @Config", elem);
                return true;
            }

            try {
                ConfigClass config = new ConfigClass((TypeElement) elem, processingEnv);
                ForgeBuilder.generate(config, processingEnv);
                PropertyBuilder.generate(config, processingEnv);
                MetadataBuilder.generate(config, processingEnv);
            } catch (IOException e) {
                processingEnv.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Error " + e.toString(), elem);
            }
        }

        return true;
    }
}
