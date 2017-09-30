package remoter.compiler.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import remoter.annotations.Oneway;
import remoter.annotations.ParamIn;
import remoter.annotations.ParamOut;

/**
 * A {@link RemoteBuilder} that knows how to generate methods for Stub and Proxy
 */
class MethodBuilder extends RemoteBuilder {


    protected MethodBuilder(Messager messager, Element element) {
        super(messager, element);
    }

    /**
     * Build the proxy methods
     */
    public void addProxyMethods(TypeSpec.Builder classBuilder) {

        int methodIndex = 0;
        for (Element member : getRemoterInterfaceElement().getEnclosedElements()) {
            if (member.getKind() == ElementKind.METHOD) {
                ExecutableElement executableElement = (ExecutableElement) member;
                String methodName = executableElement.getSimpleName().toString();
                boolean isOnewayAnnotated = member.getAnnotation(Oneway.class) != null;
                boolean isOneWay = executableElement.getReturnType().getKind() == TypeKind.VOID
                        && isOnewayAnnotated;

                if (!isOneWay && isOnewayAnnotated) {
                    logWarning("@Oneway is expected only for methods with void return. Ignoring it for " + member.getSimpleName());
                }


                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(TypeName.get(executableElement.getReturnType()));

                //add Exceptions
                for (TypeMirror exceptions : executableElement.getThrownTypes()) {
                    methodBuilder.addException(ClassName.bestGuess(exceptions.toString()));
                }

                //add parameters
                for (VariableElement params : executableElement.getParameters()) {
                    methodBuilder.addParameter(TypeName.get(params.asType()), params.getSimpleName().toString());
                }

                //add statements
                methodBuilder
                        .addStatement("android.os.Parcel data = android.os.Parcel.obtain()");

                if (!isOneWay) {
                    methodBuilder.addStatement("android.os.Parcel reply = android.os.Parcel.obtain()");
                }

                //add return if any
                if (executableElement.getReturnType().getKind() != TypeKind.VOID) {
                    TypeElement genericParceler = getBindingManager().getGenericType(executableElement.getReturnType());
                    if (genericParceler != null) {
                        methodBuilder.addStatement("$T<$T> result", List.class, genericParceler);
                    } else {
                        methodBuilder.addStatement(executableElement.getReturnType().toString() + " result");
                    }
                }

                //start main body block
                methodBuilder.beginControlFlow("try");

                //write the descriptor
                methodBuilder.addStatement("data.writeInterfaceToken(DESCRIPTOR)");

                List<VariableElement> outParams = new ArrayList<>();

                //pass parameters
                for (VariableElement param : executableElement.getParameters()) {
                    ParamBuilder.ParamType paramType = param.getAnnotation(ParamIn.class) != null ? ParamBuilder.ParamType.IN
                            : param.getAnnotation(ParamOut.class) != null ? ParamBuilder.ParamType.OUT : ParamBuilder.ParamType.IN_OUT;

                    if (paramType != ParamBuilder.ParamType.IN) {
                        outParams.add(param);
                    }
                    ParamBuilder paramBuilder = getBindingManager().getBuilderForParam(param.asType());
                    if (paramBuilder != null) {
                        paramBuilder.writeParamsToProxy(param, paramType, methodBuilder);
                    } else {
                        logError("Parameter cannot be marshalled " + param.getSimpleName());
                    }
                }

                //send remote command
                if (isOneWay) {
                    methodBuilder.addStatement("mRemote.transact(TRANSACTION_" + methodName + "_" + methodIndex + ", data, null, android.os.IBinder.FLAG_ONEWAY)");
                } else {
                    methodBuilder.addStatement("mRemote.transact(TRANSACTION_" + methodName + "_" + methodIndex + ", data, reply, 0)");
                    //read exception if any
                    methodBuilder.addStatement("reply.readException()");


                    //read result
                    if (executableElement.getReturnType().getKind() != TypeKind.VOID) {
                        ParamBuilder paramBuilder = getBindingManager().getBuilderForParam(executableElement.getReturnType());
                        if (paramBuilder != null) {
                            paramBuilder.readResultsFromProxy(executableElement.getReturnType(), methodBuilder);
                        } else {
                            logError("Unmarshellable return type " + executableElement.getReturnType());
                            methodBuilder.addStatement("result = null");
                        }
                    }

                    for (VariableElement param : outParams) {
                        ParamBuilder.ParamType paramType = param.getAnnotation(ParamIn.class) != null ? ParamBuilder.ParamType.IN
                                : param.getAnnotation(ParamOut.class) != null ? ParamBuilder.ParamType.OUT : ParamBuilder.ParamType.IN_OUT;
                        ParamBuilder paramBuilder = getBindingManager().getBuilderForParam(param.asType());
                        if (paramBuilder != null) {
                            paramBuilder.readOutParamsFromProxy(param, paramType, methodBuilder);
                        }
                    }
                }

                //end of try
                methodBuilder.endControlFlow();


                //catch rethrow
                methodBuilder.beginControlFlow("catch ($T re)", ClassName.get("android.os", "RemoteException"));
                methodBuilder.addStatement("throw new $T(re)", RuntimeException.class);
                methodBuilder.endControlFlow();


                //finally block
                methodBuilder.beginControlFlow("finally");
                if (!isOneWay) {
                    methodBuilder.addStatement("reply.recycle()");
                }
                methodBuilder.addStatement("data.recycle()");
                methodBuilder.endControlFlow();
                if (executableElement.getReturnType().getKind() != TypeKind.VOID) {
                    methodBuilder.addStatement("return result");
                }

                classBuilder.addMethod(methodBuilder.build());
                methodIndex++;
            }
        }

        addProxyExtras(classBuilder);
    }

