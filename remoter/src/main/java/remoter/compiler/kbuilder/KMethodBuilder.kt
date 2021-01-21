package remoter.compiler.kbuilder

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.throws
import kotlinx.coroutines.Dispatchers
import remoter.RemoterGlobalProperties
import remoter.RemoterProxyListener
import remoter.RemoterStub
import remoter.annotations.Oneway
import remoter.annotations.ParamIn
import remoter.annotations.ParamOut
import remoter.compiler.kbuilder.KClassBuilder.Companion.PROXY_SUFFIX
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind


/**
 * A {@link RemoteBuilder} that knows how to generate methods for Stub and Proxy
 */
class KMethodBuilder(remoterInterfaceElement: Element, bindingManager: KBindingManager) : KRemoterBuilder(remoterInterfaceElement, bindingManager) {


    /**
     * Build the proxy methods
     */
    fun addProxyMethods(classBuilder: TypeSpec.Builder) {
        processRemoterElements(classBuilder, object : ElementVisitor {
            override fun visitElement(classBuilder: TypeSpec.Builder, member: Element, methodIndex: Int, methodBuilder: FunSpec.Builder?) {
                addProxyMethods(classBuilder, member, methodIndex)
            }
        }, null)
        addProxyExtras(classBuilder)
        addCommonExtras(classBuilder)
    }

    fun addStubMethods(classBuilder: TypeSpec.Builder) {
        val methodBuilder = FunSpec.builder("onTransact")
                .addModifiers(KModifier.PUBLIC)
                .addModifiers(KModifier.OVERRIDE)
                .returns(Boolean::class)
                .throws(ClassName("android.os", "RemoteException"))
                .addParameter("code", Int::class)
                .addParameter(ParamBuilder.DATA, ClassName("android.os", "Parcel"))
                .addParameter(ParamBuilder.REPLY, ClassName("android.os", "Parcel").copy(true))
                .addParameter("flags", Int::class)

        methodBuilder.beginControlFlow("try")
        methodBuilder.beginControlFlow("when (mapTransactionCode(code))")

        methodBuilder.beginControlFlow("INTERFACE_TRANSACTION -> ")
        methodBuilder.addStatement("${ParamBuilder.REPLY}?.writeString(DESCRIPTOR)")
        methodBuilder.addStatement("return true")
        methodBuilder.endControlFlow()

        methodBuilder.beginControlFlow("TRANSACTION__getStubID -> ")
        methodBuilder.addStatement("${ParamBuilder.DATA}.enforceInterface(DESCRIPTOR)")
        methodBuilder.addStatement("${ParamBuilder.REPLY}?.writeNoException()")
        methodBuilder.addStatement("${ParamBuilder.REPLY}?.writeInt(serviceImpl.hashCode())")
        methodBuilder.addStatement("return true")
        methodBuilder.endControlFlow()


        methodBuilder.beginControlFlow("TRANSACTION__getStubProcessID -> ")
        methodBuilder.addStatement("${ParamBuilder.DATA}.enforceInterface(DESCRIPTOR)")
        methodBuilder.addStatement("${ParamBuilder.REPLY}?.writeNoException()")
        methodBuilder.addStatement("${ParamBuilder.REPLY}?.writeInt(android.os.Process.myPid())")
        methodBuilder.addStatement("return true")
        methodBuilder.endControlFlow()



        processRemoterElements(classBuilder, object : ElementVisitor {
            override fun visitElement(classBuilder: TypeSpec.Builder, member: Element, methodIndex: Int, methodBuilder: FunSpec.Builder?) {
                addStubMethods(classBuilder, member, methodIndex, methodBuilder!!)
            }
        }, methodBuilder)


        //end switch
        //end switch
        methodBuilder.endControlFlow()
        //end of try
        //end of try
        methodBuilder.endControlFlow()
        //catch rethrow
        //catch rethrow


        methodBuilder.beginControlFlow("catch (re:%T)", Throwable::class)
        methodBuilder.beginControlFlow("if ( ${ParamBuilder.REPLY} != null && (flags and FLAG_ONEWAY) == 0)")
        methodBuilder.addStatement("${ParamBuilder.REPLY}.setDataPosition(0)")
        methodBuilder.addStatement("${ParamBuilder.REPLY}.writeInt(REMOTER_EXCEPTION_CODE)")
        methodBuilder.addStatement("${ParamBuilder.REPLY}.writeString(re.message)")
        methodBuilder.addStatement("${ParamBuilder.REPLY}.writeSerializable(re)")
        methodBuilder.addStatement("return true")
        methodBuilder.endControlFlow()
        methodBuilder.beginControlFlow("else")
        methodBuilder.addStatement("%T.w(\"StubCall: serviceImpl?.toString()\", \"Binder call failed.\", re)", ClassName("android.util", "Log"))
        methodBuilder.addStatement("throw %T(re)", RuntimeException::class)
        methodBuilder.endControlFlow()
        methodBuilder.endControlFlow()

        methodBuilder.addStatement("return super.onTransact(code, ${ParamBuilder.DATA}, ${ParamBuilder.REPLY}, flags)")
        classBuilder.addFunction(methodBuilder.build())


        addCommonExtras(classBuilder)

        addStubExtras(classBuilder)

    }

