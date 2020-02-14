package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MAP
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror


/**
 * A [ParamBuilder] for Map type parameters
 */
internal class MapParamBuilder(remoterInterfaceElement: Element, bindingManager: KBindingManager) : ParamBuilder(remoterInterfaceElement, bindingManager) {
    override fun writeParamsToProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            logError("Map[] is not supported")
        } else {
            if (paramType != ParamType.OUT) {
                methodBuilder.addStatement("$DATA.writeMap(" + param.simpleName + " as %T<*, *>?)", MAP)
            }
        }
    }

    override fun readResultsFromStub(methodElement: ExecutableElement, resultType: TypeMirror, methodBuilder: FunSpec.Builder) {
        if (resultType.kind == TypeKind.ARRAY) {
            logError("Map[] is not supported")
        } else {
            methodBuilder.addStatement("$REPLY.writeMap($RESULT as %T<*, *>?)", MAP)
        }
    }

    override fun readOutResultsFromStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        methodBuilder.addStatement("$REPLY.writeMap($paramName as %T<*, *>?)", MAP)
    }

    override fun readResultsFromProxy(methodType: ExecutableElement, methodBuilder: FunSpec.Builder) {
        val resultType = methodType.getReturnAsKotlinType()
        val resultMirror = methodType.getReturnAsTypeMirror()
        if (resultMirror.kind == TypeKind.ARRAY) {
            logError("Map[] is not supported")
        } else {
            methodBuilder.addStatement("$RESULT = $REPLY.readHashMap(javaClass.getClassLoader()) as %T", resultType)
        }
    }

    override fun writeParamsToStub(methodType: ExecutableElement, param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.asType().kind == TypeKind.ARRAY) {
            logError("Map[] is not supported")
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement("$paramName = mutableMapOf()")
            } else {
                methodBuilder.addStatement("$paramName = $DATA.readHashMap(javaClass.getClassLoader()) as %T", param.asKotlinType())
            }
        }
    }

    override fun readOutParamsFromProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (paramType != ParamType.IN) {
            if (param.isNullable()){
                methodBuilder.beginControlFlow("if (${param.simpleName} != null)")
            }

            methodBuilder.addStatement( "${param.simpleName}.clear()")
            methodBuilder.addStatement("$REPLY.readMap(" + param.simpleName + " as %T<*, *>, javaClass.getClassLoader())", MAP)

            if (param.isNullable()){
                methodBuilder.endControlFlow()
            }
        }
    }
}
