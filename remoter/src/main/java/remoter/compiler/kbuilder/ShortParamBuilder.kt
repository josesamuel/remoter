package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.FunSpec
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror


/**
 * A [ParamBuilder] for short type parameters
 */
internal class ShortParamBuilder(remoterInterfaceElement: Element, bindingManager: KBindingManager) : ParamBuilder(remoterInterfaceElement, bindingManager) {
    override fun writeParamsToProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            logError("short[] not supported, use int[]" + param.simpleName)
        } else {
            methodBuilder.addStatement("$DATA.writeInt(" + param.simpleName + ".toInt())")
        }
    }

    override fun readResultsFromStub(methodElement: ExecutableElement, resultType: TypeMirror, methodBuilder: FunSpec.Builder) {
        if (resultType.kind == TypeKind.ARRAY) {
            logError("short[] not supported, use int[]")
        } else {
            methodBuilder.addStatement("$REPLY.writeInt($RESULT.toInt())")
        }
    }

    override fun readResultsFromProxy(methodType: ExecutableElement, methodBuilder: FunSpec.Builder) {
        val resultMirror = methodType.getReturnAsTypeMirror()
        if (resultMirror.kind == TypeKind.ARRAY) {
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

    override fun writeParamsToStub(methodType: ExecutableElement, param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.asType().kind == TypeKind.ARRAY) {
            logError("short[] not supported, use int[]" + param.simpleName)
            methodBuilder.addStatement("$paramName = ShortArray(0)")
        } else {
            methodBuilder.addStatement("$paramName = $DATA.readInt().toShort()")
        }
    }
}