    /**
     * Called from the [KRemoterBuilder.ElementVisitor] callback
     */
    private fun addStubMethods(classBuilder: TypeSpec.Builder, member: Element, methodIndex: Int, methodBuilder: FunSpec.Builder) {
        val executableElement = member as ExecutableElement
        val methodName = executableElement.simpleName.toString()
        val isOneWay = (executableElement.returnType.kind == TypeKind.VOID
                && member.getAnnotation(Oneway::class.java) != null)

        methodBuilder.beginControlFlow("TRANSACTION_" + methodName + "_" + methodIndex + " -> ")
        methodBuilder.addStatement("${ParamBuilder.DATA}.enforceInterface(DESCRIPTOR)")
        methodBuilder.addStatement("onDispatchTransaction(code)")

        val paramNames: MutableList<String> = mutableListOf()
        val outParams: MutableList<VariableElement> = mutableListOf()
        val outParamsNames: MutableList<String> = mutableListOf()

        val paramsSize = executableElement.parameters.size

        val isSuspendFunction = executableElement.isSuspendFunction()
        val isSuspendReturnNullable = if (isSuspendFunction) executableElement.isSuspendReturningNullable() else false
        val isSuspendUnit = if (isSuspendFunction) (executableElement.getReturnTypeOfSuspend().asTypeName() == UNIT) else false
        if (isSuspendFunction) {
            methodBuilder.beginControlFlow("kotlinx.coroutines.runBlocking")
        }


        //pass parameters
        for ((paramIndex, param) in executableElement.parameters.withIndex()) {

            if (isSuspendFunction && paramIndex == paramsSize - 1) {
                break
            }


            val paramType = when {
                param.getAnnotation(ParamIn::class.java) != null -> ParamBuilder.ParamType.IN
                param.getAnnotation(ParamOut::class.java) != null -> ParamBuilder.ParamType.OUT
                else -> ParamBuilder.ParamType.IN_OUT
            }

            val paramBuilder = bindingManager.getBuilderForParam(remoterInterfaceElement, param.asType())

            if (paramBuilder != null) {
                val paramName = "arg_stb_$paramIndex"
                paramNames.add(paramName)
                paramBuilder.writeParamsToStub(executableElement, param, paramType, paramName, methodBuilder)
                if (paramType != ParamBuilder.ParamType.IN) {
                    outParams.add(param)
                    outParamsNames.add(paramName)
                }
            } else {
                logError("Parameter cannot be marshalled " + param.simpleName)
            }
        }
        var methodCall = "serviceImpl!!.$methodName("
        val paramSize = paramNames.size
        for (paramCount in 0 until paramSize) {

            if (isSuspendFunction && paramCount == paramsSize - 1) {
                break
            }

            val varArgParamIndex = if (isSuspendFunction) paramsSize - 2 else paramsSize - 1

            if (paramCount == varArgParamIndex && executableElement.isVarArgs) {
                methodCall += "*"
            }
            methodCall += paramNames[paramCount]
            if (paramCount < paramSize - 1) {
                methodCall += ", "
            }
        }
        methodCall += ")"

        methodBuilder.addStatement("%T.set(${ParamBuilder.DATA}.readHashMap(javaClass.getClassLoader()))", RemoterGlobalProperties::class.java)

        if (isSuspendFunction) {
            if (!isSuspendUnit) {
                methodBuilder.addStatement("val ${ParamBuilder.RESULT} = $methodCall")
            } else {
                methodBuilder.addStatement(methodCall)
            }

        } else {
            if (executableElement.returnType.kind != TypeKind.VOID) {
                methodBuilder.addStatement("val ${ParamBuilder.RESULT} = $methodCall")
            } else {
                methodBuilder.addStatement(methodCall)
            }
        }
        methodBuilder.addStatement("RemoterGlobalProperties.reset()")

        if (!isOneWay) {
            methodBuilder.beginControlFlow("if (${ParamBuilder.REPLY} != null)")
            methodBuilder.addStatement("${ParamBuilder.REPLY}.writeNoException()")

            if (isSuspendFunction) {
                if (!isSuspendUnit) {
                    val paramBuilder = bindingManager.getBuilderForParam(remoterInterfaceElement, executableElement.getReturnTypeOfSuspend())
                    if (paramBuilder != null) {
                        paramBuilder.readResultsFromStub(executableElement, executableElement.getReturnAsTypeMirror(), methodBuilder)
                    } else {
                        logError("Unmarshallable return type " + executableElement.returnType)
                    }
                }
            } else {
                if (executableElement.returnType.kind != TypeKind.VOID) {
                    val paramBuilder = bindingManager.getBuilderForParam(remoterInterfaceElement, executableElement.returnType)
                    if (paramBuilder != null) {
                        paramBuilder.readResultsFromStub(executableElement, executableElement.returnType, methodBuilder)
                    } else {
                        logError("Unmarshallable return type " + executableElement.returnType)
                    }
                }
            }
            for ((pIndex, param) in outParams.withIndex()) {
                val paramType = when {
                    param.getAnnotation(ParamIn::class.java) != null -> ParamBuilder.ParamType.IN
                    param.getAnnotation(ParamOut::class.java) != null -> ParamBuilder.ParamType.OUT
                    else -> ParamBuilder.ParamType.IN_OUT
                }
                val paramBuilder = bindingManager.getBuilderForParam(remoterInterfaceElement, param.asType())
                paramBuilder.readOutResultsFromStub(param, paramType, outParamsNames[pIndex], methodBuilder)
            }

            methodBuilder.endControlFlow()
        }

        if (isSuspendFunction) {
            methodBuilder.endControlFlow()
        }

        methodBuilder.addStatement("return true")
        methodBuilder.endControlFlow()
    }