    /**
     * Build the stub methods
     */
    public void addStubMethods(TypeSpec.Builder classBuilder) {

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("onTransact")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(boolean.class)
                .addException(ClassName.get("android.os", "RemoteException"))
                .addParameter(int.class, "code")
                .addParameter(ClassName.get("android.os", "Parcel"), "data")
                .addParameter(ClassName.get("android.os", "Parcel"), "reply")
                .addParameter(int.class, "flags");

        methodBuilder.beginControlFlow("try");
        methodBuilder.beginControlFlow("switch (code)");

        methodBuilder.beginControlFlow("case INTERFACE_TRANSACTION:");
        methodBuilder.addStatement("reply.writeString(DESCRIPTOR)");
        methodBuilder.addStatement("return true");
        methodBuilder.endControlFlow();

        int methodIndex = 0;
        for (Element member : getRemoterInterfaceElement().getEnclosedElements()) {
            if (member.getKind() == ElementKind.METHOD) {
                ExecutableElement executableElement = (ExecutableElement) member;
                String methodName = executableElement.getSimpleName().toString();
                boolean isOneWay = executableElement.getReturnType().getKind() == TypeKind.VOID
                        && member.getAnnotation(Oneway.class) != null;


                methodBuilder.beginControlFlow("case TRANSACTION_" + methodName + "_" + methodIndex + ":");

                methodBuilder.addStatement("data.enforceInterface(DESCRIPTOR)");
                List<String> paramNames = new ArrayList<>();
                int paramIndex = 0;
                List<VariableElement> outParams = new ArrayList<>();
                List<String> outParamsNames = new ArrayList<>();

                //pass parameters
                for (VariableElement param : executableElement.getParameters()) {
                    ParamBuilder.ParamType paramType = param.getAnnotation(ParamIn.class) != null ? ParamBuilder.ParamType.IN
                            : param.getAnnotation(ParamOut.class) != null ? ParamBuilder.ParamType.OUT : ParamBuilder.ParamType.IN_OUT;

                    ParamBuilder paramBuilder = getBindingManager().getBuilderForParam(param.asType());
                    if (paramBuilder != null) {
                        String paramName = "arg_stb_" + paramIndex;
                        paramNames.add(paramName);
                        paramBuilder.writeParamsToStub(param, paramType, paramName, methodBuilder);
                        if (paramType != ParamBuilder.ParamType.IN) {
                            outParams.add(param);
                            outParamsNames.add(paramName);
                        }
                    } else {
                        logError("Parameter cannot be marshalled " + param.getSimpleName());
                    }
                    paramIndex++;
                }

                String methodCall = "serviceImpl." + methodName + "(";
                int paramSize = paramNames.size();
                for (int paramCount = 0; paramCount < paramSize; paramCount++) {
                    methodCall += paramNames.get(paramCount);
                    if (paramCount < paramSize - 1) {
                        methodCall += ", ";
                    }
                }
                methodCall += ")";

                if (executableElement.getReturnType().getKind() != TypeKind.VOID) {
                    methodBuilder.addStatement("$T result = " + methodCall, executableElement.getReturnType());
                } else {
                    methodBuilder.addStatement(methodCall);
                }

                if (!isOneWay) {
                    methodBuilder.addStatement("reply.writeNoException()");


                    if (executableElement.getReturnType().getKind() != TypeKind.VOID) {
                        ParamBuilder paramBuilder = getBindingManager().getBuilderForParam(executableElement.getReturnType());
                        if (paramBuilder != null) {
                            paramBuilder.readResultsFromStub(executableElement.getReturnType(), methodBuilder);
                        } else {
                            logError("Unmarshallable return type " + executableElement.getReturnType());
                        }
                    }

                    int pIndex = 0;
                    for (VariableElement param : outParams) {
                        ParamBuilder.ParamType paramType = param.getAnnotation(ParamIn.class) != null ? ParamBuilder.ParamType.IN
                                : param.getAnnotation(ParamOut.class) != null ? ParamBuilder.ParamType.OUT : ParamBuilder.ParamType.IN_OUT;
                        ParamBuilder paramBuilder = getBindingManager().getBuilderForParam(param.asType());
                        if (paramBuilder != null) {
                            paramBuilder.readOutResultsFromStub(param, paramType, outParamsNames.get(pIndex), methodBuilder);
                        }
                        pIndex++;
                    }
                }

                methodBuilder.addStatement("return true");
                methodBuilder.endControlFlow();


                methodIndex++;
            }
        }

        //end switch
        methodBuilder.endControlFlow();
        //end of try
        methodBuilder.endControlFlow();
        //catch rethrow
        methodBuilder.beginControlFlow("catch ($T re)", Exception.class);
        methodBuilder.addStatement("throw new $T(re.getMessage())", ClassName.get("android.os", "RemoteException"));
        methodBuilder.endControlFlow();


        methodBuilder.addStatement("return super.onTransact(code, data, reply, flags)");
        classBuilder.addMethod(methodBuilder.build());
    }


