package remoter.compiler.kspbuilder

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.javapoet.ArrayTypeName
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import remoter.compiler.kbuilder.ParamBuilder
import remoter.compiler.kbuilder.isNullable
import javax.lang.model.type.TypeKind

/**
 * A [KSPRemoterBuilder] for a specific type of a parameter
 */
internal abstract class KSPParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPRemoterBuilder(remoterInterfaceElement, bindingManager, resolver) {

    companion object {
        internal val DATA = "___remoter_data"
        internal val REPLY = "___remoter_reply"
        internal val RESULT = "___remoter_result"
    }

    enum class ParamType {
        IN, OUT, IN_OUT
    }

    /**
     * Called to generate code to write the params to proxy
     */
    open fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
    }

    /**
     * Called to generate code that reads results from proxy
     */
    open fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
    }

    /**
     * Called to generate code that reads results from proxy for @[remoter.annotations.ParamOut] parameters
     */
    open fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
    }

    /**
     * Called to generate code to write params for stub
     */
    open fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        var suffix = ""
        val typeName = if (param.isVararg) {
            ClassName("kotlin", "Array").parameterizedBy(param.asKotlinType()).copy(nullable = true)
        } else {
            val type = param.asKotlinType()
            if (param.isNullable()) {
                suffix = "?"
            }
            type
        }

        methodBuilder.addStatement("var $paramName: %T$suffix", typeName)
    }

    /**
     * Called to generate code to write @[remoter.annotations.ParamOut] params for stub
     */
    open fun writeOutParamsToStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("val ${paramName}_length = $DATA.readInt()")
            methodBuilder.beginControlFlow("if (${paramName}_length < 0 )")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.addStatement("$paramName = %T(0)", param.asKotlinType())
            }
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.addStatement(
                "$paramName = %T(${paramName}_length)",
                param.asKotlinType().copy(nullable = false)
            )
            methodBuilder.endControlFlow()
        }
    }

    /**
     * Called to generate code that reads results from stub
     */
    open fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray()) {
            methodBuilder.addStatement("$REPLY.writeStringArray($RESULT)")
        } else {
            methodBuilder.addStatement("$REPLY.writeString($RESULT)")
        }
    }

    /**
     * Called to generate code that reads  [remoter.annotations.ParamOut] results from stub
     */
    open fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
    }

    /**
     * Called to generate code that writes the out params for array type
     */
    protected open fun writeArrayOutParamsToProxy(
        param: KSValueParameter,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.isNullable()) {
            methodBuilder.beginControlFlow("if ($paramName == null)")
            methodBuilder.addStatement("$DATA.writeInt(-1)")
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.addStatement("$DATA.writeInt($paramName.size)")
            methodBuilder.endControlFlow()
        } else {
            methodBuilder.addStatement("$DATA.writeInt($paramName!!.size)")
        }
    }
}

internal class KSPIntParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeIntArray($paramName)")
            }
        } else {
            methodBuilder.addStatement("$DATA.writeInt($paramName)")
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray() || resultType.isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeIntArray($RESULT)")
        } else {
            methodBuilder.addStatement("$REPLY.writeInt($RESULT)")
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.returnType?.resolve()
        if (resultType?.isArray() == true || resultType?.isPrimitiveArray() == true) {
            val suffix = if (methodType.isNullable()) "" else "!!"
            methodBuilder.addStatement("$RESULT = $REPLY.createIntArray()$suffix")
        } else {
            methodBuilder.addStatement("$RESULT = $REPLY.readInt()")
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeIntArray($paramName)")
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, paramName, methodBuilder)
            } else {
                val suffix = if (param.isNullable()) "" else "!!"
                methodBuilder.addStatement("$paramName = $DATA.createIntArray()$suffix")
            }
        } else {
            methodBuilder.addStatement("$paramName = $DATA.readInt()")
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if ((param.type.resolve().isArray() || param.type.resolve()
                .isPrimitiveArray()) && paramType != ParamType.IN
        ) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
            }
            methodBuilder.addStatement("$REPLY.readIntArray($paramName)")
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
            }
        }
    }
}

internal class KSPBooleanParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeBooleanArray($paramName)")
            }
        } else {
            methodBuilder.addStatement("$DATA.writeInt(if($paramName) 1 else 0)")
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.returnType?.resolve()
        if (resultType?.isArray() == true || resultType?.isPrimitiveArray() == true) {
            val suffix = if (methodType.isNullable()) "" else "!!"
            methodBuilder.addStatement("$RESULT = $REPLY.createBooleanArray()$suffix")
        } else {
            methodBuilder.addStatement("$RESULT = $REPLY.readInt() == 1")
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray() || resultType.isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeBooleanArray($RESULT)")
        } else {
            methodBuilder.addStatement("$REPLY.writeInt(if($RESULT) 1 else 0)")
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeBooleanArray($paramName)")
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, paramName, methodBuilder)
            } else {
                val suffix = if (param.isNullable()) "" else "!!"
                methodBuilder.addStatement("$paramName = $DATA.createBooleanArray()$suffix")
            }
        } else {
            methodBuilder.addStatement("$paramName = 0 != $DATA.readInt()")
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if ((param.type.resolve().isArray() || param.type.resolve()
                .isPrimitiveArray()) && paramType != ParamType.IN
        ) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
            }
            methodBuilder.addStatement("$REPLY.readBooleanArray($paramName)")
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
            }
        }
    }
}

internal class KSPByteParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeByteArray($paramName)")
            }
        } else {
            methodBuilder.addStatement("$DATA.writeByte($paramName)")
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray() || resultType.isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeByteArray($RESULT)")
        } else {
            methodBuilder.addStatement("$REPLY.writeByte($RESULT)")
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.returnType?.resolve()
        if (resultType?.isArray() == true || resultType?.isPrimitiveArray() == true) {
            val suffix = if (methodType.isNullable()) "" else "!!"
            methodBuilder.addStatement("$RESULT = $REPLY.createByteArray()$suffix")
        } else {
            methodBuilder.addStatement("$RESULT = $REPLY.readByte()")
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if ((param.type.resolve().isArray() || param.type.resolve()
                .isPrimitiveArray()) && paramType != ParamType.IN
        ) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
            }
            methodBuilder.addStatement("$REPLY.readByteArray($paramName)")
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
            }
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeByteArray($paramName)")
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, paramName, methodBuilder)
            } else {
                val suffix = if (param.isNullable()) "" else "!!"
                methodBuilder.addStatement("$paramName = $DATA.createByteArray()$suffix")
            }
        } else {
            methodBuilder.addStatement("$paramName = $DATA.readByte()")
        }
    }
}

