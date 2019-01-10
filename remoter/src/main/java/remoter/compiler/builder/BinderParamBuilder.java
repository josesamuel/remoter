package remoter.compiler.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

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
    public void writeParamsToProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            TypeMirror type = ((ArrayType) param.asType()).getComponentType();
            methodBuilder.beginControlFlow("if(" + param.getSimpleName() + " == null)")
                    .addStatement("data.writeInt(-1)")
                    .nextControlFlow("else")
                    .addStatement("data.writeInt(" + param.getSimpleName() + ".length)")
                    .beginControlFlow("for($T item : " + param.getSimpleName() + ")", type)
                    .addStatement("data.writeStrongBinder(item != null ? new $T(item)  : null)",
                            getStubClassName(type))
                    .endControlFlow()
                    .endControlFlow();
        } else {
            methodBuilder.addStatement("data.writeStrongBinder(" + param.getSimpleName() + " != null ? new $T(" + param.getSimpleName() + ")  : null)",
                    getStubClassName(param.asType()));
        }
    }

    @Override
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            TypeMirror type = ((ArrayType) resultType).getComponentType();
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
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            TypeMirror type = ((ArrayType) resultType).getComponentType();
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
    public void readOutResultsFromStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
    }

    @Override
    public void writeParamsToStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, paramName, methodBuilder);
        if (param.asType().getKind() == TypeKind.ARRAY) {
            TypeMirror type = ((ArrayType) param.asType()).getComponentType();
            methodBuilder.addStatement("int length = data.readInt()")
                    .beginControlFlow("if(length == -1)")
                    .addStatement(paramName + " = null")
                    .nextControlFlow("else")
                    .addStatement(paramName + " = new $T[length]", type)
                    .beginControlFlow("for(int i = 0; i < length; i++)")
                    .addStatement(paramName + "[i] = new $T(reply.readStrongBinder())", getProxyClassName(type))
                    .endControlFlow()
                    .endControlFlow();
        } else {
            methodBuilder.addStatement(paramName + " = new $T(data.readStrongBinder())", getProxyClassName(param.asType()));
        }
    }

    @Override
    public void readOutParamsFromProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
    }

    /**
     * Returns the {@link ClassName} for the Stub
     */
    private ClassName getStubClassName(TypeMirror param) {
        Element element = getBindingManager().getElement(param.toString());
        return ClassName.get(getPackage(element).getQualifiedName().toString(), element.getSimpleName() + ClassBuilder.STUB_SUFFIX);
    }

    /**
     * Returns the {@link ClassName} for the Proxy
     */
    private ClassName getProxyClassName(TypeMirror param) {
        Element element = getBindingManager().getElement(param.toString());
        return ClassName.get(getPackage(element).getQualifiedName().toString(), element.getSimpleName() + ClassBuilder.PROXY_SUFFIX);
    }

}
