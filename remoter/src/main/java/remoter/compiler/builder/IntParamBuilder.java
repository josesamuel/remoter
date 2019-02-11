package remoter.compiler.builder;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

/**
 * A {@link ParamBuilder} for int type parameters
 */
class IntParamBuilder extends ParamBuilder {


    protected IntParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }

    @Override
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder);
            } else {
                methodBuilder.addStatement("data.writeIntArray($L)", param.name);
            }
        } else {
            methodBuilder.addStatement("data.writeInt($L)", param.name);
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.addStatement("reply.writeIntArray(result)");
        } else {
            methodBuilder.addStatement("reply.writeInt(result)");
        }
    }


    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.addStatement("result = reply.createIntArray()");
        } else {
            methodBuilder.addStatement("result = reply.readInt()");
        }
    }

    @Override
    public void readOutResultsFromStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            methodBuilder.addStatement("reply.writeIntArray($L)", param.name);
        }
    }

    @Override
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, methodBuilder);
        if (param.type instanceof ArrayTypeName) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, methodBuilder);
            } else {
                methodBuilder.addStatement("$L = data.createIntArray()", param.name);
            }
        } else {
            methodBuilder.addStatement("$L = data.readInt()", param.name);
        }
    }

    @Override
    public void readOutParamsFromProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName && paramType != ParamType.IN) {
            methodBuilder.addStatement("reply.readIntArray($L)", param.name);
        }
    }

}
