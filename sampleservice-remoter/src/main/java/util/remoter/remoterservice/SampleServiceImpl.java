package util.remoter.remoterservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.remoter.service.CustomData;
import util.remoter.service.FooParcelable;
import util.remoter.service.ISampleService;
import util.remoter.service.ISampleServiceListener;

public class SampleServiceImpl implements ISampleService {

    @Override
    public byte testByte(byte a, byte[] arrayIn, byte[] arrayOut, byte[] arrayInOut) {
        arrayOut[0] = arrayInOut[0];
        arrayOut[1] = arrayInOut[1];

        arrayInOut[0] = arrayIn[0];
        arrayInOut[1] = arrayIn[1];

        return a;
    }

    @Override
    public boolean testBoolean(boolean a, boolean[] arrayIn, boolean[] arrayOut, boolean[] arrayInOut) {
        arrayOut[0] = arrayInOut[0];
        arrayOut[1] = arrayInOut[1];

        arrayInOut[0] = arrayIn[0];
        arrayInOut[1] = arrayIn[1];

        return a;
    }

    @Override
    public char testChar(char a, char[] arrayIn, char[] arrayOut, char[] arrayInOut) {
        arrayOut[0] = arrayInOut[0];
        arrayOut[1] = arrayInOut[1];

        arrayInOut[0] = arrayIn[0];
        arrayInOut[1] = arrayIn[1];

        return a;
    }

    @Override
    public CharSequence testCharSequence(CharSequence a) {
        return a;
    }

    @Override
    public double testDouble(double a, double[] arrayIn, double[] arrayOut, double[] arrayInOut) {
        arrayOut[0] = arrayInOut[0];
        arrayOut[1] = arrayInOut[1];

        arrayInOut[0] = arrayIn[0];
        arrayInOut[1] = arrayIn[1];

        return a;
    }

    @Override
    public float testFloat(float a, float[] arrayIn, float[] arrayOut, float[] arrayInOut) {
        arrayOut[0] = arrayInOut[0];
        arrayOut[1] = arrayInOut[1];

        arrayInOut[0] = arrayIn[0];
        arrayInOut[1] = arrayIn[1];

        return a;
    }

    @Override
    public long testLong(long a, long[] arrayIn, long[] arrayOut, long[] arrayInOut) {
        arrayOut[0] = arrayInOut[0];
        arrayOut[1] = arrayInOut[1];

        arrayInOut[0] = arrayIn[0];
        arrayInOut[1] = arrayIn[1];

        return a;
    }

    @Override
    public int testInt(int a, int[] arrayIn, int[] arrayOut, int[] arrayInOut) {

        arrayOut[0] = arrayInOut[0];
        arrayOut[1] = arrayInOut[1];

        arrayInOut[0] = arrayIn[0];
        arrayInOut[1] = arrayIn[1];

        return a;
    }

    @Override
    public String testString(String a, String[] arrayIn, String[] arrayOut, String[] arrayInOut) {

        arrayOut[0] = arrayInOut[0];
        arrayOut[1] = arrayInOut[1];

        arrayInOut[0] = arrayIn[0];
        arrayInOut[1] = arrayIn[1];

        return a;
    }

    @Override
    public List testList(List inList, List listOut, List inOutList) {

        listOut.addAll(inOutList);

        inOutList.clear();
        inOutList.addAll(inList);

        List result = new ArrayList(inList);
        result.add(100);
        return result;
    }

    @Override
    public Map testMap(Map inMap, Map inOutMap) {

        inOutMap.clear();
        inOutMap.putAll(inMap);

        Map result = new HashMap();
        result.putAll(inMap);
        result.put("Result", 100);
        return result;
    }

    @Override
    public FooParcelable testParcelable(FooParcelable inParcelable, FooParcelable parcelableOut, FooParcelable parcelableInOut) {

        parcelableOut.setIntValue(parcelableInOut.getIntValue());
        parcelableOut.setStringValue(parcelableInOut.getStringValue());

        parcelableInOut.setIntValue(inParcelable.getIntValue());
        parcelableInOut.setStringValue(inParcelable.getStringValue());

        return new FooParcelable("Result", 100);
    }

    @Override
    public FooParcelable[] testParcelableArray(FooParcelable[] arrayIn, FooParcelable[] arrayOut, FooParcelable[] arrayInOut) {
        arrayOut[0] = arrayInOut[0];
        arrayOut[1] = arrayInOut[1];

        arrayInOut[0] = arrayIn[0];
        arrayInOut[1] = arrayIn[1];

        return arrayIn;
    }

    @Override
    public void testEcho(String string, ISampleServiceListener listener) {
        listener.onEcho(string);
    }

    @Override
    public int testOneway0(int a) {
        return 0;
    }

    @Override
    public void testOneway1(int a) {

    }

    @Override
    public CustomData testParcel(CustomData customData, CustomData customData2, CustomData customData3) {
        return customData3;
    }

    @Override
    public CustomData[] testParcelArray(CustomData[] customData, CustomData[] customData2, CustomData[] customData3) {

        customData2[0] = customData3[0];
        customData2[1] = customData3[1];

        customData3[0] = customData[0];
        customData3[1] = customData[1];

        return customData;
    }

    @Override
    public List<CustomData> testParcelList(List<CustomData> customData1, List<CustomData> customData2, List<CustomData> customData3) {
        customData2.clear();
        customData2.addAll(customData3);

        customData3.clear();
        customData3.addAll(customData1);

        return customData1;
    }

    @Override
    public List<? extends CustomData> testParcelList2(List<? extends CustomData> customData1, List<? extends CustomData> customData2, List<? extends CustomData> customData3) {
        customData2.clear();
        ((List<CustomData>) customData2).addAll(customData3);

        customData3.clear();
        ((List<CustomData>) customData3).addAll(customData1);

        return customData1;
    }
}
