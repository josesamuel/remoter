package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.FunSpec
import org.jetbrains.annotations.Nullable
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror


/**
 * A [ParamBuilder] for Parcel type parameters
 */
internal class ParcelerParamBuilder(remoterInterfaceElement: Element, bindingManager: KBindingManager) : ParamBuilder(remoterInterfaceElement, bindingManager) {

    override fun writeParamsToProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                if (param.isNullable()) {

                    methodBuilder.beginControlFlow("if (" + param.simpleName + " != null)")
                    methodBuilder.addStatement("$DATA.writeInt(" + param.simpleName + ".size)")
                    methodBuilder.beginControlFlow("for (___remoter_p_item in ${param.simpleName})")

                    methodBuilder.addStatement("val pClass" + param.simpleName + " = getParcelerClass(___remoter_p_item)")
                    methodBuilder.beginControlFlow("if (pClass" + param.simpleName + " != null)")
                    methodBuilder.addStatement("$DATA.writeString(pClass" + param.simpleName + ".getName())")
                    methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass" + param.simpleName + ", ___remoter_p_item).writeToParcel($DATA, 0)")
                    methodBuilder.endControlFlow()
                    methodBuilder.endControlFlow()
                    methodBuilder.endControlFlow()
                    methodBuilder.beginControlFlow("else")
                    methodBuilder.addStatement("$DATA.writeInt(-1)")
                    methodBuilder.endControlFlow()

                } else {

                    methodBuilder.addStatement("$DATA.writeInt(" + param.simpleName + ".size)")
                    methodBuilder.beginControlFlow("for (___remoter_p_item in ${param.simpleName})")

                    methodBuilder.addStatement("val pClass" + param.simpleName + " = getParcelerClass(___remoter_p_item)")
                    methodBuilder.beginControlFlow("if (pClass" + param.simpleName + " != null)")
                    methodBuilder.addStatement("$DATA.writeString(pClass" + param.simpleName + ".getName())")
                    methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass" + param.simpleName + ", ___remoter_p_item).writeToParcel($DATA, 0)")
                    methodBuilder.endControlFlow()
                    methodBuilder.endControlFlow()

                }
            }
        } else {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if (" + param.simpleName + " != null)")
                methodBuilder.addStatement("val pClass" + param.simpleName + " = getParcelerClass(" + param.simpleName + ")")
                methodBuilder.beginControlFlow("if (pClass" + param.simpleName + " != null)")
                methodBuilder.addStatement("$DATA.writeInt(1)")
                methodBuilder.addStatement("$DATA.writeString(pClass" + param.simpleName + ".getName())")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass" + param.simpleName + ", " + param.simpleName + ").writeToParcel($DATA, 0)")
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$DATA.writeInt(0)")
                methodBuilder.endControlFlow()
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$DATA.writeInt(0)")
                methodBuilder.endControlFlow()
            } else {
                methodBuilder.addStatement("val pClass" + param.simpleName + " = getParcelerClass(" + param.simpleName + ")")
                methodBuilder.beginControlFlow("if (pClass" + param.simpleName + " != null)")
                methodBuilder.addStatement("$DATA.writeInt(1)")
                methodBuilder.addStatement("$DATA.writeString(pClass" + param.simpleName + ".getName())")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass" + param.simpleName + ", " + param.simpleName + ").writeToParcel($DATA, 0)")
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$DATA.writeInt(0)")
                methodBuilder.endControlFlow()
            }
        }
    }

    override fun readResultsFromStub(methodElement: ExecutableElement, resultType: TypeMirror, methodBuilder: FunSpec.Builder) {
        if (resultType.kind == TypeKind.ARRAY) {
            if (methodElement.isNullable()) {
                methodBuilder.beginControlFlow("if ($RESULT != null)")
                methodBuilder.addStatement("$REPLY.writeInt($RESULT.size)")
                methodBuilder.beginControlFlow("for(item in $RESULT)")
                methodBuilder.addStatement("val pClass = getParcelerClass(item)")
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.getName())")
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
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.getName())")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
                methodBuilder.endControlFlow()
            }
        } else {
            if (methodElement.isNullable()) {
                methodBuilder.beginControlFlow("if ($RESULT != null)")
                methodBuilder.addStatement("$REPLY.writeInt(1)")
                methodBuilder.addStatement("val pClass = getParcelerClass($RESULT)")
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.getName())")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, $RESULT).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$REPLY.writeInt(0)")
                methodBuilder.endControlFlow()
            } else {
                methodBuilder.addStatement("$REPLY.writeInt(1)")
                methodBuilder.addStatement("val pClass = getParcelerClass($RESULT)")
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.getName())")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, $RESULT).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
            }
        }
    }

    override fun readOutResultsFromStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if ($paramName != null)")
                methodBuilder.addStatement("$REPLY.writeInt($paramName!!.size)")
                methodBuilder.beginControlFlow("for(item in $paramName!!)")
                methodBuilder.addStatement("val pClass = getParcelerClass(item)")
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.getName())")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
                methodBuilder.endControlFlow()
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$REPLY.writeInt(-1)")
                methodBuilder.endControlFlow()
            } else {
                methodBuilder.addStatement("$REPLY.writeInt($paramName.size)")
                methodBuilder.beginControlFlow("for(item in $paramName )")
                methodBuilder.addStatement("val pClass = getParcelerClass(item)")
                methodBuilder.addStatement("$REPLY.writeString(pClass!!.getName())")
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel($REPLY, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)")
                methodBuilder.endControlFlow()
            }
        }
    }

    override fun readResultsFromProxy(methodType: ExecutableElement, methodBuilder: FunSpec.Builder) {
        val resultType = methodType.getReturnAsKotlinType()
        val resultMirror = methodType.getReturnAsTypeMirror()
        if (resultMirror.kind == TypeKind.ARRAY) {
            methodBuilder.addStatement("val __size_result = $REPLY.readInt()")
            methodBuilder.beginControlFlow("if (__size_result >= 0)")

            methodBuilder.beginControlFlow("$RESULT = %T (__size_result) ", resultType.copy(false))
            methodBuilder.addStatement("getParcelerObject($REPLY.readString(), $REPLY) as %T", (resultMirror as ArrayType).componentType)
            methodBuilder.endControlFlow()

            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (resultType.isNullable) {
                methodBuilder.addStatement("$RESULT = null")
            } else {
                methodBuilder.addStatement("throw %T(\"Unexpected null result\")", NullPointerException::class)
            }
            methodBuilder.endControlFlow()


        } else {
            methodBuilder.beginControlFlow("if ($REPLY.readInt() != 0)")
            methodBuilder.addStatement("$RESULT = getParcelerObject($REPLY.readString(), $REPLY) as %T", resultType.copy(false))
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
                _writeOutParamsToStub(param, paramType, paramName, methodBuilder)
            } else {
                methodBuilder.addStatement("val size_$paramName = $DATA.readInt()")
                methodBuilder.beginControlFlow("if (size_$paramName >= 0)")
                methodBuilder.beginControlFlow("$paramName = %T(size_$paramName)", param.asKotlinType().copy(false))
                methodBuilder.addStatement("getParcelerObject($DATA.readString(), $DATA) as %T", (param.asType() as ArrayType).componentType.asKotlinType())
                methodBuilder.endControlFlow()
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                if (param.isNullable()) {
                    methodBuilder.addStatement("$paramName = null")
                } else {
                    methodBuilder.addStatement("throw %T(\"Unexpected null param\")", NullPointerException::class)
                }
                methodBuilder.endControlFlow()
            }
        } else {
            methodBuilder.beginControlFlow("if ( $DATA.readInt() != 0)")
            methodBuilder.addStatement("$paramName = getParcelerObject($DATA.readString(), $DATA) as %T", param.asType())
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

    /**
     * Called to generate code to write @[remoter.annotations.ParamOut] params for stub
     */
    private fun _writeOutParamsToStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
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
            methodBuilder.addStatement("%T()", (param.asType() as ArrayType).componentType.asKotlinType())
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
        }
    }

    override fun readOutParamsFromProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (paramType != ParamType.IN) {
            if (param.asType().kind == TypeKind.ARRAY) {
                methodBuilder.addStatement("val _size_" + param.simpleName + " = $REPLY.readInt()")
                methodBuilder.beginControlFlow("for(i in 0 until _size_" + param.simpleName + ")")
                if (param.isNullable()) {
                    methodBuilder.addStatement(param.simpleName.toString() + "?.set(i, getParcelerObject($REPLY.readString(), $REPLY) as %T)", (param.asType() as ArrayType).componentType)
                } else {
                    methodBuilder.addStatement(param.simpleName.toString() + ".set(i, getParcelerObject($REPLY.readString(), $REPLY) as %T)", (param.asType() as ArrayType).componentType)
                }
                methodBuilder.endControlFlow()
            }
        }
    }
}