internal class KSPCharParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeCharArray($paramName)")
            }
        } else {
            methodBuilder.addStatement("$DATA.writeInt($paramName.toInt())")
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray() || resultType.isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeCharArray($RESULT)")
        } else {
            methodBuilder.addStatement("$REPLY.writeInt($RESULT.toInt())")
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.returnType?.resolve()
        if (resultType?.isArray() == true || resultType?.isPrimitiveArray() == true) {
            val suffix = if (methodType.isNullable()) "" else "!!"
            methodBuilder.addStatement("$RESULT = $REPLY.createCharArray()$suffix")
        } else {
            methodBuilder.addStatement("$RESULT = $REPLY.readInt().toChar()")
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeCharArray($paramName)")
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, paramName, methodBuilder)
            } else {
                val suffix = if (param.isNullable()) "" else "!!"
                methodBuilder.addStatement("$paramName = $DATA.createCharArray()$suffix")
            }
        } else {
            methodBuilder.addStatement("$paramName = $DATA.readInt().toChar()")
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if ((param.type.resolve().isArray() || param.type.resolve()
                .isPrimitiveArray()) && paramType != ParamType.IN
        ) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
            }
            methodBuilder.addStatement("$REPLY.readCharArray($paramName)")
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
            }
        }
    }
}

internal class KSPDoubleParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeDoubleArray($paramName)")
            }
        } else {
            methodBuilder.addStatement("$DATA.writeDouble($paramName)")
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray() || resultType.isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeDoubleArray($RESULT)")
        } else {
            methodBuilder.addStatement("$REPLY.writeDouble($RESULT)")
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.returnType?.resolve()
        if (resultType?.isArray() == true || resultType?.isPrimitiveArray() == true) {
            val suffix = if (methodType.isNullable()) "" else "!!"
            methodBuilder.addStatement("$RESULT = $REPLY.createDoubleArray()$suffix")
        } else {
            methodBuilder.addStatement("$RESULT = $REPLY.readDouble()")
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeDoubleArray($paramName)")
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, paramName, methodBuilder)
            } else {
                val suffix = if (param.isNullable()) "" else "!!"
                methodBuilder.addStatement("$paramName = $DATA.createDoubleArray()$suffix")
            }
        } else {
            methodBuilder.addStatement("$paramName = $DATA.readDouble()")
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if ((param.type.resolve().isArray() || param.type.resolve()
                .isPrimitiveArray()) && paramType != ParamType.IN
        ) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
            }
            methodBuilder.addStatement("$REPLY.readDoubleArray($paramName)")
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
            }
        }
    }
}

internal class KSPFloatParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeFloatArray($paramName)")
            }
        } else {
            methodBuilder.addStatement("$DATA.writeFloat($paramName)")
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray() || resultType.isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeFloatArray($RESULT)")
        } else {
            methodBuilder.addStatement("$REPLY.writeFloat($RESULT)")
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.returnType?.resolve()
        if (resultType?.isArray() == true || resultType?.isPrimitiveArray() == true) {
            val suffix = if (methodType.isNullable()) "" else "!!"
            methodBuilder.addStatement("$RESULT = $REPLY.createFloatArray()$suffix")
        } else {
            methodBuilder.addStatement("$RESULT = $REPLY.readFloat()")
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeFloatArray($paramName)")
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, paramName, methodBuilder)
            } else {
                val suffix = if (param.isNullable()) "" else "!!"
                methodBuilder.addStatement("$paramName = $DATA.createFloatArray()$suffix")
            }
        } else {
            methodBuilder.addStatement("$paramName = $DATA.readFloat()")
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if ((param.type.resolve().isArray() || param.type.resolve()
                .isPrimitiveArray()) && paramType != ParamType.IN
        ) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
            }
            methodBuilder.addStatement("$REPLY.readFloatArray($paramName)")
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
            }
        }
    }
}

internal class KSPLongParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeLongArray($paramName)")
            }
        } else {
            methodBuilder.addStatement("$DATA.writeLong($paramName)")
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray() || resultType.isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeLongArray($RESULT)")
        } else {
            methodBuilder.addStatement("$REPLY.writeLong($RESULT)")
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.returnType?.resolve()
        if (resultType?.isArray() == true || resultType?.isPrimitiveArray() == true) {
            val suffix = if (methodType.isNullable()) "" else "!!"
            methodBuilder.addStatement("$RESULT = $REPLY.createLongArray()$suffix")
        } else {
            methodBuilder.addStatement("$RESULT = $REPLY.readLong()")
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            methodBuilder.addStatement("$REPLY.writeLongArray($paramName)")
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, paramName, methodBuilder)
            } else {
                val suffix = if (param.isNullable()) "" else "!!"
                methodBuilder.addStatement("$paramName = $DATA.createLongArray()$suffix")
            }
        } else {
            methodBuilder.addStatement("$paramName = $DATA.readLong()")
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if ((param.type.resolve().isArray() || param.type.resolve()
                .isPrimitiveArray()) && paramType != ParamType.IN
        ) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
            }
            methodBuilder.addStatement("$REPLY.readLongArray($paramName)")
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
            }
        }
    }
}

internal class KSPShortParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            logError("short[] not supported, use int[] $paramName")
        } else {
            methodBuilder.addStatement("$DATA.writeInt($paramName.toInt())")
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray() || resultType.isPrimitiveArray()) {
            logError("short[] not supported, use int[]")
        } else {
            methodBuilder.addStatement("$REPLY.writeInt($RESULT.toInt())")
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.returnType?.resolve()
        if (resultType?.isArray() == true || resultType?.isPrimitiveArray() == true) {
            logError("short[] is not supported, use int[]")
            if (methodType.isNullable()) {
                methodBuilder.addStatement("$RESULT = null")
            } else {
                methodBuilder.addStatement("$RESULT = ShortArray(0)")
            }
        } else {
            methodBuilder.addStatement("$RESULT = $REPLY.readInt().toShort()")
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray() || param.type.resolve().isPrimitiveArray()) {
            logError("short[] not supported, use int[] $paramName")
            methodBuilder.addStatement("$paramName = ShortArray(0)")
        } else {
            methodBuilder.addStatement("$paramName = $DATA.readInt().toShort()")
        }
    }
}

