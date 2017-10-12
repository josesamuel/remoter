package util.remoter.remoterservice;


import util.remoter.service.CustomData;
import util.remoter.service.ITest;

public class ITestImpl<T, U, V> implements ITest<T, U, V> {

    @Override
    public V echo(T param1, U param2) {
        CustomData customData = new CustomData();
        customData.setData(param1.toString() + ((CustomData) param2).getData());
        return (V) customData;
    }
}
