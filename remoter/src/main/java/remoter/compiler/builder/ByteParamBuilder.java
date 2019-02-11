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
 * A {@link ParamBuilder} for byte type parameters
 */
class ByteParamBuilder extends ParamBuilder {


    protected ByteParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }

    @Override
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder);
            } else {
                methodBuilder.addStatement("data.writeByteArray($L)", param.name);
            }
        } else {
            methodBuilder.addStatement("data.writeByte($L)", param.name);
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.addStatement("reply.writeByteArray(result)");
        } else {
            methodBuilder.addStatement("reply.writeByte(result)");
        }
    }


    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.addStatement("result = reply.createByteArray()");
        } else {
            methodBuilder.addStatement("result = reply.readByte()");
        }
    }

    @Override
    public void readOutParamsFromProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName && paramType != ParamType.IN) {
            methodBuilder.addStatement("reply.readByteArray($L)", param.name);
        }
    }

    @Override
    public void readOutResultsFromStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            methodBuilder.addStatement("reply.writeByteArray($L)", param.name);
        }
    }

    @Override
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, methodBuilder);
        if (param.type instanceof ArrayTypeName) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, methodBuilder);
            } else {
                methodBuilder.addStatement("$L = data.createByteArray()", param.name);
            }
        } else {
            methodBuilder.addStatement("$L = data.readByte()", param.name);
        }
    }
}
