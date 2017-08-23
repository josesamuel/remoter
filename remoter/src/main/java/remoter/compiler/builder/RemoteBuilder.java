package remoter.compiler.builder;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
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
    Element getRemoterInterfaceElement() {
        return remoterInterfaceElement;
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
}
