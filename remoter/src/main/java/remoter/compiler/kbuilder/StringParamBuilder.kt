package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.FunSpec
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror


/**
 * A [ParamBuilder] for String type parameters
 */
internal class StringParamBuilder(remoterInterfaceElement: Element, bindingManager: KBindingManager) : ParamBuilder(remoterInterfaceElement, bindingManager) {
    override fun writeParamsToProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeStringArray(" + param.simpleName + ")")
            }
        } else {
            methodBuilder.addStatement("$DATA.writeString(" + param.simpleName + ")")
        }
    }

    override fun readResultsFromStub(methodElement: ExecutableElement, resultType: TypeMirror, methodBuilder: FunSpec.Builder) {
        if (resultType.kind == TypeKind.ARRAY) {
            methodBuilder.addStatement("$REPLY.writeStringArray($RESULT)")
        } else {
            methodBuilder.addStatement("$REPLY.writeString($RESULT)")
        }
    }

    override fun readResultsFromProxy(methodType: ExecutableElement, methodBuilder: FunSpec.Builder) {
        val resultMirror = methodType.getReturnAsTypeMirror()
        val resultType = methodType.getReturnAsKotlinType()
        if (resultMirror.kind == TypeKind.ARRAY) {
            methodBuilder.addStatement("$RESULT = $REPLY.createStringArray()")
        } else {
            if(resultType.isNullable) {
                methodBuilder.addStatement("$RESULT = $REPLY.readString()")
            } else {
                methodBuilder.addStatement("$RESULT = $REPLY.readString()!!")
            }
        }
    }

    override fun readOutResultsFromStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            methodBuilder.addStatement("$REPLY.writeStringArray($paramName)")
        }
    }

    override fun writeParamsToStub(methodType: ExecutableElement, param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.asType().kind == TypeKind.ARRAY) {
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

    /**
     * Called to generate code to write @[remoter.annotations.ParamOut] params for stub
     */
    override fun writeOutParamsToStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("val " + paramName + "_length = $DATA.readInt()")
            methodBuilder.beginControlFlow("if (" + paramName + "_length < 0 )")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.addStatement(paramName + " = " + param.asKotlinType()
                        + "(0){\"\"}")
            }
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.addStatement(paramName + " = " + param.asKotlinType().copy(false)
                    + "(" + paramName + "_length){\"\"}")
            methodBuilder.endControlFlow()
        }
    }

    override fun readOutParamsFromProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY && paramType != ParamType.IN) {
            if (param.isNullable()){
                methodBuilder.beginControlFlow("if (${param.simpleName} != null)")
            }

            methodBuilder.addStatement("$REPLY.readStringArray(" + param.simpleName + ")")

            if (param.isNullable()){
                methodBuilder.endControlFlow()
            }
        }
    }
}