internal class KSPStringParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray() || param.isVararg) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeStringArray($paramName)")
            }
        } else {
            methodBuilder.addStatement("$DATA.writeString($paramName)")
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray()) {
            methodBuilder.addStatement("$REPLY.writeStringArray($RESULT)")
        } else {
            methodBuilder.addStatement("$REPLY.writeString($RESULT)")
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.returnType?.resolve()
        if (resultType?.isArray() == true) {
            methodBuilder.addStatement("$RESULT = $REPLY.createStringArray()")
        } else {
            if (methodType.isNullable()) {
                methodBuilder.addStatement("$RESULT = $REPLY.readString()")
            } else {
                methodBuilder.addStatement("$RESULT = $REPLY.readString()!!")
            }
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (param.type.resolve().isArray()) {
            methodBuilder.addStatement("$REPLY.writeStringArray($paramName)")
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray() || param.isVararg) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, paramName, methodBuilder)
            } else {
                if (param.isNullable()) {
                    methodBuilder.addStatement("$paramName = $DATA.createStringArray()")
                } else {
                    methodBuilder.addStatement("$paramName = $DATA.createStringArray()!!")
                }
            }
        } else {
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = $DATA.readString()")
            } else {
                methodBuilder.addStatement("$paramName = $DATA.readString()!!")
            }
        }
    }

    override fun writeOutParamsToStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("val ${paramName}_length = $DATA.readInt()")
            methodBuilder.beginControlFlow("if (${paramName}_length < 0 )")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.addStatement("$paramName = %T(0) { \"\" }", param.asKotlinType())
            }
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.addStatement(
                "$paramName = %T(${paramName}_length) { \"\" }",
                param.asKotlinType().copy(nullable = false)
            )
            methodBuilder.endControlFlow()
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray() && paramType != ParamType.IN) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
            }
            methodBuilder.addStatement("$REPLY.readStringArray($paramName)")
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
            }
        }
    }
}

internal class KSPCharSequenceParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray()) {
            logError("CharSequence[] cannot be marshalled")
        } else {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
                methodBuilder.addStatement("$DATA.writeInt(1)")
                methodBuilder.addStatement("android.text.TextUtils.writeToParcel($paramName, $DATA, 0)")
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$DATA.writeInt(0)")
                methodBuilder.endControlFlow()
            } else {
                methodBuilder.addStatement("$DATA.writeInt(1)")
                methodBuilder.addStatement("android.text.TextUtils.writeToParcel($paramName, $DATA, 0)")
            }
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray()) {
            logError("CharSequence[] cannot be marshalled")
        } else {
            methodBuilder.beginControlFlow("if ($RESULT != null)")
            methodBuilder.addStatement("$REPLY.writeInt(1)")
            methodBuilder.addStatement("android.text.TextUtils.writeToParcel($RESULT, $REPLY, 0)")
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.addStatement("$REPLY.writeInt(0)")
            methodBuilder.endControlFlow()
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.getReturnAsKotlinType()
        if (methodType.returnType?.resolve()?.isArray() == true) {
            logError("CharSequence[] cannot be marshalled")
        } else {
            methodBuilder.beginControlFlow("if ($REPLY.readInt() != 0)")
            methodBuilder.addStatement("$RESULT = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel($REPLY)")
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (methodType.isNullable()) {
                methodBuilder.addStatement("$RESULT = null")
            } else {
                methodBuilder.addStatement(
                    "throw %T(\"Unexpected null result\")",
                    NullPointerException::class
                )
            }
            methodBuilder.endControlFlow()
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray()) {
            logError("CharSequence[] cannot be marshalled")
        } else {
            methodBuilder.beginControlFlow("if ($DATA.readInt() != 0)")
            methodBuilder.addStatement("$paramName = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel($DATA)")
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.addStatement(
                    "throw %T(\"Not expecting null\")",
                    NullPointerException::class.java
                )
            }
            methodBuilder.endControlFlow()
        }
    }
}

internal class KSPListParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray()) {
            logError("List[] is not supported")
        } else {
            if (paramType != ParamType.OUT) {
                if (isListOfStrings(param.type.resolve())) {
                    methodBuilder.addStatement("$DATA.writeStringList($paramName)")
                } else {
                    methodBuilder.addStatement("$DATA.writeList($paramName)")
                }
            }
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray()) {
            logError("List[] is not supported")
        } else {
            if (isListOfStrings(resultType)) {
                methodBuilder.addStatement("$REPLY.writeStringList($RESULT)")
            } else {
                methodBuilder.addStatement("$REPLY.writeList($RESULT)")
            }
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (isListOfStrings(param.type.resolve())) {
            methodBuilder.addStatement("$REPLY.writeStringList($paramName)")
        } else {
            methodBuilder.addStatement("$REPLY.writeList($paramName)")
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.getReturnAsKotlinType()
        val resultKSType = methodType.returnType?.resolve()
        if (resultKSType?.isArray() == true) {
            logError("List[] is not supported")
        } else {
            if (isListOfStrings(resultKSType!!)) {
                methodBuilder.addStatement(
                    "$RESULT = $REPLY.createStringArrayList() as %T",
                    resultType
                )
            } else {
                methodBuilder.addStatement(
                    "$RESULT = $REPLY.readArrayList(javaClass.classLoader) as %T",
                    resultType
                )
            }
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray()) {
            logError("List[] is not supported")
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement("$paramName = mutableListOf()")
            } else {
                val suffix = if (param.isNullable()) "" else "!!"
                val typeSuffix = if (param.isNullable()) "?" else ""
                if (isListOfStrings(param.type.resolve())) {
                    methodBuilder.addStatement("$paramName = $DATA.createStringArrayList()$suffix")
                } else {
                    methodBuilder.addStatement(
                        "$paramName = $DATA.readArrayList(javaClass.classLoader)$suffix as MutableList<%T>$typeSuffix",
                        param.type.resolve().arguments.first().type!!.resolve().asKotlinType()
                    )
                }
            }
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (paramType != ParamType.IN) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
            }
            if (isListOfStrings(param.type.resolve())) {
                methodBuilder.addStatement("$REPLY.readStringList($paramName)")
            } else {
                methodBuilder.addStatement("$REPLY.readList($paramName, javaClass.classLoader)")
            }
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
            }
        }
    }

    private fun isListOfStrings(ksType: KSType): Boolean {
        return (ksType.declaration.qualifiedName?.asString() == "kotlin.collections.List" || ksType.declaration.qualifiedName?.asString() == "kotlin.collections.MutableList") &&
                ksType.arguments.firstOrNull()?.type?.resolve()?.declaration?.qualifiedName?.asString() == "kotlin.String"
    }
}

