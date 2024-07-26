package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import static com.google.auto.common.MoreElements.getPackage;

/**
 * A builder that knows to generate some part of code like class, fields or methods etc
 */
abstract class RemoteBuilder {

    private Messager messager;
    private Element remoterInterfaceElement;
    private String remoterInterfacePackageName;
    private String remoterInterfaceClassName;
    private BindingManager bindingManager;

    /**
     * Callback that visit each element to be processed
     */
    interface ElementVisitor {
        void visitElement(TypeSpec.Builder classBuilder, Element member, int methodIndex, MethodSpec.Builder methodBuilder);
    }


    protected RemoteBuilder(Messager messager, Element remoterInterfaceElement) {
        this.messager = messager;
        this.remoterInterfaceElement = remoterInterfaceElement;
        if (remoterInterfaceElement != null) {
            this.remoterInterfacePackageName = getPackage(remoterInterfaceElement).getQualifiedName().toString();
            this.remoterInterfaceClassName = remoterInterfaceElement.getSimpleName().toString();
        }
    }

    /**
     * Returns the Interface element
     */
    TypeElement getRemoterInterfaceElement() {
        return (TypeElement)remoterInterfaceElement;
    }

    /**
     * Returns the class name of the main Remote interface that is being built
     */
    String getRemoterInterfaceClassName() {
        return remoterInterfaceClassName;
    }

    /**
     * Returns the package name of the main Remote interface that is being built
     */
    String getRemoterInterfacePackageName() {
        return remoterInterfacePackageName;
    }

    /**
     * Logs an error
     */
    void logError(String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message);
    }

    /**
     * Logs a warning
     */
    void logWarning(String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, message);
    }

    /**
     * Logs an info
     */
    void logInfo(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, message);
    }

    /**
     * Returns the {@link BindingManager}
     */
    BindingManager getBindingManager() {
        return bindingManager;
    }

    void setBindingManager(BindingManager bindingManager) {
        this.bindingManager = bindingManager;
    }


    /**
     * Finds that elements that needs to be processed
     */
    protected void processRemoterElements(TypeSpec.Builder classBuilder, ElementVisitor elementVisitor, MethodSpec.Builder methodBuilder) {
        processRemoterElements(classBuilder, getRemoterInterfaceElement(), 0, elementVisitor, methodBuilder);
    }

    /**
     * Recursevely Visit extended elements
     */
    private int processRemoterElements(TypeSpec.Builder classBuilder, Element element, int methodIndex, ElementVisitor elementVisitor, MethodSpec.Builder methodBuilder) {
        if (element instanceof TypeElement) {
            for (TypeMirror typeMirror : ((TypeElement) element).getInterfaces()) {
                if (typeMirror instanceof DeclaredType) {
                    Element superElement = ((DeclaredType) typeMirror).asElement();
                    methodIndex = processRemoterElements(classBuilder, superElement, methodIndex, elementVisitor, methodBuilder);
                }
            }
            for (Element member : element.getEnclosedElements()) {
                if (member.getKind() == ElementKind.METHOD) {
                    elementVisitor.visitElement(classBuilder, member, methodIndex, methodBuilder);
                    methodIndex++;
                }
            }
        }
        return methodIndex;
    }


}
