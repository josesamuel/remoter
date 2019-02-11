package remoter.compiler.builder;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link ParamBuilder} for Parcellable type parameters
 */
class ParcellableParamBuilder extends ParamBuilder {


    protected ParcellableParamBuilder(Messager messager, Element element) {
        super(messager, element);
    }


    @Override
    public void writeParamsToProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder);
            } else {
                methodBuilder.addStatement("data.writeTypedArray($L, 0)", param.name);
            }
        } else {
            if (paramType != ParamType.OUT) {
                methodBuilder.beginControlFlow("if ($L != null)", param.name);
                methodBuilder.addStatement("data.writeInt(1)");
                methodBuilder.addStatement("$L.writeToParcel(data, 0)", param.name);
                methodBuilder.endControlFlow();
                methodBuilder.beginControlFlow("else");
                methodBuilder.addStatement("data.writeInt(0)");
                methodBuilder.endControlFlow();
            }
        }
    }

    @Override
    public void readResultsFromStub(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.addStatement("reply.writeTypedArray(result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
        } else {
            methodBuilder.beginControlFlow("if (result != null)");
            methodBuilder.addStatement("reply.writeInt(1)");
            methodBuilder.addStatement("result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("reply.writeInt(0)");
            methodBuilder.endControlFlow();
        }
    }

    @Override
    public void readOutResultsFromStub(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.type instanceof ArrayTypeName) {
            methodBuilder.addStatement("reply.writeTypedArray($L, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)", param.name);
        } else {
            methodBuilder.beginControlFlow("if ($L != null)", param.name);
            methodBuilder.addStatement("reply.writeInt(1)");
            methodBuilder.addStatement("$L.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)", param.name);
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("reply.writeInt(0)");
            methodBuilder.endControlFlow();
        }
    }


    @Override
    public void readResultsFromProxy(TypeName resultType, MethodSpec.Builder methodBuilder) {
        if (resultType instanceof ArrayTypeName) {
            methodBuilder.addStatement("result = reply.createTypedArray($T.CREATOR)", getParcelableClassName(resultType));
        } else {
            methodBuilder.beginControlFlow("if (reply.readInt() != 0)");
            methodBuilder.addStatement("result = ($1T)$1T.CREATOR.createFromParcel(reply)", getParcelableClassName(resultType));
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
                methodBuilder.addStatement("$L = data.createTypedArray($T.CREATOR)", param.name, getParcelableClassName(param.type));
            }
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement("$L = new $T()", param.name, getParcelableClassName(param.type));
            } else {
                methodBuilder.beginControlFlow("if ( data.readInt() != 0)");
                methodBuilder.addStatement("$1L = ($2T)$2T.CREATOR.createFromParcel(data)", param.name, getParcelableClassName(param.type));
                methodBuilder.endControlFlow();
                methodBuilder.beginControlFlow("else");
                methodBuilder.addStatement("$L = null", param.name);
                methodBuilder.endControlFlow();
            }
        }
    }

    private ClassName getParcelableClassName(TypeName typeName) {
        if (!(typeName instanceof ArrayTypeName)) {
            String pClassName = typeName.toString();
            int genericStartIndex = pClassName.indexOf('<');
            if (genericStartIndex != -1) {
                pClassName = pClassName.substring(0, genericStartIndex).trim();
            }
            return ClassName.bestGuess(pClassName);
        } else {
            return getParcelableClassName(((ArrayTypeName) typeName).componentType);
        }
    }

    @Override
    public void readOutParamsFromProxy(ParameterSpec param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            if (param.type instanceof ArrayTypeName) {
                methodBuilder.addStatement("reply.readTypedArray($L, $T.CREATOR)", param.name, getParcelableClassName(param.type));
            } else {
                methodBuilder.beginControlFlow("if (reply.readInt() != 0)");
                methodBuilder.addStatement("$L.readFromParcel(reply)", param.name);
                methodBuilder.endControlFlow();
            }
        }
    }
}
