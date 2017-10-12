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
        methodBuilder.addStatement("Class pClass"+ param.getSimpleName()+" = getParcelerClass(" + param.getSimpleName() + ")");
        methodBuilder.beginControlFlow("if (pClass"+ param.getSimpleName()+" != null)");
        methodBuilder.addStatement("data.writeInt(1)");
        methodBuilder.addStatement("data.writeString(pClass"+ param.getSimpleName()+".getName())");
        methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass"+ param.getSimpleName()+", " + param.getSimpleName() + ").writeToParcel(data, 0)");
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("data.writeInt(2)");
        methodBuilder.addStatement("data.writeValue(" + param.getSimpleName() + " )");
        methodBuilder.endControlFlow();
    }

    @Override
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
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
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if (reply.readInt() == 1)");
        methodBuilder.addStatement("result = ($T)getParcelerObject(reply.readString(), reply)", resultType);
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("result = (" + resultType.toString() + ")reply.readValue(getClass().getClassLoader())");
        methodBuilder.endControlFlow();
    }

    @Override
    public void writeParamsToStub(VariableElement param, ParamType pType, String paramName, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, pType, paramName, methodBuilder);
        String paramType = param.asType().toString();

        methodBuilder.beginControlFlow("if (data.readInt() == 1)");
        methodBuilder.addStatement(paramName + " = ($T)getParcelerObject(data.readString(), data)", param.asType());
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement(paramName + " = (" + paramType + ")data.readValue(getClass().getClassLoader())");
        methodBuilder.endControlFlow();
    }
}
