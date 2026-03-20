package remoter.compiler

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import remoter.annotations.Remoter
import remoter.compiler.kspbuilder.KSPBindingManager
import remoter.compiler.kspbuilder.KSPRemoterBuilder

/**
 * A KSP processor for Remoter
 */
class RemoterKSPProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Remoter::class.java.name)
        val unableToProcess = symbols.filterNot { it.validate() }.toList()

        symbols.filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .forEach { classDeclaration ->
                if (classDeclaration.classKind == ClassKind.INTERFACE) {
                    val classesToWrap = getClassesToWrap(classDeclaration)
                    if (classesToWrap.isNotEmpty()) {
                        classesToWrap.forEach { ksType ->
                            val wrappedDeclaration = ksType.declaration
                            if (wrappedDeclaration is KSClassDeclaration && wrappedDeclaration.classKind == ClassKind.INTERFACE) {
                                processRemoterInterface(wrappedDeclaration, resolver)
                            } else {
                                environment.logger.warn(
                                    "@Remoter classesToWrap expected an interface. Ignoring ${wrappedDeclaration.simpleName.asString()}",
                                    classDeclaration
                                )
                            }
                        }
                    } else {
                        processRemoterInterface(classDeclaration, resolver)
                    }
                } else {
                    environment.logger.warn(
                        "@Remoter is expected only for interface. Ignoring ${classDeclaration.simpleName.asString()}",
                        classDeclaration
                    )
                }
            }

        return unableToProcess
    }

    private fun getClassesToWrap(classDeclaration: KSClassDeclaration): List<KSType> {
        val annotation = classDeclaration.annotations.find {
            it.shortName.asString() == Remoter::class.simpleName
        }
        val classesToWrapArg =
            annotation?.arguments?.find { it.name?.asString() == "classesToWrap" }
        return (classesToWrapArg?.value as? List<*>)?.filterIsInstance<KSType>() ?: emptyList()
    }

    private fun processRemoterInterface(classDeclaration: KSClassDeclaration, resolver: Resolver) {
        val bindingManager = KSPBindingManager(environment, resolver)
        val builder = KSPRemoterBuilder(classDeclaration, bindingManager, resolver)
        if (builder.hasSuspendFunction()) {
            bindingManager.generateProxy(classDeclaration, resolver)
            bindingManager.generateStub(classDeclaration, resolver)
        } else {
            // For now, assume Kotlin, but in future might need Java equivalent
            bindingManager.generateProxy(classDeclaration, resolver)
            bindingManager.generateStub(classDeclaration, resolver)
        }
    }
}
