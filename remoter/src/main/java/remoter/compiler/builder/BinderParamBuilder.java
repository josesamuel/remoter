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

            String binderName = param.getSimpleName() +"_binder";
            methodBuilder.addStatement("IBinder "+ binderName +" = null");
            methodBuilder.beginControlFlow("if (" + param.getSimpleName()  + " != null)");
            methodBuilder.beginControlFlow("synchronized (stubMap)");
            methodBuilder.addStatement(binderName + " = stubMap.get(" + param.getSimpleName()  +")");
            methodBuilder.beginControlFlow("if (" + binderName + " == null)");
            methodBuilder.addStatement(binderName + " = new $T("+ param.getSimpleName() +")", getStubClassName(param.asType()));
            methodBuilder.addStatement("stubMap.put(" +  param.getSimpleName() + ", " + binderName +")");
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();

            methodBuilder.addStatement("data.writeStrongBinder(" + binderName + ")");
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
            String binderName = "result_binder";
            methodBuilder.addStatement("IBinder "+ binderName +" = reply.readStrongBinder()");
            methodBuilder.addStatement("result = null");
            methodBuilder.beginControlFlow("if("+ binderName +" != null)");
            methodBuilder.addStatement("result = new $T("+ binderName +")", getProxyClassName(resultType));
            methodBuilder.addStatement("(($T)result).setRemoterGlobalProperties(__global_properties)", getProxyClassName(resultType));
            methodBuilder.endControlFlow();
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
                    .addStatement(paramName + "[i] = new $T(data.readStrongBinder())", getProxyClassName(type))
                    .endControlFlow()
                    .endControlFlow();
        } else {
            String binderName = paramName +"_binder";
            methodBuilder.addStatement("IBinder "+ binderName +" = data.readStrongBinder()");
            methodBuilder.addStatement(paramName + " = null");
            methodBuilder.beginControlFlow("if("+ binderName +" != null)");
            methodBuilder.addStatement(paramName + " = new $T("+ binderName +")", getProxyClassName(param.asType()));
            methodBuilder.endControlFlow();
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
