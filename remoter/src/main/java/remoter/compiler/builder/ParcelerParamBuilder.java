package remoter.compiler.builder;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

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
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder);
            } else {
                methodBuilder.beginControlFlow("if ($L != null)", param.name);
                methodBuilder.addStatement("data.writeInt($L.length)", param.name);
                methodBuilder.beginControlFlow("for($T item : $L)", ((ArrayTypeName) param.type).componentType, param.name);
                methodBuilder.addStatement("Class pClass = getParcelerClass(item)");
                methodBuilder.addStatement("data.writeString(pClass.getName())");
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel(data, 0)");
                methodBuilder.endControlFlow();
                methodBuilder.endControlFlow();
                methodBuilder.beginControlFlow("else");
                methodBuilder.addStatement("data.writeInt(-1)");
                methodBuilder.endControlFlow();


            }
        } else {
            methodBuilder.beginControlFlow("if ($L != null)", param.name);
            methodBuilder.addStatement("data.writeInt(1)");
            methodBuilder.addStatement("Class pClass = getParcelerClass($L)", param.name);
            methodBuilder.addStatement("data.writeString(pClass.getName())");
            methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, $L).writeToParcel(data, 0)", param.name);
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("data.writeInt(0)");
            methodBuilder.endControlFlow();
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.beginControlFlow("if (result != null)");
            methodBuilder.addStatement("reply.writeInt(result.length)");
            methodBuilder.beginControlFlow("for($T item:result )", ((ArrayTypeName) resultType).componentType);
            methodBuilder.addStatement("Class pClass = getParcelerClass(item)");
            methodBuilder.addStatement("reply.writeString(pClass.getName())");
            methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("reply.writeInt(-1)");
            methodBuilder.endControlFlow();

        } else {
            methodBuilder.beginControlFlow("if (result != null)");
            methodBuilder.addStatement("reply.writeInt(1)");
            methodBuilder.addStatement("Class pClass = getParcelerClass(result)");
            methodBuilder.addStatement("reply.writeString(pClass.getName())");
            methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, result).writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("reply.writeInt(0)");
            methodBuilder.endControlFlow();
        }
    }

    @Override
    public void readOutResultsFromStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            methodBuilder.beginControlFlow("if ($L != null)", param.name);
            methodBuilder.addStatement("reply.writeInt($L.length)", param.name);
            methodBuilder.beginControlFlow("for($T item : $L)", ((ArrayTypeName) param.type).componentType, param.name);
            methodBuilder.addStatement("Class pClass = getParcelerClass(item)");
            methodBuilder.addStatement("reply.writeString(pClass.getName())");
            methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("reply.writeInt(-1)");
            methodBuilder.endControlFlow();
        }
    }


    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.addStatement("int size_result = reply.readInt()");
            methodBuilder.beginControlFlow("if (size_result >= 0)");
            methodBuilder.addStatement("result = new $T[size_result]", ((ArrayTypeName) resultType).componentType);
            methodBuilder.beginControlFlow("for(int i=0; i<size_result; i++)");
            methodBuilder.addStatement("result[i]= ($T)getParcelerObject(reply.readString(), reply)", ((ArrayTypeName) resultType).componentType);
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("result = null");
            methodBuilder.endControlFlow();
        } else {
            methodBuilder.beginControlFlow("if (reply.readInt() != 0)");
            methodBuilder.addStatement("result = ($T)getParcelerObject(reply.readString(), reply)", resultType);
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("result = null");
            methodBuilder.endControlFlow();

        }
    }

    @Override
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        super.writeParamsToStub(param, paramType, methodBuilder);
        if (param.type instanceof ArrayTypeName) {
            if (paramType == ParamType.OUT) {
                writeOutParamsToStub(param, paramType, methodBuilder);
            } else {
                methodBuilder.addStatement("int size_$L = data.readInt()", param.name);
                methodBuilder.beginControlFlow("if (size_$L >= 0)", param.name);
                methodBuilder.addStatement("$1L = new $2T[size_$1L]", param.name, ((ArrayTypeName) param.type).componentType);
                methodBuilder.beginControlFlow("for(int i=0; i<size_$L; i++)", param.name);
                methodBuilder.addStatement("$L[i] = ($T)getParcelerObject(data.readString(), data)", param.name, ((ArrayTypeName) param.type).componentType);
                methodBuilder.endControlFlow();
                methodBuilder.endControlFlow();
                methodBuilder.beginControlFlow("else");
                methodBuilder.addStatement("$L = null", param.name);
                methodBuilder.endControlFlow();
            }
        } else {
            methodBuilder.beginControlFlow("if ( data.readInt() != 0)");
            methodBuilder.addStatement("$L = ($T)getParcelerObject(data.readString(), data)", param.name, param.type);
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("$L = null", param.name);
            methodBuilder.endControlFlow();
        }
    }

    @Override
    public void readOutParamsFromProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            if (param.type instanceof ArrayTypeName) {
                methodBuilder.addStatement("int size_$L = reply.readInt()", param.name);
                methodBuilder.beginControlFlow("for(int i=0; i<size_$L; i++)", param.name);
                methodBuilder.addStatement("$L[i] = ($T)getParcelerObject(reply.readString(), reply)", param.name, ((ArrayTypeName) param.type).componentType);
                methodBuilder.endControlFlow();
            }
        }
    }
}
