package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;

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
    public void writeParamsToProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder);
            } else {
                methodBuilder.addStatement("data.writeDoubleArray(" + param.getSimpleName() + ")");
            }
        } else {
            methodBuilder.addStatement("data.writeDouble(" + param.getSimpleName() + ")");
        }
    }

    @Override
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            methodBuilder.addStatement("reply.writeDoubleArray(result)");
        } else {
            methodBuilder.addStatement("reply.writeDouble(result)");
        }
    }


    @Override
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            methodBuilder.addStatement("result = reply.createDoubleArray()");
        } else {
            methodBuilder.addStatement("result = reply.readDouble()");
        }
    }

    @Override
    public void readOutResultsFromStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            methodBuilder.addStatement("reply.writeDoubleArray(" + paramName + ")");
        }
    }

    @Override
    public void writeParamsToStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, paramName, methodBuilder);
        if (param.asType().getKind() == TypeKind.ARRAY) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, paramName, methodBuilder);
            } else {
                methodBuilder.addStatement(paramName + " = data.createDoubleArray()");
            }
        } else {
            methodBuilder.addStatement(paramName + " = data.readDouble()");
        }
    }

    @Override
    public void readOutParamsFromProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY && paramType != ParamType.IN) {
            methodBuilder.addStatement("reply.readDoubleArray(" + param.getSimpleName() + ")");
        }
    }

}