internal class KSPParcelerParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray()) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                if (param.isNullable()) {
                    methodBuilder.beginControlFlow("if ($paramName != null)")
                    methodBuilder.addStatement("$DATA.writeInt($paramName.size)")
                    methodBuilder.beginControlFlow("for (___remoter_p_item in $paramName)")
                    methodBuilder.addStatement("val pClass$paramName = getParcelerClass(___remoter_p_item)")
                    methodBuilder.beginControlFlow("if (pClass$paramName != null)")
                    methodBuilder.addStatement("$DATA.writeString(pClass$paramName.name)")
                    methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass$paramName, ___remoter_p_item).writeToParcel($DATA, 0)")
                    methodBuilder.endControlFlow()
                    methodBuilder.endControlFlow()
                    methodBuilder.endControlFlow()
                    methodBuilder.beginControlFlow("else")
                    methodBuilder.addStatement("$DATA.writeInt(-1)")
                    methodBuilder.endControlFlow()
                } else {
                    methodBuilder.addStatement("$DATA.writeInt($paramName!!.size)")
                    methodBuilder.beginControlFlow("for (___remoter_p_item in $paramName!!)")
                    methodBuilder.addStatement("val pClass$paramName = getParcelerClass(___remoter_p_item)")
                    methodBuilder.beginControlFlow("if (pClass$paramName != null)")
                    methodBuilder.addStatement("$DATA.writeString(pClass$paramName.name)")
                    methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass$paramName, ___remoter_p_item).writeToParcel($DATA, 0)")
                    methodBuilder.endControlFlow()
                    methodBuilder.endControlFlow()
                }
            }
        } else {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
                methodBuilder.addStatement("val pClass$paramName = getParcelerClass($paramName)")
                methodBuilder.beginControlFlow("if (pClass$paramName != null)")
                methodBuilder.addStatement("$DATA.writeInt(1)")
                methodBuilder.addStatement("$DATA.writeString(pClass$paramName.name)")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass$paramName, $paramName).writeToParcel($DATA, 0)")
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$DATA.writeInt(0)")
                methodBuilder.endControlFlow()
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$DATA.writeInt(0)")
                methodBuilder.endControlFlow()
            } else {
                methodBuilder.addStatement("val pClass$paramName = getParcelerClass($paramName!!)")
                methodBuilder.beginControlFlow("if (pClass$paramName != null)")
                methodBuilder.addStatement("$DATA.writeInt(1)")
                methodBuilder.addStatement("$DATA.writeString(pClass$paramName.name)")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass$paramName, $paramName).writeToParcel($DATA, 0)")
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$DATA.writeInt(0)")
                methodBuilder.endControlFlow()
            }
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray()) {
            if (methodElement.isNullable()) {
                methodBuilder.beginControlFlow("if ($RESULT != null)")
                methodBuilder.addStatement("$REPLY.writeInt($RESULT.size)")
                methodBuilder.beginControlFlow("for(item in $RESULT)")
                methodBuilder.addStatement("val pClass = getParcelerClass(item)")
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.name)")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
                methodBuilder.endControlFlow()
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$REPLY.writeInt(-1)")
                methodBuilder.endControlFlow()
            } else {
                methodBuilder.addStatement("$REPLY.writeInt($RESULT.size)")
                methodBuilder.beginControlFlow("for(item in $RESULT)")
                methodBuilder.addStatement("val pClass = getParcelerClass(item)")
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.name)")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
                methodBuilder.endControlFlow()
            }
        } else {
            if (methodElement.isNullable()) {
                methodBuilder.beginControlFlow("if ($RESULT != null)")
                methodBuilder.addStatement("$REPLY.writeInt(1)")
                methodBuilder.addStatement("val pClass = getParcelerClass($RESULT)")
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.name)")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, $RESULT).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$REPLY.writeInt(0)")
                methodBuilder.endControlFlow()
            } else {
                methodBuilder.addStatement("$REPLY.writeInt(1)")
                methodBuilder.addStatement("val pClass = getParcelerClass($RESULT)")
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.name)")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, $RESULT).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
            }
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (param.type.resolve().isArray()) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
                methodBuilder.addStatement("$REPLY.writeInt($paramName!!.size)")
                methodBuilder.beginControlFlow("for(item in $paramName!!)")
                methodBuilder.addStatement("val pClass = getParcelerClass(item)")
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.name)")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
                methodBuilder.endControlFlow()
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$REPLY.writeInt(-1)")
                methodBuilder.endControlFlow()
            } else {
                methodBuilder.addStatement("$REPLY.writeInt($paramName.size)")
                methodBuilder.beginControlFlow("for(item in $paramName)")
                methodBuilder.addStatement("val pClass = getParcelerClass(item)")
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.name)")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
                methodBuilder.endControlFlow()
            }
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.getReturnAsKotlinType()
        val resultKSType = methodType.returnType?.resolve()
        if (resultKSType?.isArray() == true) {
            methodBuilder.addStatement("val __size_result = $REPLY.readInt()")
            methodBuilder.beginControlFlow("if (__size_result >= 0)")
            methodBuilder.beginControlFlow(
                "$RESULT = %T(__size_result)",
                resultType.copy(nullable = false)
            )
            val argType = resultKSType.arguments.first().type?.resolve()?.asKotlinType()
            methodBuilder.addStatement(
                "getParcelerObject($REPLY.readString(), $REPLY) as %T",
                argType ?: ANY
            )
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (methodType.isNullable()) {
                methodBuilder.addStatement("$RESULT = null")
            } else {
                methodBuilder.addStatement(
                    "throw %T(\"Unexpected null result\")",
                    NullPointerException::class
                )
            }
            methodBuilder.endControlFlow()
        } else {
            methodBuilder.beginControlFlow("if ($REPLY.readInt() != 0)")
            methodBuilder.addStatement(
                "$RESULT = getParcelerObject($REPLY.readString(), $REPLY) as %T",
                resultType.copy(nullable = false)
            )
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (methodType.isNullable()) {
                methodBuilder.addStatement("$RESULT = null")
            } else {
                methodBuilder.addStatement(
                    "throw %T(\"Unexpected null result\")",
                    NullPointerException::class
                )
            }
            methodBuilder.endControlFlow()
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        val paramKSType = param.type.resolve()
        if (paramKSType.isArray()) {
            if (paramType == ParamType.OUT) {
                _writeOutParamsToStub(param, paramType, paramName, methodBuilder)
            } else {
                methodBuilder.addStatement("val size_$paramName = $DATA.readInt()")
                methodBuilder.beginControlFlow("if (size_$paramName >= 0)")
                methodBuilder.beginControlFlow(
                    "$paramName = %T(size_$paramName)",
                    param.asKotlinType().copy(nullable = false)
                )
                val argType = paramKSType.arguments.first().type?.resolve()?.asKotlinType()
                methodBuilder.addStatement(
                    "getParcelerObject($DATA.readString(), $DATA) as %T",
                    argType ?: ANY
                )
                methodBuilder.endControlFlow()
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                if (param.isNullable()) {
                    methodBuilder.addStatement("$paramName = null")
                } else {
                    methodBuilder.addStatement(
                        "throw %T(\"Unexpected null param\")",
                        NullPointerException::class
                    )
                }
                methodBuilder.endControlFlow()
            }
        } else {
            methodBuilder.beginControlFlow("if ($DATA.readInt() != 0)")
            methodBuilder.addStatement(
                "$paramName = getParcelerObject($DATA.readString(), $DATA) as %T",
                param.asKotlinType().copy(nullable = false)
            )
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.addStatement(
                    "throw %T(\"Unexpected null result\")",
                    NullPointerException::class
                )
            }
            methodBuilder.endControlFlow()
        }
    }

    private fun _writeOutParamsToStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("val ${paramName}_length = $DATA.readInt()")
            methodBuilder.beginControlFlow("if (${paramName}_length < 0 )")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.beginControlFlow("$paramName = %T(0)", param.asKotlinType())
                val argType = param.type.resolve().arguments.first().type?.resolve()?.asKotlinType()
                methodBuilder.addStatement("%T()", argType ?: ANY)
                methodBuilder.endControlFlow()
            }
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.beginControlFlow(
                "$paramName = %T(${paramName}_length)",
                param.asKotlinType().copy(nullable = false)
            )
            val argType = param.type.resolve().arguments.first().type?.resolve()?.asKotlinType()
                ?.copy(nullable = false)
            methodBuilder.addStatement("%T()", argType ?: ANY)
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (paramType != ParamType.IN) {
            if (param.type.resolve().isArray()) {
                methodBuilder.addStatement("val _size_$paramName = $REPLY.readInt()")
                methodBuilder.beginControlFlow("for(i in 0 until _size_$paramName)")
                val access = if (param.isNullable()) "$paramName?." else "$paramName."
                val argType = param.type.resolve().arguments.first().type?.resolve()?.asKotlinType()
                methodBuilder.addStatement(
                    "${access}set(i, getParcelerObject($REPLY.readString(), $REPLY) as %T)",
                    argType ?: ANY
                )
                methodBuilder.endControlFlow()
            }
        }
    }
}

