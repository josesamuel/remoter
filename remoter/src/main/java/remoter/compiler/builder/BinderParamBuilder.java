package remoter.compiler.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
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
            logError("Binder[] is not supported");
        } else {
            methodBuilder.addStatement("data.writeStrongBinder(" + param.getSimpleName() + " != null ? new $T(" + param.getSimpleName() + ")  : null)",
                    getStubClassName(param.asType()));
        }
    }

    @Override
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            logError("Binder[] is not supported");
        } else {
            methodBuilder.addStatement("reply.writeStrongBinder(result != null ? new $T(result)  : null)",
                    getStubClassName(resultType));

        }
    }


    @Override
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            logError("Binder[] is not supported");
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
            logError("Binder[] is not supported");
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
