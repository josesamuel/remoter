package util.remoter.service;

import remoter.annotations.Remoter;


/**
 * Test aidl interface with similar signature with an AIDL one to test interoperability
 */
@Remoter
public interface IServiceInterfaceWithMoreMethods {

    String echo(String input);

    String echo1(String input);
}