internal class KSPMapParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray()) {
            logError("Map[] is not supported")
        } else {
            if (paramType != ParamType.OUT) {
                methodBuilder.addStatement("$DATA.writeMap(" + paramName + " as %T<*, *>?)", MAP)
            }
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray()) {
            logError("Map[] is not supported")
        } else {
            methodBuilder.addStatement("$REPLY.writeMap($RESULT as %T<*, *>?)", MAP)
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        methodBuilder.addStatement("$REPLY.writeMap($paramName as %T<*, *>?)", MAP)
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.getReturnAsKotlinType()
        val resultKSType = methodType.returnType?.resolve()
        if (resultKSType?.isArray() == true) {
            logError("Map[] is not supported")
        } else {
            methodBuilder.addStatement(
                "$RESULT = $REPLY.readHashMap(javaClass.classLoader) as %T",
                resultType
            )
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray()) {
            logError("Map[] is not supported")
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement("$paramName = mutableMapOf()")
            } else {
                methodBuilder.addStatement(
                    "$paramName = $DATA.readHashMap(javaClass.classLoader) as %T",
                    param.asKotlinType()
                )
            }
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (paramType != ParamType.IN) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
            }
            methodBuilder.addStatement("$paramName.clear()")
            methodBuilder.addStatement(
                "$REPLY.readMap(" + paramName + " as %T<*, *>, javaClass.classLoader)",
                MAP
            )
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
            }
        }
    }
}

