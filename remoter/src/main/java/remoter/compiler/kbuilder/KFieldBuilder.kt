package remoter.compiler.kbuilder


import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.*
import javax.lang.model.element.Element


/**
 * A [KRemoterBuilder] that knows how to generate the fields for stub and proxy
 */
internal class KFieldBuilder(element: Element, bindingManager: KBindingManager) : KRemoterBuilder(element, bindingManager) {

    fun addProxyFields(classBuilder: TypeSpec.Builder) {

        classBuilder.addProperty(PropertySpec.builder("_binderID", Int::class,
                KModifier.PRIVATE)
                .initializer("__remoter_getStubID()")
                .mutable()
                .build())
                .addProperty(PropertySpec.builder("_stubProcess", Int::class,
                        KModifier.PRIVATE)
                        .initializer("__remoter_getStubProcessID()")
                        .mutable()
                        .build())
                .addProperty(PropertySpec.builder("proxyListener",
                        ClassName.bestGuess("DeathRecipient").copy(true)).mutable()
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("null")
                        .build())

        if (bindingManager.hasRemoterBuilder()) {
            classBuilder.addProperty(PropertySpec.builder("_remoterServiceConnector", ClassName("remoter.builder", "IServiceConnector").copy(true),
                    KModifier.PRIVATE)
                    .mutable()
                    .initializer("null")
                    .build())


            classBuilder.addProperty(PropertySpec.builder("_proxyScope", CoroutineScope::class,
                    KModifier.PRIVATE)
                    .initializer("%M(%T.IO)", MemberName("kotlinx.coroutines", "CoroutineScope"), Dispatchers::class)
                    .build())

        }


        val companion = TypeSpec.companionObjectBuilder()

        addCommonFields(companion)

        val lastMethodIndex = intArrayOf(0)
        processRemoterElements(companion, object : ElementVisitor {
            override fun visitElement(classBuilder: TypeSpec.Builder, member: Element, methodIndex: Int, methodBuilder: FunSpec.Builder?) {
                addCommonFields(classBuilder, member, methodIndex)
                lastMethodIndex[0] = methodIndex
            }
        }, null)

        lastMethodIndex[0]++


        companion.addProperty(PropertySpec.builder("TRANSACTION__getStubID", Int::class)
                .addModifiers(KModifier.PRIVATE)
                .initializer("android.os.IBinder.FIRST_CALL_TRANSACTION + " + lastMethodIndex[0])
                .build())
        lastMethodIndex[0]++
        companion.addProperty(PropertySpec.builder("TRANSACTION__getStubProcessID", Int::class)
                .addModifiers(KModifier.PRIVATE)
                .initializer("android.os.IBinder.FIRST_CALL_TRANSACTION + " + lastMethodIndex[0])
                .build())

        classBuilder.addType(companion.build())

        classBuilder.addProperty(PropertySpec.builder("stubMap", ClassName("kotlin.collections", "MutableMap").parameterizedBy(
                Any::class.asTypeName(),
                ClassName("android.os", "IBinder").copy(true)),
                KModifier.PRIVATE)
                .initializer("%T()", WeakHashMap::class)
                .build())

        classBuilder.addProperty(PropertySpec.builder("__global_properties", ClassName("kotlin.collections", "MutableMap").parameterizedBy(
                String::class.asTypeName(),
                Any::class.asTypeName())
                .copy(nullable = true),
                KModifier.PRIVATE)
                .mutable()
                .initializer("null")
                .build())


    }

    fun addStubFields(classBuilder: TypeSpec.Builder) {

        val companion = TypeSpec.companionObjectBuilder()
        addCommonFields(companion)

        classBuilder.addProperty(PropertySpec.builder("binderWrapper", ClassName.bestGuess("BinderWrapper"))
                .initializer("BinderWrapper(this)")
                .addModifiers(KModifier.PRIVATE).build())


        classBuilder.addProperty(PropertySpec.builder("__lastMethodIndexOfProxy", Int::class)
                .addModifiers(KModifier.PRIVATE)
                .initializer("-1")
                .mutable()
                .build())


        val lastMethodIndex = intArrayOf(0)

        processRemoterElements(companion, object : ElementVisitor {
            override fun visitElement(classBuilder: TypeSpec.Builder, member: Element, methodIndex: Int, methodBuilder: FunSpec.Builder?) {
                addCommonFields(classBuilder, member, methodIndex)
                lastMethodIndex[0] = methodIndex
            }
        }, null)



        companion.addProperty(PropertySpec.builder("__lastMethodIndex", Int::class)
                .addModifiers(KModifier.PRIVATE)
                .initializer("android.os.IBinder.FIRST_CALL_TRANSACTION + " + lastMethodIndex[0])
                .build())

        companion.addProperty(PropertySpec.builder("checkStubProxyMatch", Boolean::class)
                .addModifiers(KModifier.PUBLIC)
                .addKdoc("Enable or disable stub proxy mismatch check. Default enabled. Turn it off if using Remoter client with AIDL server")
                .mutable()
                .initializer("true")
                .build())


        lastMethodIndex[0]++

        companion.addProperty(PropertySpec.builder("TRANSACTION__getStubID", Int::class)
                .addModifiers(KModifier.PRIVATE)
                .initializer("android.os.IBinder.FIRST_CALL_TRANSACTION + " + lastMethodIndex[0])
                .build())
        lastMethodIndex[0]++
        companion.addProperty(PropertySpec.builder("TRANSACTION__getStubProcessID", Int::class)
                .addModifiers(KModifier.PRIVATE)
                .initializer("android.os.IBinder.FIRST_CALL_TRANSACTION + " + lastMethodIndex[0])
                .build())

        classBuilder.addType(companion.build())
    }

    private fun addCommonFields(classBuilder: TypeSpec.Builder) { //Add descriptor
        classBuilder.addProperty(PropertySpec.builder("DESCRIPTOR", String::class)
                .addModifiers(KModifier.PRIVATE)
                .initializer("\"$remoterInterfacePackageName.$remoterInterfaceClassName\"")
                .build())
        classBuilder.addProperty(PropertySpec.builder("REMOTER_EXCEPTION_CODE", Int::class)
                .addModifiers(KModifier.PRIVATE)
                .initializer("-99999")
                .build())
    }

    private fun addCommonFields(classBuilder: TypeSpec.Builder, member: Element, methodIndex: Int) {
        val methodName = member.simpleName.toString()

        classBuilder.addProperty(PropertySpec.builder("TRANSACTION_" + methodName + "_" + methodIndex, Int::class)
                .addModifiers(KModifier.PROTECTED)
                .initializer("android.os.IBinder.FIRST_CALL_TRANSACTION + $methodIndex").build())
    }
}
