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
 * A {@link ParamBuilder} for Parcel type parameters
 */
class ParcelerParamBuilder extends ParamBuilder {

    /**
     * Initialize the builder
     */
    protected ParcelerParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }

    @Override
    public void writeParamsToProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder);
            } else {
                String wrapperName = param.getSimpleName() + "_wrapper";
                methodBuilder.addStatement("$T[] " + wrapperName + " = null", ClassName.get("android.os", "Parcelable"));
                methodBuilder.beginControlFlow("if (" + param.getSimpleName() + " != null)");
                methodBuilder.addStatement(wrapperName + " =  new Parcelable[" + param.getSimpleName() + ".length]");
                methodBuilder.beginControlFlow("for(int i=0; i<" + wrapperName + ".length; i++)");
                methodBuilder.addStatement(wrapperName + "[i] = org.parceler.Parcels.wrap( " + param.getSimpleName() + "[i])");
                methodBuilder.endControlFlow();
                methodBuilder.endControlFlow();
                methodBuilder.addStatement("data.writeTypedArray(" + wrapperName + ", 0)");
            }
        } else {
            methodBuilder.beginControlFlow("if (" + param.getSimpleName() + " != null)");
            methodBuilder.addStatement("data.writeInt(1)");
            methodBuilder.addStatement("org.parceler.Parcels.wrap(" + param.getSimpleName() + ").writeToParcel(data, 0)");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("data.writeInt(0)");
            methodBuilder.endControlFlow();
        }
    }

    @Override
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            String wrapperName = "result_wrapper";
            methodBuilder.addStatement("$T[] " + wrapperName + " = null", ClassName.get("android.os", "Parcelable"));
            methodBuilder.beginControlFlow("if (result != null)");
            methodBuilder.addStatement(wrapperName + " =  new Parcelable[result.length]");
            methodBuilder.beginControlFlow("for(int i=0; i<" + wrapperName + ".length; i++)");
            methodBuilder.addStatement(wrapperName + "[i] = org.parceler.Parcels.wrap(result[i])");
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("data.writeTypedArray(" + wrapperName + ", android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
        } else {
            methodBuilder.beginControlFlow("if (result != null)");
            methodBuilder.addStatement("reply.writeInt(1)");
            methodBuilder.addStatement("org.parceler.Parcels.wrap(result).writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("reply.writeInt(0)");
            methodBuilder.endControlFlow();
        }
    }

    @Override
    public void readOutResultsFromStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            String wrapperName = paramName + "_result_wrapper";
            methodBuilder.addStatement("$T[] " + wrapperName + " = null", ClassName.get("android.os", "Parcelable"));
            methodBuilder.beginControlFlow("if (" + paramName + " != null)");
            methodBuilder.addStatement(wrapperName + " =  new Parcelable[" + paramName + ".length]");
            methodBuilder.beginControlFlow("for(int i=0; i<" + wrapperName + ".length; i++)");
            methodBuilder.addStatement(wrapperName + "[i] = org.parceler.Parcels.wrap(" + paramName + "[i])");
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("data.writeTypedArray(" + wrapperName + ", android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
        }
    }


    @Override
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            String wrapperName = "result_wrapper";
            methodBuilder.addStatement("$T[] result_wrapper = reply.createTypedArray(" + getParcelableClassName(resultType) + ".CREATOR)", ClassName.get("android.os", "Parcelable"));

            methodBuilder.beginControlFlow("if (result_wrapper != null)");
            methodBuilder.addStatement("result =  new " + ((ArrayType) resultType).getComponentType() + "[result_wrapper.length]");
            methodBuilder.beginControlFlow("for(int i=0; i<" + wrapperName + ".length; i++)");
            methodBuilder.addStatement("result[i] = " + getParcelableClassName(resultType) + ".CREATOR.createFromParcel(reply).getParcel()");
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("result = null");
            methodBuilder.endControlFlow();
        } else {
            methodBuilder.beginControlFlow("if (reply.readInt() != 0)");
            methodBuilder.addStatement("result = " + getParcelableClassName(resultType) + ".CREATOR.createFromParcel(reply).getParcel()");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("result = null");
            methodBuilder.endControlFlow();

        }
    }

    @Override
    public void writeParamsToStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, paramName, methodBuilder);
        if (param.asType().getKind() == TypeKind.ARRAY) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, paramName, methodBuilder);
            } else {
                String wrapperName = param.getSimpleName() + "_wrapper";

                methodBuilder.addStatement("$T[] " + wrapperName + " = reply.createTypedArray(" + getParcelableClassName(param.asType()) + ".CREATOR)", ClassName.get("android.os", "Parcelable"));

                methodBuilder.beginControlFlow("if (" + wrapperName + " != null)");
                methodBuilder.addStatement(paramName + " =  new " + ((ArrayType) param.asType()).getComponentType() + "[" + wrapperName + ".length]");
                methodBuilder.beginControlFlow("for(int i=0; i<" + wrapperName + ".length; i++)");
                methodBuilder.addStatement(paramName + "[i] = " + getParcelableClassName(param.asType()) + ".CREATOR.createFromParcel(reply).getParcel()");
                methodBuilder.endControlFlow();
                methodBuilder.endControlFlow();
                methodBuilder.beginControlFlow("else");
                methodBuilder.addStatement(paramName + " = null");
                methodBuilder.endControlFlow();
            }
        } else {
            methodBuilder.beginControlFlow("if ( data.readInt() != 0)");
            methodBuilder.addStatement(paramName + " = " + getParcelableClassName(param.asType()) + ".CREATOR.createFromParcel(data).getParcel()");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement(paramName + " = null");
            methodBuilder.endControlFlow();
        }
    }

    private String getParcelableClassName(TypeMirror typeMirror) {
        if (typeMirror.getKind() != TypeKind.ARRAY) {
            return typeMirror.toString() + "$$$$Parcelable";
        } else {
            return getParcelableClassName(((ArrayType) typeMirror).getComponentType());
        }
    }

    @Override
    public void readOutParamsFromProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            if (param.asType().getKind() == TypeKind.ARRAY) {

                String wrapperName = param.getSimpleName() + "_result_wrapper";
                methodBuilder.addStatement("$T[] " + wrapperName + " = reply.createTypedArray(" + getParcelableClassName(param.asType()) + ".CREATOR)", ClassName.get("android.os", "Parcelable"));

                methodBuilder.beginControlFlow("if (" + wrapperName + " != null)");
                methodBuilder.beginControlFlow("for(int i=0; i<" + wrapperName + ".length; i++)");
                methodBuilder.addStatement(param.getSimpleName() + "[i] = " + getParcelableClassName(param.asType()) + ".CREATOR.createFromParcel(reply).getParcel()");
                methodBuilder.endControlFlow();
                methodBuilder.endControlFlow();

            }
        }
    }
}
