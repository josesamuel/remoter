package remoter.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;


/**
 * Marks which of the types in a type parameter are nullable
 */
@Retention(CLASS)
@Target({PARAMETER, METHOD})
public @interface NullableType {
    /**
     * Array of indexes (0 based) of the type that are nullable.
     * <p>
     * Default is {0}. If the type is single type param, then this can be omitted.
     */
    int[] nullableIndexes() default {0};
}