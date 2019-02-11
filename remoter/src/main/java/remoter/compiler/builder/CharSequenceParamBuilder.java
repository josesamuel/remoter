package remoter.compiler.builder;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

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
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            logError("CharSequence[] cannot be marshalled");
        } else {
            methodBuilder.beginControlFlow("if($L != null)", param.name);
            methodBuilder.addStatement("data.writeInt(1)");
            methodBuilder.addStatement("android.text.TextUtils.writeToParcel($L, data, 0)", param.name);
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("data.writeInt(0)");
            methodBuilder.endControlFlow();
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
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
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
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
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, methodBuilder);
        if (param.type instanceof ArrayTypeName) {
            logError("CharSequence[] cannot be marshalled");
        } else {
            methodBuilder.beginControlFlow("if(data.readInt() != 0)");
            methodBuilder.addStatement("$L = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data)", param.name);
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("$L = null", param.name);
            methodBuilder.endControlFlow();
        }
    }


}
