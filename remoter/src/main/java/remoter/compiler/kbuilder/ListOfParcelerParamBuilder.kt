package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.FunSpec
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror


/**
 * A [ParamBuilder] for List of Parceler type parameters
 */
internal class ListOfParcelerParamBuilder (private val genericType: TypeElement, remoterInterfaceElement: Element, bindingManager: KBindingManager) : ParamBuilder(remoterInterfaceElement, bindingManager) {
    override fun writeParamsToProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            logError("List[] is not supported")
        } else {
            if (paramType != ParamType.OUT) {
                if (param.isNullable()) {
                    methodBuilder.beginControlFlow("if (" + param.simpleName + " != null)")
                    methodBuilder.addStatement("$DATA.writeInt(" + param.simpleName + ".size)")
                    methodBuilder.beginControlFlow("for(__r_item in " + param.simpleName + " )", genericType)
                    methodBuilder.addStatement("val pClass = getParcelerClass(__r_item)")
                    methodBuilder.addStatement("$DATA.writeString(pClass?.getName())")
                    methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, __r_item).writeToParcel($DATA, 0)")
                    methodBuilder.endControlFlow()
                    methodBuilder.endControlFlow()
                    methodBuilder.beginControlFlow("else")
                    methodBuilder.addStatement("$DATA.writeInt(-1)")
                    methodBuilder.endControlFlow()
                } else {
                    methodBuilder.addStatement("$DATA.writeInt(" + param.simpleName + ".size)")
                    methodBuilder.beginControlFlow("for(__r_item in " + param.simpleName + " )", genericType)
                    methodBuilder.addStatement("val pClass = getParcelerClass(__r_item)")
                    methodBuilder.addStatement("$DATA.writeString(pClass?.getName())")
                    methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, __r_item).writeToParcel($DATA, 0)")
                    methodBuilder.endControlFlow()
                }
            }
        }
    }

    override fun readResultsFromStub(methodElement: ExecutableElement, resultType: TypeMirror, methodBuilder: FunSpec.Builder) {
        if (resultType.kind == TypeKind.ARRAY) {
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

    override fun readOutResultsFromStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
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

    override fun readResultsFromProxy(methodType: ExecutableElement, methodBuilder: FunSpec.Builder) {
        val resultMirror = methodType.getReturnAsTypeMirror()
        val resultType = methodType.getReturnAsKotlinType()
        if (resultMirror.kind == TypeKind.ARRAY) {
            logError("List[] is not supported")
        } else {

            methodBuilder.addStatement("val _size_result = $REPLY.readInt()")

            methodBuilder.beginControlFlow("if(_size_result >= 0)")
            methodBuilder.addStatement("$RESULT = mutableListOf()")
            methodBuilder.beginControlFlow("for(i in 0 until _size_result) ")
            methodBuilder.addStatement("$RESULT.add(getParcelerObject($REPLY.readString(), $REPLY) as %T)", genericType.asType())
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (resultType.isNullable) {
                methodBuilder.addStatement("$RESULT = null")
            } else {
                methodBuilder.addStatement("$RESULT = mutableListOf()")
            }
            methodBuilder.endControlFlow()

        }
    }

    override fun writeParamsToStub(methodType: ExecutableElement, param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        methodBuilder.addStatement("val $paramName:%T ", param.asKotlinType())
        if (param.asType().kind == TypeKind.ARRAY) {
            logError("List[] is not supported")
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement("$paramName = mutableListOf()")
            } else {


                //read
                methodBuilder.addStatement("val size_$paramName = $DATA.readInt()")
                methodBuilder.beginControlFlow("if (size_$paramName >=0)")
                methodBuilder.addStatement("$paramName = mutableListOf()")
                methodBuilder.beginControlFlow("for(i in 0 until size_$paramName)")
                methodBuilder.addStatement("$paramName.add(getParcelerObject($DATA.readString(), $DATA) as %T )", genericType.asKotlinType())
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

    override fun readOutParamsFromProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (paramType != ParamType.IN) {
            var callOperation = "."
            if (param.isNullable()) {
                callOperation = "?."
            }
            methodBuilder.addStatement("val _size_" + param.simpleName + " = $REPLY.readInt()")
            methodBuilder.beginControlFlow("if(_size_" + param.simpleName + " >= 0)")
            methodBuilder.addStatement(param.simpleName.toString() + "${callOperation}clear()")
            methodBuilder.beginControlFlow("for(i in 0 until _size_" + param.simpleName + ")")
            methodBuilder.addStatement("${param.simpleName}${callOperation}add(getParcelerObject($REPLY.readString(), $REPLY) as %T)", genericType.asType())
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
        }
    }

}
