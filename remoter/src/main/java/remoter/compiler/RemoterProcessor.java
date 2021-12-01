package remoter.compiler;


import static net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING;

import com.google.auto.service.AutoService;

import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import remoter.annotations.Remoter;
import remoter.compiler.builder.BindingManager;
import remoter.compiler.kbuilder.CommonKt;
import remoter.compiler.kbuilder.KBindingManager;

/**
 * AnnotationProcessor that processes the @{@link Remoter} annotations and
 * generates the Stub and Proxy classes that allows a plain old interface
 * to be used as an android remote interface.
 *
 * @author js
 */
@AutoService(Processor.class)
@IncrementalAnnotationProcessor(ISOLATING)
public class RemoterProcessor extends AbstractProcessor {

    private BindingManager bindingManager;
    private KBindingManager kotlinBindingManager;
    private Messager messager;


    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.messager = env.getMessager();
        bindingManager = new BindingManager(env.getElementUtils(), env.getFiler(), messager, env.getTypeUtils());
        kotlinBindingManager = new KBindingManager(env);
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * Only one annotation is supported at class level - @{@link Remoter}
     */
    private Set<Class<? extends Annotation>> getSupportedAnnotations() {
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
        annotations.add(Remoter.class);
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        for (Element element : env.getElementsAnnotatedWith(Remoter.class)) {
            if (element.getKind() == ElementKind.INTERFACE) {

                boolean wrapperFound = false;
                for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> elements : annotationMirror.getElementValues().entrySet()) {
                        ExecutableElement executableElement = elements.getKey();
                        AnnotationValue annotationValue = elements.getValue();

                        if (executableElement.getSimpleName().toString().equals("classesToWrap")) {
                            List classesToConvert = (List) annotationValue.getValue();
                            if (classesToConvert != null) {
                                for (Object classToConvert : classesToConvert) {
                                    String convertName = classToConvert.toString();
                                    if (convertName.endsWith(".class")) {
                                        convertName = convertName.substring(0, convertName.length() - 6);
                                    }
                                    Element wrappedElement = getElement(convertName);
                                    if (wrappedElement != null) {
                                        generateClassesFor(wrappedElement);
                                    }
                                    wrapperFound = true;
                                }
                            }
                        }
                    }
                }

                if (!wrapperFound) {
                    generateClassesFor(element);
                }
            } else {
                messager.printMessage(Diagnostic.Kind.WARNING, "@Remoter is expected only for interface. Ignoring " + element.getSimpleName());
            }
        }
        return false;
    }

    /**
     * Generate the Remoter proxy/stub for given element
     * @param element
     */
    private void generateClassesFor(Element element) {
        if (element.getKind() == ElementKind.INTERFACE) {
            if (CommonKt.hasSuspendFunction(element)) {
                kotlinBindingManager.generateProxy(element);
                kotlinBindingManager.generateStub(element);
            } else {
                bindingManager.generateProxy(element);
                bindingManager.generateStub(element);
            }
        } else {
            messager.printMessage(Diagnostic.Kind.WARNING, "@Remoter is expected only for interface. Ignoring " + element.getSimpleName());
        }
    }

    /**
     * Returns a [Element] for the given class
     */
    private Element getElement(String className) {
        String cName = className;
        int templateStart = className.indexOf('<');
        if (templateStart != -1) {
            cName = className.substring(0, templateStart).trim();
        }
        return processingEnv.getElementUtils().getTypeElement(cName);
    }
}
