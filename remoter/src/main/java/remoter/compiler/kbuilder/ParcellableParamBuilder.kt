package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.FunSpec
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror


/**
 * A [ParamBuilder] for Parcellable type parameters
 */
internal class ParcellableParamBuilder(remoterInterfaceElement: Element, bindingManager: KBindingManager) : ParamBuilder(remoterInterfaceElement, bindingManager) {
    override fun writeParamsToProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeTypedArray(" + param.simpleName + ", 0)")
            }
        } else {
            if (paramType != ParamType.OUT) {
                if (param.isNullable()) {
                    methodBuilder.beginControlFlow("if (" + param.simpleName + " != null)")
                    methodBuilder.addStatement("$DATA.writeInt(1)")
                    methodBuilder.addStatement(param.simpleName.toString() + ".writeToParcel($DATA, 0)")
                    methodBuilder.endControlFlow()
                    methodBuilder.beginControlFlow("else")
                    methodBuilder.addStatement("$DATA.writeInt(0)")
                    methodBuilder.endControlFlow()
                } else {
                    methodBuilder.addStatement("$DATA.writeInt(1)")
                    methodBuilder.addStatement(param.simpleName.toString() + ".writeToParcel($DATA, 0)")
                }
            }
        }
    }

    override fun readResultsFromStub(methodElement: ExecutableElement, resultType: TypeMirror, methodBuilder: FunSpec.Builder) {
        if (resultType.kind == TypeKind.ARRAY) {
            methodBuilder.addStatement("$REPLY.writeTypedArray($RESULT, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
        } else {
            if (methodElement.isNullable()) {
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

    override fun readOutResultsFromStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            methodBuilder.addStatement("$REPLY.writeTypedArray($paramName, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
        } else {
            if (param.asKotlinType().isNullable) {
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

    override fun readResultsFromProxy(methodType: ExecutableElement, methodBuilder: FunSpec.Builder) {
        val resultType = methodType.getReturnAsKotlinType()
        val resultMirror = methodType.getReturnAsTypeMirror()
        if (resultMirror.kind == TypeKind.ARRAY) {
            methodBuilder.addStatement("$RESULT = $REPLY.createTypedArray(" + getParcelableClassName(resultMirror) + ".CREATOR) as %T", resultType)
        } else {
            methodBuilder.beginControlFlow("if ($REPLY.readInt() != 0)")
            methodBuilder.addStatement("$RESULT = (" + getParcelableClassName(resultMirror) + ".CREATOR.createFromParcel($REPLY) as %T)", resultType)
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (resultType.isNullable) {
                methodBuilder.addStatement("$RESULT = null")
            } else {
                methodBuilder.addStatement("throw %T(\"Unexpected null result\")", NullPointerException::class)
            }
            methodBuilder.endControlFlow()
        }
    }

    override fun writeParamsToStub(methodType: ExecutableElement, param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.asType().kind == TypeKind.ARRAY) {
            if (paramType == ParamType.OUT) {
                _writeOutParamsToStub(methodType, param, paramType, paramName, methodBuilder)
            } else {
                methodBuilder.addStatement(paramName + " =  $DATA.createTypedArray(" + getParcelableClassName(param.asType()) + ".CREATOR) as " + param.asKotlinType())
            }
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement(paramName + " = " + getParcelableClassName(param.asType()) + "()")
            } else {
                methodBuilder.beginControlFlow("if ( $DATA.readInt() != 0)")
                methodBuilder.addStatement(paramName + " = " + getParcelableClassName(param.asType()) + ".CREATOR.createFromParcel($DATA) as " + param.asKotlinType())
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                if (param.isNullable()) {
                    methodBuilder.addStatement("$paramName = null")
                } else {
                    methodBuilder.addStatement("throw %T(\"Unexpected null result\")", NullPointerException::class)
                }
                methodBuilder.endControlFlow()
            }
        }
    }

    private fun _writeOutParamsToStub(methodType: ExecutableElement, param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("val " + paramName + "_length = $DATA.readInt()")
            methodBuilder.beginControlFlow("if (" + paramName + "_length < 0 )")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.beginControlFlow(paramName + " = " + param.asKotlinType()
                        + "(0)")
                methodBuilder.addStatement("%T()", (param.asType() as ArrayType).componentType.asKotlinType())
                methodBuilder.endControlFlow()
            }
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.beginControlFlow(paramName + " = " + param.asKotlinType().copy(false)
                    + "(" + paramName + "_length)")
            methodBuilder.addStatement("%T()", (param.asType() as ArrayType).componentType.asKotlinType(null, methodType))
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
        }
    }

    private fun getParcelableClassName(typeMirror: TypeMirror): String {
        return if (typeMirror.kind != TypeKind.ARRAY) {
            var pClassName = typeMirror.toString()
            val genericStartIndex = pClassName.indexOf('<')
            if (genericStartIndex != -1) {
                pClassName = pClassName.substring(0, genericStartIndex).trim { it <= ' ' }
            }
            pClassName
        } else {
            getParcelableClassName((typeMirror as ArrayType).componentType)
        }
    }

    override fun readOutParamsFromProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (paramType != ParamType.IN) {
            if (param.asType().kind == TypeKind.ARRAY) {
                if (param.isNullable()){
                    methodBuilder.beginControlFlow("if (${param.simpleName} != null)")
                }

                methodBuilder.addStatement("$REPLY.readTypedArray(" + param.simpleName + ", " + getParcelableClassName(param.asType()) + ".CREATOR)")

                if (param.isNullable()){
                    methodBuilder.endControlFlow()
                }
            } else {
                methodBuilder.beginControlFlow("if ($REPLY.readInt() != 0)")
                if (param.isNullable()) {
                    methodBuilder.addStatement(param.simpleName.toString() + "?.readFromParcel($REPLY)")
                } else {
                    methodBuilder.addStatement(param.simpleName.toString() + ".readFromParcel($REPLY)")
                }
                methodBuilder.endControlFlow()
            }
        }
    }
}
