package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import remoter.annotations.NullableType
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.*

/**
 * Returns whether this element is nullable
 */
internal fun Element.isNullable(builder: KRemoterBuilder? = null): Boolean {
    return (this.getAnnotation(org.jetbrains.annotations.Nullable::class.java) != null)
}

internal fun TypeMirror.isNullable(builder: KRemoterBuilder? = null): Boolean {
    return (this.getAnnotation(org.jetbrains.annotations.Nullable::class.java) != null)
}

internal fun Element.isNullableType(typeIndex: Int, builder: KRemoterBuilder? = null): Boolean {
    return this.getAnnotation(NullableType::class.java)?.nullableIndexes?.contains(typeIndex) == true
}


/**
 * Checks whether this element has any suspend function methods in it
 */
fun Element.hasSuspendFunction(): Boolean {
    var suspendFound = false
    if (this is TypeElement) {
        for (typeMirror in interfaces) {
            if (typeMirror is DeclaredType) {
                if (!suspendFound) {
                    val superElement = typeMirror.asElement()
                    suspendFound = superElement.hasSuspendFunction()
                } else {
                    break
                }
            }
        }
        if (!suspendFound) {
            for (member in this.getEnclosedElements()) {
                if (!suspendFound) {
                    if (member.kind == ElementKind.METHOD) {
                        val executableElement = member as ExecutableElement
                        suspendFound = executableElement.isSuspendFunction()
                    }
                } else {
                    break
                }
            }
        }
    }
    return suspendFound
}


internal fun Element.asKotlinType(builder: KRemoterBuilder? = null, isMutable: Boolean = false) = asType().asKotlinType(builder, this, isMutable).copy(isNullable(builder))

internal fun getNameForClassNameSearch(name: String): String {
    val result = name.split(' ').last()
    return javaToKotlinMap[result] ?: result
}

internal fun ExecutableElement.getReturnAsKotlinType(builder: KRemoterBuilder? = null): TypeName {
    return if (isSuspendFunction()) {
        getReturnTypeOfSuspend().asKotlinType(builder, this).copy(this.isSuspendReturningNullable())
    } else {
        returnType.asKotlinType(builder, this).copy(isNullable())
    }
}

internal fun ExecutableElement.getReturnAsTypeMirror(builder: KRemoterBuilder? = null): TypeMirror {
    return if (isSuspendFunction()) {
        getReturnTypeOfSuspend()
    } else {
        returnType
    }
}

internal fun TypeMirror.asKotlinType(builder: KRemoterBuilder? = null, sourceElement: Element? = null, isMutable: Boolean = false): TypeName {
    val name = asTypeName()
    val isNullable = getAnnotation(org.jetbrains.annotations.Nullable::class.java) != null
    val result = when (kind) {
        TypeKind.ARRAY -> {
            val aKind = this as ArrayType
            val arrayComponentType = aKind.componentType
            when (arrayComponentType.kind) {
                TypeKind.BOOLEAN -> BooleanArray::class.asTypeName()
                TypeKind.BYTE -> ByteArray::class.asTypeName()
                TypeKind.CHAR -> CharArray::class.asTypeName()
                TypeKind.DOUBLE -> DoubleArray::class.asTypeName()
                TypeKind.FLOAT -> FloatArray::class.asTypeName()
                TypeKind.INT -> IntArray::class.asTypeName()
                TypeKind.LONG -> LongArray::class.asTypeName()
                TypeKind.SHORT -> ShortArray::class.asTypeName()
                else -> {
                    val typeIsNullable = sourceElement?.isNullableType(0) == true
                    ClassName.bestGuess("kotlin.Array").parameterizedBy(arrayComponentType.asKotlinType(builder, sourceElement).copy(typeIsNullable))
                }
            }
        }
        TypeKind.WILDCARD -> {
            val wType = this as WildcardType
            if (name == STAR) {
                STAR
            } else {
                val mappedName = javaToKotlinMap[wType.toString().split(' ').last()]
                if (mappedName != null) {
                    ClassName.bestGuess(getNameForClassNameSearch(mappedName))
                } else {
                    val nameString = name.toString()
                    if (nameString.contains(' ') || nameString.contains('<')) {
                        val mainType = ClassName.bestGuess(getNameForClassNameSearch(nameString.split('<').first()))
                        var result: TypeName = mainType
                        if (nameString.contains('<')) {
                            val typeString = nameString.substring(nameString.indexOf('<') + 1, nameString.indexOf('>'))
                            result = mainType.parameterizedBy(typeString.split(',').map { ClassName.bestGuess(getNameForClassNameSearch(it)).copy(true) })
                        }
                        result
                    } else {
                        name
                    }

                }
            }
        }
        TypeKind.BOOLEAN -> Boolean::class.asTypeName()
        TypeKind.BYTE -> Byte::class.asTypeName()
        TypeKind.CHAR -> Char::class.asTypeName()
        TypeKind.DOUBLE -> Double::class.asTypeName()
        TypeKind.FLOAT -> Float::class.asTypeName()
        TypeKind.INT -> Int::class.asTypeName()
        TypeKind.LONG -> Long::class.asTypeName()
        TypeKind.SHORT -> Short::class.asTypeName()
        TypeKind.DECLARED -> {
            val declaredType = this as DeclaredType
            val elementType = declaredType.asElement()
            val mappedName = javaToKotlinMap[elementType.toString()]
            val declaredClassName = if (mappedName != null) {
                ClassName.bestGuess(getNameForClassNameSearch(mappedName))
            } else {
                ClassName.bestGuess(elementType.toString())
            }

            var result: TypeName = declaredClassName

            if (declaredType.typeArguments.isNotEmpty()) {
                var typeIndex = 0

                result = declaredClassName.parameterizedBy(declaredType.typeArguments.map {
                    val typeArgTypeMirror = it.asKotlinType()
                    val typeIsNullable = sourceElement?.isNullableType(typeIndex) == true
                    typeIndex++
                    if (typeArgTypeMirror != STAR) {
                        ClassName.bestGuess(getNameForClassNameSearch(it.asKotlinType().toString())).copy(typeIsNullable || it.isNullable())
                    } else {
                        typeArgTypeMirror
                    }

                })
            }
            result
        }
        else -> {
            name
        }
    }

    return result.copy(isNullable)
}

fun ExecutableElement.isSuspendFunction() = parameters.isNotEmpty()
        && parameters.last().asType().toString().contains("kotlin.coroutines.Continuation")

fun ExecutableElement.isSuspendReturningNullable() = getAnnotation(NullableType::class.java) != null


fun ExecutableElement.getReturnTypeOfSuspend(): TypeMirror {
    val contiuatinDeclaredType = parameters.last().asType() as DeclaredType
    return (contiuatinDeclaredType.typeArguments.first() as WildcardType).superBound
}