internal class KSPParcellableParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray()) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeTypedArray(" + paramName + ", 0)")
            }
        } else {
            if (paramType != ParamType.OUT) {
                if (param.isNullable()) {
                    methodBuilder.beginControlFlow("if (" + paramName + " != null)")
                    methodBuilder.addStatement("$DATA.writeInt(1)")
                    methodBuilder.addStatement(paramName + ".writeToParcel($DATA, 0)")
                    methodBuilder.endControlFlow()
                    methodBuilder.beginControlFlow("else")
                    methodBuilder.addStatement("$DATA.writeInt(0)")
                    methodBuilder.endControlFlow()
                } else {
                    methodBuilder.addStatement("$DATA.writeInt(1)")
                    methodBuilder.addStatement(paramName + ".writeToParcel($DATA, 0)")
                }
            }
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray()) {
            methodBuilder.addStatement("$REPLY.writeTypedArray($RESULT, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
        } else {
            if (resultType.isNullable()) {
                methodBuilder.beginControlFlow("if ($RESULT != null)")
                methodBuilder.addStatement("$REPLY.writeInt(1)")
                methodBuilder.addStatement("$RESULT.writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$REPLY.writeInt(0)")
                methodBuilder.endControlFlow()
            } else {
                methodBuilder.addStatement("$REPLY.writeInt(1)")
                methodBuilder.addStatement("$RESULT.writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
            }
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (param.type.resolve().isArray()) {
            methodBuilder.addStatement("$REPLY.writeTypedArray($paramName, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
        } else {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
                methodBuilder.addStatement("$REPLY.writeInt(1)")
                methodBuilder.addStatement("$paramName!!.writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$REPLY.writeInt(0)")
                methodBuilder.endControlFlow()
            } else {
                methodBuilder.addStatement("$REPLY.writeInt(1)")
                methodBuilder.addStatement("$paramName.writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
            }
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.getReturnAsKotlinType()
        val resultKSType = methodType.returnType?.resolve()
        if (resultKSType?.isArray() == true) {
            methodBuilder.addStatement(
                "$RESULT = $REPLY.createTypedArray(" + getParcelableClassName(
                    resultKSType
                ) + ".CREATOR) as %T", resultType
            )
        } else {
            methodBuilder.beginControlFlow("if ($REPLY.readInt() != 0)")
            methodBuilder.addStatement(
                "$RESULT = (" + getParcelableClassName(resultKSType!!) + ".CREATOR.createFromParcel($REPLY) as %T)",
                resultType
            )
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (methodType.isNullable()) {
                methodBuilder.addStatement("$RESULT = null")
            } else {
                methodBuilder.addStatement(
                    "throw %T(\"Unexpected null result\")",
                    NullPointerException::class
                )
            }
            methodBuilder.endControlFlow()
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray()) {
            if (paramType == ParamType.OUT) {
                _writeOutParamsToStub(param, paramType, paramName, methodBuilder)
            } else {
                methodBuilder.addStatement(
                    paramName + " =  $DATA.createTypedArray(" + getParcelableClassName(
                        param.type.resolve()
                    ) + ".CREATOR) as " + param.asKotlinType()
                )
            }
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement(paramName + " = " + getParcelableClassName(param.type.resolve()) + "()")
            } else {
                methodBuilder.beginControlFlow("if ( $DATA.readInt() != 0)")
                methodBuilder.addStatement(paramName + " = " + getParcelableClassName(param.type.resolve()) + ".CREATOR.createFromParcel($DATA) as " + param.asKotlinType())
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                if (param.isNullable()) {
                    methodBuilder.addStatement("$paramName = null")
                } else {
                    methodBuilder.addStatement(
                        "throw %T(\"Unexpected null result\")",
                        NullPointerException::class
                    )
                }
                methodBuilder.endControlFlow()
            }
        }
    }

    private fun _writeOutParamsToStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("val " + paramName + "_length = $DATA.readInt()")
            methodBuilder.beginControlFlow("if (" + paramName + "_length < 0 )")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.beginControlFlow(
                    paramName + " = " + param.asKotlinType()
                            + "(0)"
                )
                methodBuilder.addStatement(
                    "%T()",
                    param.type.resolve().arguments.first().type?.resolve()?.asKotlinType() ?: ANY
                )
                methodBuilder.endControlFlow()
            }
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.beginControlFlow(
                paramName + " = " + param.asKotlinType().copy(false)
                        + "(" + paramName + "_length)"
            )
            methodBuilder.addStatement(
                "%T()",
                param.type.resolve().arguments.first().type?.resolve()?.asKotlinType()?.copy(false)
                    ?: ANY
            )
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (paramType != ParamType.IN) {
            if (param.type.resolve().isArray()) {
                if (param.isNullable()) {
                    methodBuilder.beginControlFlow("if ($paramName != null)")
                }
                methodBuilder.addStatement(
                    "$REPLY.readTypedArray($paramName, " + getParcelableClassName(
                        param.type.resolve()
                    ) + ".CREATOR) "
                )
                if (param.isNullable()) {
                    methodBuilder.endControlFlow()
                }
            } else {
                methodBuilder.beginControlFlow("if ($REPLY.readInt() != 0)")
                if (param.isNullable()) {
                    methodBuilder.addStatement(paramName + "?.readFromParcel($REPLY)")
                } else {
                    methodBuilder.addStatement(paramName + ".readFromParcel($REPLY)")
                }
                methodBuilder.endControlFlow()
            }
        }
    }

    private fun getParcelableClassName(ksType: KSType): String {
        if (!ksType.isArray()) {
            val pClassName = ksType.declaration.qualifiedName?.asString() ?: ""
            return pClassName
        } else {
            return getParcelableClassName(ksType.arguments.first().type?.resolve()!!)
        }
    }
}