    /**
     * Build the proxy methods
     */
    private fun addProxyMethods(classBuilder: TypeSpec.Builder, member: Element, methodIndex: Int) {
        val executableElement = member as ExecutableElement
        val methodName = executableElement.simpleName.toString()
        val isOnewayAnnotated = member.getAnnotation(Oneway::class.java) != null
        val isOneWay = (executableElement.returnType.kind == TypeKind.VOID
                && isOnewayAnnotated)
        if (!isOneWay && isOnewayAnnotated) {
            logWarning("@Oneway is expected only for methods with void return. Ignoring it for " + member.getSimpleName())
        }

        val isSuspendFunction = executableElement.isSuspendFunction()
        val isSuspendReturnNullable = if (isSuspendFunction) executableElement.isSuspendReturningNullable() else false
        val isSuspendUnit = if (isSuspendFunction) (executableElement.getReturnTypeOfSuspend().asTypeName() == UNIT) else false

        val methodBuilder = FunSpec.builder(methodName)
                .addModifiers(KModifier.PUBLIC)
                .addModifiers(KModifier.OVERRIDE)


        if (isSuspendFunction) {
            methodBuilder.addModifiers(KModifier.SUSPEND)
            methodBuilder.returns(executableElement.getReturnTypeOfSuspend().asKotlinType(this, member).copy(isSuspendReturnNullable))
        } else {
            methodBuilder.returns(executableElement.returnType.asKotlinType(this, executableElement).copy(executableElement.isNullable()))
        }


        //add Exceptions
        if (executableElement.thrownTypes?.isNotEmpty() == true) {
            methodBuilder.throws(executableElement.thrownTypes.map { it.asKotlinType() })
        }

        //add parameters
        val paramsSize = executableElement.parameters.size
        var paramIndex = 0
        for (params in executableElement.parameters) {


            if (isSuspendFunction && paramIndex == paramsSize - 1) {
                break
            }

            val varArgParamIndex = if (isSuspendFunction) paramsSize - 2 else paramsSize - 1


            if (paramIndex == varArgParamIndex && executableElement.isVarArgs) {
                val arrayComponentType = (params.asType() as ArrayType).componentType
                methodBuilder.addParameter(ParameterSpec.builder(params.simpleName.toString(),
                        arrayComponentType.asKotlinType().copy(true)).addModifiers(KModifier.VARARG).build())

            } else {
                methodBuilder.addParameter(ParameterSpec.builder(params.simpleName.toString(),
                        params.asKotlinType(this)).build())
            }

            paramIndex++
        }

        if (isSuspendFunction) {
            methodBuilder.beginControlFlow("return kotlinx.coroutines.withContext(%T.IO)", Dispatchers::class)
        }

//        methodBuilder
//                .addStatement("__checkProxy()")
        //add statements
        methodBuilder.addStatement("val ${ParamBuilder.DATA} = %T.obtain()", ClassName.bestGuess("android.os.Parcel"))
        if (!isOneWay) {
            methodBuilder.addStatement("val ${ParamBuilder.REPLY} = %T.obtain()", ClassName.bestGuess("android.os.Parcel"))
        }

        if (isSuspendFunction) {
            if (!isSuspendUnit) {
                methodBuilder.addStatement("var ${ParamBuilder.RESULT}: %T", executableElement.getReturnTypeOfSuspend().asKotlinType(this, member).copy(isSuspendReturnNullable))
            }
        } else {

            //add return if any
            if (executableElement.returnType.kind != TypeKind.VOID) {
                methodBuilder.addStatement("var ${ParamBuilder.RESULT}: %T", executableElement.returnType.asKotlinType(this, executableElement).copy(executableElement.isNullable()))
            }
        }


        //start main body block
        methodBuilder.beginControlFlow("try")
        //write the descriptor
        methodBuilder.addStatement("${ParamBuilder.DATA}.writeInterfaceToken(DESCRIPTOR)")
        val outParams: MutableList<VariableElement> = ArrayList()
        //pass parameters
        paramIndex = 0
        for (param in executableElement.parameters) {

            if (isSuspendFunction && paramIndex == paramsSize - 1) {
                break
            }

            val paramType = when {
                param.getAnnotation(ParamIn::class.java) != null -> ParamBuilder.ParamType.IN
                param.getAnnotation(ParamOut::class.java) != null -> ParamBuilder.ParamType.OUT
                else -> ParamBuilder.ParamType.IN_OUT
            }


            if (paramType != ParamBuilder.ParamType.IN) {
                outParams.add(param)
            }
            val paramBuilder: ParamBuilder = bindingManager.getBuilderForParam(remoterInterfaceElement, param.asType())
            if (paramBuilder != null) {
                paramBuilder.writeParamsToProxy(param, paramType, methodBuilder)
            } else {
                logError("Parameter cannot be marshalled " + param.simpleName)
            }
            paramIndex++
        }

        var remoterCall = "_getRemoteServiceBinder().transact"
        if (bindingManager.hasRemoterBuilder()) {
            if (isSuspendFunction) {
                remoterCall = "_getRemoteServiceBinderSuspended().transact"
            }
        }

        methodBuilder.addStatement("${ParamBuilder.DATA}.writeMap(__global_properties as %T<*, *>?)", MAP);

        //send remote command
        if (isOneWay) {
            methodBuilder.addStatement("$remoterCall(TRANSACTION_" + methodName + "_" + methodIndex + ", ${ParamBuilder.DATA}, null, android.os.IBinder.FLAG_ONEWAY)")
        } else {
            methodBuilder.addStatement("$remoterCall(TRANSACTION_" + methodName + "_" + methodIndex + ", ${ParamBuilder.DATA}, ${ParamBuilder.REPLY}, 0)")
            //read exception if any
            methodBuilder.addStatement("val __exception = checkException(${ParamBuilder.REPLY})")
            methodBuilder.beginControlFlow("if(__exception != null)")
            methodBuilder.addStatement("throw __exception ")
//            for (exceptions in executableElement.thrownTypes) {
//                val exceptionCName = ClassName.bestGuess(exceptions.toString())
//                methodBuilder.beginControlFlow("if(__exception is %T)", exceptionCName)
//                methodBuilder.addStatement("throw __exception ")
//                methodBuilder.endControlFlow()
//            }
//            methodBuilder.addStatement("throw (__exception as %T)", RuntimeException::class)
            methodBuilder.endControlFlow()
            //read result
            if (isSuspendFunction) {
                if (!isSuspendUnit) {
                    val paramBuilder: ParamBuilder = bindingManager.getBuilderForParam(remoterInterfaceElement, executableElement.getReturnTypeOfSuspend())
                    if (paramBuilder != null) {
                        paramBuilder.readResultsFromProxy(executableElement, methodBuilder)
                    } else {
                        logError("Unmarshellable return type " + executableElement.returnType)
                        methodBuilder.addStatement("result = null")
                    }
                }

            } else {
                if (executableElement.returnType.kind != TypeKind.VOID) {
                    val paramBuilder: ParamBuilder = bindingManager.getBuilderForParam(remoterInterfaceElement, executableElement.returnType)
                    if (paramBuilder != null) {
                        paramBuilder.readResultsFromProxy(executableElement, methodBuilder)
                    } else {
                        logError("Unmarshellable return type " + executableElement.returnType)
                        methodBuilder.addStatement("result = null")
                    }
                }
            }
            for (param in outParams) {
                val paramType = if (param.getAnnotation(ParamIn::class.java) != null) ParamBuilder.ParamType.IN else if (param.getAnnotation(ParamOut::class.java) != null) ParamBuilder.ParamType.OUT else ParamBuilder.ParamType.IN_OUT
                val paramBuilder: ParamBuilder = bindingManager.getBuilderForParam(remoterInterfaceElement, param.asType())
                paramBuilder?.readOutParamsFromProxy(param, paramType, methodBuilder)
            }
        }
        //end of try
        methodBuilder.endControlFlow()
        //catch rethrow
        methodBuilder.beginControlFlow("catch (re:%T)", ClassName("android.os", "RemoteException"))
        methodBuilder.addStatement("throw %T(re)", RuntimeException::class)
        methodBuilder.endControlFlow()
        //finally block
        methodBuilder.beginControlFlow("finally")
        if (!isOneWay) {
            methodBuilder.addStatement("${ParamBuilder.REPLY}.recycle()")
        }
        methodBuilder.addStatement("${ParamBuilder.DATA}.recycle()")
        methodBuilder.endControlFlow()
        if (isSuspendFunction) {
            if (!isSuspendUnit) {
                methodBuilder.addStatement(ParamBuilder.RESULT)
            }
            methodBuilder.endControlFlow()
        } else {
            if (executableElement.returnType.kind != TypeKind.VOID) {
                methodBuilder.addStatement("return ${ParamBuilder.RESULT}")
            }
        }

        classBuilder.addFunction(methodBuilder.build())
    }

