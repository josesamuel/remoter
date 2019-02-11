package remoter.compiler.builder;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link ParamBuilder} for List of Parceler type parameters
 */
class ListOfParcelerParamBuilder extends ParamBuilder {

    TypeElement genericType;

    protected ListOfParcelerParamBuilder(Messager messager, Element element, TypeElement genericType) {
        super(messager, element);
        this.genericType = genericType;
    }


    @Override
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            logError("List[] is not supported");
        } else {
            if (paramType != ParamType.OUT) {
                methodBuilder.beginControlFlow("if ($L != null)", param.name);
                methodBuilder.addStatement("data.writeInt($L.size())", param.name);
                methodBuilder.beginControlFlow("for($T item : $L)", genericType, param.name);
                methodBuilder.addStatement("Class pClass = getParcelerClass(item)");
                methodBuilder.addStatement("data.writeString(pClass.getName())");
                methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel(data, 0)");
                methodBuilder.endControlFlow();
                methodBuilder.endControlFlow();
                methodBuilder.beginControlFlow("else");
                methodBuilder.addStatement("data.writeInt(-1)");
                methodBuilder.endControlFlow();
            }
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            logError("List[] is not supported");
        } else {
            methodBuilder.beginControlFlow("if (result != null)");
            methodBuilder.addStatement("reply.writeInt(result.size())");
            methodBuilder.beginControlFlow("for($T item:result )", genericType);
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
    public void readOutResultsFromStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if ($L != null)", param.name);
        methodBuilder.addStatement("reply.writeInt($L.size())", param.name);
        methodBuilder.beginControlFlow("for($T item : $L)", genericType, param.name);
        methodBuilder.addStatement("Class pClass = getParcelerClass(item)");
        methodBuilder.addStatement("reply.writeString(pClass.getName())");
        methodBuilder.addStatement("org.parceler.Parcels.wrap(pClass, item).writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("reply.writeInt(-1)");
        methodBuilder.endControlFlow();
    }


    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            logError("List[] is not supported");
        } else {
            methodBuilder.addStatement("result = new $T()", ArrayList.class);
            methodBuilder.addStatement("int size_result = reply.readInt()");
            methodBuilder.beginControlFlow("for(int i=0; i<size_result; i++)");
            methodBuilder.addStatement("result.add(($T)getParcelerObject(reply.readString(), reply))", genericType.asType());
            methodBuilder.endControlFlow();
        }
    }


    @Override
    public void writeParamsToStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$T<$T> $L", List.class, genericType.asType(), param.name);
        if (param.type instanceof ArrayTypeName) {
            logError("List[] is not supported");
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement("$L = new $T()", param.name, ArrayList.class);
            } else {
                methodBuilder.addStatement("$L = new $T()", param.name, ArrayList.class);
                //read
                methodBuilder.addStatement("int size_$L = data.readInt()", param.name);
                methodBuilder.beginControlFlow("for(int i=0; i<size_$L; i++)", param.name);
                methodBuilder.addStatement("$L.add(($T)getParcelerObject(data.readString(), data))", param.name, genericType.asType());
                methodBuilder.endControlFlow();
            }
        }
    }

    @Override
    public void readOutParamsFromProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("int size_$L = reply.readInt()", param.name);
            methodBuilder.beginControlFlow("if(size_$L >= 0)", param.name);
            methodBuilder.addStatement("$L.clear()", param.name);
            methodBuilder.beginControlFlow("for(int i=0; i<size_$L; i++)", param.name);
            methodBuilder.addStatement("((List)$L).add(($T)getParcelerObject(reply.readString(), reply))", param.name, genericType.asType());
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
        }
    }

}
