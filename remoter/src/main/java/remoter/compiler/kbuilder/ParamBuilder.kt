package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.FunSpec
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror


/**
 * A [KRemoterBuilder] for a specific type of a parameter
 */
internal abstract class ParamBuilder(remoterInterfaceElement: Element, bindingManager: KBindingManager) : KRemoterBuilder(remoterInterfaceElement, bindingManager) {

    companion object {
        internal val DATA = "___remoter_data"
        internal val REPLY = "___remoter_reply"
        internal val RESULT = "___remoter_result"
    }

    /**
     * Called to generate code to write the params to proxy
     */
    open fun writeParamsToProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {}

    /**
     * Called to generate code that reads results from proxy
     */
    open fun readResultsFromProxy(methodType: ExecutableElement, methodBuilder: FunSpec.Builder) {}

    /**
     * Called to generate code that reads results from proxy for @[remoter.annotations.ParamOut] parameters
     */
    open fun readOutParamsFromProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {}

    /**
     * Called to generate code to write params for stub
     */
    open fun writeParamsToStub(methodType: ExecutableElement, param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        methodBuilder.addStatement("var $paramName:%T", param.asKotlinType())
    }

    /**
     * Called to generate code to write @[remoter.annotations.ParamOut] params for stub
     */
    open fun writeOutParamsToStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("val " + paramName + "_length = $DATA.readInt()")
            methodBuilder.beginControlFlow("if (" + paramName + "_length < 0 )")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.addStatement(paramName + " = " + param.asKotlinType()
                        + "(0)")
            }
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.addStatement(paramName + " = " + param.asKotlinType().copy(false)
                    + "(" + paramName + "_length)")
            methodBuilder.endControlFlow()
        }
    }

    /**
     * Called to generate code that reads results from stub
     */
    open fun readResultsFromStub(methodElement: ExecutableElement, resultType: TypeMirror, methodBuilder: FunSpec.Builder) {}

    /**
     * Called to generate code that reads  [remoter.annotations.ParamOut] results from stub
     */
    open fun readOutResultsFromStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {}

    /**
     * Called to generate code that writes the out params for array type
     */
    protected open fun writeArrayOutParamsToProxy(param: VariableElement, methodBuilder: FunSpec.Builder) {
        if (param.isNullable()) {
            methodBuilder.beginControlFlow("if (" + param.simpleName + " == null)")
            methodBuilder.addStatement("$DATA.writeInt(-1)")
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.addStatement("$DATA.writeInt(" + param.simpleName + ".size)")
            methodBuilder.endControlFlow()
        } else {
            methodBuilder.addStatement("$DATA.writeInt(" + param.simpleName + ".size)")
        }
    }

    /**
     * Represents the type of parameter
     */
    enum class ParamType {
        IN, OUT, IN_OUT
    }
}
