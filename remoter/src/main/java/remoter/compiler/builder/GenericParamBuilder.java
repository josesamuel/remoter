package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

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
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("Class pClass$1L = getParcelerClass($1L)", param.name);
        methodBuilder.beginControlFlow("if (pClass$L != null)", param.name);
        methodBuilder.addStatement("data.writeInt(1)");
        methodBuilder.addStatement("data.writeString(pClass$L.getName())", param.name);
        methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass$1L, $1L).writeToParcel(data, 0)", param.name);
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("data.writeInt(2)");
        methodBuilder.addStatement("data.writeValue($L)", param.name);
        methodBuilder.endControlFlow();
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("Class pClassResult = getParcelerClass(result)");
        methodBuilder.beginControlFlow("if (pClassResult != null)");
        methodBuilder.addStatement("reply.writeInt(1)");
        methodBuilder.addStatement("reply.writeString(pClassResult.getName())");
        methodBuilder.addStatement("org.parceler.Parcels.wrap(pClassResult, result).writeToParcel(reply, 0)");
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("reply.writeInt(2)");
        methodBuilder.addStatement("reply.writeValue(result)");
        methodBuilder.endControlFlow();
    }


    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if (reply.readInt() == 1)");
        methodBuilder.addStatement("result = ($T)getParcelerObject(reply.readString(), reply)", resultType);
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("result = (" + resultType.toString() + ")reply.readValue(getClass().getClassLoader())");
        methodBuilder.endControlFlow();
    }

    @Override
    public void writeParamsToStub(ParameterSpec param, ParamType pType, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, pType, methodBuilder);
        methodBuilder.beginControlFlow("if (data.readInt() == 1)");
        methodBuilder.addStatement("$L = ($T)getParcelerObject(data.readString(), data)", param.name, param.type);
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("$L = ($T)data.readValue(getClass().getClassLoader())", param.name, param.type);
        methodBuilder.endControlFlow();
    }
}
