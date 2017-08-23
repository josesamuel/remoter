package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link ParamBuilder} for {@link CharSequence} type parameters
 */
class CharSequenceParamBuilder extends ParamBuilder {


    protected CharSequenceParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }

    @Override
    public void writeParamsToProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            logError("CharSequence[] cannot be marshalled");
        } else {
            methodBuilder.beginControlFlow("if(" + param.getSimpleName() + " != null)");
            methodBuilder.addStatement("data.writeInt(1)");
            methodBuilder.addStatement("android.text.TextUtils.writeToParcel(" + param.getSimpleName() + ", data, 0)");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("data.writeInt(0)");
            methodBuilder.endControlFlow();
        }
    }

    @Override
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            logError("CharSequence[] cannot be marshalled");
        } else {
            methodBuilder.beginControlFlow("if(result!= null)");
            methodBuilder.addStatement("reply.writeInt(1)");
            methodBuilder.addStatement("android.text.TextUtils.writeToParcel(result, reply, 0)");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("reply.writeInt(0)");
            methodBuilder.endControlFlow();
        }
    }


    @Override
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            logError("CharSequence[] cannot be marshalled");
        } else {
            methodBuilder.beginControlFlow("if(reply.readInt() != 0)");
            methodBuilder.addStatement("result = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(reply)");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("result = null");
            methodBuilder.endControlFlow();
        }
    }

    @Override
    public void writeParamsToStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, paramName, methodBuilder);
        if (param.asType().getKind() == TypeKind.ARRAY) {
            logError("CharSequence[] cannot be marshalled");
        } else {
            methodBuilder.beginControlFlow("if(data.readInt() != 0)");
            methodBuilder.addStatement(paramName + " = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data)");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement(paramName + " = null");
            methodBuilder.endControlFlow();
        }
    }


}
