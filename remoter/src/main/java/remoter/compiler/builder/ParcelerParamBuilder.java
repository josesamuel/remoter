package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

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
                methodBuilder.beginControlFlow("if (" + param.getSimpleName() + " != null)");
                methodBuilder.addStatement("data.writeInt(" + param.getSimpleName() + ".length)");
                methodBuilder.beginControlFlow("for($T item:" + param.getSimpleName() + " )", ((ArrayType) param.asType()).getComponentType());
                methodBuilder.addStatement("org.parceler.Parcels.wrap(item).writeToParcel(data, 0)");
                methodBuilder.endControlFlow();
                methodBuilder.endControlFlow();
                methodBuilder.beginControlFlow("else");
                methodBuilder.addStatement("data.writeInt(-1)");
                methodBuilder.endControlFlow();


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
            methodBuilder.beginControlFlow("if (result != null)");
            methodBuilder.addStatement("reply.writeInt(result.length)");
            methodBuilder.beginControlFlow("for($T item:result )", ((ArrayType) resultType).getComponentType());
            methodBuilder.addStatement("org.parceler.Parcels.wrap(item).writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("reply.writeInt(-1)");
            methodBuilder.endControlFlow();

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
            methodBuilder.beginControlFlow("if (" + paramName + " != null)");
            methodBuilder.addStatement("reply.writeInt(" + paramName + ".length)");
            methodBuilder.beginControlFlow("for($T item:" + paramName + " )", ((ArrayType) param.asType()).getComponentType());
            methodBuilder.addStatement("org.parceler.Parcels.wrap(item).writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("reply.writeInt(-1)");
            methodBuilder.endControlFlow();
        }
    }


    @Override
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            methodBuilder.addStatement("int size_result = reply.readInt()");
            methodBuilder.beginControlFlow("if (size_result >= 0)");
            methodBuilder.addStatement("result = new $T[size_result]", ((ArrayType) resultType).getComponentType());
            methodBuilder.beginControlFlow("for(int i=0; i<size_result; i++)");
            methodBuilder.addStatement("result[i]= " + getParcelableClassName(((ArrayType) resultType).getComponentType()) + ".CREATOR.createFromParcel(reply).getParcel()");
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
                methodBuilder.addStatement("int size_" + paramName + " = data.readInt()");
                methodBuilder.beginControlFlow("if (size_" + paramName + " >= 0)");
                methodBuilder.addStatement(paramName + " = new $T[size_" + paramName + "]", ((ArrayType) param.asType()).getComponentType());
                methodBuilder.beginControlFlow("for(int i=0; i<size_" + paramName + "; i++)");
                methodBuilder.addStatement(paramName + "[i] = " + getParcelableClassName(((ArrayType) param.asType()).getComponentType()) + ".CREATOR.createFromParcel(data).getParcel()");
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
                methodBuilder.addStatement("int size_" + param.getSimpleName() + " = reply.readInt()");
                methodBuilder.beginControlFlow("for(int i=0; i<size_" + param.getSimpleName() + "; i++)");
                methodBuilder.addStatement(param.getSimpleName() + "[i]= " + getParcelableClassName(((ArrayType) param.asType()).getComponentType()) + ".CREATOR.createFromParcel(reply).getParcel()");
                methodBuilder.endControlFlow();
            }
        }
    }
}