    /**
     * Add common extra methods
     */
    private fun addCommonExtras(classBuilder: TypeSpec.Builder) {
        addGetParcelClass(classBuilder)
        addGetParcelObject(classBuilder)
    }

    /**
     * getParcelClass method
     */
    private fun addGetParcelClass(classBuilder: TypeSpec.Builder) {
        val methodBuilder = FunSpec.builder("getParcelerClass")
                .addModifiers(KModifier.PRIVATE)
                .returns(Class::class.asTypeName().parameterizedBy(STAR).copy(true))
                .addParameter("pObject", Any::class.asTypeName().copy(true))
                .beginControlFlow("if (pObject != null)")
                .addStatement("var objClass: Class<*>? = pObject.javaClass")
                .addStatement("var found = false")

                .beginControlFlow("while (!found && objClass != null)")
                .beginControlFlow("try")
                .addStatement("Class.forName(objClass.name + \"\\\$\\\$Parcelable\")")
                .addStatement("found = true")
                .endControlFlow()
                .beginControlFlow("catch (ignored: ClassNotFoundException) ")
                .addStatement("objClass = objClass.superclass")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return objClass")
                .endControlFlow()
                .addStatement("return null")
        classBuilder.addFunction(methodBuilder.build())
    }

    /**
     * getParcelObject method
     */
    private fun addGetParcelObject(classBuilder: TypeSpec.Builder) {
        val methodBuilder = FunSpec.builder("getParcelerObject")
                .addModifiers(KModifier.PRIVATE)
                .returns(Any::class.asTypeName().copy(true))
                .addParameter("pClassName", String::class.asTypeName().copy(true))
                .addParameter("data", ClassName("android.os", "Parcel"))
                .beginControlFlow("return try")
                .beginControlFlow("if (pClassName != null)")
                .addStatement("val creator = Class.forName(\"\$pClassName\$\\\$Parcelable\").getField(\"CREATOR\")[null] as %T.Creator<*>", ClassName("android.os", "Parcelable"))
                .addStatement("val pWrapper = creator.createFromParcel(data)")
                .addStatement("pWrapper.javaClass.getMethod(\"getParcel\").invoke(pWrapper)")
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("null")
                .endControlFlow()
                .endControlFlow()
                .beginControlFlow("catch (ignored: Exception)")
                .addStatement("null")
                .endControlFlow()
        classBuilder.addFunction(methodBuilder.build())
    }

