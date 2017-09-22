package util.remoter.service;

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
public interface ISampleService {

    byte testByte(byte a, @ParamIn byte[] arrayIn, @ParamOut byte[] arrayOut, byte[] arrayInOut);

    boolean testBoolean(boolean a, @ParamIn boolean[] arrayIn, @ParamOut boolean[] arrayOut, boolean[] arrayInOut);

    char testChar(char a, @ParamIn char[] arrayIn, @ParamOut char[] arrayOut, char[] arrayInOut);

    CharSequence testCharSequence(CharSequence a);

    double testDouble(double a, @ParamIn double[] arrayIn, @ParamOut double[] arrayOut, double[] arrayInOut);

    float testFloat(float a, @ParamIn float[] arrayIn, @ParamOut float[] arrayOut, float[] arrayInOut);

    long testLong(long a, @ParamIn long[] arrayIn, @ParamOut long[] arrayOut, long[] arrayInOut);

    int testInt(int a, @ParamIn int[] arrayIn, @ParamOut int[] arrayOut, int[] arrayInOut);

    String testString(String a, @ParamIn String[] arrayIn, @ParamOut String[] arrayOut, String[] arrayInOut);

    List testList(@ParamIn List inList, @ParamOut List listOut, List inOutList);

    Map testMap(@ParamIn Map inMap, Map inOutMap);

    FooParcelable testParcelable(@ParamIn FooParcelable inParcelable, @ParamOut FooParcelable parcelableOut, FooParcelable parcelableInOut);

    FooParcelable[] testParcelableArray(@ParamIn FooParcelable[] arrayIn, @ParamOut FooParcelable[] arrayOut, FooParcelable[] arrayInOut);

    void testEcho(String string, ISampleServiceListener listener);

    //one way should get ignored
    @Oneway
    int testOneway0(int a);

    @Oneway
    void testOneway1(int a);

    CustomData testParcel(@ParamIn CustomData customData, @ParamOut CustomData customData2, CustomData customData3);

    CustomData[] testParcelArray(@ParamIn CustomData[] customData, @ParamOut CustomData[] customData2, CustomData[] customData3);

    List<CustomData> testParcelList(@ParamIn List<CustomData> customData1, @ParamOut List<CustomData> customData2, List<CustomData> customData3);

    List<? extends CustomData> testParcelList2(@ParamIn List<? extends CustomData> customData1, @ParamOut List<? extends CustomData> customData2, List<? extends CustomData> customData3);

}
