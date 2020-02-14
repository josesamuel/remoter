package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.FunSpec
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror


/**
 * A [ParamBuilder] for double type parameters
 */
internal class DoubleParamBuilder(remoterInterfaceElement: Element, bindingManager: KBindingManager) : ParamBuilder(remoterInterfaceElement, bindingManager) {
    override fun writeParamsToProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder)
            } else {
                methodBuilder.addStatement("$DATA.writeDoubleArray(" + param.simpleName + ")")
            }
        } else {
            methodBuilder.addStatement("$DATA.writeDouble(" + param.simpleName + ")")
        }
    }

    override fun readResultsFromStub(methodElement: ExecutableElement, resultType: TypeMirror, methodBuilder: FunSpec.Builder) {
        if (resultType.kind == TypeKind.ARRAY) {
            methodBuilder.addStatement("$REPLY.writeDoubleArray($RESULT)")
        } else {
            methodBuilder.addStatement("$REPLY.writeDouble($RESULT)")
        }
    }

    override fun readResultsFromProxy(methodType: ExecutableElement, methodBuilder: FunSpec.Builder) {
        val resultMirror = methodType.getReturnAsTypeMirror()
        val resultType = methodType.getReturnAsKotlinType()
        if (resultMirror.kind == TypeKind.ARRAY) {
            val suffix = if (resultType.isNullable) "" else "!!"
            methodBuilder.addStatement("$RESULT = $REPLY.createDoubleArray()$suffix")
        } else {
            methodBuilder.addStatement("$RESULT = $REPLY.readDouble()")
        }
    }

    override fun readOutResultsFromStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            methodBuilder.addStatement("$REPLY.writeDoubleArray($paramName)")
        }
    }

    override fun writeParamsToStub(methodType: ExecutableElement, param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.asType().kind == TypeKind.ARRAY) {
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

    override fun readOutParamsFromProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY && paramType != ParamType.IN) {
            if (param.isNullable()){
                methodBuilder.beginControlFlow("if (${param.simpleName} != null)")
            }

            methodBuilder.addStatement("$REPLY.readDoubleArray(" + param.simpleName + ")")
            if (param.isNullable()){
                methodBuilder.endControlFlow()
            }
        }
    }
}
