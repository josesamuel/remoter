package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link ParamBuilder} for generic type parameters
 */
class GenericParamBuilder extends ParamBuilder {


    protected GenericParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }

    @Override
    public void writeParamsToProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("data.writeValue(" + param.getSimpleName() + " )");
    }

    @Override
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("reply.writeValue(result)");
    }


    @Override
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("result = (" + resultType.toString() + ")reply.readValue(getClass().getClassLoader())");
    }

    @Override
    public void writeParamsToStub(VariableElement param, ParamType pType, String paramName, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, pType, paramName, methodBuilder);
        String paramType = param.asType().toString();
        methodBuilder.addStatement(paramName + " = (" + paramType + ")data.readValue(getClass().getClassLoader())");
    }
}
