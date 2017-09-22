package remoter.compiler.builder;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import remoter.annotations.Remoter;

/**
 * Holds the mapping between the {@link ParamBuilder} for different types
 */
public final class BindingManager {

    private Elements elementUtils;
    private Messager messager;
    private Types typeUtils;
    private Filer filer;
    private Map<TypeMirror, ParamBuilder> typeBuilderMap;

    private TypeMirror stringTypeMirror;
    private TypeMirror charSequenceTypeMirror;
    private TypeMirror listTypeMirror;
    private TypeMirror mapTypeMirror;
    private TypeMirror parcellableTypeMirror;
    private Class parcelClass;

    /**
     * Initialize with the given details from the annotation processing enviornment
     */
    public BindingManager(Elements elementUtils, Filer filer, Messager messager, Types typeUtils) {
        this.elementUtils = elementUtils;
        this.filer = filer;
        this.messager = messager;
        this.typeUtils = typeUtils;
        this.typeBuilderMap = new HashMap<>();

        stringTypeMirror = getType("java.lang.String");
        listTypeMirror = getType("java.util.List");
        mapTypeMirror = getType("java.util.Map");
        charSequenceTypeMirror = getType("java.lang.CharSequence");
        parcellableTypeMirror = getType("android.os.Parcelable");
        try {
            parcelClass = Class.forName("org.parceler.Parcel");
        } catch (ClassNotFoundException ignored) {
        }
    }

    /**
     * Generates the Proxy for the given @{@link Remoter} interface element
     */
    public void generateProxy(Element element) {
        try {
            getClassBuilder(element)
                    .buildProxyClass()
                    .build()
                    .writeTo(filer);
        } catch (Exception ex) {
            messager.printMessage(Diagnostic.Kind.WARNING, "Error while generating Proxy " + ex.getMessage());
        }
    }

    /**
     * Generates the Stub for the given @{@link Remoter} interface element
     */
    public void generateStub(Element element) {
        try {
            getClassBuilder(element)
                    .buildStubClass()
                    .build()
                    .writeTo(filer);
        } catch (Exception ex) {
            messager.printMessage(Diagnostic.Kind.WARNING, "Error while generating Stub " + ex.getMessage());
        }
    }

    /**
     * Returns a {@link TypeMirror} for the given class
     */
    TypeMirror getType(String className) {
        return elementUtils.getTypeElement(className).asType();
    }

    /**
     * Returns a {@link Element} for the given class
     */
    Element getElement(String className) {
        return elementUtils.getTypeElement(className);
    }

    /**
     * Returns the {@link ClassBuilder} that generates the Builder for the Proxy and Stub classes
     */
    private ClassBuilder getClassBuilder(Element element) {
        ClassBuilder classBuilder = new ClassBuilder(messager, element);
        classBuilder.setBindingManager(this);
        return classBuilder;
    }

    /**
     * Returns the {@link FieldBuilder} that  adds fields to the class spec
     */
    FieldBuilder getFieldBuilder(Element element) {
        FieldBuilder fieldBuilder = new FieldBuilder(messager, element);
        fieldBuilder.setBindingManager(this);
        return fieldBuilder;
    }

    /**
     * Returns the {@link MethodBuilder} that adds methods to the class spec
     */
    MethodBuilder getMethoddBuilder(Element element) {
        MethodBuilder methodBuilder = new MethodBuilder(messager, element);
        methodBuilder.setBindingManager(this);
        return methodBuilder;
    }