    /**
     * Add other extra methods
     */
    private void addProxyExtras(TypeSpec.Builder classBuilder) {
        addProxyDeathMethod(classBuilder, "linkToDeath", "Register a {@link android.os.IBinder.DeathRecipient} to know of binder connection lose\n");
        addProxyDeathMethod(classBuilder, "unlinkToDeath", "UnRegisters a {@link android.os.IBinder.DeathRecipient}\n");
        addProxyRemoteAlive(classBuilder);
    }

    /**
     * Add proxy method that exposes the linkToDeath
     */
    private void addProxyDeathMethod(TypeSpec.Builder classBuilder, String deathMethod, String doc) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(deathMethod)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(ClassName.get("android.os", "IBinder.DeathRecipient"), "deathRecipient")
                .beginControlFlow("try")
                .addStatement("mRemote." + deathMethod + "(deathRecipient, 0)")
                .endControlFlow()
                .beginControlFlow("catch ($T ignored)", Exception.class)
                .endControlFlow()
                .addJavadoc(doc);
        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Add proxy method that exposes whether remote is alive
     */
    private void addProxyRemoteAlive(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("isRemoteAlive")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addStatement("boolean alive = false")
                .beginControlFlow("try")
                .addStatement("alive = mRemote.isBinderAlive()")
                .endControlFlow()
                .beginControlFlow("catch ($T ignored)", Exception.class)
                .endControlFlow()
                .addStatement("return alive")
                .addJavadoc("Checks whether the remote process is alive\n");
        classBuilder.addMethod(methodBuilder.build());
    }

}
