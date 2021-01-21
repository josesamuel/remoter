package util.remoter.remoterservice;

import util.remoter.service.IServiceInterfaceWithLesserMethods;

public class TestServiceImplWithLesser implements IServiceInterfaceWithLesserMethods {

    @Override
    public String echo(String input) {
        return input;
    }
}
