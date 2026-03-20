package remoter.compiler.kspbuilder

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.throws
import remoter.RemoterGlobalProperties
import remoter.RemoterProxyListener
import remoter.RemoterStub
import remoter.annotations.ParamIn
import remoter.annotations.ParamOut
import remoter.compiler.kbuilder.ParamBuilder
import remoter.compiler.kspbuilder.KSPClassBuilder.Companion.PROXY_SUFFIX

/**
 * A [KSPRemoterBuilder] that knows how to generate methods for Stub and Proxy
 */
internal class KSPMethodBuilder(
    remoterInterfaceElement: KSClassDeclaration,
    bindingManager: KSPBindingManager,
    resolver: Resolver
) : KSPRemoterBuilder(remoterInterfaceElement, bindingManager, resolver) {

    /**
     * Build the proxy methods
     */
    fun addProxyMethods(classBuilder: TypeSpec.Builder) {
        processRemoterElements(classBuilder, object : ElementVisitor {
            override fun visitElement(
                classBuilder: TypeSpec.Builder,
                member: KSFunctionDeclaration,
                methodIndex: Int,
                methodBuilder: FunSpec.Builder?
            ) {
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
            .addParameter(KSPParamBuilder.DATA, ClassName("android.os", "Parcel"))
            .addParameter(KSPParamBuilder.REPLY, ClassName("android.os", "Parcel").copy(true))
            .addParameter("flags", Int::class)

        methodBuilder.beginControlFlow("try")
        methodBuilder.beginControlFlow("when (mapTransactionCode(code))")

        methodBuilder.beginControlFlow("INTERFACE_TRANSACTION -> ")
        methodBuilder.addStatement("${KSPParamBuilder.REPLY}?.writeString(DESCRIPTOR)")
        methodBuilder.addStatement("return true")
        methodBuilder.endControlFlow()

        methodBuilder.beginControlFlow("TRANSACTION__getStubID -> ")
        methodBuilder.addStatement("${KSPParamBuilder.DATA}.enforceInterface(DESCRIPTOR)")
        methodBuilder.addStatement("${KSPParamBuilder.REPLY}?.writeNoException()")
        methodBuilder.addStatement("${KSPParamBuilder.REPLY}?.writeInt(serviceImpl.hashCode())")
        methodBuilder.addStatement("return true")
        methodBuilder.endControlFlow()

        methodBuilder.beginControlFlow("TRANSACTION__getStubProcessID -> ")
        methodBuilder.addStatement("${KSPParamBuilder.DATA}.enforceInterface(DESCRIPTOR)")
        methodBuilder.addStatement("${KSPParamBuilder.REPLY}?.writeNoException()")
        methodBuilder.addStatement("${KSPParamBuilder.REPLY}?.writeInt(android.os.Process.myPid())")
        methodBuilder.addStatement("return true")
        methodBuilder.endControlFlow()

        processRemoterElements(classBuilder, object : ElementVisitor {
            override fun visitElement(
                classBuilder: TypeSpec.Builder,
                member: KSFunctionDeclaration,
                methodIndex: Int,
                methodBuilder: FunSpec.Builder?
            ) {
                addStubMethods(classBuilder, member, methodIndex, methodBuilder!!)
            }
        }, methodBuilder)

        methodBuilder.endControlFlow()
        methodBuilder.endControlFlow()

        methodBuilder.beginControlFlow("catch (re:%T)", Throwable::class)
        methodBuilder.beginControlFlow("if ( (flags and FLAG_ONEWAY) == 0)")
        methodBuilder.beginControlFlow("if ( ${KSPParamBuilder.REPLY} != null )")
        methodBuilder.addStatement("${KSPParamBuilder.REPLY}.setDataPosition(0)")
        methodBuilder.addStatement("${KSPParamBuilder.REPLY}.writeInt(REMOTER_EXCEPTION_CODE)")
        methodBuilder.addStatement("${KSPParamBuilder.REPLY}.writeString(re.message)")
        methodBuilder.addStatement("${KSPParamBuilder.REPLY}.writeSerializable(re)")
        methodBuilder.endControlFlow()
        methodBuilder.addStatement("return true")
        methodBuilder.endControlFlow()
        methodBuilder.beginControlFlow("else")
        methodBuilder.addStatement(
            "%T.w(\"StubCall: serviceImpl?.toString()\", \"Binder call failed.\", re)",
            ClassName("android.util", "Log")
        )
        methodBuilder.addStatement("throw %T(re)", RuntimeException::class)
        methodBuilder.endControlFlow()
        methodBuilder.endControlFlow()

        methodBuilder.addStatement("return super.onTransact(code, ${KSPParamBuilder.DATA}, ${KSPParamBuilder.REPLY}, flags)")
        classBuilder.addFunction(methodBuilder.build())

        addCommonExtras(classBuilder)
        addStubExtras(classBuilder)
    }

    /**
     * Called from the [KSPRemoterBuilder.ElementVisitor] callback
     */
    private fun addStubMethods(
        classBuilder: TypeSpec.Builder,
        member: KSFunctionDeclaration,
        methodIndex: Int,
        methodBuilder: FunSpec.Builder
    ) {
        val methodName = member.simpleName.asString()
        val isOneWay = (member.returnType?.resolve()
            ?.isUnit() == true && member.getAnnotation("remoter.annotations.Oneway") != null)

        methodBuilder.beginControlFlow("TRANSACTION_" + methodName + "_" + methodIndex + " -> ")
        methodBuilder.addStatement("${KSPParamBuilder.DATA}.enforceInterface(DESCRIPTOR)")
        methodBuilder.addStatement("onDispatchTransaction(code)")

        val paramNames: MutableList<String> = mutableListOf()
        val outParams: MutableList<KSFunctionDeclaration> = mutableListOf()
        val outParamsNames: MutableList<String> = mutableListOf()

        val paramsSize = member.parameters.size

        val isSuspendFunction = member.isSuspendFunction()
        val isSuspendReturnNullable =
            if (isSuspendFunction) member.isSuspendReturningNullable() else false
        val isSuspendUnit =
            if (isSuspendFunction) (member.getReturnTypeOfSuspend(resolver).isUnit()) else false
        if (isSuspendFunction) {
            methodBuilder.beginControlFlow("kotlinx.coroutines.runBlocking")
        }
        val declaredParamsSize = member.parameters.size
        //pass parameters
        for ((paramIndex, param) in member.parameters.withIndex()) {
            if (isSuspendFunction && param.type.resolve().declaration.qualifiedName?.asString() == "kotlin.coroutines.Continuation") {
                break
            }
            val varArgParamIndex = declaredParamsSize - 1
            val paramType = when {
                param.getAnnotation("remoter.annotations.ParamIn") != null -> KSPParamBuilder.ParamType.IN
                param.getAnnotation("remoter.annotations.ParamOut") != null -> KSPParamBuilder.ParamType.OUT
                else -> KSPParamBuilder.ParamType.IN_OUT
            }

            val paramBuilder =
                bindingManager.getBuilderForParam(remoterInterfaceElement, param.type.resolve())

            if (paramBuilder != null) {
                val paramName = "arg_stb_$paramIndex"
                paramNames.add(paramName)
                paramBuilder.writeParamsToStub(member, param, paramType, paramName, methodBuilder)
                if (paramType != KSPParamBuilder.ParamType.IN) {
                    outParams.add(member)
                    outParamsNames.add(paramName)
                }
            } else {
                logError("Parameter cannot be marshalled " + param.name)
            }
        }

        var methodCall = "serviceImpl!!.$methodName("
        val paramSize = paramNames.size
        for (paramCount in 0 until paramSize) {
//            if (isSuspendFunction && paramCount == paramsSize - 1) {
//                break
//            }

            val varArgParamIndex = declaredParamsSize - 1
            if (paramCount == varArgParamIndex && member.isVarArgs) {
                methodCall += "*"
            }
            methodCall += paramNames[paramCount]
            if (paramCount == varArgParamIndex && member.isVarArgs) {
                methodCall += "!!"
            }
            if (paramCount < paramSize - 1) {
                methodCall += ", "
            }
        }
        methodCall += ")"

        methodBuilder.addStatement(
            "%T.set(${KSPParamBuilder.DATA}.readHashMap(javaClass.getClassLoader()))",
            RemoterGlobalProperties::class.java
        )

        if (isSuspendFunction) {
            if (!isSuspendUnit) {
                methodBuilder.addStatement("val ${KSPParamBuilder.RESULT} = $methodCall")
            } else {
                methodBuilder.addStatement(methodCall)
            }
        } else {
            if (member.returnType?.resolve()?.isUnit() != true) {
                methodBuilder.addStatement("val ${KSPParamBuilder.RESULT} = $methodCall")
            } else {
                methodBuilder.addStatement(methodCall)
            }
        }
        methodBuilder.addStatement("RemoterGlobalProperties.reset()")

        if (!isOneWay) {
            methodBuilder.beginControlFlow("if (${KSPParamBuilder.REPLY} != null)")
            methodBuilder.addStatement("${KSPParamBuilder.REPLY}.writeNoException()")

            if (isSuspendFunction) {
                if (!isSuspendUnit) {
                    val paramBuilder = bindingManager.getBuilderForParam(
                        remoterInterfaceElement,
                        member.getReturnTypeOfSuspend(resolver)
                    )
                    if (paramBuilder != null) {
                        paramBuilder.readResultsFromStub(
                            member,
                            member.getReturnTypeOfSuspend(resolver),
                            methodBuilder
                        )
                    } else {
                        logError("Unmarshallable return type " + member.returnType)
                    }
                }
            } else {
                if (member.returnType?.resolve()?.isUnit() != true) {
                    val paramBuilder = bindingManager.getBuilderForParam(
                        remoterInterfaceElement,
                        member.returnType!!.resolve()
                    )
                    if (paramBuilder != null) {
                        paramBuilder.readResultsFromStub(
                            member,
                            member.returnType!!.resolve(),
                            methodBuilder
                        )
                    } else {
                        logError("Unmarshallable return type " + member.returnType)
                    }
                }
            }


            for ((paramIndex, param) in member.parameters.withIndex()) {
                if (isSuspendFunction && param.type.resolve().declaration.qualifiedName?.asString() == "kotlin.coroutines.Continuation") {
                    break
                }
                val varArgParamIndex = declaredParamsSize - 1
                val paramType = when {
                    param.getAnnotation("remoter.annotations.ParamIn") != null -> KSPParamBuilder.ParamType.IN
                    param.getAnnotation("remoter.annotations.ParamOut") != null -> KSPParamBuilder.ParamType.OUT
                    else -> KSPParamBuilder.ParamType.IN_OUT
                }

                if (paramType != KSPParamBuilder.ParamType.IN) {
                    val paramBuilder = bindingManager.getBuilderForParam(
                        remoterInterfaceElement,
                        param.type.resolve()
                    )

                    if (paramBuilder != null) {
                        val paramName = "arg_stb_$paramIndex"
                        paramNames.add(paramName)
                        paramBuilder.readOutResultsFromStub(
                            param,
                            paramType,
                            paramName,
                            methodBuilder
                        )
                    }
                }
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
    private fun addProxyMethods(
        classBuilder: TypeSpec.Builder,
        member: KSFunctionDeclaration,
        methodIndex: Int
    ) {
        val methodName = member.simpleName.asString()
        val isOnewayAnnotated = member.getAnnotation("remoter.annotations.Oneway") != null
        val isOneWay = (member.returnType?.resolve()?.isUnit() == true && isOnewayAnnotated)
        if (!isOneWay && isOnewayAnnotated) {
            logWarning("@Oneway is expected only for methods with void return. Ignoring it for $methodName")
        }

        val isSuspendFunction = member.isSuspendFunction()
        val isSuspendReturnNullable =
            if (isSuspendFunction) member.isSuspendReturningNullable() else false
        val isSuspendUnit =
            if (isSuspendFunction) (member.getReturnTypeOfSuspend(resolver).isUnit()) else false

        val methodBuilder = FunSpec.builder(methodName)
            .addModifiers(KModifier.PUBLIC)
            .addModifiers(KModifier.OVERRIDE)

        if (isSuspendFunction) {
            methodBuilder.addModifiers(KModifier.SUSPEND)
            methodBuilder.returns(
                member.getReturnTypeOfSuspend(resolver).asKotlinType().copy(isSuspendReturnNullable)
            )
        } else {
            methodBuilder.returns(
                member.returnType?.resolve()?.asKotlinType()?.copy(member.isNullable()) ?: UNIT
            )
        }

        //add parameters
        val declaredParamsSize = member.parameters.size
        var paramIndex = 0
        for (params in member.parameters) {
            if (isSuspendFunction && params.type.resolve().declaration.qualifiedName?.asString() == "kotlin.coroutines.Continuation") {
                break
            }

            val varArgParamIndex = declaredParamsSize - 1

            if (paramIndex == varArgParamIndex && member.isVarArgs) {
                val arrayComponentType = params.type.resolve()
                methodBuilder.addParameter(
                    ParameterSpec.builder(
                        params.name!!.asString(),
                        arrayComponentType.asKotlinType().copy(true)
                    ).addModifiers(KModifier.VARARG).build()
                )
            } else {
                methodBuilder.addParameter(
                    ParameterSpec.builder(
                        params.name!!.asString(),
                        params.type.resolve().asKotlinType().copy(params.isNullable())
                    ).build()
                )
            }

            paramIndex++
        }

        if (isSuspendFunction) {
            methodBuilder.beginControlFlow(
                "return kotlinx.coroutines.withContext(%T.IO)",
                ClassName("kotlinx.coroutines", "Dispatchers")
            )
        }

        //add statements
        methodBuilder.addStatement(
            "val ${KSPParamBuilder.DATA} = %T.obtain()",
            ClassName.bestGuess("android.os.Parcel")
        )
        if (!isOneWay) {
            methodBuilder.addStatement(
                "val ${KSPParamBuilder.REPLY} = %T.obtain()",
                ClassName.bestGuess("android.os.Parcel")
            )
        }

        if (isSuspendFunction) {
            if (!isSuspendUnit) {
                methodBuilder.addStatement(
                    "var ${KSPParamBuilder.RESULT}: %T",
                    member.getReturnTypeOfSuspend(resolver).asKotlinType()
                        .copy(isSuspendReturnNullable)
                )
            }
        } else {
            if (member.returnType?.resolve()?.isUnit() != true) {
                methodBuilder.addStatement(
                    "var ${KSPParamBuilder.RESULT}: %T",
                    member.returnType?.resolve()?.asKotlinType()?.copy(member.isNullable()) ?: UNIT
                )
            }
        }

        //start main body block
        methodBuilder.beginControlFlow("try")
        methodBuilder.addStatement("${KSPParamBuilder.DATA}.writeInterfaceToken(DESCRIPTOR)")
        val outParams: MutableList<KSFunctionDeclaration> = mutableListOf()

        //pass parameters
        paramIndex = 0
        for (param in member.parameters) {
            if (isSuspendFunction && param.type.resolve().declaration.qualifiedName?.asString() == "kotlin.coroutines.Continuation") {
                break
            }

            val paramType = when {
                param.getAnnotation("remoter.annotations.ParamIn") != null -> KSPParamBuilder.ParamType.IN
                param.getAnnotation("remoter.annotations.ParamOut") != null -> KSPParamBuilder.ParamType.OUT
                else -> KSPParamBuilder.ParamType.IN_OUT
            }

            if (paramType != KSPParamBuilder.ParamType.IN) {
                outParams.add(member)
            }
            val paramBuilder: KSPParamBuilder =
                bindingManager.getBuilderForParam(remoterInterfaceElement, param.type.resolve())
            if (paramBuilder != null) {
                paramBuilder.writeParamsToProxy(param, paramType, methodBuilder)
            } else {
                logError("Parameter cannot be marshalled " + param.name)
            }
            paramIndex++
        }

        var remoterCall = "_getRemoteServiceBinder().transact"
        if (bindingManager.hasRemoterBuilder()) {
            if (isSuspendFunction) {
                remoterCall = "_getRemoteServiceBinderSuspended().transact"
            }
        }

        methodBuilder.addStatement(
            "${KSPParamBuilder.DATA}.writeMap(__global_properties as %T<*, *>?)",
            MAP
        );

        //send remote command
        if (isOneWay) {
            methodBuilder.addStatement("$remoterCall(TRANSACTION_" + methodName + "_" + methodIndex + ", ${KSPParamBuilder.DATA}, null, android.os.IBinder.FLAG_ONEWAY)")
        } else {
            methodBuilder.addStatement("$remoterCall(TRANSACTION_" + methodName + "_" + methodIndex + ", ${KSPParamBuilder.DATA}, ${KSPParamBuilder.REPLY}, 0)")
            //read exception if any
            methodBuilder.addStatement("val __exception = checkException(${KSPParamBuilder.REPLY})")
            methodBuilder.beginControlFlow("if(__exception != null)")
            methodBuilder.addStatement("throw __exception ")
            methodBuilder.endControlFlow()
            //read result
            if (isSuspendFunction) {
                if (!isSuspendUnit) {
                    val paramBuilder: KSPParamBuilder = bindingManager.getBuilderForParam(
                        remoterInterfaceElement,
                        member.getReturnTypeOfSuspend(resolver)
                    )
                    if (paramBuilder != null) {
                        paramBuilder.readResultsFromProxy(member, methodBuilder)
                    } else {
                        logError("Unmarshallable return type " + member.returnType)
                        methodBuilder.addStatement("result = null")
                    }
                }
            } else {
                if (member.returnType?.resolve()?.isUnit() != true) {
                    val paramBuilder: KSPParamBuilder = bindingManager.getBuilderForParam(
                        remoterInterfaceElement,
                        member.returnType!!.resolve()
                    )
                    if (paramBuilder != null) {
                        paramBuilder.readResultsFromProxy(member, methodBuilder)
                    } else {
                        logError("Unmarshallable return type " + member.returnType)
                        methodBuilder.addStatement("result = null")
                    }
                }
            }

            for (param in member.parameters) {
                if (isSuspendFunction && param.type.resolve().declaration.qualifiedName?.asString() == "kotlin.coroutines.Continuation") {
                    break
                }

                val paramType = when {
                    param.getAnnotation("remoter.annotations.ParamIn") != null -> KSPParamBuilder.ParamType.IN
                    param.getAnnotation("remoter.annotations.ParamOut") != null -> KSPParamBuilder.ParamType.OUT
                    else -> KSPParamBuilder.ParamType.IN_OUT
                }

                if (paramType != KSPParamBuilder.ParamType.IN) {
                    val paramBuilder: KSPParamBuilder = bindingManager.getBuilderForParam(
                        remoterInterfaceElement,
                        param.type.resolve()
                    )
                    if (paramBuilder != null) {
                        paramBuilder?.readOutParamsFromProxy(param, paramType, methodBuilder)
                    }
                }
                paramIndex++
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
            methodBuilder.addStatement("${KSPParamBuilder.REPLY}.recycle()")
        }
        methodBuilder.addStatement("${KSPParamBuilder.DATA}.recycle()")
        methodBuilder.endControlFlow()

        if (isSuspendFunction) {
            if (!isSuspendUnit) {
                methodBuilder.addStatement(KSPParamBuilder.RESULT)
            }
            methodBuilder.endControlFlow()
        } else {
            if (member.returnType?.resolve()?.isUnit() != true) {
                methodBuilder.addStatement("return ${KSPParamBuilder.RESULT}")
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
            .addStatement(
                "val creator = Class.forName(\"\$pClassName\$\\\$Parcelable\").getField(\"CREATOR\")[null] as %T.Creator<*>",
                ClassName("android.os", "Parcelable")
            )
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
        addProxyDeathMethod(
            classBuilder,
            "linkToDeath",
            "Register a {@link android.os.IBinder.DeathRecipient} to know of binder connection lose\n"
        )
        addProxyDeathMethod(
            classBuilder,
            "unlinkToDeath",
            "UnRegisters a {@link android.os.IBinder.DeathRecipient}\n"
        )
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

        methodBuilder = FunSpec.builder("destroyProxy")
            .addModifiers(KModifier.PUBLIC)
            .addModifiers(KModifier.OVERRIDE)
            .returns(Unit::class)
        if (bindingManager.hasRemoterBuilder()) {
            methodBuilder.addStatement(
                "_proxyScope.%M()",
                MemberName("kotlinx.coroutines", "cancel")
            )
        }
        methodBuilder.addStatement("this.remoteBinder = null")
            .addStatement("unRegisterProxyListener(null)")
            .beginControlFlow("synchronized (stubMap)")
            .beginControlFlow("stubMap.values.forEach")
            .addStatement("(it as %T).destroyStub()", RemoterStub::class)
            .endControlFlow()
            .addStatement("stubMap.clear()")
            .endControlFlow()
        if (bindingManager.hasRemoterBuilder()) {
            methodBuilder.addStatement("_remoterServiceConnector?.disconnect()")
        }
        classBuilder.addFunction(methodBuilder.build())

        methodBuilder = FunSpec.builder("setRemoterGlobalProperties")
            .addModifiers(KModifier.PUBLIC)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                "properties", ClassName("kotlin.collections", "MutableMap").parameterizedBy(
                    String::class.asTypeName(),
                    Any::class.asTypeName()
                ).copy(true)
            )
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
    private fun addProxyDeathMethod(
        classBuilder: TypeSpec.Builder,
        deathMethod: String,
        doc: String
    ) {
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
     * Add proxy method to set hashcode to unique id of binder
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
        val typeParams = remoterInterfaceElement.typeParameters.joinToString(", ") { "*" }
        val proxyClassWithGenerics =
            if (typeParams.isNotEmpty()) "$remoterInterfaceClassName${KSPClassBuilder.PROXY_SUFFIX}<$typeParams>" else "$remoterInterfaceClassName${KSPClassBuilder.PROXY_SUFFIX}"
        val methodBuilder = FunSpec.builder("equals")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("other", Any::class.asTypeName().copy(true))
            .returns(Boolean::class)
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("return (other is $proxyClassWithGenerics) && (other.hashCode() == hashCode()) && (_stubProcess == other._stubProcess)")
        classBuilder.addFunction(methodBuilder.build())
    }

    /**
     * Add proxy method for toString
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
     * Add proxy method for _getRemoteServiceBinderSuspended
     */
    private fun addProxyGetServiceSuspended(classBuilder: TypeSpec.Builder) {
        if (bindingManager.hasRemoterBuilder()) {
            val methodBuilder = FunSpec.builder("_getRemoteServiceBinderSuspended")
                .addModifiers(KModifier.PRIVATE)
                .addModifiers(KModifier.SUSPEND)
                .addParameter(
                    ParameterSpec.builder("waitForInit", Boolean::class)
                        .defaultValue("true")
                        .build()
                )
                .returns(ClassName("android.os", "IBinder"))
                .addStatement("val sConnector = _remoterServiceConnector")
                .beginControlFlow("return if (sConnector != null)")
                .addStatement("val rBinder = sConnector.getService()")
                .beginControlFlow("if (waitForInit)")
                .addStatement("_serviceInitComplete.await()")
                .endControlFlow()
                .addStatement("rBinder")
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement(
                    "remoteBinder?: throw %T(\"No remote binder or IServiceConnector set\")",
                    RuntimeException::class
                )
                .endControlFlow()

            classBuilder.addFunction(methodBuilder.build())
        }

        val methodBuilderNonSuspended = FunSpec.builder("_getRemoteServiceBinder")
            .addModifiers(KModifier.PRIVATE)
            .returns(ClassName("android.os", "IBinder"))
            .addParameter(
                ParameterSpec.builder("waitForInit", Boolean::class)
                    .defaultValue("true")
                    .build()
            )
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
                .beginControlFlow("if (remoteBinder == null && waitForInit)")
                .addStatement("kotlinx.coroutines.runBlocking { _serviceInitComplete.await() }")
                .endControlFlow()
                .addStatement(
                    "return result?: throw %T(\"No remote binder or IServiceConnector set\")",
                    RuntimeException::class
                )
        } else {
            methodBuilderNonSuspended.addStatement("val result = remoteBinder")
            if (bindingManager.hasRemoterBuilder()) {
                methodBuilderNonSuspended.beginControlFlow("if (waitForInit)")
                    .addStatement("kotlinx.coroutines.runBlocking { _serviceInitComplete.await() }")
                    .endControlFlow()
            }
            methodBuilderNonSuspended.addStatement(
                "return result?: throw %T(\"No remote binder or IServiceConnector set\")",
                RuntimeException::class
            )
        }

        classBuilder.addFunction(methodBuilderNonSuspended.build())
    }

    /**
     * Add proxy method to get unique id
     */
    private fun addGetId(classBuilder: TypeSpec.Builder) {
        addGetIdMethod(
            classBuilder,
            "__remoter_getStubID",
            "TRANSACTION__getStubID",
            "_getRemoteServiceBinder(false)"
        )
        addGetIdMethod(
            classBuilder,
            "__remoter_getStubProcessID",
            "TRANSACTION__getStubProcessID",
            "_getRemoteServiceBinder(false)"
        )
        if (bindingManager.hasRemoterBuilder()) {
            addGetIdMethod(
                classBuilder,
                "__remoter_getStubID_sus",
                "TRANSACTION__getStubID",
                "_getRemoteServiceBinderSuspended(false)",
                true
            )
            addGetIdMethod(
                classBuilder,
                "__remoter_getStubProcessID_sus",
                "TRANSACTION__getStubProcessID",
                "_getRemoteServiceBinderSuspended(false)",
                true
            )
        }
    }

    /**
     * Add proxy method to get unique id
     */
    private fun addGetIdMethod(
        classBuilder: TypeSpec.Builder,
        methodName: String,
        descriptorName: String,
        getMethod: String,
        isSuspended: Boolean = false
    ) {
        val methodBuilder = FunSpec.builder(methodName)
            .addModifiers(KModifier.PRIVATE)
            .returns(Int::class)
            .addStatement("val data = Parcel.obtain()")
            .addStatement("val reply = Parcel.obtain()")
        methodBuilder.beginControlFlow("val result = try ")
        methodBuilder.addStatement("data.writeInterfaceToken(DESCRIPTOR)")
        methodBuilder.addStatement("$getMethod.transact($descriptorName, data, reply, 0)")
        methodBuilder.addStatement("val exception = checkException(reply)")
        methodBuilder.beginControlFlow("if(exception != null)")
        methodBuilder.addStatement("throw (exception as %T)", RuntimeException::class)
        methodBuilder.endControlFlow()
        methodBuilder.addStatement("reply.readInt()")
        methodBuilder.endControlFlow()
        methodBuilder.beginControlFlow("catch (ignored: %T)", Exception::class)
        methodBuilder.addStatement("hashCode()")
        methodBuilder.endControlFlow()
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
     * Add stub destroy methods
     */
    private fun addSubDestroyMethods(classBuilder: TypeSpec.Builder) {
        var methodBuilder = FunSpec.builder("destroyStub")
            .addModifiers(KModifier.PUBLIC)
            .addModifiers(KModifier.OVERRIDE)
            .beginControlFlow("try")
            .addStatement("this.attachInterface(null, DESCRIPTOR)")
            .addStatement("binderWrapper.binder = null")
            .endControlFlow()
            .beginControlFlow("catch (t:%T)", Throwable::class)
            .endControlFlow()
            .addStatement("serviceImpl = null")
        classBuilder.addFunction(methodBuilder.build())
    }

    /**
     * Add stub method that get called for each transaction from where an exception could be thrown
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
            .addStatement(
                "val callingUid = %T.getMappedUid(Binder.getCallingUid())",
                RemoterGlobalProperties::class.java
            )

            .beginControlFlow("if (callingUid == 0)")
            .addStatement("return code")
            .endControlFlow()

            .addStatement("var __lastMethodIndexOfProxy = __processLastMethodMap.getOrDefault(callingUid, -1)")

            .beginControlFlow("if (__lastMethodIndexOfProxy == -1)")
            .addStatement("__lastMethodIndexOfProxy = code - 1")
            .addStatement("__processLastMethodMap[callingUid] = __lastMethodIndexOfProxy")
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

            .addStatement("return mappedCode")

        classBuilder.addFunction(methodBuilder.build())
    }
}
















