package remoter.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Marks a parameter as an input only type.
 * <p>
 * Only applies to primitive array types (eg int[], long[], String[], etc), {@link java.util.List}, {@link java.util.Map},
 * or Parcelable types. By default these types are treated as input and output, unless
 * they are marked otherwise using either @{@link ParamOut} or {@link ParamIn}
 * <p>
 * Annotating on any unsupported types will be ignored
 *
 * @see Remoter
 * @see ParamOut
 */
@Retention(CLASS)
@Target(PARAMETER)
public @interface ParamIn {

}