    /**
     * Add other extra methods
     */
    private fun addProxyExtras(classBuilder: TypeSpec.Builder) {
        addRemoterProxyMethods(classBuilder)
        addProxyDeathMethod(classBuilder, "linkToDeath", "Register a {@link android.os.IBinder.DeathRecipient} to know of binder connection lose\n")
        addProxyDeathMethod(classBuilder, "unlinkToDeath", "UnRegisters a {@link android.os.IBinder.DeathRecipient}\n")
        addProxyRemoteAlive(classBuilder)
        addProxyCheckException(classBuilder)
        addGetId(classBuilder)
        addHashCode(classBuilder)
        addEquals(classBuilder)
        addProxyToString(classBuilder)
        addProxyDestroyMethods(classBuilder)
        addProxyGetServiceSuspended(classBuilder)
    }


    /**
     * Add proxy method for destroystub
     */
    private fun addProxyDestroyMethods(classBuilder: TypeSpec.Builder) {
        var methodBuilder = FunSpec.builder("destroyStub")
                .addModifiers(KModifier.PUBLIC)
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("pObject", Any::class.asTypeName().copy(true))
                .returns(Unit::class)
                .beginControlFlow("if(pObject != null)")
                .beginControlFlow("synchronized (stubMap)")
                .addStatement("val binder = stubMap[pObject]")
                .beginControlFlow("if (binder != null)")
                .addStatement("(binder as %T).destroyStub()", RemoterStub::class)
                .addStatement("stubMap.remove(pObject)")
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
        classBuilder.addFunction(methodBuilder.build())

//        methodBuilder = FunSpec.builder("__checkProxy")
//                .addModifiers(KModifier.PRIVATE)
//                .returns(Unit::class)
//                .addStatement("if(remoteBinder == null) throw RuntimeException(\"Not connected with service\")")
//        classBuilder.addFunction(methodBuilder.build())

        methodBuilder = FunSpec.builder("destroyProxy")
                .addModifiers(KModifier.PUBLIC)
                .addModifiers(KModifier.OVERRIDE)
                .returns(Unit::class)
                .addStatement("_proxyScope.%M()", MemberName("kotlinx.coroutines", "cancel"))
                .addStatement("this.remoteBinder = null")
                .addStatement("unRegisterProxyListener(null)")
                .beginControlFlow("synchronized (stubMap)")
                .beginControlFlow("stubMap.values.forEach")
                .addStatement("(it as %T).destroyStub()", RemoterStub::class)
                .endControlFlow()
                .addStatement("stubMap.clear()")
                .endControlFlow()
        classBuilder.addFunction(methodBuilder.build())


        methodBuilder = FunSpec.builder("setRemoterGlobalProperties")
                .addModifiers(KModifier.PUBLIC)
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("properties", ClassName("kotlin.collections", "MutableMap").parameterizedBy(
                        String::class.asTypeName(),
                        Any::class.asTypeName()).copy(true))
                .returns(Unit::class)
                .addStatement("this.__global_properties = properties")
        classBuilder.addFunction(methodBuilder.build())
    }

