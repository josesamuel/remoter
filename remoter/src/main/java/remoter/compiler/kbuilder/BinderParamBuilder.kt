package remoter.compiler.kbuilder

import com.google.auto.common.MoreElements
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror


/**
 * A [ParamBuilder] for Binder type parameters
 */
internal class BinderParamBuilder(remoterInterfaceElement: Element, bindingManager: KBindingManager) : ParamBuilder(remoterInterfaceElement, bindingManager) {


    override fun writeParamsToProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {
        if (param.asType().kind == TypeKind.ARRAY) {
            val type = (param.asType() as ArrayType).componentType
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if(" + param.simpleName + " == null)")
                        .addStatement("$DATA.writeInt(-1)")
                        .nextControlFlow("else")
                        .addStatement("$DATA.writeInt(" + param.simpleName + ".size)")
                        .beginControlFlow("for(__item in " + param.simpleName + ")", type)
                        .addStatement("$DATA.writeStrongBinder(if(__item != null)  %T(__item)  else null)",
                                getStubClassName(type))
                        .endControlFlow()
                        .endControlFlow()
            } else {
                methodBuilder.addStatement("$DATA.writeInt(" + param.simpleName + ".size)")
                        .beginControlFlow("for(__item in " + param.simpleName + ")", type)
                        .addStatement("$DATA.writeStrongBinder(if(__item != null)  %T(__item)  else null)",
                                getStubClassName(type))
                        .endControlFlow()
            }
        } else {
            val binderName = param.simpleName.toString() + "_binder"
            methodBuilder.addStatement("var $binderName:IBinder?")
            if (param.isNullable()) {
                methodBuilder.beginControlFlow("if (" + param.simpleName + " != null)")
            }
            methodBuilder.beginControlFlow("synchronized (stubMap)")
            methodBuilder.addStatement(binderName + " = stubMap.get(" + param.simpleName + ")")
            methodBuilder.beginControlFlow("if ($binderName == null)")
            methodBuilder.addStatement(binderName + " = %T(" + param.simpleName + ")", getStubClassName(param.asType()))
            methodBuilder.addStatement("stubMap.put(" + param.simpleName + ", " + binderName + ")")
            methodBuilder.endControlFlow()
            methodBuilder.endControlFlow()
            if (param.isNullable()) {
                methodBuilder.endControlFlow()
                methodBuilder.beginControlFlow("else")
                methodBuilder.addStatement("$binderName = null")
                methodBuilder.endControlFlow()
            }
            methodBuilder.addStatement("$DATA.writeStrongBinder($binderName)")
        }
    }

    override fun readResultsFromStub(methodElement: ExecutableElement, resultType: TypeMirror, methodBuilder: FunSpec.Builder) {
        if (resultType.kind == TypeKind.ARRAY) {
            val type = (resultType as ArrayType).componentType
            if (methodElement.isNullable()) {
                methodBuilder.beginControlFlow("if($RESULT == null)")
                        .addStatement("$DATA.writeInt(-1)")
                        .nextControlFlow("else")
                        .addStatement("$DATA.writeInt($RESULT.size)")
                        .beginControlFlow("for(item in $RESULT)")
                        .addStatement("$DATA.writeStrongBinder( if(item != null) %T(item)  else null)",
                                getStubClassName(type))
                        .endControlFlow()
                        .endControlFlow()
            } else {
                methodBuilder.addStatement("$DATA.writeInt($RESULT.size)")
                        .beginControlFlow("for(item in $RESULT)")
                        .addStatement("$DATA.writeStrongBinder( if(item != null) %T(item)  else null)",
                                getStubClassName(type))
                        .endControlFlow()
            }
        } else {
            if (methodElement.isNullable()) {
                methodBuilder.addStatement("$REPLY.writeStrongBinder(if($RESULT != null)  %T($RESULT)  else null)",
                        getStubClassName(resultType))
            } else {
                methodBuilder.addStatement("$REPLY.writeStrongBinder(%T($RESULT))",
                        getStubClassName(resultType))

            }
        }
    }

    override fun readResultsFromProxy(methodType: ExecutableElement, methodBuilder: FunSpec.Builder) {
        val resultType = methodType.getReturnAsKotlinType()
        val resultMirror = methodType.getReturnAsTypeMirror()
        if (resultMirror.kind == TypeKind.ARRAY) {
            val type = (resultMirror as ArrayType).componentType
            methodBuilder.addStatement("val ___result_length = $REPLY.readInt()")
                    .beginControlFlow("if(___result_length == -1)")
            if (methodType.isNullable()) {
                methodBuilder.addStatement("$RESULT = null")
            } else {
                methodBuilder.addStatement("throw %T(\"Unexpected null result\")", NullPointerException::class)
            }

            methodBuilder.nextControlFlow("else")
                    .beginControlFlow("$RESULT = Array(___result_length)")
                    .addStatement("%T($REPLY.readStrongBinder())", getProxyClassName(type))
                    .endControlFlow()
                    .endControlFlow()
        } else {
            val binderName = "__result_binder"
            methodBuilder.addStatement("val $binderName = $REPLY.readStrongBinder()")
            if (resultType.isNullable) {
                methodBuilder.addStatement("$RESULT = null")
            }
            methodBuilder.beginControlFlow("if($binderName != null)")
            methodBuilder.addStatement("$RESULT = %T($binderName)", getProxyClassName(resultMirror))
            methodBuilder.addStatement("$RESULT.setRemoterGlobalProperties(__global_properties)")
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (!resultType.isNullable) {
                methodBuilder.addStatement("throw %T(\"Unexpected null result\")", NullPointerException::class.java)
            }
            methodBuilder.endControlFlow()

        }
    }

    override fun readOutResultsFromStub(param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {}
    override fun writeParamsToStub(methodType: ExecutableElement, param: VariableElement, paramType: ParamType, paramName: String, methodBuilder: FunSpec.Builder) {
        super.writeParamsToStub(methodType, param, paramType, paramName, methodBuilder)
        if (param.asType().kind == TypeKind.ARRAY) {
            val type = param.asKotlinType()
            methodBuilder.addStatement("val length = $DATA.readInt()")
                    .beginControlFlow("if(length == -1)")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            } else {
                methodBuilder.addStatement("throw NullPointerException(\"Not expecting null\")")
            }
            methodBuilder.nextControlFlow("else")
                    .beginControlFlow("$paramName = %T(length) ", type.copy(false))
                    .addStatement("%T($DATA.readStrongBinder())", getProxyClassName((param.asType() as ArrayType).componentType))
                    .endControlFlow()
                    .endControlFlow()
        } else {
            val binderName = paramName + "_binder"
            methodBuilder.addStatement("val $binderName = $DATA.readStrongBinder()")
            if (param.isNullable()) {
                methodBuilder.addStatement("$paramName = null")
            }
            methodBuilder.beginControlFlow("if($binderName != null)")
            methodBuilder.addStatement("$paramName =  %T($binderName)", getProxyClassName(param.asType()))
            methodBuilder.endControlFlow()
            methodBuilder.beginControlFlow("else")
            if (!param.isNullable()) {
                methodBuilder.addStatement("throw %T(\"Not expecting null\")", NullPointerException::class)
            }
            methodBuilder.endControlFlow()

        }
    }

    override fun readOutParamsFromProxy(param: VariableElement, paramType: ParamType, methodBuilder: FunSpec.Builder) {}
    /**
     * Returns the [ClassName] for the Stub
     */
    private fun getStubClassName(param: TypeMirror): ClassName {
        return getStubClassName(param.toString())
    }

    /**
     * Returns the [ClassName] for the Proxy
     */
    private fun getProxyClassName(param: TypeMirror): ClassName {
        return getProxyClassName(param.toString())
    }

    /**
     * Returns the [ClassName] for the Stub
     */
    private fun getStubClassName(name: String): ClassName {
        val element = bindingManager.getElement(name)
        return ClassName(MoreElements.getPackage(element).qualifiedName.toString(), element.simpleName.toString() + KClassBuilder.STUB_SUFFIX)
    }

    /**
     * Returns the [ClassName] for the Proxy
     */
    private fun getProxyClassName(name: String): ClassName {
        val element = bindingManager.getElement(name)
        return ClassName(MoreElements.getPackage(element).qualifiedName.toString(), element.simpleName.toString() + KClassBuilder.PROXY_SUFFIX)
    }
}