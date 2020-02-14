package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;

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
    public void writeParamsToProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            logError("Map[] is not supported");
        } else {
            if (paramType != ParamType.OUT) {
                methodBuilder.addStatement("data.writeMap(" + param.getSimpleName() + ")");
            }
        }
    }

    @Override
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            logError("Map[] is not supported");
        } else {
            methodBuilder.addStatement("reply.writeMap(result)");
        }
    }

    @Override
    public void readOutResultsFromStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("reply.writeMap(" + paramName + ")");
    }


    @Override
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            logError("Map[] is not supported");
        } else {
            methodBuilder.addStatement("result = reply.readHashMap(getClass().getClassLoader())");
        }
    }

    @Override
    public void writeParamsToStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, paramName, methodBuilder);
        if (param.asType().getKind() == TypeKind.ARRAY) {
            logError("Map[] is not supported");
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement(paramName + " = new $T()", HashMap.class);
            } else {
                methodBuilder.addStatement(paramName + " = data.readHashMap(getClass().getClassLoader())");
            }
        }
    }

    @Override
    public void readOutParamsFromProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            methodBuilder.beginControlFlow("if (" + param.getSimpleName() +" != null)");
            methodBuilder.addStatement(param.getSimpleName() +".clear()");
            methodBuilder.addStatement("reply.readMap(" + param.getSimpleName() + ", getClass().getClassLoader())");
            methodBuilder.endControlFlow();
        }
    }
}
