package remoter.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

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
}
