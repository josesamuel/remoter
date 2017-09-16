package util.remoter.service;

import util.remoter.service.FooParcelable;
import util.remoter.service.ISampleServiceListener;


// Test aidl interface with similar signature with a normal one to test interoperability
interface ISampleService {

    byte testByte(byte a, in byte[] arrayIn, out byte[] arrayOut, inout byte[] arrayInOut);
    boolean testBoolean(boolean a, in boolean[] arrayIn, out boolean[] arrayOut, inout boolean[] arrayInOut);
    char testChar(char a, in char[] arrayIn, out char[] arrayOut, inout char[] arrayInOut);
    CharSequence testCharSequence(CharSequence a);
    double testDouble(double a, in double[] arrayIn, out double[] arrayOut, inout double[] arrayInOut);
    float testFloat(float a, in float[] arrayIn, out float[] arrayOut, inout float[] arrayInOut);
    long testLong(long a, in long[] arrayIn, out long[] arrayOut, inout long[] arrayInOut);

    int testInt(int a, in int[] arrayIn, out int[] arrayOut, inout int[] arrayInOut);
    String testString(String a, in String[] arrayIn, out String[] arrayOut, inout String[] arrayInOut);
    List testList(in List inList, out List listOut, inout List inOutList);
    Map testMap(in Map inMap, inout Map inOutMap);
    FooParcelable testParcelable(in FooParcelable inParcelable, out FooParcelable parcelableOut, inout FooParcelable parcelableInOut);
    FooParcelable[] testParcelableArray(in FooParcelable[] arrayIn, out FooParcelable[] arrayOut, inout FooParcelable[] arrayInOut);
    void testEcho(String string, ISampleServiceListener listener);

    int testOneway0(int a);
    oneway void testOneway1(int a);

}