internal class KSPBinderParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray()) {
            val argType = param.type.resolve().arguments.first().type?.resolve()
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if(" + paramName + " == null)")
                    .addStatement("$DATA.writeInt(-1)")
                    .nextControlFlow("else")
                    .addStatement("$DATA.writeInt(" + paramName + ".size)")
                    .beginControlFlow("for(__item in " + paramName + ")")
                    .addStatement(
                        "$DATA.writeStrongBinder(if(__item != null)  %T(__item)  else null)",
                        getStubClassName(argType)
                    )
                    .endControlFlow()
                    .endControlFlow()
            } else {
                methodBuilder.addStatement("$DATA.writeInt(" + paramName + ".size)")
                    .beginControlFlow("for(__item in " + paramName + ")")
                    .addStatement(
                        "$DATA.writeStrongBinder(if(__item != null)  %T(__item)  else null)",
                        getStubClassName(argType)
                    )
                    .endControlFlow()
            }
        } else {
            val binderName = paramName + "_binder"
            methodBuilder.addStatement("var $binderName:android.os.IBinder?")
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if (" + paramName + " != null)")
            }
            methodBuilder.beginControlFlow("synchronized (stubMap)")
            methodBuilder.addStatement(binderName + " = stubMap.get(" + paramName + ")")
            methodBuilder.beginControlFlow("if ($binderName == null)")
            methodBuilder.addStatement(
                binderName + " = %T(" + paramName + ")",
                getStubClassName(param.type.resolve())
            )
            methodBuilder.addStatement("stubMap.put(" + paramName + ", " + binderName + ")")
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$binderName = null")
                methodBuilder.endControlFlow()
            }
            methodBuilder.addStatement("$DATA.writeStrongBinder($binderName)")
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray()) {
            val argType = resultType.arguments.first().type?.resolve()
            if (resultType.isNullable()) {
                methodBuilder.beginControlFlow("if($RESULT == null)")
                    .addStatement("$DATA.writeInt(-1)")
                    .nextControlFlow("else")
                    .addStatement("$DATA.writeInt($RESULT.size)")
                    .beginControlFlow("for(item in $RESULT)")
                    .addStatement(
                        "$DATA.writeStrongBinder( if(item != null) %T(item)  else null)",
                        getStubClassName(argType)
                    )
                    .endControlFlow()
                    .endControlFlow()
            } else {
                methodBuilder.addStatement("$DATA.writeInt($RESULT.size)")
                    .beginControlFlow("for(item in $RESULT)")
                    .addStatement(
                        "$DATA.writeStrongBinder( if(item != null) %T(item)  else null)",
                        getStubClassName(argType)
                    )
                    .endControlFlow()
            }
        } else {
            if (resultType.isNullable()) {
                methodBuilder.addStatement(
                    "$REPLY.writeStrongBinder(if($RESULT != null)  %T($RESULT)  else null)",
                    getStubClassName(resultType)
                )
            } else {
                methodBuilder.addStatement(
                    "$REPLY.writeStrongBinder(%T($RESULT))",
                    getStubClassName(resultType)
                )
            }
        }
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.getReturnAsKotlinType()
        val resultKSType = methodType.returnType?.resolve()
        if (resultKSType?.isArray() == true) {
            val argType = resultKSType.arguments.first().type?.resolve()
            methodBuilder.addStatement("val ___result_length = $REPLY.readInt()")
                .beginControlFlow("if(___result_length == -1)")
            if (methodType.isNullable()) {
                methodBuilder.addStatement("$RESULT = null")
            } else {
                methodBuilder.addStatement(
                    "throw %T(\"Unexpected null result\")",
                    NullPointerException::class
                )
            }
            methodBuilder.nextControlFlow("else")
                .beginControlFlow("$RESULT = Array(___result_length)")
                .addStatement("%T($REPLY.readStrongBinder())", getProxyClassName(argType))
                .endControlFlow()
                .endControlFlow()
        } else {
            val binderName = "__result_binder"
            methodBuilder.addStatement("val $binderName = $REPLY.readStrongBinder()")
            if (methodType.isNullable()) {
                methodBuilder.addStatement("$RESULT = null")
            }
            methodBuilder.beginControlFlow("if($binderName != null)")
            methodBuilder.addStatement("$RESULT = %T($binderName)", getProxyClassName(resultKSType))
            methodBuilder.addStatement("$RESULT.setRemoterGlobalProperties(__global_properties)")
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (!methodType.isNullable()) {
                methodBuilder.addStatement(
                    "throw %T(\"Unexpected null result\")",
                    NullPointerException::class.java
                )
            }
            methodBuilder.endControlFlow()
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.type.resolve().isArray()) {
            val type = param.asKotlinType()
            val argType = param.type.resolve().arguments.first().type?.resolve()
            methodBuilder.addStatement("val length = $DATA.readInt()")
                .beginControlFlow("if(length == -1)")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.addStatement("throw NullPointerException(\"Not expecting null\")")
            }
            methodBuilder.nextControlFlow("else")
                .beginControlFlow("$paramName = %T(length) ", type.copy(false))
                .addStatement("%T($DATA.readStrongBinder())", getProxyClassName(argType))
                .endControlFlow()
                .endControlFlow()
        } else {
            val binderName = paramName + "_binder"
            methodBuilder.addStatement("val $binderName = $DATA.readStrongBinder()")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            }
            methodBuilder.beginControlFlow("if($binderName != null)")
            methodBuilder.addStatement(
                "$paramName =  %T($binderName)",
                getProxyClassName(param.type.resolve())
            )
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (!param.isNullable()) {
                methodBuilder.addStatement(
                    "throw %T(\"Not expecting null\")",
                    NullPointerException::class
                )
            }
            methodBuilder.endControlFlow()
        }
    }

    private fun getStubClassName(ksType: KSType?): ClassName {
        return getStubClassName(ksType?.declaration?.qualifiedName?.asString() ?: "")
    }

    private fun getProxyClassName(ksType: KSType?): ClassName {
        return getProxyClassName(ksType?.declaration?.qualifiedName?.asString() ?: "")
    }

    private fun getStubClassName(name: String): ClassName {
        val classDeclaration =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString(name))
        val packageName = classDeclaration?.packageName?.asString() ?: ""
        val className = (classDeclaration?.simpleName?.asString() ?: "") + "_Stub"
        return ClassName(packageName, className)
    }

    private fun getProxyClassName(name: String): ClassName {
        val classDeclaration =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString(name))
        val packageName = classDeclaration?.packageName?.asString() ?: ""
        val className = (classDeclaration?.simpleName?.asString() ?: "") + "_Proxy"
        return ClassName(packageName, className)
    }
}

