package remoter.compiler.kspbuilder

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.processing.Resolver
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Base class of all the KSP builders
 */
open class KSPRemoterBuilder(
    val remoterInterfaceElement: KSClassDeclaration,
    val bindingManager: KSPBindingManager,
    val resolver: Resolver
) {

    val remoterInterfacePackageName: String = remoterInterfaceElement.packageName.asString()
    val remoterInterfaceClassName: String = remoterInterfaceElement.simpleName.asString()
    private val logger = bindingManager.getLogger()

    /**
     * Callback that visit each element to be processed
     */
    interface ElementVisitor {
        fun visitElement(
            classBuilder: TypeSpec.Builder,
            member: KSFunctionDeclaration,
            methodIndex: Int,
            methodBuilder: FunSpec.Builder?
        )
    }

    /**
     * Logs an error
     */
    open fun logError(message: String?) {
        logger.error(message ?: "")
    }

    /**
     * Logs a warning
     */
    open fun logWarning(message: String?) {
        logger.warn(message ?: "")
    }

    /**
     * Logs an info
     */
    open fun logInfo(message: String?) {
        logger.info(message ?: "")
    }

    /**
     * Finds that elements that needs to be processed
     */
    protected open fun processRemoterElements(
        classBuilder: TypeSpec.Builder,
        elementVisitor: ElementVisitor,
        methodBuilder: FunSpec.Builder?
    ) {
        processRemoterElements(
            classBuilder,
            remoterInterfaceElement,
            0,
            elementVisitor,
            methodBuilder
        )
    }

    /**
     * Recursively visit extended elements
     */
    internal open fun processRemoterElements(
        classBuilder: TypeSpec.Builder,
        element: KSClassDeclaration,
        methodIndex: Int,
        elementVisitor: ElementVisitor,
        methodBuilder: FunSpec.Builder?
    ): Int {
        var methodIndex = methodIndex
        // Process super interfaces
        for (superType in element.superTypes) {
            val superDeclaration = superType.resolve().declaration
            if (superDeclaration is KSClassDeclaration && superDeclaration.classKind == com.google.devtools.ksp.symbol.ClassKind.INTERFACE) {
                methodIndex = processRemoterElements(
                    classBuilder,
                    superDeclaration,
                    methodIndex,
                    elementVisitor,
                    methodBuilder
                )
            }
        }
        // Process functions
        for (declaration in element.declarations) {
            if (declaration is KSFunctionDeclaration) {
                elementVisitor.visitElement(classBuilder, declaration, methodIndex, methodBuilder)
                methodIndex++
            }
        }
        return methodIndex
    }

    /**
     * Checks whether this element has any suspend function methods in it
     */
    fun hasSuspendFunction(): Boolean {
        var suspendFound = false
        // Check super interfaces
        for (superType in remoterInterfaceElement.superTypes) {
            val superDeclaration = superType.resolve().declaration
            if (superDeclaration is KSClassDeclaration && superDeclaration.classKind == com.google.devtools.ksp.symbol.ClassKind.INTERFACE) {
                if (!suspendFound) {
                    suspendFound = KSPRemoterBuilder(
                        superDeclaration,
                        bindingManager,
                        resolver
                    ).hasSuspendFunction()
                } else {
                    break
                }
            }
        }
        if (!suspendFound) {
            for (declaration in remoterInterfaceElement.declarations) {
                if (!suspendFound) {
                    if (declaration is KSFunctionDeclaration) {
                        suspendFound = declaration.modifiers.contains(Modifier.SUSPEND)
                    }
                } else {
                    break
                }
            }
        }
        return suspendFound
    }
}
