package util.remoter.service;

import remoter.annotations.Remoter;

/**
 * To test templated remoter
 */
@Remoter
public interface ITest<T, U, V> {
    V echo(T param1, U param2);
}
