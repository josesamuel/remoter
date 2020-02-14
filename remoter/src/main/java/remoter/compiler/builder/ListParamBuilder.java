package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;

import java.util.ArrayList;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link ParamBuilder} for List type parameters
 */
class ListParamBuilder extends ParamBuilder {


    protected ListParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }


    @Override
    public void writeParamsToProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            logError("List[] is not supported");
        } else {
            if (paramType != ParamType.OUT) {
                if (isListOfStrings(param.asType())) {
                    methodBuilder.addStatement("data.writeStringList(" + param.getSimpleName() + ")");
                } else {
                    methodBuilder.addStatement("data.writeList(" + param.getSimpleName() + ")");
                }
            }
        }
    }

    @Override
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            logError("List[] is not supported");
        } else {
            if (isListOfStrings(resultType)) {
                methodBuilder.addStatement("reply.writeStringList(result)");
            } else {
                methodBuilder.addStatement("reply.writeList(result)");
            }
        }
    }

    @Override
    public void readOutResultsFromStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        if (isListOfStrings(param.asType())) {
            methodBuilder.addStatement("reply.writeStringList(" + paramName + ")");
        } else {
            methodBuilder.addStatement("reply.writeList(" + paramName + ")");
        }
    }


    @Override
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            logError("List[] is not supported");
        } else {
            if (isListOfStrings(resultType)) {
                methodBuilder.addStatement("result = reply.createStringArrayList()");
            } else {
                methodBuilder.addStatement("result = reply.readArrayList(getClass().getClassLoader())");
            }
        }
    }

    @Override
    public void writeParamsToStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, paramName, methodBuilder);
        if (param.asType().getKind() == TypeKind.ARRAY) {
            logError("List[] is not supported");
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement(paramName + " = new $T()", ArrayList.class);
            } else {
                if (isListOfStrings(param.asType())) {
                    methodBuilder.addStatement(paramName + " = data.createStringArrayList()");
                } else {
                    methodBuilder.addStatement(paramName + " = data.readArrayList(getClass().getClassLoader())");
                }
            }
        }
    }

    @Override
    public void readOutParamsFromProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            methodBuilder.beginControlFlow("if (" + param.getSimpleName()  + " != null)");
            if (isListOfStrings(param.asType())) {
                methodBuilder.addStatement("reply.readStringList(" + param.getSimpleName() + ")");
            } else {
                methodBuilder.addStatement("reply.readList(" + param.getSimpleName() + ", getClass().getClassLoader())");
            }
            methodBuilder.endControlFlow();
        }
    }

    private boolean isListOfStrings(TypeMirror typeMirror) {
        return typeMirror.toString().equals("java.util.List<java.lang.String>");
    }

}
