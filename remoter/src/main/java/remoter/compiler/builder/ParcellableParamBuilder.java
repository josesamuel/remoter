package remoter.compiler.builder;

import com.squareup.javapoet.MethodSpec;

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
    public void writeParamsToProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            if (paramType == ParamType.OUT) {
                writeArrayOutParamsToProxy(param, methodBuilder);
            } else {
                methodBuilder.addStatement("data.writeTypedArray(" + param.getSimpleName() + ", 0)");
            }
        } else {
            if (paramType != ParamType.OUT) {
                methodBuilder.beginControlFlow("if (" + param.getSimpleName() + " != null)");
                methodBuilder.addStatement("data.writeInt(1)");
                methodBuilder.addStatement(param.getSimpleName() + ".writeToParcel(data, 0)");
                methodBuilder.endControlFlow();
                methodBuilder.beginControlFlow("else");
                methodBuilder.addStatement("data.writeInt(0)");
                methodBuilder.endControlFlow();
            }
        }
    }

    @Override
    public void readResultsFromStub(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
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
    public void readOutResultsFromStub(VariableElement param, ParamType paramType, String paramName, MethodSpec.Builder methodBuilder) {
        if (param.asType().getKind() == TypeKind.ARRAY) {
            methodBuilder.addStatement("reply.writeTypedArray(" + paramName + ", android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
        } else {
            methodBuilder.beginControlFlow("if (" + paramName + " != null)");
            methodBuilder.addStatement("reply.writeInt(1)");
            methodBuilder.addStatement(paramName + ".writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE)");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("else");
            methodBuilder.addStatement("reply.writeInt(0)");
            methodBuilder.endControlFlow();
        }
    }


    @Override
    public void readResultsFromProxy(TypeMirror resultType, MethodSpec.Builder methodBuilder) {
        if (resultType.getKind() == TypeKind.ARRAY) {
            methodBuilder.addStatement("result = ("+ getParcelableClassName(resultType) + "[])reply.createTypedArray(" + getParcelableClassName(resultType) + ".CREATOR)");
        } else {
            methodBuilder.beginControlFlow("if (reply.readInt() != 0)");
            methodBuilder.addStatement("result = (" + getParcelableClassName(resultType) + ")" + getParcelableClassName(resultType) + ".CREATOR.createFromParcel(reply)");
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
                methodBuilder.addStatement(paramName + " = ("+ getParcelableClassName(param.asType()) +"[]) data.createTypedArray(" + getParcelableClassName(param.asType()) + ".CREATOR)");
            }
        } else {
            if (paramType == ParamType.OUT) {
                methodBuilder.addStatement(paramName + " = new " + getParcelableClassName(param.asType()) + "()");
            } else {
                methodBuilder.beginControlFlow("if ( data.readInt() != 0)");
                methodBuilder.addStatement(paramName + " = (" + getParcelableClassName(param.asType()) +")" + getParcelableClassName(param.asType()) + ".CREATOR.createFromParcel(data)");
                methodBuilder.endControlFlow();
                methodBuilder.beginControlFlow("else");
                methodBuilder.addStatement(paramName + " = null");
                methodBuilder.endControlFlow();
            }
        }
    }

    private String getParcelableClassName(TypeMirror typeMirror) {
        if (typeMirror.getKind() != TypeKind.ARRAY) {
            String pClassName = typeMirror.toString();
            int genericStartIndex = pClassName.indexOf('<');
            if (genericStartIndex != -1) {
                pClassName = pClassName.substring(0, genericStartIndex).trim();
            }
            return pClassName;
        } else {
            return getParcelableClassName(((ArrayType) typeMirror).getComponentType());
        }
    }

    @Override
    public void readOutParamsFromProxy(VariableElement param, ParamType paramType, MethodSpec.Builder methodBuilder) {
        if (paramType != ParamType.IN) {
            if (param.asType().getKind() == TypeKind.ARRAY) {
                methodBuilder.addStatement("reply.readTypedArray(" + param.getSimpleName() + ", " + getParcelableClassName(param.asType()) + ".CREATOR)");
            } else {
                methodBuilder.beginControlFlow("if (reply.readInt() != 0)");
                methodBuilder.addStatement(param.getSimpleName() + ".readFromParcel(reply)");
                methodBuilder.endControlFlow();
            }
        }
    }
}
