package remoter.compiler.builder;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

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
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            logError("List[] is not supported");
        } else {
            if (paramType != ParamType.OUT) {
                if (isListOfStrings(param.type)) {
                    methodBuilder.addStatement("data.writeStringList($L)", param.name);
                } else {
                    methodBuilder.addStatement("data.writeList($L)", param.name);
                }
            }
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
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
    public void readOutResultsFromStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (isListOfStrings(param.type)) {
            methodBuilder.addStatement("reply.writeStringList($L)", param.name);
        } else {
            methodBuilder.addStatement("reply.writeList($L)", param.name);
        }
    }


    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
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
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, methodBuilder);
        if (param.type instanceof ArrayTypeName) {
            logError("List[] is not supported");
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement("$L = new $T()", param.name, ArrayList.class);
            } else {
                if (isListOfStrings(param.type)) {
                    methodBuilder.addStatement("$L = data.createStringArrayList()", param.name);
                } else {
                    methodBuilder.addStatement("$L = data.readArrayList(getClass().getClassLoader())", param.name);
                }
            }
        }
    }

    @Override
    public void readOutParamsFromProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            if (isListOfStrings(param.type)) {
                methodBuilder.addStatement("reply.readStringList($L)", param.name);
            } else {
                methodBuilder.addStatement("reply.readList($L, getClass().getClassLoader())", param.name);
            }
        }
    }

    private boolean isListOfStrings(TypeName typeMirror) {
        return typeMirror.toString().equals("java.util.List<java.lang.String>");
    }

}