    /**
     * Add proxy method that adds the [remoter.RemoterProxy] methods
     */
    private fun addRemoterProxyMethods(classBuilder: TypeSpec.Builder) {
        var methodBuilder = FunSpec.builder("registerProxyListener")
                .addModifiers(KModifier.PUBLIC)
                .returns(Unit::class)
                .addParameter("listener", RemoterProxyListener::class)
                .addStatement("unRegisterProxyListener(null)")
                .addStatement("val pListener = DeathRecipient(listener)")
                .addStatement("linkToDeath(pListener)")
                .addStatement("proxyListener = pListener")
                .addModifiers(KModifier.OVERRIDE)
        classBuilder.addFunction(methodBuilder.build())


        methodBuilder = FunSpec.builder("unRegisterProxyListener")
                .addModifiers(KModifier.PUBLIC)
                .returns(Unit::class)
                .addParameter("listener", RemoterProxyListener::class.asTypeName().copy(true))
                .beginControlFlow("proxyListener?.let ")
                .addStatement("unlinkToDeath(it)")
                .addStatement("it.unregister()")
                .endControlFlow()
                .addStatement("proxyListener = null")
                .addModifiers(KModifier.OVERRIDE)
        classBuilder.addFunction(methodBuilder.build())
    }


    /**
     * Add proxy method that exposes the linkToDeath
     */
    private fun addProxyDeathMethod(classBuilder: TypeSpec.Builder, deathMethod: String, doc: String) {

        val methodBuilder = FunSpec.builder(deathMethod)
                .addModifiers(KModifier.PUBLIC)
                .returns(Unit::class)
                .addParameter("deathRecipient", ClassName("android.os", "IBinder.DeathRecipient"))
                .beginControlFlow("try")
                .addStatement("_getRemoteServiceBinder().$deathMethod(deathRecipient, 0)")
                .endControlFlow()
                .beginControlFlow("catch (ignored: %T)", Exception::class)
                .endControlFlow()
                .addKdoc(doc)
        classBuilder.addFunction(methodBuilder.build())
    }

    /**
     * Add proxy method that exposes whether remote is alive
     */
    private fun addProxyRemoteAlive(classBuilder: TypeSpec.Builder) {
        val methodBuilder = FunSpec.builder("isRemoteAlive")
                .addModifiers(KModifier.PUBLIC)
                .addModifiers(KModifier.OVERRIDE)
                .returns(Boolean::class)
                .addStatement("var alive = false")
                .beginControlFlow("try")
                .addStatement("alive = _getRemoteServiceBinder().isBinderAlive() == true")
                .endControlFlow()
                .beginControlFlow("catch (ignored:%T)", Exception::class)
                .endControlFlow()
                .addStatement("return alive")
                .addKdoc("Checks whether the remote process is alive\n")
        classBuilder.addFunction(methodBuilder.build())
    }

    /**
     * Add proxy method to check for exception
     */
    private fun addProxyCheckException(classBuilder: TypeSpec.Builder) {
        val methodBuilder = FunSpec.builder("checkException")
                .addModifiers(KModifier.PRIVATE)
                .returns(Throwable::class.asTypeName().copy(true))
                .addParameter("reply", ClassName("android.os", "Parcel"))
                .addStatement("val code = reply.readInt()")
                .addStatement("var exception: Throwable? = null")
                .beginControlFlow("if (code != 0)")
                .addStatement("val msg = reply.readString()")
                .beginControlFlow("exception = if (code == REMOTER_EXCEPTION_CODE) ")
                .addStatement("reply.readSerializable() as Throwable")
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("RuntimeException(msg)")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return exception")
        classBuilder.addFunction(methodBuilder.build())
    }


