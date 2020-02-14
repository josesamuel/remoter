package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.FunSpec
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror


/**
 * A [ParamBuilder] for List type parameters
 */
internal class ListParamBuilder(remoterInterfaceElement: Element, bindingManager: KBindingManager) : ParamBuilder(remoterInterfaceElement, bindingManager) {
    override fun writeParamsToProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            logError("List[] is not supported")
        } else {
            if (paramType != ParamType.OUT) {
                if (isListOfStrings(param.asType())) {
                    methodBuilder.addStatement("$DATA.writeStringList(" + param.simpleName + ")")
                } else {
                    methodBuilder.addStatement("$DATA.writeList(" + param.simpleName + ")")
                }
            }
        }
    }

    override fun readResultsFromStub(methodElement: ExecutableElement, resultType: TypeMirror, methodBuilder: FunSpec.Builder) {
        if (resultType.kind == TypeKind.ARRAY) {
            logError("List[] is not supported")
        } else {
            if (isListOfStrings(resultType)) {
                methodBuilder.addStatement("$REPLY.writeStringList($RESULT)")
            } else {
                methodBuilder.addStatement("$REPLY.writeList($RESULT)")
            }
        }
    }

    override fun readOutResultsFromStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        if (isListOfStrings(param.asType())) {
            methodBuilder.addStatement("$REPLY.writeStringList($paramName)")
        } else {
            methodBuilder.addStatement("$REPLY.writeList($paramName)")
        }
    }

    override fun readResultsFromProxy(methodType: ExecutableElement, methodBuilder: FunSpec.Builder) {
        val resultType = methodType.getReturnAsKotlinType()
        val resultMirror = methodType.getReturnAsTypeMirror()
        if (resultMirror.kind == TypeKind.ARRAY) {
            logError("List[] is not supported")
        } else {
            if (isListOfStrings(resultMirror)) {
                methodBuilder.addStatement("$RESULT = $REPLY.createStringArrayList() as %T ", resultType)
            } else {
                methodBuilder.addStatement("$RESULT = $REPLY.readArrayList(javaClass.getClassLoader()) as %T", resultType)
            }
        }
    }

    override fun writeParamsToStub(methodType: ExecutableElement, param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.asType().kind == TypeKind.ARRAY) {
            logError("List[] is not supported")
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement("$paramName = mutableListOf()")
            } else {
                val suffix = if(param.isNullable()) "" else "!!"
                if (isListOfStrings(param.asType())) {
                    methodBuilder.addStatement("$paramName = $DATA.createStringArrayList()$suffix")
                } else {
                    methodBuilder.addStatement("$paramName = $DATA.readArrayList(getClass().getClassLoader())$suffix")
                }
            }
        }
    }

    override fun readOutParamsFromProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (paramType != ParamType.IN) {
            if (param.isNullable()){
                methodBuilder.beginControlFlow("if (${param.simpleName} != null)")
            }

            if (isListOfStrings(param.asType())) {
                methodBuilder.addStatement("$REPLY.readStringList(" + param.simpleName + ")")
            } else {
                methodBuilder.addStatement("$REPLY.readList(" + param.simpleName + ", getClass().getClassLoader())")
            }

            if (param.isNullable()){
                methodBuilder.endControlFlow()
            }
        }
    }

    private fun isListOfStrings(typeMirror: TypeMirror): Boolean {
        return typeMirror.toString() == "java.util.List<java.lang.String>"
    }
}
