package util.remoter.service;

import remoter.annotations.Remoter;

/**
 * To test interface extensions
 */
@Remoter
public interface IExtE extends IExtD {
    long echoLong(long s);
    long echoLong(long s, long s2);
}
