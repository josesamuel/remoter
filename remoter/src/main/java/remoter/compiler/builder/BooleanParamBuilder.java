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
 * A {@link ParamBuilder} for boolean type parameters
 */
class BooleanParamBuilder extends ParamBuilder {


    protected BooleanParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }


    @Override
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder);
            } else {
                methodBuilder.addStatement("data.writeBooleanArray($L)", param.name);
            }
        } else {
            methodBuilder.addStatement("data.writeInt($L ? 1 : 0 )", param.name);
        }
    }

    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.addStatement("result = reply.createBooleanArray()");
        } else {
            methodBuilder.addStatement("result = reply.readInt() == 1");
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.addStatement("reply.writeBooleanArray(result)");
        } else {
            methodBuilder.addStatement("reply.writeInt(result ? 1 : 0 )");
        }
    }

    @Override
    public void readOutResultsFromStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            methodBuilder.addStatement("reply.writeBooleanArray($L)", param.name);
        }
    }


    @Override
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, methodBuilder);
        if (param.type instanceof ArrayTypeName) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, methodBuilder);
            } else {
                methodBuilder.addStatement("$L = data.createBooleanArray()", param.name);
            }
        } else {
            methodBuilder.addStatement("$L = 0 != data.readInt()", param.name);
        }
    }

    @Override
    public void readOutParamsFromProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName && paramType != ParamType.IN) {
            methodBuilder.addStatement("reply.readBooleanArray($L)", param.name);
        }
    }


}