    /**
     * Returns the {@link ParamBuilder} that knows how to generate code for the given type of parameter
     */
    ParamBuilder getBuilderForParam(TypeMirror typeMirror) {
        ParamBuilder paramBuilder = typeBuilderMap.get(typeMirror);
        if (paramBuilder == null) {
            switch (typeMirror.getKind()) {
                case BOOLEAN:
                    paramBuilder = new BooleanParamBuilder(messager, null);
                    break;
                case BYTE:
                    paramBuilder = new ByteParamBuilder(messager, null);
                    break;
                case CHAR:
                    paramBuilder = new CharParamBuilder(messager, null);
                    break;
                case DOUBLE:
                    paramBuilder = new DoubleParamBuilder(messager, null);
                    break;
                case FLOAT:
                    paramBuilder = new FloatParamBuilder(messager, null);
                    break;
                case INT:
                    paramBuilder = new IntParamBuilder(messager, null);
                    break;
                case LONG:
                    paramBuilder = new LongParamBuilder(messager, null);
                    break;
                case SHORT:
                    paramBuilder = new ShortParamBuilder(messager, null);
                    break;
                case ARRAY:
                    paramBuilder = getBuilderForParam(((ArrayType) typeMirror).getComponentType());
                    break;
                case DECLARED:
                    TypeElement genericList = getGenericType(typeMirror);
                    if (genericList != null) {
                        paramBuilder = new ListOfParcelerParamBuilder(messager, null, genericList);
                    } else if (typeUtils.isAssignable(typeMirror, stringTypeMirror)) {
                        paramBuilder = new StringParamBuilder(messager, null);
                    } else if (typeUtils.isAssignable(typeMirror, charSequenceTypeMirror)) {
                        paramBuilder = new CharSequenceParamBuilder(messager, null);
                    } else if (typeUtils.isAssignable(typeMirror, listTypeMirror) || typeMirror.toString().equals("java.util.List<java.lang.String>")) {
                        paramBuilder = new ListParamBuilder(messager, null);
                    } else if (typeUtils.isAssignable(typeMirror, mapTypeMirror)) {
                        paramBuilder = new MapParamBuilder(messager, null);
                    } else if (typeUtils.isAssignable(typeMirror, parcellableTypeMirror)) {
                        paramBuilder = new ParcellableParamBuilder(messager, null);
                    } else {
                        TypeElement typeElement = elementUtils.getTypeElement(typeMirror.toString());
                        if (typeElement != null) {
                            if (typeElement.getKind() == ElementKind.INTERFACE && typeElement.getAnnotation(Remoter.class) != null) {
                                paramBuilder = new BinderParamBuilder(messager, null);
                            } else if (parcelClass != null && typeElement.getAnnotation(parcelClass) != null) {
                                paramBuilder = new ParcelerParamBuilder(messager, null);
                            }
                        }
                    }
                    break;
            }
            if (paramBuilder != null) {
                paramBuilder.setBindingManager(this);
                typeBuilderMap.put(typeMirror, paramBuilder);
            } else {
                paramBuilder = new GenericParamBuilder(messager, null);
                paramBuilder.setBindingManager(this);
                typeBuilderMap.put(typeMirror, paramBuilder);
            }
        }
        return paramBuilder;
    }

    /**
     * Return the generic type if any
     */
    public TypeElement getGenericType(TypeMirror typeMirror) {
        return typeMirror.accept(new SimpleTypeVisitor6<TypeElement, Void>() {
            @Override
            public TypeElement visitDeclared(DeclaredType declaredType, Void v) {
                TypeElement genericTypeElement = null;
                TypeElement typeElement = (TypeElement) declaredType.asElement();
                if (parcelClass != null && typeUtils.isAssignable(typeElement.asType(), listTypeMirror)) {
                    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                    if (!typeArguments.isEmpty()) {
                        for (TypeMirror genericType : typeArguments) {
                            if (genericType instanceof WildcardType) {
                                WildcardType wildcardType = (WildcardType) genericType;
                                TypeMirror extendsType = wildcardType.getExtendsBound();
                                if (extendsType != null) {
                                    typeElement = elementUtils.getTypeElement(extendsType.toString());
                                    if (typeElement.getAnnotation(parcelClass) != null) {
                                        genericTypeElement = typeElement;
                                        break;
                                    }

                                }
                            } else {
                                typeElement = elementUtils.getTypeElement(genericType.toString());
                                if (typeElement.getAnnotation(parcelClass) != null) {
                                    genericTypeElement = typeElement;
                                    break;
                                }
                            }
                        }
                    }
                }
                return genericTypeElement;
            }
        }, null);
    }


}
