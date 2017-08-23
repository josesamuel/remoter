package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link ParamBuilder} for short type parameters
 */
class ShortParamBuilder extends ParamBuilder {


    protected ShortParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }

    @Override
    public void writeParamsToProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            logError("short[] not supported, use int[]" + param.getSimpleName());
        } else {
            methodBuilder.addStatement("data.writeInt((int)" + param.getSimpleName() + ")");
        }
    }

    @Override
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            logError("short[] not supported, use int[]");
        } else {
            methodBuilder.addStatement("reply.writeInt(result)");
        }
    }

    @Override
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            logError("short[] is not supported, use int[]");
            methodBuilder.addStatement("result = null");
        } else {
            methodBuilder.addStatement("result = (short)reply.readInt()");
        }
    }


    @Override
    public void writeParamsToStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, paramName, methodBuilder);
        if (param.asType().getKind() == TypeKind.ARRAY) {
            logError("short[] not supported, use int[]" + param.getSimpleName());
        } else {
            methodBuilder.addStatement(paramName + " = (short)data.readInt()");
        }
    }

}
