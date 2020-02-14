package remoter.compiler.kbuilder


import com.squareup.kotlinpoet.*
import remoter.annotations.Remoter
import java.io.File
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.*
import javax.lang.model.util.Elements
import javax.lang.model.util.SimpleTypeVisitor6

/**
 * Manages kotlin file generation
 */
open class KBindingManager(private val processingEnvironment: ProcessingEnvironment) {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
        const val PARCELER_ANNOTATION = "org.parceler.Parcel"
    }

    private val elementUtils: Elements = processingEnvironment.elementUtils
    private var typeUtils = processingEnvironment.typeUtils
    private var typeBuilderMap: MutableMap<TypeMirror, ParamBuilder> = mutableMapOf()

    private var stringTypeMirror: TypeMirror? = null
    private var charSequenceTypeMirror: TypeMirror? = null
    private var listTypeMirror: TypeMirror? = null
    private var setTypeMirror: TypeMirror? = null
    private var mapTypeMirror: TypeMirror? = null
    private var parcellableTypeMirror: TypeMirror? = null
    private var parcelClass: Class<*>? = null
    private var remoterBuilderClass: Class<*>? = null

    private val javaByeType = ClassName("java.lang", "Byte")
    private val javaBooleanType = ClassName("java.lang", "Boolean")
    private val javaIntegerType = ClassName("java.lang", "Integer")
    private val javaShortType = ClassName("java.lang", "Short")
    private val javaLongType = ClassName("java.lang", "Long")
    private val javaFloatType = ClassName("java.lang", "Float")
    private val javaDoubleType = ClassName("java.lang", "Double")
    private val javaCharType = ClassName("java.lang", "Character")


    init {

        stringTypeMirror = getType("java.lang.String")
        listTypeMirror = getType("java.util.List")
        setTypeMirror = getType("java.util.Set")
        mapTypeMirror = getType("java.util.Map")
        charSequenceTypeMirror = getType("java.lang.CharSequence")
        parcellableTypeMirror = getType("android.os.Parcelable")
        try {
            parcelClass = Class.forName("org.parceler.Parcel")
        } catch (ignored: ClassNotFoundException) {
        }

        try {
            remoterBuilderClass = Class.forName("remoter.builder.ServiceConnector")
        } catch (ignored: ClassNotFoundException) {
        }

    }

    /**
     * Returns a [TypeMirror] for the given class
     */
    fun getType(className: String?): TypeMirror {
        return elementUtils.getTypeElement(className).asType()
    }


    /**
     * Returns a [Element] for the given class
     */
    fun getElement(className: String): Element {
        var cName = className
        val templateStart = className.indexOf('<')
        if (templateStart != -1) {
            cName = className.substring(0, templateStart).trim()
        }
        return elementUtils.getTypeElement(cName)
    }

    /**
     * Generates the abstract publisher class
     */
    fun generateProxy(element: Element) {
        val kaptKotlinGeneratedDir = processingEnvironment.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]?.replace("kaptKotlin", "kapt")
        kaptKotlinGeneratedDir?.let {
            KClassBuilder(element, this).generateProxy().writeTo(File(kaptKotlinGeneratedDir))
        }
    }

    /**
     * Generates the abstract publisher class
     */
    fun generateStub(element: Element) {
        val kaptKotlinGeneratedDir = processingEnvironment.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]?.replace("kaptKotlin", "kapt")
        kaptKotlinGeneratedDir?.let {
            KClassBuilder(element, this).generateStub().writeTo(File(kaptKotlinGeneratedDir))
        }
    }


    fun getMessager(): Messager = processingEnvironment.messager

    fun hasRemoterBuilder() = remoterBuilderClass != null


    /**
     * Returns the [KFieldBuilder] that  adds fields to the class spec
     */
    internal fun getFieldBuilder(element: Element) = KFieldBuilder(element, this)


    internal fun getFunctiondBuilder(element: Element) = KMethodBuilder(element, this)


    /**
     * Returns the [ParamBuilder] that knows how to generate code for the given type of parameter
     */
    internal open fun getBuilderForParam(remoteElement: Element, typeMirror: TypeMirror): ParamBuilder {
        val typeName = typeMirror.asTypeName()

        var typeElementType: Element? = null
        if (typeMirror is DeclaredType) {
            typeElementType = typeMirror.asElement()
        }


        var paramBuilder: ParamBuilder? = typeBuilderMap[typeMirror]
        if (paramBuilder == null) {
            when (typeMirror.kind) {
                TypeKind.BOOLEAN -> paramBuilder = BooleanParamBuilder(remoteElement, this)
                TypeKind.BYTE -> paramBuilder = ByteParamBuilder(remoteElement, this)
                TypeKind.CHAR -> paramBuilder = CharParamBuilder(remoteElement, this)
                TypeKind.DOUBLE -> paramBuilder = DoubleParamBuilder(remoteElement, this)
                TypeKind.FLOAT -> paramBuilder = FloatParamBuilder(remoteElement, this)
                TypeKind.INT -> paramBuilder = IntParamBuilder(remoteElement, this)
                TypeKind.LONG -> paramBuilder = LongParamBuilder(remoteElement, this)
                TypeKind.SHORT -> paramBuilder = ShortParamBuilder(remoteElement, this)
                TypeKind.ARRAY -> paramBuilder = getBuilderForParam(remoteElement, (typeMirror as ArrayType).componentType)
                TypeKind.DECLARED -> {
                    when (typeName) {
                        javaIntegerType -> paramBuilder = IntParamBuilder(remoteElement, this)
                        javaByeType -> paramBuilder = ByteParamBuilder(remoteElement, this)
                        javaBooleanType -> paramBuilder = BooleanParamBuilder(remoteElement, this)
                        javaCharType -> paramBuilder = CharParamBuilder(remoteElement, this)
                        javaDoubleType -> paramBuilder = DoubleParamBuilder(remoteElement, this)
                        javaFloatType -> paramBuilder = FloatParamBuilder(remoteElement, this)
                        javaLongType -> paramBuilder = LongParamBuilder(remoteElement, this)
                        javaShortType -> paramBuilder = ShortParamBuilder(remoteElement, this)
                        else -> {


                            val baseElementName = typeElementType?.simpleName?.toString()
                            val genericList: TypeElement? = getGenericType(typeMirror)
                            if (genericList != null) {
                                paramBuilder = ListOfParcelerParamBuilder(genericList, remoteElement, this)
                            } else if (typeUtils.isAssignable(typeMirror, stringTypeMirror) || typeName == STRING || baseElementName == "String") {
                                paramBuilder = StringParamBuilder(remoteElement, this)
                            } else if (typeUtils.isAssignable(typeMirror, charSequenceTypeMirror) || typeName == CHAR_SEQUENCE || baseElementName == "CharSequence") {
                                paramBuilder = CharSequenceParamBuilder(remoteElement, this)
                            } else if (typeUtils.isAssignable(typeMirror, listTypeMirror) || typeMirror.toString() == "java.util.List<java.lang.String>"
                                    || typeName == MUTABLE_LIST || baseElementName == "List") {
                                paramBuilder = ListParamBuilder(remoteElement, this)
                            } else if (typeUtils.isAssignable(typeMirror, mapTypeMirror) || typeName == MUTABLE_MAP || baseElementName == "Map") {
                                paramBuilder = MapParamBuilder(remoteElement, this)
                            } else if (typeUtils.isAssignable(typeMirror, parcellableTypeMirror)) {
                                paramBuilder = ParcellableParamBuilder(remoteElement, this)
                            } else {
                                val elementName = (typeMirror as DeclaredType).asElement().toString()
                                val typeElement: TypeElement? = elementUtils.getTypeElement(elementName)

                                if (typeElement != null) {
                                    if (typeElement.kind == ElementKind.INTERFACE && typeElement.getAnnotation(Remoter::class.java) != null) {
                                        paramBuilder = BinderParamBuilder(remoteElement, this)
                                    } else if (parcelClass != null && (typeElement.annotationMirrors.any {
                                                it.annotationType.toString() == PARCELER_ANNOTATION
                                            })) {
                                        paramBuilder = ParcelerParamBuilder(remoteElement, this)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                }
            }
            if (paramBuilder != null) {
                typeBuilderMap[typeMirror] = paramBuilder
            } else {
                paramBuilder = GenericParamBuilder(remoteElement, this)
                typeBuilderMap[typeMirror] = paramBuilder
            }
        }
        return paramBuilder
    }


    /**
     * Return the generic type if any
     */
    private fun getGenericType(typeMirror: TypeMirror?): TypeElement? {
        return typeMirror?.accept(object : SimpleTypeVisitor6<TypeElement?, Void>() {
            override fun visitDeclared(declaredType: DeclaredType?, v: Void?): TypeElement? {
                var genericTypeElement: TypeElement? = null
                var typeElement = declaredType?.asElement() as TypeElement?
                if (parcelClass != null && typeElement != null && typeUtils.isAssignable(typeElement.asType(), listTypeMirror)) {
                    val typeArguments = declaredType?.typeArguments
                    if (typeArguments != null && typeArguments.isNotEmpty()) {
                        for (genericType in typeArguments) {
                            if (genericType is WildcardType) {
                                val extendsType = genericType.extendsBound
                                if (extendsType != null) {
                                    typeElement = elementUtils.getTypeElement(extendsType.toString())

                                    if (typeElement.annotationMirrors.any { it.annotationType.toString() == PARCELER_ANNOTATION }) {
                                        genericTypeElement = typeElement
                                        break
                                    }
                                }
                            } else {
                                typeElement = elementUtils.getTypeElement(genericType.toString())
                                if (typeElement.annotationMirrors.any { it.annotationType.toString() == PARCELER_ANNOTATION }) {
                                    genericTypeElement = typeElement
                                    break
                                }
                            }
                        }
                    }
                }
                return genericTypeElement
            }
        }, null)
    }
}
