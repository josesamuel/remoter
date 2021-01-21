package remoter.compiler.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import remoter.RemoterGlobalProperties;
import remoter.RemoterProxyListener;
import remoter.RemoterStub;
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
        processRemoterElements(classBuilder, new ElementVisitor() {
            @Override
            public void visitElement(TypeSpec.Builder classBuilder, Element member, int methodIndex, MethodSpec.Builder methodBuilder) {
                addProxyMethods(classBuilder, member, methodIndex);
            }
        }, null);

        addProxyExtras(classBuilder);
        addCommonExtras(classBuilder);
    }

    /**
     * Build the proxy methods
     */
    private void addProxyMethods(TypeSpec.Builder classBuilder, Element member, int methodIndex) {
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
        int paramIndex = 0;
        for (VariableElement params : executableElement.getParameters()) {
            methodBuilder.addParameter(TypeName.get(params.asType()), params.getSimpleName().toString() + "_" + paramIndex);
            paramIndex++;
        }

        methodBuilder
                .addStatement("__checkProxy()");
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
        paramIndex = 0;
        for (VariableElement param : executableElement.getParameters()) {
            ParamBuilder.ParamType paramType = param.getAnnotation(ParamIn.class) != null ? ParamBuilder.ParamType.IN
                    : param.getAnnotation(ParamOut.class) != null ? ParamBuilder.ParamType.OUT : ParamBuilder.ParamType.IN_OUT;

            VariableElement wrappedParam = new VariableElementWrapper(param, paramIndex);
            if (paramType != ParamBuilder.ParamType.IN) {
                outParams.add(wrappedParam);
            }
            ParamBuilder paramBuilder = getBindingManager().getBuilderForParam(param.asType());
            if (paramBuilder != null) {
                paramBuilder.writeParamsToProxy(wrappedParam, paramType, methodBuilder);
            } else {
                logError("Parameter cannot be marshalled " + param.getSimpleName());
            }
            paramIndex ++;
        }

        methodBuilder.addStatement("data.writeMap(__global_properties)");

        //send remote command
        if (isOneWay) {
            methodBuilder.addStatement("mRemote.transact(TRANSACTION_" + methodName + "_" + methodIndex + ", data, null, android.os.IBinder.FLAG_ONEWAY)");
        } else {
            methodBuilder.addStatement("mRemote.transact(TRANSACTION_" + methodName + "_" + methodIndex + ", data, reply, 0)");
            //read exception if any
            methodBuilder.addStatement("Throwable exception = checkException(reply)");
            methodBuilder.beginControlFlow("if(exception != null)");

            for (TypeMirror exceptions : executableElement.getThrownTypes()) {
                ClassName exceptionCName = ClassName.bestGuess(exceptions.toString());
                methodBuilder.beginControlFlow("if(exception instanceof $T)", exceptionCName);
                methodBuilder.addStatement("throw ($T)exception", exceptionCName);
                methodBuilder.endControlFlow();
            }
            methodBuilder.addStatement("throw ($T)exception", RuntimeException.class);
            methodBuilder.endControlFlow();

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
        methodBuilder.beginControlFlow("switch (mapTransactionCode(code))");

        methodBuilder.beginControlFlow("case INTERFACE_TRANSACTION:");
        methodBuilder.addStatement("reply.writeString(DESCRIPTOR)");
        methodBuilder.addStatement("return true");
        methodBuilder.endControlFlow();

        processRemoterElements(classBuilder, new ElementVisitor() {
            @Override
            public void visitElement(TypeSpec.Builder classBuilder, Element member, int methodIndex, MethodSpec.Builder methodBuilder) {
                addStubMethods(classBuilder, member, methodIndex, methodBuilder);
            }
        }, methodBuilder);


        methodBuilder.beginControlFlow("case TRANSACTION__getStubID:");
        methodBuilder.addStatement("data.enforceInterface(DESCRIPTOR)");
        methodBuilder.addStatement("reply.writeNoException()");
        methodBuilder.addStatement("reply.writeInt(serviceImpl.hashCode())");
        methodBuilder.addStatement("return true");
        methodBuilder.endControlFlow();


        methodBuilder.beginControlFlow("case TRANSACTION__getStubProcessID:");
        methodBuilder.addStatement("data.enforceInterface(DESCRIPTOR)");
        methodBuilder.addStatement("reply.writeNoException()");
        methodBuilder.addStatement("reply.writeInt(android.os.Process.myPid())");
        methodBuilder.addStatement("return true");
        methodBuilder.endControlFlow();


        //end switch
        methodBuilder.endControlFlow();
        //end of try
        methodBuilder.endControlFlow();
        //catch rethrow
        methodBuilder.beginControlFlow("catch ($T re)", Throwable.class);
        methodBuilder.beginControlFlow("if ((flags & FLAG_ONEWAY) == 0)");
        methodBuilder.addStatement("reply.setDataPosition(0)");
        methodBuilder.addStatement("reply.writeInt(REMOTER_EXCEPTION_CODE)");
        methodBuilder.addStatement("reply.writeString(re.getMessage())");
        methodBuilder.addStatement("reply.writeSerializable(re)");
        methodBuilder.addStatement("return true");
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("$T.w(serviceImpl.getClass().getName(), \"Binder call failed.\", re)", ClassName.get("android.util", "Log"));
        methodBuilder.addStatement("throw new RuntimeException(re)");
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
        methodBuilder.addStatement("return super.onTransact(code, data, reply, flags)");
        classBuilder.addMethod(methodBuilder.build());
        addCommonExtras(classBuilder);

        addStubExtras(classBuilder);

    }

    /**
     * Called from the {@link RemoteBuilder.ElementVisitor} callback
     */
    private void addStubMethods(TypeSpec.Builder classBuilder, Element member, int methodIndex, MethodSpec.Builder methodBuilder) {
        ExecutableElement executableElement = (ExecutableElement) member;
        String methodName = executableElement.getSimpleName().toString();
        boolean isOneWay = executableElement.getReturnType().getKind() == TypeKind.VOID
                && member.getAnnotation(Oneway.class) != null;


        methodBuilder.beginControlFlow("case TRANSACTION_" + methodName + "_" + methodIndex + ":");

        methodBuilder.addStatement("data.enforceInterface(DESCRIPTOR)");
        methodBuilder.addStatement("onDispatchTransaction(code)");
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

        methodBuilder.addStatement("$T.set(data.readHashMap(getClass().getClassLoader()))", RemoterGlobalProperties.class);

        if (executableElement.getReturnType().getKind() != TypeKind.VOID) {
            methodBuilder.addStatement("$T result = " + methodCall, executableElement.getReturnType());
        } else {
            methodBuilder.addStatement(methodCall);
        }
        methodBuilder.addStatement("RemoterGlobalProperties.reset()");

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
    }


    /**
     * Add common extra methods
     */
    private void addCommonExtras(TypeSpec.Builder classBuilder) {
        addGetParcelClass(classBuilder);
        addGetParcelObject(classBuilder);
    }

    /**
     * getParcelClass method
     */
    private void addGetParcelClass(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getParcelerClass")
                .addModifiers(Modifier.PRIVATE)
                .returns(Class.class)
                .addParameter(Object.class, "object")
                .beginControlFlow("if (object != null)")
                .addStatement("Class objClass = object.getClass()")
                .addStatement("boolean found = false")
                .beginControlFlow("while (!found && objClass != null)")
                .beginControlFlow("try")
                .addStatement("Class.forName(objClass.getName() + \"$$$$Parcelable\")")
                .addStatement("found = true")
                .endControlFlow()
                .beginControlFlow("catch (ClassNotFoundException ignored)")
                .addStatement("objClass = objClass.getSuperclass()")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return objClass")
                .endControlFlow()
                .addStatement("return null");

        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * getParcelObject method
     */
    private void addGetParcelObject(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getParcelerObject")
                .addModifiers(Modifier.PRIVATE)
                .returns(Object.class)
                .addParameter(String.class, "pClassName")
                .addParameter(ClassName.get("android.os", "Parcel"), "data")
                .beginControlFlow("try")
                .addStatement("$T p = null", ClassName.get("android.os", "Parcelable"))
                .addStatement("$T creator  = (Parcelable.Creator)Class.forName(pClassName+\"$$$$Parcelable\").getField(\"CREATOR\").get(null)", ClassName.get("android.os", "Parcelable.Creator"))
                .addStatement("Object pWrapper = creator.createFromParcel(data)")
                .addStatement("return pWrapper.getClass().getMethod(\"getParcel\", (Class[])null).invoke(pWrapper)")
                .endControlFlow()
                .beginControlFlow("catch (Exception ignored)")
                .addStatement("return null")
                .endControlFlow();
        classBuilder.addMethod(methodBuilder.build());
    }


    /**
     * Add other extra methods for stub
     */
    private void addStubExtras(TypeSpec.Builder classBuilder) {
        addSubDestroyMethods(classBuilder);
        addSubInterceptMethods(classBuilder);
        addSubMapTransaction(classBuilder);
    }


    /**
     * Add proxy method that adds the {@link remoter.RemoterProxy} methods
     */
    private void addSubDestroyMethods(TypeSpec.Builder classBuilder) {
//        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("finalize")
//                .addModifiers(Modifier.PROTECTED)
//                .returns(TypeName.VOID)
//                .addException(Throwable.class)
//                .addStatement("super.finalize()")
//                .addAnnotation(Override.class);
//        classBuilder.addMethod(methodBuilder.build());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("destroyStub")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.VOID)
                .beginControlFlow("try")
                //.addStatement("finalize()")
                .addStatement("this.attachInterface(null, DESCRIPTOR)")
                .addStatement("binderWrapper.binder = null")
                .endControlFlow()
                .beginControlFlow("catch (Throwable t)")
                .endControlFlow()
                .addStatement("serviceImpl = null");

        classBuilder.addMethod(methodBuilder.build());
    }

    private void addSubMapTransaction(TypeSpec.Builder classBuilder) {

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("mapTransactionCode")
                .addModifiers(Modifier.PRIVATE)
                .returns(TypeName.INT)
                .addParameter(TypeName.INT, "code")

                .beginControlFlow("if (__checkStubProxyMatch == false || code == INTERFACE_TRANSACTION)")
                .addStatement("return code")
                .endControlFlow()

                .addStatement("int mappedCode = code")

                .beginControlFlow("if (__lastMethodIndexOfProxy == -1)")
                .addStatement("__lastMethodIndexOfProxy = code - 1")
                .endControlFlow()

                .beginControlFlow("if (__lastMethodIndexOfProxy < __lastMethodIndex) ")
                .beginControlFlow("if (code > __lastMethodIndexOfProxy)")
                .addStatement("mappedCode = __lastMethodIndex + (code - __lastMethodIndexOfProxy)")
                .endControlFlow()
                .endControlFlow()

                .beginControlFlow("else if (__lastMethodIndexOfProxy > __lastMethodIndex)")
                .beginControlFlow("if (code > __lastMethodIndexOfProxy) ")
                .addStatement("mappedCode = __lastMethodIndex + (code - __lastMethodIndexOfProxy)")
                .endControlFlow()
                .beginControlFlow("else if (code > __lastMethodIndex)")
                .addStatement("throw new RuntimeException(\"Interface mismatch between Proxy and Stub \" + code + \" [\" + __lastMethodIndex + \"]. Use same interface for both client and server\")")
                .endControlFlow()
                .endControlFlow()

                .addStatement("return mappedCode");

        classBuilder.addMethod(methodBuilder.build());


        MethodSpec.Builder methodBuilder2 = MethodSpec.methodBuilder("setStubProxyCheck")
                .addModifiers(Modifier.STATIC)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.BOOLEAN, "enable")
                .addJavadoc("Enable or disable stub proxy mismatch check. Default enabled. Turn it off if using Remoter client with AIDL server")
                .addStatement("__checkStubProxyMatch = enable");

        classBuilder.addMethod(methodBuilder2.build());
    }


    /**
     * Add stub method that  get called for each transaction from where an exception could be thrown
     */
    private void addSubInterceptMethods(TypeSpec.Builder classBuilder) {

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("onDispatchTransaction")
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.VOID)
                .addException(Exception.class)
                .addJavadoc("Override to intercept before binder call for validation\n")
                .addParameter(int.class, "code");

        classBuilder.addMethod(methodBuilder.build());
    }


    /**
     * Add other extra methods
     */
    private void addProxyExtras(TypeSpec.Builder classBuilder) {
        addRemoterProxyMethods(classBuilder);
        addProxyDeathMethod(classBuilder, "linkToDeath", "Register a {@link android.os.IBinder.DeathRecipient} to know of binder connection lose\n");
        addProxyDeathMethod(classBuilder, "unlinkToDeath", "UnRegisters a {@link android.os.IBinder.DeathRecipient}\n");
        addProxyRemoteAlive(classBuilder);
        addProxyCheckException(classBuilder);
        addGetId(classBuilder);
        addHashCode(classBuilder);
        addEquals(classBuilder);
        addProxyToString(classBuilder);
        addProxyDestroyMethods(classBuilder);
    }


    /**
     * Add proxy method for destroystub
     */
    private void addProxyDestroyMethods(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("destroyStub")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(Object.class, "object")
                .returns(TypeName.VOID)
                .beginControlFlow("if(object != null)")
                .beginControlFlow("synchronized (stubMap)")
                .addStatement("IBinder binder = stubMap.get(object)")
                .beginControlFlow("if (binder != null)")
                .addStatement("(($T)binder).destroyStub()", RemoterStub.class)
                .addStatement("stubMap.remove(object)")
                .endControlFlow()
                .endControlFlow()
                .endControlFlow();

        classBuilder.addMethod(methodBuilder.build());

        methodBuilder = MethodSpec.methodBuilder("__checkProxy")
                .addModifiers(Modifier.PRIVATE)
                .returns(TypeName.VOID)
                .addStatement("if(mRemote == null) throw new RuntimeException(\"Trying to use a destroyed Proxy\")");

        classBuilder.addMethod(methodBuilder.build());

        methodBuilder = MethodSpec.methodBuilder("destroyProxy")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.VOID)
                .addStatement("this.mRemote = null")
                .addStatement("unRegisterProxyListener(null)")
                .beginControlFlow("synchronized (stubMap)")
                .beginControlFlow("for(IBinder binder:stubMap.values())")
                .addStatement("((RemoterStub)binder).destroyStub()")
                .endControlFlow()
                .addStatement("stubMap.clear()")
                .endControlFlow();

        classBuilder.addMethod(methodBuilder.build());


        methodBuilder = MethodSpec.methodBuilder("setRemoterGlobalProperties")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Map.class),
                        ClassName.get(String.class),
                        ClassName.get(Object.class)), "properties")
                .returns(TypeName.VOID)
                .addStatement("this.__global_properties = properties");

        classBuilder.addMethod(methodBuilder.build());


    }

    /**
     * Add proxy method that adds the {@link remoter.RemoterProxy} methods
     */
    private void addRemoterProxyMethods(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("registerProxyListener")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(ClassName.get(RemoterProxyListener.class), "listener")
                .addStatement("unRegisterProxyListener(null)")
                .addStatement("proxyListener = new DeathRecipient(listener)")
                .addStatement("linkToDeath(proxyListener)")
                .addAnnotation(Override.class);
        classBuilder.addMethod(methodBuilder.build());

        methodBuilder = MethodSpec.methodBuilder("unRegisterProxyListener")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(ClassName.get(RemoterProxyListener.class), "listener")
                .beginControlFlow("if(proxyListener != null)")
                .addStatement("unlinkToDeath(proxyListener)")
                .addStatement("proxyListener.proxyListener = null")
                .endControlFlow()
                .addStatement("proxyListener = null")
                .addAnnotation(Override.class);
        classBuilder.addMethod(methodBuilder.build());
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

    /**
     * Add proxy method to check for exception
     */
    private void addProxyCheckException(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("checkException")
                .addModifiers(Modifier.PRIVATE)
                .returns(Throwable.class)
                .addParameter(ClassName.get("android.os", "Parcel"), "reply")
                .addStatement("int code = reply.readInt()")
                .addStatement("Throwable exception = null")
                .beginControlFlow("if (code != 0)")
                .addStatement("String msg = reply.readString()")
                .beginControlFlow("if (code == REMOTER_EXCEPTION_CODE)")
                .addStatement("exception = (Throwable) reply.readSerializable()")
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("exception = new RuntimeException(msg)")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return exception");
        classBuilder.addMethod(methodBuilder.build());
    }


    /**
     * Add proxy method to set hashcode to uniqueu id of binder
     */
    private void addHashCode(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addAnnotation(Override.class)
                .addStatement("return _binderID");
        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Add proxy method for equals
     */
    private void addEquals(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Object.class), "obj")
                .returns(boolean.class)
                .addAnnotation(Override.class)
                .addStatement("return (obj instanceof " + getRemoterInterfaceClassName() + ClassBuilder.PROXY_SUFFIX + ") && obj.hashCode() == hashCode() && _stubProcess == ((" + getRemoterInterfaceClassName() + ClassBuilder.PROXY_SUFFIX +")obj)._stubProcess");
        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Add proxy method for equals
     */
    private void addProxyToString(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addAnnotation(Override.class)
                .addStatement("return \""+ getRemoterInterfaceClassName() + ClassBuilder.PROXY_SUFFIX  + "[ \"+ _stubProcess + \":\" + _binderID + \"]\"");
        classBuilder.addMethod(methodBuilder.build());
    }


    /**
     * Add proxy method to get unique id
     */
    private void addGetId(TypeSpec.Builder classBuilder) {
        addGetIdMethod(classBuilder, "__getStubID", "TRANSACTION__getStubID");
        addGetIdMethod(classBuilder, "__getStubProcessID", "TRANSACTION__getStubProcessID");
    }

    /**
     * Add proxy method to get unique id
     */
    private void addGetIdMethod(TypeSpec.Builder classBuilder, String methodName, String descriptorName) {

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE)
                .returns(int.class)
                .addStatement("android.os.Parcel data = android.os.Parcel.obtain()")
                .addStatement("android.os.Parcel reply = android.os.Parcel.obtain()")
                .addStatement("int result");

        methodBuilder.beginControlFlow("try");

        //write the descriptor
        methodBuilder.addStatement("data.writeInterfaceToken(DESCRIPTOR)");
        methodBuilder.addStatement("mRemote.transact(" + descriptorName + ", data, reply, 0)");
        //read exception if any
        methodBuilder.addStatement("Throwable exception = checkException(reply)");
        methodBuilder.beginControlFlow("if(exception != null)");

        methodBuilder.addStatement("throw ($T)exception", RuntimeException.class);
        methodBuilder.endControlFlow();

        methodBuilder.addStatement("result = reply.readInt()");


        //end of try
        methodBuilder.endControlFlow();


        //catch rethrow
        methodBuilder.beginControlFlow("catch ($T re)", ClassName.get("android.os", "RemoteException"));
        methodBuilder.addStatement("throw new $T(re)", RuntimeException.class);
        methodBuilder.endControlFlow();


        //finally block
        methodBuilder.beginControlFlow("finally");
        methodBuilder.addStatement("reply.recycle()");
        methodBuilder.addStatement("data.recycle()");
        methodBuilder.endControlFlow();
        methodBuilder.addStatement("return result");

        classBuilder.addMethod(methodBuilder.build());
    }



}
