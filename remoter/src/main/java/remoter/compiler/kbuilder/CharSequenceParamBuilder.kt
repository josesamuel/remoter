package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.FunSpec
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * A [ParamBuilder] for [CharSequence] type parameters
 */
internal class CharSequenceParamBuilder(remoterInterfaceElement: Element, bindingManager: KBindingManager) : ParamBuilder(remoterInterfaceElement, bindingManager) {
    override fun writeParamsToProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            logError("CharSequence[] cannot be marshalled")
        } else {
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if(" + param.simpleName + " != null)")
                methodBuilder.addStatement("$DATA.writeInt(1)")
                methodBuilder.addStatement("android.text.TextUtils.writeToParcel(" + param.simpleName + ", $DATA, 0)")
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$DATA.writeInt(0)")
                methodBuilder.endControlFlow()
            } else {
                methodBuilder.addStatement("$DATA.writeInt(1)")
                methodBuilder.addStatement("android.text.TextUtils.writeToParcel(" + param.simpleName + ", $DATA, 0)")
            }
        }
    }

    override fun readResultsFromStub(methodElement: ExecutableElement, resultType: TypeMirror, methodBuilder: FunSpec.Builder) {
        if (resultType.kind == TypeKind.ARRAY) {
            logError("CharSequence[] cannot be marshalled")
        } else {
            methodBuilder.beginControlFlow("if($RESULT!= null)")
            methodBuilder.addStatement("$REPLY.writeInt(1)")
            methodBuilder.addStatement("android.text.TextUtils.writeToParcel($RESULT, $REPLY, 0)")
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            methodBuilder.addStatement("$REPLY.writeInt(0)")
            methodBuilder.endControlFlow()
        }
    }

    override fun readResultsFromProxy(methodType: ExecutableElement, methodBuilder: FunSpec.Builder) {
        val resultType = methodType.getReturnAsKotlinType()
        val resultMirror = methodType.getReturnAsTypeMirror()
        if (resultMirror.kind == TypeKind.ARRAY) {
            logError("CharSequence[] cannot be marshalled")
        } else {
            methodBuilder.beginControlFlow("if($REPLY.readInt() != 0)")
            methodBuilder.addStatement("$RESULT = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel($REPLY)")
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
            logError("CharSequence[] cannot be marshalled")
        } else {

            methodBuilder.beginControlFlow("if($DATA.readInt() != 0)")
            methodBuilder.addStatement("$paramName = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel($DATA)")
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.addStatement("throw %T(\"Not expecting null\")", NullPointerException::class.java)
            }
            methodBuilder.endControlFlow()
        }
    }
}
