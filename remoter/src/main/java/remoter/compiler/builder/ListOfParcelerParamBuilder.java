package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;

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
    public void writeParamsToProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            logError("List[] is not supported");
        } else {
            if (paramType != ParamType.OUT) {
                methodBuilder.beginControlFlow("if (" + param.getSimpleName() + " != null)");
                methodBuilder.addStatement("data.writeInt(" + param.getSimpleName() + ".size())");
                methodBuilder.beginControlFlow("for($T item:" + param.getSimpleName() + " )", genericType);
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
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
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
    public void readOutResultsFromStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if (" + paramName + " != null)");
        methodBuilder.addStatement("reply.writeInt(" + paramName + ".size())");
        methodBuilder.beginControlFlow("for($T item:" + paramName + " )", genericType);
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
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            logError("List[] is not supported");
        } else {
            methodBuilder.addStatement("result = null");
            methodBuilder.addStatement("int size_result = reply.readInt()");
            methodBuilder.beginControlFlow("if(size_result >=0)");
            methodBuilder.addStatement("result = new $T()", ArrayList.class);
            methodBuilder.beginControlFlow("for(int i=0; i<size_result; i++)");
            methodBuilder.addStatement("result.add(($T)getParcelerObject(reply.readString(), reply))", genericType.asType());
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
        }
    }


    @Override
    public void writeParamsToStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$T<$T> " + paramName, List.class, genericType.asType());
        if (param.asType().getKind() == TypeKind.ARRAY) {
            logError("List[] is not supported");
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement(paramName + " = new $T()", ArrayList.class);
            } else {
                methodBuilder.addStatement(paramName + " = null");
                //read
                methodBuilder.addStatement("int size_" + paramName + " = data.readInt()");
                methodBuilder.beginControlFlow("if(size_" + paramName + " >=0 )");
                methodBuilder.addStatement(paramName + " = new $T()", ArrayList.class);
                methodBuilder.beginControlFlow("for(int i=0; i<size_" + paramName + "; i++)");
                methodBuilder.addStatement(paramName + ".add(($T)getParcelerObject(data.readString(), data))", genericType.asType());
                methodBuilder.endControlFlow();
                methodBuilder.endControlFlow();
            }
        }
    }

    @Override
    public void readOutParamsFromProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            methodBuilder.addStatement("int size_" + param.getSimpleName() + " = reply.readInt()");
            methodBuilder.beginControlFlow("if(size_" + param.getSimpleName() + " >= 0)");
            methodBuilder.beginControlFlow("if(" + param.getSimpleName() + " == null)");
            methodBuilder.addStatement(param.getSimpleName() + " = new $T()", ArrayList.class);
            methodBuilder.endControlFlow();

            methodBuilder.addStatement(param.getSimpleName() + ".clear()");
            methodBuilder.beginControlFlow("for(int i=0; i<size_" + param.getSimpleName() + "; i++)");
            methodBuilder.addStatement("((List)" + param.getSimpleName() + ").add(($T)getParcelerObject(reply.readString(), reply))", genericType.asType());
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
        }
    }

}
