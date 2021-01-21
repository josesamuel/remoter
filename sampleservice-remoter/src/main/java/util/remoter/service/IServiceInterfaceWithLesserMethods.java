package util.remoter.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import remoter.annotations.Oneway;
import remoter.annotations.ParamIn;
import remoter.annotations.ParamOut;
import remoter.annotations.Remoter;


/**
 * Test aidl interface with similar signature with an AIDL one to test interoperability
 */
@Remoter
public interface IServiceInterfaceWithLesserMethods {

    String echo(String input);
}
