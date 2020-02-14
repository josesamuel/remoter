package remoter.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Marks a method call as asynchronous one way call
 * <p>
 * Only applies to methods with void return, and will be ignored for others.
 * <p>
 *
 * @see Remoter
 * @see ParamOut
 * @see ParamIn
 */
@Retention(CLASS)
@Target(METHOD)
public @interface Oneway {

}
