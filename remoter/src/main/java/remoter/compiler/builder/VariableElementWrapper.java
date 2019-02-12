package remoter.compiler.builder;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Wraps a {@link VariableElement} to provide a custom name
 */
class VariableElementWrapper implements VariableElement {

    private VariableElement element;
    private int paramIndex;

    /**
     * Wraps the given element and changes its name by appending the given index
     */
    VariableElementWrapper(VariableElement element, int paramIndex) {
        this.element = element;
        this.paramIndex = paramIndex;
    }

    @Override
    public Object getConstantValue() {
        return element.getConstantValue();
    }

    @Override
    public TypeMirror asType() {
        return element.asType();
    }

    @Override
    public ElementKind getKind() {
        return element.getKind();
    }

    @Override
    public Set<Modifier> getModifiers() {
        return element.getModifiers();
    }

    @Override
    public Name getSimpleName() {
        return new SimpleName(element.getSimpleName().toString() + "_" + paramIndex);
    }

    @Override
    public Element getEnclosingElement() {
        return element.getEnclosingElement();
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return element.getEnclosedElements();
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return element.getAnnotationMirrors();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> aClass) {
        return element.getAnnotation(aClass);
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> aClass) {
        return element.getAnnotationsByType(aClass);
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> elementVisitor, P p) {
        return element.accept(elementVisitor, p);
    }

    static class SimpleName implements Name {

        private String name;

        SimpleName(String name) {
            this.name = name;
        }

        @Override
        public boolean contentEquals(CharSequence charSequence) {
            return name.contentEquals(charSequence);
        }

        @Override
        public int length() {
            return name.length();
        }

        @Override
        public char charAt(int i) {
            return name.charAt(i);
        }

        @Override
        public CharSequence subSequence(int i, int i1) {
            return name.subSequence(i, i1);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
