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
 * A {@link ParamBuilder} for short type parameters
 */
class ShortParamBuilder extends ParamBuilder {


    protected ShortParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }

    @Override
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            logError("short[] not supported, use int[]" + param.name);
        } else {
            methodBuilder.addStatement("data.writeInt((int)$L)", param.name);
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            logError("short[] not supported, use int[]");
        } else {
            methodBuilder.addStatement("reply.writeInt(result)");
        }
    }

    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            logError("short[] is not supported, use int[]");
            methodBuilder.addStatement("result = null");
        } else {
            methodBuilder.addStatement("result = (short)reply.readInt()");
        }
    }


    @Override
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, methodBuilder);
        if (param.type instanceof ArrayTypeName) {
            logError("short[] not supported, use int[]" + param.name);
        } else {
            methodBuilder.addStatement("$L = (short)data.readInt()", param.name);
        }
    }

}
