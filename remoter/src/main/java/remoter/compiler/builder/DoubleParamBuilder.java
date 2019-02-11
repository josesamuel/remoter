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
 * A {@link ParamBuilder} for double type parameters
 */
class DoubleParamBuilder extends ParamBuilder {


    protected DoubleParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }

    @Override
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder);
            } else {
                methodBuilder.addStatement("data.writeDoubleArray($L)", param.name);
            }
        } else {
            methodBuilder.addStatement("data.writeDouble($L)", param.name);
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.addStatement("reply.writeDoubleArray(result)");
        } else {
            methodBuilder.addStatement("reply.writeDouble(result)");
        }
    }


    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.addStatement("result = reply.createDoubleArray()");
        } else {
            methodBuilder.addStatement("result = reply.readDouble()");
        }
    }

    @Override
    public void readOutResultsFromStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            methodBuilder.addStatement("reply.writeDoubleArray($L)", param.name);
        }
    }

    @Override
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, methodBuilder);
        if (param.type instanceof ArrayTypeName) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, methodBuilder);
            } else {
                methodBuilder.addStatement("$L = data.createDoubleArray()", param.name);
            }
        } else {
            methodBuilder.addStatement("$L = data.readDouble()", param.name);
        }
    }

    @Override
    public void readOutParamsFromProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName && paramType != ParamType.IN) {
            methodBuilder.addStatement("reply.readDoubleArray($L)", param.name);
        }
    }

}
