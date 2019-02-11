package remoter.compiler.builder;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static com.google.auto.common.MoreElements.getPackage;

/**
 * A {@link ParamBuilder} for Binder type parameters
 */
class BinderParamBuilder extends ParamBuilder {

    /**
     * Initialize the builder
     */
    protected BinderParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }


    @Override
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            TypeName type = ((ArrayTypeName) param.type).componentType;
            methodBuilder.beginControlFlow("if($L == null)", param.name)
                    .addStatement("data.writeInt(-1)")
                    .nextControlFlow("else")
                    .addStatement("data.writeInt($L.length)", param.name)
                    .beginControlFlow("for($T item : $L)", type, param.name)
                    .addStatement("data.writeStrongBinder(item != null ? new $T(item) : null)", getStubClassName(type))
                    .endControlFlow()
                    .endControlFlow();
        } else {
            methodBuilder.addStatement("data.writeStrongBinder($1L != null ? new $2T($1L) : null)", param.name, getStubClassName(param.type));
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            TypeName type = ((ArrayTypeName) resultType).componentType;
            methodBuilder.beginControlFlow("if(result == null)")
                    .addStatement("data.writeInt(-1)")
                    .nextControlFlow("else")
                    .addStatement("data.writeInt(result.length)")
                    .beginControlFlow("for($T item : result)", type)
                    .addStatement("data.writeStrongBinder(item != null ? new $T(item)  : null)",
                            getStubClassName(type))
                    .endControlFlow()
                    .endControlFlow();
        } else {
            methodBuilder.addStatement("reply.writeStrongBinder(result != null ? new $T(result)  : null)",
                    getStubClassName(resultType));

        }
    }


    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            TypeName type = ((ArrayTypeName) resultType).componentType;
            methodBuilder.addStatement("int length = reply.readInt()")
                    .beginControlFlow("if(length == -1)")
                    .addStatement("result = null")
                    .nextControlFlow("else")
                    .addStatement("result = new $T[length]", type)
                    .beginControlFlow("for(int i = 0; i < length; i++)")
                    .addStatement("result[i] = new $T(reply.readStrongBinder())", getProxyClassName(type))
                    .endControlFlow()
                    .endControlFlow();
        } else {
            methodBuilder.addStatement("result = new $T(reply.readStrongBinder())", getProxyClassName(resultType));
        }
    }

    @Override
    public void readOutResultsFromStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
    }

    @Override
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, methodBuilder);
        if (param.type instanceof ArrayTypeName) {
            TypeName type = ((ArrayTypeName) param.type).componentType;
            methodBuilder.addStatement("int length = data.readInt()")
                    .beginControlFlow("if(length == -1)")
                    .addStatement("$L = null", param.name)
                    .nextControlFlow("else")
                    .addStatement("$L = new $T[length]", param.name, type)
                    .beginControlFlow("for(int i = 0; i < length; i++)")
                    .addStatement("$L[i] = new $T(reply.readStrongBinder())", param.name, getProxyClassName(type))
                    .endControlFlow()
                    .endControlFlow();
        } else {
            methodBuilder.addStatement("$L = new $T(data.readStrongBinder())", param.name, getProxyClassName(param.type));
        }
    }

    @Override
    public void readOutParamsFromProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Returns the {@link ClassName} for the Stub
     */
    private ClassName getStubClassName(TypeName param) {
        Element element = getBindingManager().getElement(param.toString());
        return ClassName.get(getPackage(element).getQualifiedName().toString(), element.getSimpleName() + ClassBuilder.STUB_SUFFIX);
    }

    /**
     * Returns the {@link ClassName} for the Proxy
     */
    private ClassName getProxyClassName(TypeName param) {
        Element element = getBindingManager().getElement(param.toString());
        return ClassName.get(getPackage(element).getQualifiedName().toString(), element.getSimpleName() + ClassBuilder.PROXY_SUFFIX);
    }

}
