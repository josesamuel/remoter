package remoter.compiler.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/**
 * A {@link RemoteBuilder} that knows how to generate the fields for stub and proxy
 */
class FieldBuilder extends RemoteBuilder {


    protected FieldBuilder(Messager messager, Element element) {
        super(messager, element);
    }


    public void addProxyFields(TypeSpec.Builder classBuilder) {
        //add IBinder
        classBuilder.addField(FieldSpec.builder(ClassName.get("android.os", "IBinder"), "mRemote")
                .addModifiers(Modifier.PRIVATE).build());
        addCommonFields(classBuilder);
        processRemoterElements(classBuilder, new ElementVisitor() {
            @Override
            public void visitElement(TypeSpec.Builder classBuilder, Element member, int methodIndex, MethodSpec.Builder methodBuilder) {
                addCommonFields(classBuilder, member, methodIndex);
            }
        }, null);

    }

    public void addStubFields(TypeSpec.Builder classBuilder) {
        classBuilder.addField(FieldSpec.builder(TypeName.get(getRemoterInterfaceElement().asType()), "serviceImpl")
                .addModifiers(Modifier.PRIVATE).build());
        addCommonFields(classBuilder);
        processRemoterElements(classBuilder, new ElementVisitor() {
            @Override
            public void visitElement(TypeSpec.Builder classBuilder, Element member, int methodIndex, MethodSpec.Builder methodBuilder) {
                addCommonFields(classBuilder, member, methodIndex);
            }
        }, null);
    }

    private void addCommonFields(TypeSpec.Builder classBuilder) {
        //Add descriptor
        classBuilder.addField(FieldSpec.builder(ClassName.get(String.class), "DESCRIPTOR")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("\"" + getRemoterInterfacePackageName() + "." + getRemoterInterfaceClassName() + "\"")
                .build());
        classBuilder.addField(FieldSpec.builder(TypeName.INT, "REMOTER_EXCEPTION_CODE")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("-99999")
                .build());

    }

    private void addCommonFields(TypeSpec.Builder classBuilder, Element member, int methodIndex) {
        String methodName = member.getSimpleName().toString();
        classBuilder.addField(FieldSpec.builder(TypeName.INT, "TRANSACTION_" + methodName + "_" + methodIndex)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("android.os.IBinder.FIRST_CALL_TRANSACTION + " + methodIndex).build());
    }
}
