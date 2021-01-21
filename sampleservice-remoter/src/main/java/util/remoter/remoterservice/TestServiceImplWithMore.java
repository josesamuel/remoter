package util.remoter.remoterservice;

import util.remoter.service.IServiceInterfaceWithLesserMethods;
import util.remoter.service.IServiceInterfaceWithMoreMethods;

public class TestServiceImplWithMore implements IServiceInterfaceWithMoreMethods {

    @Override
    public String echo(String input) {
        return input;
    }

    @Override
    public String echo1(String input) {
        return input.toLowerCase();
    }
}
