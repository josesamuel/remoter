package remoter.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks an interface as a Remote interface.
 * <p>
 * A Remote interface is an interface that is implemented and then
 * exposed by an android service using a Binder.
 * <p>
 * Normally this is done by defining this interface as
 * an aidl file. Remoter allows you to do this
 * the normal android way using normal interface
 * and a class implementing that interface.
 *
 * @see ParamIn
 * @see ParamOut
 * @see Oneway
 */
@Retention(CLASS)
@Target(TYPE)
public @interface Remoter {

    /**
     * Specify this only for special case where you want to treat the current interface as just a marker interface that simply provides
     * a list of actual interfaces that needs to be treated as Remoter interfaces.
     * <p/>
     * If specified, this current interface is treated just as marker that wraps this given list
     * of classes for which Proxy and Stub needs to be generated
     */
    Class[] classesToWrap() default {};
}