    /**
     * Add proxy method to set hashcode to uniqueu id of binder
     */
    private fun addHashCode(classBuilder: TypeSpec.Builder) {
        val methodBuilder = FunSpec.builder("hashCode")
                .addModifiers(KModifier.PUBLIC)
                .returns(Int::class)
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("return _binderID")
        classBuilder.addFunction(methodBuilder.build())
    }

    /**
     * Add proxy method for equals
     */
    private fun addEquals(classBuilder: TypeSpec.Builder) {

        val remoteInterfaceDeclaredType = (remoterInterfaceElement.asType() as DeclaredType)
        var typeAddition = ""
        val totalTypesArguments = remoteInterfaceDeclaredType.typeArguments.size
        if (totalTypesArguments > 0) {
            typeAddition = "<*"
            for (index in 1 until totalTypesArguments) {
                typeAddition += ",*"
            }
            typeAddition += ">"
        }
        val methodBuilder = FunSpec.builder("equals")
                .addModifiers(KModifier.PUBLIC)
                .addParameter("other", Any::class.asTypeName().copy(true))
                .returns(Boolean::class)
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("return (other is $remoterInterfaceClassName$PROXY_SUFFIX$typeAddition) && (other.hashCode() == hashCode()) && (_stubProcess == other._stubProcess)")
        classBuilder.addFunction(methodBuilder.build())
    }

    /**
     * Add proxy method for equals
     */
    private fun addProxyToString(classBuilder: TypeSpec.Builder) {
        val methodBuilder = FunSpec.builder("toString")
                .addModifiers(KModifier.PUBLIC)
                .returns(String::class)
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("return \"$remoterInterfaceClassName$PROXY_SUFFIX[ \"+ _stubProcess + \":\" + _binderID + \"]\"")
        classBuilder.addFunction(methodBuilder.build())
    }

    /**
     * Add proxy method for equals
     */
    private fun addProxyGetServiceSuspended(classBuilder: TypeSpec.Builder) {
        if (bindingManager.hasRemoterBuilder()) {
            val methodBuilder = FunSpec.builder("_getRemoteServiceBinderSuspended")
                    .addModifiers(KModifier.PRIVATE)
                    .addModifiers(KModifier.SUSPEND)
                    .returns(ClassName("android.os", "IBinder"))
                    .addStatement("val sConnector = _remoterServiceConnector")
                    .beginControlFlow("return if (sConnector != null)")
                    .addStatement("sConnector.getService()")
                    .endControlFlow()
                    .beginControlFlow("else")
                    .addStatement("remoteBinder?: throw %T(\"No remote binder or IServiceConnector set\")", RuntimeException::class)
                    .endControlFlow()

            classBuilder.addFunction(methodBuilder.build())
        }
        val methodBuilderNonSuspended = FunSpec.builder("_getRemoteServiceBinder")
                .addModifiers(KModifier.PRIVATE)
                .returns(ClassName("android.os", "IBinder"))
        if (bindingManager.hasRemoterBuilder()) {
            methodBuilderNonSuspended.beginControlFlow("val result = if (remoteBinder != null)")
                    .addStatement("remoteBinder")
                    .endControlFlow()
                    .beginControlFlow("else if (_remoterServiceConnector != null)")
                    .beginControlFlow("kotlinx.coroutines.runBlocking")
                    .addStatement("_remoterServiceConnector?.getService()")
                    .endControlFlow()
                    .endControlFlow()
                    .beginControlFlow("else ")
                    .addStatement("remoteBinder")
                    .endControlFlow()
                    .addStatement("return result?: throw %T(\"No remote binder or IServiceConnectot set\")", RuntimeException::class)
        } else {
            methodBuilderNonSuspended.addStatement("val result = remoteBinder")
            methodBuilderNonSuspended.addStatement("return result?: throw %T(\"No remote binder or IServiceConnectot set\")", RuntimeException::class)
        }

        classBuilder.addFunction(methodBuilderNonSuspended.build())


    }


    /**
     * Add proxy method to get unique id
     */
    private fun addGetId(classBuilder: TypeSpec.Builder) {
        addGetIdMethod(classBuilder, "__remoter_getStubID", "TRANSACTION__getStubID", "_getRemoteServiceBinder()")
        addGetIdMethod(classBuilder, "__remoter_getStubProcessID", "TRANSACTION__getStubProcessID", "_getRemoteServiceBinder()")
        if (bindingManager.hasRemoterBuilder()) {
            addGetIdMethod(classBuilder, "__remoter_getStubID_sus", "TRANSACTION__getStubID", "_getRemoteServiceBinderSuspended()", true)
            addGetIdMethod(classBuilder, "__remoter_getStubProcessID_sus", "TRANSACTION__getStubProcessID", "_getRemoteServiceBinderSuspended()", true)
        }
    }

