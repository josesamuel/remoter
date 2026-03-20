package remoter.compiler.kspbuilder

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import remoter.annotations.NullableType


internal fun KSAnnotated.isNullable(): Boolean {
    return (this as? KSType)?.isNullable() ?: (this as? KSValueParameter)?.isNullable()
    ?: (this as? KSFunctionDeclaration)?.isNullable() ?: false
}

internal fun KSType.isNullable(): Boolean {
    return isMarkedNullable || nullability != Nullability.NOT_NULL
}


internal fun KSAnnotated.isNullableType(typeIndex: Int): Boolean {
    val annotation = annotations.find { it.shortName.asString() == "NullableType" }
    val indexes =
        annotation?.arguments?.find { it.name?.asString() == "nullableIndexes" }?.value as? IntArray
    return indexes?.contains(typeIndex) == true
}

internal fun KSType.asKotlinType(sourceElement: KSAnnotated? = null): TypeName {
    val declaration = this.declaration
    if (declaration is KSTypeParameter) {
        return TypeVariableName(declaration.simpleName.asString()).copy(isMarkedNullable)
    }
    val typeName = when (declaration.qualifiedName?.asString()) {
        "java.util.List" -> LIST
        "java.util.Map" -> MAP
        "java.util.Set" -> SET
        "kotlin.Boolean" -> BOOLEAN
        "kotlin.Byte" -> BYTE
        "kotlin.Char" -> CHAR
        "kotlin.Double" -> DOUBLE
        "kotlin.Float" -> FLOAT
        "kotlin.Int" -> INT
        "kotlin.Long" -> LONG
        "kotlin.Short" -> SHORT
        "kotlin.String" -> STRING
        "kotlin.CharSequence" -> CHAR_SEQUENCE
        "kotlin.Unit" -> UNIT
        "kotlin.BooleanArray" -> BOOLEAN_ARRAY
        "kotlin.ByteArray" -> BYTE_ARRAY
        "kotlin.CharArray" -> CHAR_ARRAY
        "kotlin.DoubleArray" -> DOUBLE_ARRAY
        "kotlin.FloatArray" -> FLOAT_ARRAY
        "kotlin.IntArray" -> INT_ARRAY
        "kotlin.LongArray" -> LONG_ARRAY
        "kotlin.ShortArray" -> SHORT_ARRAY
        "kotlin.collections.List" -> LIST
        "kotlin.collections.MutableList" -> {
            MUTABLE_LIST
        }

        "kotlin.collections.Map" -> MAP
        "kotlin.collections.MutableMap" -> MUTABLE_MAP
        "kotlin.collections.Set" -> SET
        "kotlin.collections.MutableSet" -> MUTABLE_SET
        "kotlin.Array" -> {
            val componentType = arguments.firstOrNull()?.type?.resolve()
            val componentTypeName = componentType?.asKotlinType(sourceElement) ?: ANY
            ClassName("kotlin", "Array").parameterizedBy(componentTypeName)
        }

        else -> {
            val className =
                declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
            ClassName.bestGuess(className)
        }
    }

    return if (arguments.isNotEmpty() && typeName is ClassName) {
        var typeIndex = 0
        typeName.parameterizedBy(arguments.map { argument ->
            val argType = argument.type?.resolve()
            var argTypeName = argType?.asKotlinType(sourceElement) ?: STAR

            // Handle variance
            argTypeName = when (argument.variance) {
                Variance.COVARIANT -> WildcardTypeName.producerOf(argTypeName)
                Variance.CONTRAVARIANT -> WildcardTypeName.consumerOf(argTypeName)
                else -> argTypeName
            }

            val typeIsNullable = sourceElement?.isNullableType(typeIndex) == true
            typeIndex++
            argTypeName.copy(argType?.isMarkedNullable == true || typeIsNullable)
        })
    } else {
        typeName
    }.copy(isMarkedNullable)
}

internal fun KSFunctionDeclaration.getReturnAsKotlinType(): TypeName {
    return returnType?.resolve()?.asKotlinType(this) ?: UNIT
}

internal fun KSValueParameter.asKotlinType(): TypeName {
    return type.resolve().asKotlinType(this)
}

internal fun KSClassDeclaration.asKotlinType(): TypeName {
    val className = qualifiedName?.asString() ?: simpleName.asString()
    return ClassName.bestGuess(className)
}

internal fun KSType.isArray(): Boolean = declaration.qualifiedName?.asString() == "kotlin.Array"
internal fun KSType.isPrimitiveArray(): Boolean {
    return when (declaration.qualifiedName?.asString()) {
        "kotlin.BooleanArray", "kotlin.ByteArray", "kotlin.CharArray", "kotlin.DoubleArray",
        "kotlin.FloatArray", "kotlin.IntArray", "kotlin.LongArray", "kotlin.ShortArray" -> true

        else -> false
    }
}

internal fun KSFunctionDeclaration.isSuspendFunction(): Boolean {
    return modifiers.contains(Modifier.SUSPEND)
}

internal fun KSFunctionDeclaration.isSuspendReturningNullable(): Boolean {
    return annotations.any { it.shortName.asString() == "NullableType" }
}

internal fun KSFunctionDeclaration.getReturnTypeOfSuspend(resolver: Resolver): KSType {
    return returnType?.resolve() ?: resolver.builtIns.unitType
}

internal fun KSFunctionDeclaration.isUnit(): Boolean {
    return returnType?.resolve()?.declaration?.qualifiedName?.asString() == "kotlin.Unit"
}

internal fun KSType.isUnit(): Boolean {
    return declaration.qualifiedName?.asString() == "kotlin.Unit"
}

internal fun KSValueParameter.isNullable(): Boolean {
    val resolvedType = type.resolve()
    return resolvedType.isMarkedNullable || resolvedType.nullability != Nullability.NOT_NULL

}

internal fun KSFunctionDeclaration.isNullable(): Boolean {
    val resolvedType = returnType?.resolve()
    return resolvedType != null && (resolvedType.isMarkedNullable || resolvedType.nullability != Nullability.NOT_NULL)
}

internal fun KSClassDeclaration.getAnnotation(annotationName: String): KSAnnotation? {
    return annotations.find {
        it.shortName.asString() == annotationName.substringAfterLast('.')
                || it.shortName.asString() == annotationName
    }
}

internal fun KSValueParameter.getAnnotation(annotationName: String): KSAnnotation? {
    return annotations.find {
        it.shortName.asString() == annotationName.substringAfterLast('.')
                || it.shortName.asString() == annotationName
    }
}

internal fun KSFunctionDeclaration.getAnnotation(annotationName: String): KSAnnotation? {
    return annotations.find {
        it.shortName.asString() == annotationName.substringAfterLast('.')
                || it.shortName.asString() == annotationName
    }
}

internal val KSFunctionDeclaration.isVarArgs: Boolean
    get() = parameters.isNotEmpty() && parameters.last().isVararg

internal val KSClassDeclaration.isVarArgs: Boolean
    get() = false


