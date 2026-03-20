package remoter.compiler.kspbuilder

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*

/**
 * Manages KSP-based kotlin file generation for Remoter
 */
open class KSPBindingManager(
    private val environment: SymbolProcessorEnvironment,
    private val resolver: Resolver
) {

    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private val logger: KSPLogger = environment.logger
    private val typeBuilderMap: MutableMap<KSType, KSPParamBuilder> = mutableMapOf()

    private var stringType: KSType? = null
    private var charSequenceType: KSType? = null
    private var listType: KSType? = null
    private var setType: KSType? = null
    private var mapType: KSType? = null
    private var parcellableType: KSType? = null
    private var parcelFound: Boolean = false
    private var remoterBuilderFound: Boolean = false

    init {
        // Initialize types
        stringType =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("kotlin.String"))
                ?.asType(emptyList())
        charSequenceType =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("kotlin.CharSequence"))
                ?.asType(emptyList())
        listType =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("kotlin.collections.List"))
                ?.asType(emptyList())
        setType =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("kotlin.collections.Set"))
                ?.asType(emptyList())
        mapType =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("kotlin.collections.Map"))
                ?.asType(emptyList())
        parcellableType =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("android.os.Parcelable"))
                ?.asType(emptyList())
        //parcelClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString(PARCELER_ANNOTATION))
        //remoterBuilderClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("remoter.builder.RemoterBuilder"))

        val parcelPresent = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString("org.parceler.Parcel")
        )
        parcelFound = parcelPresent != null

        val serviceConnectorType = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString("remoter.builder.ServiceConnector")
        )
        remoterBuilderFound = serviceConnectorType != null

    }

    /**
     * Generates the proxy class
     */
    fun generateProxy(classDeclaration: KSClassDeclaration, resolver: Resolver) {
        val fileSpec = KSPClassBuilder(classDeclaration, this, resolver).generateProxy()
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false, classDeclaration.containingFile!!),
            packageName = classDeclaration.packageName.asString(),
            fileName = fileSpec.name
        )
        file.writer().use { fileSpec.writeTo(it) }
    }

    /**
     * Generates the stub class
     */
    fun generateStub(classDeclaration: KSClassDeclaration, resolver: Resolver) {
        val fileSpec = KSPClassBuilder(classDeclaration, this, resolver).generateStub()
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false, classDeclaration.containingFile!!),
            packageName = classDeclaration.packageName.asString(),
            fileName = fileSpec.name
        )
        file.writer().use { fileSpec.writeTo(it) }
    }

    fun getLogger(): KSPLogger = logger

    fun hasRemoterBuilder() = remoterBuilderFound

    /**
     * Returns the [KSPFieldBuilder] that adds fields to the class spec
     */
    internal fun getFieldBuilder(classDeclaration: KSClassDeclaration, resolver: Resolver) =
        KSPFieldBuilder(classDeclaration, this, resolver)

    internal fun getMethodBuilder(classDeclaration: KSClassDeclaration, resolver: Resolver) =
        KSPMethodBuilder(classDeclaration, this, resolver)

    /**
     * Returns the [KSPParamBuilder] that knows how to generate code for the given type of parameter
     */
    internal open fun getBuilderForParam(
        remoteElement: KSClassDeclaration,
        ksType: KSType
    ): KSPParamBuilder {

        // Check cache first
        var paramBuilder: KSPParamBuilder? = typeBuilderMap[ksType]
        if (paramBuilder != null) {
            return paramBuilder
        }

        // Handle primitive types
        paramBuilder = when (ksType.declaration.qualifiedName?.asString()) {
            "kotlin.Boolean" -> KSPBooleanParamBuilder(remoteElement, this, resolver)
            "kotlin.Byte" -> KSPByteParamBuilder(remoteElement, this, resolver)
            "kotlin.Char" -> KSPCharParamBuilder(remoteElement, this, resolver)
            "kotlin.Double" -> KSPDoubleParamBuilder(remoteElement, this, resolver)
            "kotlin.Float" -> KSPFloatParamBuilder(remoteElement, this, resolver)
            "kotlin.Int" -> KSPIntParamBuilder(remoteElement, this, resolver)
            "kotlin.Long" -> KSPLongParamBuilder(remoteElement, this, resolver)
            "kotlin.Short" -> KSPShortParamBuilder(remoteElement, this, resolver)

            "kotlin.BooleanArray" -> KSPBooleanParamBuilder(remoteElement, this, resolver)
            "kotlin.ByteArray" -> KSPByteParamBuilder(remoteElement, this, resolver)
            "kotlin.CharArray" -> KSPCharParamBuilder(remoteElement, this, resolver)
            "kotlin.DoubleArray" -> KSPDoubleParamBuilder(remoteElement, this, resolver)
            "kotlin.FloatArray" -> KSPFloatParamBuilder(remoteElement, this, resolver)
            "kotlin.IntArray" -> KSPIntParamBuilder(remoteElement, this, resolver)
            "kotlin.LongArray" -> KSPLongParamBuilder(remoteElement, this, resolver)
            "kotlin.ShortArray" -> KSPShortParamBuilder(remoteElement, this, resolver)

            else -> null
        }

        if (paramBuilder != null) {
            typeBuilderMap[ksType] = paramBuilder
            return paramBuilder
        }

        // Handle Array types recursively
        if (ksType.isArray() || ksType.isPrimitiveArray()) {
            val componentType = ksType.arguments.firstOrNull()?.type?.resolve()
            if (componentType != null) {
                return getBuilderForParam(remoteElement, componentType)
            }
        }

        // Handle declared (class/interface) types
        if (ksType.declaration is KSClassDeclaration) {
            val qualifiedName = ksType.declaration.qualifiedName?.asString() ?: ""

            // Check for List of Parceler (generic type with @Parceler annotation)
            val genericListType = getGenericListOfParceler(ksType)
            if (genericListType != null) {
                paramBuilder =
                    KSPListOfParcelerParamBuilder(genericListType, remoteElement, this, resolver)
            }
            // Check for String
            else if (qualifiedName == "kotlin.String" || qualifiedName == "java.lang.String") {
                paramBuilder = KSPStringParamBuilder(remoteElement, this, resolver)
            }
            // Check for CharSequence
            else if (qualifiedName == "kotlin.CharSequence" || qualifiedName == "java.lang.CharSequence") {
                paramBuilder = KSPCharSequenceParamBuilder(remoteElement, this, resolver)
            }
            // Check for List
            else if (qualifiedName == "kotlin.collections.List" || qualifiedName == "kotlin.collections.MutableList" || qualifiedName == "java.util.List") {
                paramBuilder = KSPListParamBuilder(remoteElement, this, resolver)
            }
            // Check for Map
            else if (qualifiedName == "kotlin.collections.Map" || qualifiedName == "kotlin.collections.MutableMap" || qualifiedName == "java.util.Map") {
                paramBuilder = KSPMapParamBuilder(remoteElement, this, resolver)
            }
            // Check for Parcelable
            else if (isAssignableToParcelable(ksType)) {
                paramBuilder = KSPParcellableParamBuilder(remoteElement, this, resolver)
            }
            // Check for @Remoter interface (should use Binder)
            else if ((ksType.declaration as? KSClassDeclaration)?.classKind == com.google.devtools.ksp.symbol.ClassKind.INTERFACE &&
                (ksType.declaration as? KSClassDeclaration)?.getAnnotation("Remoter") != null
            ) {
                paramBuilder = KSPBinderParamBuilder(remoteElement, this, resolver)
            }
            // Check for @Parceler annotation
            else if ((ksType.declaration as? KSClassDeclaration)?.getAnnotation("Parcel") != null) {
                paramBuilder = KSPParcelerParamBuilder(remoteElement, this, resolver)
            }
        }

        // Default to generic builder
        if (paramBuilder == null) {
            paramBuilder = KSPGenericParamBuilder(remoteElement, this, resolver)
        }

        typeBuilderMap[ksType] = paramBuilder

        return paramBuilder
    }

    /**
     * Checks if a KSType is assignable to Parcelable
     */
    private fun isAssignableToParcelable(ksType: KSType): Boolean {
        val qualifiedName = ksType.declaration.qualifiedName?.asString() ?: return false
        if (qualifiedName == "android.os.Parcelable") return true

        // Check if it implements Parcelable by checking superTypes
        (ksType.declaration as? KSClassDeclaration)?.superTypes?.forEach { superType ->
            if (superType.resolve().declaration.qualifiedName?.asString() == "android.os.Parcelable") {
                return true
            }
        }
        return false
    }

    /**
     * Returns the generic type if it's a List of Parceler types
     */
    private fun getGenericListOfParceler(ksType: KSType): KSClassDeclaration? {
        val declaration = ksType.declaration
        if (declaration is KSClassDeclaration) {
            val qualifiedName = declaration.qualifiedName?.asString()
            if (qualifiedName == "kotlin.collections.List" || qualifiedName == "kotlin.collections.MutableList" || qualifiedName == "java.util.List") {
                // Check type arguments for @Parceler annotation
                ksType.arguments.forEach { typeArg ->
                    val argType = typeArg.type?.resolve()
                    if (argType != null && argType.declaration is KSClassDeclaration) {
                        val argDeclaration = argType.declaration as KSClassDeclaration
                        if (argDeclaration.getAnnotation("Parcel") != null) {
                            return argDeclaration
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Returns the fully qualified name of a KS type
     */
    fun getTypeFullName(ksType: KSType): String {
        val declaration = ksType.declaration
        return if (declaration is KSClassDeclaration) {
            declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
        } else {
            ksType.toString()
        }
    }
}