    /**
     * Add proxy method to get unique id
     */
    private fun addGetIdMethod(classBuilder: TypeSpec.Builder, methodName: String, descriptorName: String, getMethod: String, isSuspended: Boolean = false) {
        val methodBuilder = FunSpec.builder(methodName)
                .addModifiers(KModifier.PRIVATE)
                .returns(Int::class)
                .addStatement("val data = Parcel.obtain()")
                .addStatement("val reply = Parcel.obtain()")
        methodBuilder.beginControlFlow("val result = try ")
        //write the descriptor
        methodBuilder.addStatement("data.writeInterfaceToken(DESCRIPTOR)")
        methodBuilder.addStatement("$getMethod.transact($descriptorName, data, reply, 0)")
        //read exception if any
        methodBuilder.addStatement("val exception = checkException(reply)")
        methodBuilder.beginControlFlow("if(exception != null)")
        methodBuilder.addStatement("throw (exception as %T)", RuntimeException::class)
        methodBuilder.endControlFlow()
        methodBuilder.addStatement("reply.readInt()")
        //end of try
        methodBuilder.endControlFlow()
        //catch rethrow
        methodBuilder.beginControlFlow("catch (ignored: %T)", Exception::class)
//        methodBuilder.addStatement("throw %T(re)", RuntimeException::class)
        methodBuilder.addStatement("hashCode()")
        methodBuilder.endControlFlow()
        //finally block
        methodBuilder.beginControlFlow("finally")
        methodBuilder.addStatement("reply.recycle()")
        methodBuilder.addStatement("data.recycle()")
        methodBuilder.endControlFlow()
        methodBuilder.addStatement("return result")

        if (isSuspended) {
            methodBuilder.addModifiers(KModifier.SUSPEND)
        }

        classBuilder.addFunction(methodBuilder.build())
    }


    /**
     * Add other extra methods for stub
     */
    private fun addStubExtras(classBuilder: TypeSpec.Builder) {
        addSubDestroyMethods(classBuilder)
        addSubInterceptMethods(classBuilder)
        addSubMapTransaction(classBuilder)
    }


    /**
     * Add proxy method that adds the [remoter.RemoterProxy] methods
     */
    private fun addSubDestroyMethods(classBuilder: TypeSpec.Builder) {
//        var methodBuilder = FunSpec.builder("finalize")
//                .addModifiers(KModifier.PROTECTED)
//                .throws(Throwable::class)
//                .addStatement("super.finalize()")
//                .addModifiers(KModifier.OVERRIDE)
//        classBuilder.addFunction(methodBuilder.build())

        var methodBuilder = FunSpec.builder("destroyStub")
                .addModifiers(KModifier.PUBLIC)
                .addModifiers(KModifier.OVERRIDE)
                .beginControlFlow("try")
                //.addStatement("finalize()")
                .addStatement("this.attachInterface(null, DESCRIPTOR)")
                .addStatement("binderWrapper.binder = null")
                .endControlFlow()
                .beginControlFlow("catch (t:%T)", Throwable::class)
                .endControlFlow()
                .addStatement("serviceImpl = null")
        classBuilder.addFunction(methodBuilder.build())
    }


    /**
     * Add stub method that  get called for each transaction from where an exception could be thrown
     */
    private fun addSubInterceptMethods(classBuilder: TypeSpec.Builder) {
        val methodBuilder = FunSpec.builder("onDispatchTransaction")
                .addModifiers(KModifier.PROTECTED)
                .addModifiers(KModifier.OPEN)
                .throws(Exception::class.java)
                .addKdoc("Override to intercept before binder call for validation\n")
                .addParameter("code", Int::class)
        classBuilder.addFunction(methodBuilder.build())
    }

    private fun addSubMapTransaction(classBuilder: TypeSpec.Builder) {
        val methodBuilder = FunSpec.builder("mapTransactionCode")
                .addModifiers(KModifier.PRIVATE)
                .returns(Int::class)
                .addParameter("code", Int::class)

                .beginControlFlow("if (checkStubProxyMatch == false || code == INTERFACE_TRANSACTION)")
                .addStatement("return code")
                .endControlFlow()

                .addStatement("var mappedCode = code")

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
                .addStatement("throw RuntimeException(\"Interface mismatch between Proxy and Stub \" + code + \" [\" + __lastMethodIndex + \"]. Use same interface for both client and server\")")
                .endControlFlow()
                .endControlFlow()

                .addStatement("return mappedCode");


        classBuilder.addFunction(methodBuilder.build())
    }
}