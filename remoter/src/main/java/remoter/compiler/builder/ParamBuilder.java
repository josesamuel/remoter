package remoter.compiler.builder;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

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
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Called to generate code that reads results from proxy
     */
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Called to generate code that reads results from proxy for @{@link remoter.annotations.ParamOut} parameters
     */
    public void readOutParamsFromProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Called to generate code to write params for stub
     */
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$L" , param);
    }

    /**
     * Called to generate code to write @{@link remoter.annotations.ParamOut} params for stub
     */
    public void writeOutParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("int $L_length = data.readInt()", param.name);
            methodBuilder.beginControlFlow("if ($L_length < 0 )", param.name);
            methodBuilder.addStatement("$L = null", param.name);
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("$1L = new $2T[$1L_length]", param.name, ((ArrayTypeName) param.type).componentType);
            methodBuilder.endControlFlow();
        }
    }

    /**
     * Called to generate code that reads results from stub
     */
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Called to generate code that reads  {@link remoter.annotations.ParamOut} results from stub
     */
    public void readOutResultsFromStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Called to generate code that writes the out params for array type
     */
    protected void writeArrayOutParamsToProxy(ParameterSpec param, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if ($L == null)", param.name);
        methodBuilder.addStatement("data.writeInt(-1)");
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("data.writeInt($L.length)", param.name);
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
