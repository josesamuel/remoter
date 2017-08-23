package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link RemoteBuilder} for a specific type of a parameter
 */
abstract class ParamBuilder extends RemoteBuilder {

    protected ParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }

    /**
     * Called to generate code to write the params to proxy
     */
    public void writeParamsToProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Called to generate code that reads results from proxy
     */
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Called to generate code that reads results from proxy for @{@link remoter.annotations.ParamOut} parameters
     */
    public void readOutParamsFromProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Called to generate code to write params for stub
     */
    public void writeParamsToStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$T " + paramName, param.asType());
    }

    /**
     * Called to generate code to write @{@link remoter.annotations.ParamOut} params for stub
     */
    public void writeOutParamsToStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("int " + paramName + "_length = data.readInt()");
            methodBuilder.beginControlFlow("if (" + paramName + "_length < 0 )");
            methodBuilder.addStatement(paramName + " = null");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement(paramName + " = new " + (((ArrayType) param.asType()).getComponentType().toString())
                    + "[" + paramName + "_length]");
            methodBuilder.endControlFlow();
        }
    }

    /**
     * Called to generate code that reads results from stub
     */
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Called to generate code that reads  {@link remoter.annotations.ParamOut} results from stub
     */
    public void readOutResultsFromStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Called to generate code that writes the out params for array type
     */
    protected void writeArrayOutParamsToProxy(VariableElement param, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if (" + param.getSimpleName() + " == null)");
        methodBuilder.addStatement("data.writeInt(-1)");
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("data.writeInt(" + param.getSimpleName() + ".length)");
        methodBuilder.endControlFlow();
    }

    /**
     * Represents the type of parameter
     */
    enum ParamType {
        IN,
        OUT,
        IN_OUT
    }

}
