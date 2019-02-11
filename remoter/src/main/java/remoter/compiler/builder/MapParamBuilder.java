package remoter.compiler.builder;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import java.util.HashMap;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link ParamBuilder} for Map type parameters
 */
class MapParamBuilder extends ParamBuilder {


    protected MapParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }


    @Override
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            logError("Map[] is not supported");
        } else {
            if (paramType != ParamType.OUT) {
                methodBuilder.addStatement("data.writeMap($L)", param.name);
            }
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            logError("Map[] is not supported");
        } else {
            methodBuilder.addStatement("reply.writeMap(result)");
        }
    }

    @Override
    public void readOutResultsFromStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("reply.writeMap($L)", param.name);
    }


    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            logError("Map[] is not supported");
        } else {
            methodBuilder.addStatement("result = reply.readHashMap(getClass().getClassLoader())");
        }
    }

    @Override
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, methodBuilder);
        if (param.type instanceof ArrayTypeName) {
            logError("Map[] is not supported");
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement("$L = new $T()", param.name, HashMap.class);
            } else {
                methodBuilder.addStatement("$L = data.readHashMap(getClass().getClassLoader())", param.name);
            }
        }
    }

    @Override
    public void readOutParamsFromProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("reply.readMap($L, getClass().getClassLoader())", param.name);
        }
    }
}