internal class KSPListOfParcelerParamBuilder(
    val genericList: KSClassDeclaration,
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (param.type.resolve().isArray()) {
            logError("List[] is not supported")
        } else {
            if (paramType != ParamType.OUT) {
                if (param.isNullable()) {
                    methodBuilder.beginControlFlow("if (" + paramName + " != null)")
                    methodBuilder.addStatement("$DATA.writeInt(" + paramName + ".size)")
                    methodBuilder.beginControlFlow("for(__r_item in " + paramName + " )")
                    methodBuilder.addStatement("val pClass = getParcelerClass(__r_item)")
                    methodBuilder.addStatement("$DATA.writeString(pClass?.getName())")
                    methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, __r_item).writeToParcel($DATA, 0)")
                    methodBuilder.endControlFlow()
                    methodBuilder.endControlFlow()
                    methodBuilder.beginControlFlow("else")
                    methodBuilder.addStatement("$DATA.writeInt(-1)")
                    methodBuilder.endControlFlow()
                } else {
                    methodBuilder.addStatement("$DATA.writeInt(" + paramName + ".size)")
                    methodBuilder.beginControlFlow("for(__r_item in " + paramName + " )")
                    methodBuilder.addStatement("val pClass = getParcelerClass(__r_item)")
                    methodBuilder.addStatement("$DATA.writeString(pClass?.getName())")
                    methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, __r_item).writeToParcel($DATA, 0)")
                    methodBuilder.endControlFlow()
                }
            }
        }
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        if (resultType.isArray()) {
            logError("List[] is not supported")
        } else {
            methodBuilder.beginControlFlow("if ($RESULT != null)")
            methodBuilder.addStatement("$REPLY.writeInt($RESULT.size)")
            methodBuilder.beginControlFlow("for(item in $RESULT )")
            methodBuilder.addStatement("val pClass = getParcelerClass(item)")
            methodBuilder.addStatement("$REPLY.writeString(pClass!!.getName())")
            methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.addStatement("$REPLY.writeInt(-1)")
            methodBuilder.endControlFlow()
        }
    }

    override fun readOutResultsFromStub(
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        methodBuilder.beginControlFlow("if ($paramName != null)")
        methodBuilder.addStatement("$REPLY.writeInt($paramName.size)")
        methodBuilder.beginControlFlow("for(item in $paramName )")
        methodBuilder.addStatement("val pClass = getParcelerClass(item)")
        methodBuilder.addStatement("$REPLY.writeString(pClass!!.getName())")
        methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
        methodBuilder.endControlFlow()
        methodBuilder.endControlFlow()
        methodBuilder.beginControlFlow("else")
        methodBuilder.addStatement("$REPLY.writeInt(-1)")
        methodBuilder.endControlFlow()
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultKSType = methodType.returnType?.resolve()
        val resultType = methodType.getReturnAsKotlinType()
        if (resultKSType?.isArray() == true) {
            logError("List[] is not supported")
        } else {
            methodBuilder.addStatement("val _size_result = $REPLY.readInt()")
            methodBuilder.beginControlFlow("if(_size_result >= 0)")
            methodBuilder.addStatement("$RESULT = mutableListOf()")
            methodBuilder.beginControlFlow("for(i in 0 until _size_result) ")
            methodBuilder.addStatement(
                "$RESULT.add(getParcelerObject($REPLY.readString(), $REPLY) as %T)",
                genericList.asKotlinType()
            )
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (methodType.isNullable()) {
                methodBuilder.addStatement("$RESULT = null")
            } else {
                methodBuilder.addStatement("$RESULT = mutableListOf()")
            }
            methodBuilder.endControlFlow()
        }
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        val suffix = if (param.isNullable()) {
            "?"
        } else {
            ""
        }
        methodBuilder.addStatement("val $paramName:%T$suffix ", param.asKotlinType())
        if (param.type.resolve().isArray()) {
            logError("List[] is not supported")
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement("$paramName = mutableListOf()")
            } else {
                methodBuilder.addStatement("val size_$paramName = $DATA.readInt()")
                methodBuilder.beginControlFlow("if (size_$paramName >=0)")
                methodBuilder.addStatement("$paramName = mutableListOf()")
                methodBuilder.beginControlFlow("for(i in 0 until size_$paramName)")
                methodBuilder.addStatement(
                    "$paramName.add(getParcelerObject($DATA.readString(), $DATA) as %T )",
                    genericList.asKotlinType()
                )
                methodBuilder.endControlFlow()
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                if (param.isNullable()) {
                    methodBuilder.addStatement("$paramName = null")
                } else {
                    methodBuilder.addStatement("$paramName = mutableListOf()")
                }
                methodBuilder.endControlFlow()
            }
        }
    }

    override fun readOutParamsFromProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        if (paramType != ParamType.IN) {
            var callOperation = "."
            if (param.isNullable()) {
                callOperation = "?."
            }
            methodBuilder.addStatement("val _size_" + paramName + " = $REPLY.readInt()")
            methodBuilder.beginControlFlow("if(_size_" + paramName + " >= 0)")
            methodBuilder.addStatement(paramName + "${callOperation}clear()")
            methodBuilder.beginControlFlow("for(i in 0 until _size_" + paramName + ")")
            methodBuilder.addStatement(
                "${paramName}${callOperation}add(getParcelerObject($REPLY.readString(), $REPLY) as %T)",
                genericList.asKotlinType()
            )
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
        }
    }
}

internal class KSPGenericParamBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPParamBuilder(remoterInterfaceElement, bindingManager, resolver) {
    override fun writeParamsToProxy(
        param: KSValueParameter,
        paramType: ParamType,
        methodBuilder: FunSpec.Builder
    ) {
        val paramName = param.name?.asString()
        methodBuilder.addStatement("val pClass" + paramName + " = getParcelerClass(" + paramName + ")")
        methodBuilder.beginControlFlow("if (pClass" + paramName + " != null)")
        methodBuilder.addStatement("$DATA.writeInt(1)")
        methodBuilder.addStatement("$DATA.writeString(pClass" + paramName + ".getName())")
        methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass" + paramName + ", " + paramName + ").writeToParcel($DATA, 0)")
        methodBuilder.endControlFlow()
        methodBuilder.beginControlFlow("else")
        methodBuilder.addStatement("$DATA.writeInt(2)")
        methodBuilder.addStatement("$DATA.writeValue(" + paramName + " )")
        methodBuilder.endControlFlow()
    }

    override fun readResultsFromStub(
        methodElement: KSFunctionDeclaration,
        resultType: KSType,
        methodBuilder: FunSpec.Builder
    ) {
        methodBuilder.addStatement("val pClassResult = getParcelerClass($RESULT)")
        methodBuilder.beginControlFlow("if (pClassResult != null)")
        methodBuilder.addStatement("$REPLY.writeInt(1)")
        methodBuilder.addStatement("$REPLY.writeString(pClassResult.getName())")
        methodBuilder.addStatement("org.parceler.Parcels.wrap(pClassResult, $RESULT).writeToParcel($REPLY, 0)")
        methodBuilder.endControlFlow()
        methodBuilder.beginControlFlow("else")
        methodBuilder.addStatement("$REPLY.writeInt(2)")
        methodBuilder.addStatement("$REPLY.writeValue($RESULT)")
        methodBuilder.endControlFlow()
    }

    override fun readResultsFromProxy(
        methodType: KSFunctionDeclaration,
        methodBuilder: FunSpec.Builder
    ) {
        val resultType = methodType.getReturnAsKotlinType()
        methodBuilder.beginControlFlow("if ($REPLY.readInt() == 1)")
        methodBuilder.addStatement(
            "$RESULT = getParcelerObject($REPLY.readString(), $REPLY) as %T",
            resultType
        )
        methodBuilder.endControlFlow()
        methodBuilder.beginControlFlow("else")
        methodBuilder.addStatement(
            "$RESULT = $REPLY.readValue(javaClass.classLoader) as %T",
            resultType
        )
        methodBuilder.endControlFlow()
    }

    override fun writeParamsToStub(
        methodType: KSFunctionDeclaration,
        param: KSValueParameter,
        paramType: ParamType,
        paramName: String,
        methodBuilder: FunSpec.Builder
    ) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        methodBuilder.beginControlFlow("if ($DATA.readInt() == 1)")
        methodBuilder.addStatement(
            "$paramName = getParcelerObject($DATA.readString(), $DATA) as %T",
            param.asKotlinType()
        )
        methodBuilder.endControlFlow()
        methodBuilder.beginControlFlow("else")
        methodBuilder.addStatement(
            "$paramName = $DATA.readValue(javaClass.classLoader) as %T",
            param.asKotlinType()
        )
        methodBuilder.endControlFlow()
    }
}

