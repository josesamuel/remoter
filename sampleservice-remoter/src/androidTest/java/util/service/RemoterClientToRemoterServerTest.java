package util.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import remoter.RemoterProxy;
import util.remoter.remoterservice.ExtEImpl;
import util.remoter.remoterservice.TestActivity;
import util.remoter.service.CustomData;
import util.remoter.service.CustomData2;
import util.remoter.service.ExtCustomData;
import util.remoter.service.ExtCustomData2;
import util.remoter.service.FooParcelable;
import util.remoter.service.IExtE;
import util.remoter.service.ISampleService;
import util.remoter.service.ISampleServiceListener;
import util.remoter.service.ISampleService_Proxy;
import util.remoter.service.ITest;
import util.remoter.service.TestEnum;

import static util.remoter.remoterservice.ServiceIntents.INTENT_AIDL_SERVICE;
import static util.remoter.remoterservice.ServiceIntents.INTENT_REMOTER_SERVICE;
import static util.remoter.remoterservice.ServiceIntents.INTENT_REMOTER_TEST_ACTIVITY;


/**
 * Tests Remoter client -  Remoter server
 */
public class RemoterClientToRemoterServerTest {

    private static final String TAG = RemoterClientToRemoterServerTest.class.getSimpleName();
    private Object objectLock = new Object();
    private ISampleService sampleService;


    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            sampleService = new ISampleService_Proxy(iBinder);
            Log.v(TAG, "Got Service " + sampleService);
            synchronized (objectLock) {
                objectLock.notify();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };


    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<TestActivity>(TestActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Intent intent = new Intent(INTENT_REMOTER_TEST_ACTIVITY);
            return intent;
        }
    };

    @Before
    public void setup() throws InterruptedException {
        synchronized (objectLock) {
            Intent remoterServiceIntent = new Intent(INTENT_REMOTER_SERVICE);
            remoterServiceIntent.setClassName("util.remoter.remoterservice", INTENT_REMOTER_SERVICE);

            mActivityRule.getActivity().startService(remoterServiceIntent);
            mActivityRule.getActivity().bindService(remoterServiceIntent, serviceConnection, 0);

            objectLock.wait();
            Log.i(TAG, "Service connected");
        }
    }

    public void teardown() {
        mActivityRule.getActivity().unbindService(serviceConnection);
    }


    @Test
    public void testRemoterInstance() {
        Assert.assertTrue(sampleService instanceof RemoterProxy);
    }

    @Test
    public void testByteParams() throws RemoteException {
        byte a = 1;
        byte[] arrayIn = new byte[]{2, 3};
        byte[] arrayOut = new byte[]{4, 5};
        byte[] arrayInOut = new byte[]{6, 7};

        byte result = sampleService.testByte(a, arrayIn, arrayOut, arrayInOut);

        Log.i(TAG, "Byte Result " + result);

        Assert.assertEquals(a, result);
        Assert.assertEquals(6, arrayOut[0]);
        Assert.assertEquals(7, arrayOut[1]);
        Assert.assertEquals(2, arrayInOut[0]);
        Assert.assertEquals(3, arrayInOut[1]);
    }

    @Test
    public void testBooleanParams() throws RemoteException {
        boolean a = true;
        boolean[] arrayIn = new boolean[]{false, false};
        boolean[] arrayOut = new boolean[]{true, true};
        boolean[] arrayInOut = new boolean[]{true, false};

        boolean result = sampleService.testBoolean(a, arrayIn, arrayOut, arrayInOut);

        Log.i(TAG, "Boolean Result " + result);

        Assert.assertEquals(a, result);
        Assert.assertEquals(true, arrayOut[0]);
        Assert.assertEquals(false, arrayOut[1]);
        Assert.assertEquals(false, arrayInOut[0]);
        Assert.assertEquals(false, arrayInOut[1]);
    }

    @Test
    public void testCharParams() throws RemoteException {
        char a = 1;
        char[] arrayIn = new char[]{2, 3};
        char[] arrayOut = new char[]{4, 5};
        char[] arrayInOut = new char[]{6, 7};

        char result = sampleService.testChar(a, arrayIn, arrayOut, arrayInOut);

        Log.i(TAG, "Char Result " + result);

        Assert.assertEquals(a, result);
        Assert.assertEquals(6, arrayOut[0]);
        Assert.assertEquals(7, arrayOut[1]);
        Assert.assertEquals(2, arrayInOut[0]);
        Assert.assertEquals(3, arrayInOut[1]);
    }

    @Test
    public void testDoubleParams() throws RemoteException {
        double a = 1;
        double[] arrayIn = new double[]{2, 3};
        double[] arrayOut = new double[]{4, 5};
        double[] arrayInOut = new double[]{6, 7};

        double result = sampleService.testDouble(a, arrayIn, arrayOut, arrayInOut);

        Log.i(TAG, "Double Result " + result);

        Assert.assertEquals(a, result, .0);
        Assert.assertEquals(6, arrayOut[0], .0);
        Assert.assertEquals(7, arrayOut[1], .0);
        Assert.assertEquals(2, arrayInOut[0], .0);
        Assert.assertEquals(3, arrayInOut[1], .0);
    }

    @Test
    public void testFloatParams() throws RemoteException {
        float a = 1;
        float[] arrayIn = new float[]{2, 3};
        float[] arrayOut = new float[]{4, 5};
        float[] arrayInOut = new float[]{6, 7};

        float result = sampleService.testFloat(a, arrayIn, arrayOut, arrayInOut);

        Log.i(TAG, "Float Result " + result);

        Assert.assertEquals(a, result, .0);
        Assert.assertEquals(6, arrayOut[0], .0);
        Assert.assertEquals(7, arrayOut[1], .0);
        Assert.assertEquals(2, arrayInOut[0], .0);
        Assert.assertEquals(3, arrayInOut[1], .0);
    }

    @Test
    public void testLongParams() throws RemoteException {
        long a = 1;
        long[] arrayIn = new long[]{2, 3};
        long[] arrayOut = new long[]{4, 5};
        long[] arrayInOut = new long[]{6, 7};

        long result = sampleService.testLong(a, arrayIn, arrayOut, arrayInOut);

        Log.i(TAG, "Long Result " + result);

        Assert.assertEquals(a, result);
        Assert.assertEquals(6, arrayOut[0]);
        Assert.assertEquals(7, arrayOut[1]);
        Assert.assertEquals(2, arrayInOut[0]);
        Assert.assertEquals(3, arrayInOut[1]);
    }


    @Test
    public void testCharSequence() throws RemoteException {
        CharSequence c = "Test";

        CharSequence result = sampleService.testCharSequence(c);

        Log.i(TAG, "CharSequence Result " + result);

        Assert.assertEquals("Test", result);
    }


    @Test
    public void testIntParams() throws RemoteException {
        int a = 1;
        int[] arrayIn = new int[]{2, 3};
        int[] arrayOut = new int[]{4, 5};
        int[] arrayInOut = new int[]{6, 7};

        int result = sampleService.testInt(a, arrayIn, arrayOut, arrayInOut);

        Log.i(TAG, "Int Result " + result);

        Assert.assertEquals(a, result);
        Assert.assertEquals(6, arrayOut[0]);
        Assert.assertEquals(7, arrayOut[1]);
        Assert.assertEquals(2, arrayInOut[0]);
        Assert.assertEquals(3, arrayInOut[1]);
    }

    @Test
    public void testStringParams() throws RemoteException {
        String a = "1";
        String[] arrayIn = new String[]{"2", "3"};
        String[] arrayOut = new String[]{"4", "5"};
        String[] arrayInOut = new String[]{"6", "7"};

        String result = sampleService.testString(a, arrayIn, arrayOut, arrayInOut);

        Log.i(TAG, "String Result " + result);

        Assert.assertEquals(a, result);
        Assert.assertEquals("6", arrayOut[0]);
        Assert.assertEquals("7", arrayOut[1]);
        Assert.assertEquals("2", arrayInOut[0]);
        Assert.assertEquals("3", arrayInOut[1]);
    }

    @Test
    public void testListParams() throws RemoteException {
        List inList = new ArrayList();
        List outList = new ArrayList();
        List inOutList = new ArrayList();

        inList.add(1);
        inList.add(2);

        outList.add(3);
        outList.add(4);

        inOutList.add(5);
        inOutList.add(6);


        List result = sampleService.testList(inList, outList, inOutList);

        Log.i(TAG, "List Result " + result);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals(100, result.get(2));

        Assert.assertEquals(4, outList.size());
        Assert.assertEquals(5, outList.get(2));
        Assert.assertEquals(6, outList.get(3));

        Assert.assertEquals(4, inOutList.size());
        Assert.assertEquals(1, inOutList.get(2));
        Assert.assertEquals(2, inOutList.get(3));
    }

    @Test
    public void testMapParams() throws RemoteException {
        Map inList = new HashMap();
        Map inOutList = new HashMap();

        inList.put(1, 1);
        inList.put(2, 2);

        inOutList.put(5, 5);
        inOutList.put(6, 6);


        Map result = sampleService.testMap(inList, inOutList);

        Log.i(TAG, "Map Result " + result);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals(100, result.get("Result"));

        Assert.assertEquals(4, inOutList.size());
        Assert.assertEquals(1, inOutList.get(1));
        Assert.assertEquals(2, inOutList.get(2));
    }

    @Test
    public void testParcelable() throws RemoteException {
        FooParcelable in = new FooParcelable("1", 1);
        FooParcelable out = new FooParcelable("2", 2);
        FooParcelable inOut = new FooParcelable("3", 3);


        FooParcelable result = sampleService.testParcelable(in, out, inOut);

        Log.i(TAG, "Parcellable Result " + result);

        Assert.assertEquals("Result", result.getStringValue());
        Assert.assertEquals(100, result.getIntValue());

        Assert.assertEquals("3", out.getStringValue());
        Assert.assertEquals(3, out.getIntValue());

        Assert.assertEquals("1", inOut.getStringValue());
        Assert.assertEquals(1, inOut.getIntValue());
    }

    @Test
    public void testParcelableArray() throws RemoteException {
        FooParcelable[] in = new FooParcelable[]{new FooParcelable("1", 1), new FooParcelable("10", 10)};
        FooParcelable[] out = new FooParcelable[]{new FooParcelable("2", 2), new FooParcelable("20", 20)};
        FooParcelable[] inOut = new FooParcelable[]{new FooParcelable("3", 3), new FooParcelable("30", 30)};


        FooParcelable[] result = sampleService.testParcelableArray(in, out, inOut);

        Log.i(TAG, "Parcellable[] Result " + result);

        Assert.assertEquals(2, result.length);

        Assert.assertEquals("1", result[0].getStringValue());
        Assert.assertEquals(1, result[0].getIntValue());
        Assert.assertEquals("10", result[1].getStringValue());
        Assert.assertEquals(10, result[1].getIntValue());

        Assert.assertEquals("3", out[0].getStringValue());
        Assert.assertEquals(3, out[0].getIntValue());
        Assert.assertEquals("30", out[1].getStringValue());
        Assert.assertEquals(30, out[1].getIntValue());

        Assert.assertEquals("1", inOut[0].getStringValue());
        Assert.assertEquals(1, inOut[0].getIntValue());
        Assert.assertEquals("10", inOut[1].getStringValue());
        Assert.assertEquals(10, inOut[1].getIntValue());
    }


    @Test
    public void testBinder() throws RemoteException {
        final String message = "Hello";
        final List callBack = new ArrayList();
        ISampleServiceListener listener = new ISampleServiceListener() {
            @Override
            public void onEcho(String echo) {
                Log.i(TAG, "Callback " + echo);
                Assert.assertEquals(message, echo);
                callBack.add(echo);
            }
        };

        sampleService.testEcho(message, listener);
        Assert.assertEquals(1, callBack.size());
    }


    @Test
    public void testParceler() throws RemoteException {
        CustomData data1 = new CustomData();
        data1.setIntData(1);
        data1.setEnumData(TestEnum.TWO);
        CustomData2 customData2 = new CustomData2();
        customData2.setIntData(10);
        customData2.setEnumData(TestEnum.TWO);
        data1.setCustomData2(customData2);
        data1.setCustomData2Array(new CustomData2[]{customData2});


        CustomData data2 = new ExtCustomData(new ExtEImpl());
        data2.setIntData(2);
        data2.setEnumData(TestEnum.ONE);
        CustomData2 customData22 = new CustomData2();
        customData22.setIntData(20);
        customData22.setEnumData(TestEnum.ONE);
        data2.setCustomData2(customData22);
        data2.setCustomData2Array(new CustomData2[]{customData22});

        CustomData data3 = new ExtCustomData(new ExtEImpl());
        data3.setIntData(3);
        data3.setEnumData(TestEnum.THREE);
        CustomData2 customData23 = new CustomData2();
        customData23.setIntData(30);
        customData23.setEnumData(TestEnum.THREE);
        data3.setCustomData2(customData23);
        data3.setCustomData2Array(new CustomData2[]{customData23});


        CustomData result = sampleService.testParcel(data1, data2, data3);

        Assert.assertTrue(result instanceof ExtCustomData);
        IExtE extE = ((ExtCustomData) result).getRemoteInterface();
        Assert.assertNotNull(extE);
        Assert.assertEquals(1, extE.echoInt(1));
        Assert.assertEquals("ab", extE.echoString("a", "b"));
        Assert.assertEquals(3, extE.echoLong(1, 2));


        Assert.assertEquals(3, result.getIntData());
        Assert.assertEquals(TestEnum.THREE, result.getEnumData());

        Assert.assertEquals(30, result.getCustomData2().getIntData());
        Assert.assertEquals(TestEnum.THREE, result.getCustomData2Array()[0].getEnumData());
    }

    @Test
    public void testParcelerArray() throws RemoteException {
        CustomData data1 = new ExtCustomData2();
        data1.setIntData(1);
        data1.setEnumData(TestEnum.TWO);
        CustomData2 customData2 = new CustomData2();
        customData2.setIntData(10);
        customData2.setEnumData(TestEnum.TWO);
        data1.setCustomData2(customData2);
        data1.setCustomData2Array(new CustomData2[]{customData2});


        CustomData data2 = new ExtCustomData(new ExtEImpl());
        data2.setIntData(2);
        data2.setEnumData(TestEnum.ONE);
        CustomData2 customData22 = new CustomData2();
        customData22.setIntData(20);
        customData22.setEnumData(TestEnum.ONE);
        data2.setCustomData2(customData22);
        data2.setCustomData2Array(new CustomData2[]{customData22});

        CustomData data3 = new CustomData();
        data3.setIntData(3);
        data3.setEnumData(TestEnum.THREE);
        CustomData2 customData23 = new CustomData2();
        customData23.setIntData(30);
        customData23.setEnumData(TestEnum.THREE);
        data3.setCustomData2(customData23);
        data3.setCustomData2Array(new CustomData2[]{customData23});


        CustomData[] param1 = new CustomData[]{data1, data2};
        CustomData[] param2 = new CustomData[]{data2, data3};
        CustomData[] param3 = new CustomData[]{data3, data1};

        CustomData[] result = sampleService.testParcelArray(param1, param2, param3);

        Assert.assertEquals(data1.getIntData(), result[0].getIntData());
        Assert.assertEquals(data1.getEnumData(), result[0].getEnumData());

        Assert.assertEquals(data3.getIntData(), param2[0].getIntData());
        Assert.assertEquals(data1.getEnumData(), param2[1].getEnumData());

        Assert.assertEquals(data1.getIntData(), param3[0].getIntData());
        Assert.assertEquals(data2.getEnumData(), param3[1].getEnumData());

    }

    @Test
    public void testParcelerList() throws RemoteException {
        CustomData data1 = new ExtCustomData2();
        data1.setIntData(1);
        data1.setEnumData(TestEnum.TWO);
        CustomData2 customData2 = new CustomData2();
        customData2.setIntData(10);
        customData2.setEnumData(TestEnum.TWO);
        data1.setCustomData2(customData2);
        data1.setCustomData2Array(new CustomData2[]{customData2});


        CustomData data2 = new CustomData();
        data2.setIntData(2);
        data2.setEnumData(TestEnum.ONE);
        CustomData2 customData22 = new CustomData2();
        customData22.setIntData(20);
        customData22.setEnumData(TestEnum.ONE);
        data2.setCustomData2(customData22);
        data2.setCustomData2Array(new CustomData2[]{customData22});

        CustomData data3 = new ExtCustomData(new ExtEImpl());
        data3.setIntData(3);
        data3.setEnumData(TestEnum.THREE);
        CustomData2 customData23 = new CustomData2();
        customData23.setIntData(30);
        customData23.setEnumData(TestEnum.THREE);
        data3.setCustomData2(customData23);
        data3.setCustomData2Array(new CustomData2[]{customData23});


        CustomData[] param1 = new CustomData[]{data1, data2};
        CustomData[] param2 = new CustomData[]{data2, data3};
        CustomData[] param3 = new CustomData[]{data3, data1};

        List<CustomData> p1 = new ArrayList(Arrays.asList(param1));
        List<CustomData> p2 = new ArrayList(Arrays.asList(param2));
        List<CustomData> p3 = new ArrayList(Arrays.asList(param3));

        List<CustomData> result = sampleService.testParcelList(p1, p2, p3);

        Assert.assertEquals(data1.getIntData(), result.get(0).getIntData());
        Assert.assertEquals(data2.getEnumData(), result.get(1).getEnumData());

        Assert.assertEquals(data3.getIntData(), p2.get(0).getIntData());
        Assert.assertEquals(data1.getEnumData(), p2.get(1).getEnumData());

        Assert.assertEquals(data1.getIntData(), p3.get(0).getIntData());
        Assert.assertEquals(data2.getEnumData(), p3.get(1).getEnumData());


        List<? extends CustomData> p11 = new ArrayList(Arrays.asList(param1));
        List<? extends CustomData> p22 = new ArrayList(Arrays.asList(param2));
        List<? extends CustomData> p33 = new ArrayList(Arrays.asList(param3));

        List<? extends CustomData> result2 = sampleService.testParcelList2(p11, p22, p33);

        Assert.assertEquals(data1.getIntData(), result2.get(0).getIntData());
        Assert.assertEquals(data2.getEnumData(), result2.get(1).getEnumData());

        Assert.assertEquals(data3.getIntData(), p22.get(0).getIntData());
        Assert.assertEquals(data1.getEnumData(), p22.get(1).getEnumData());

        Assert.assertEquals(data1.getIntData(), p33.get(0).getIntData());
        Assert.assertEquals(data2.getEnumData(), p33.get(1).getEnumData());
    }

    @Test
    public void testExtendedRemoter() throws RemoteException {
        IExtE extE = sampleService.getExtE();
        Log.v(TAG, "Got ext Service " + extE);
        Assert.assertEquals(1, extE.echoInt(1));
        Assert.assertEquals("ab", extE.echoString("a", "b"));
        Assert.assertEquals(3, extE.echoLong(1, 2));
        Assert.assertNull(extE.echoString("GlobalKey1", "b"));
    }


    @Test
    public void testGlobalProperties() throws RemoteException {
        Map<String, Object> globalProperties = new HashMap<String, Object>();
        globalProperties.put("GlobalKey1", "Value1");
        ((ISampleService_Proxy)sampleService).setRemoterGlobalProperties(globalProperties);


        IExtE extE = sampleService.getExtE();
        Log.v(TAG, "Got ext Service " + extE);
        Assert.assertEquals(1, extE.echoInt(1));
        Assert.assertEquals("ab", extE.echoString("a", "b"));
        Assert.assertEquals(3, extE.echoLong(1, 2));
        Assert.assertEquals("Value1", extE.echoString("GlobalKey1", "b"));
    }


    @Test
    public void testExtendedRemoterArray() throws RemoteException {
        IExtE[] array = sampleService.getExtEArray();
        for (IExtE extE : array) {
            Log.v(TAG, "Got ext Service " + extE);
            Assert.assertEquals(1, extE.echoInt(1));
            Assert.assertEquals("ab", extE.echoString("a", "b"));
            Assert.assertEquals(3, extE.echoLong(1, 2));
        }
    }

    @Test
    public void testTemplatedRemoter() throws RemoteException {
        ITest<String, CustomData, CustomData> templateRemoter = sampleService.getTemplateRemoter();
        Assert.assertNotNull(templateRemoter);
        CustomData customData = new ExtCustomData2();
        customData.setData("2");
        Assert.assertEquals("12", templateRemoter.echo("1", customData).getData());
    }


    @Test
    public void testException() {
        try {
            sampleService.testException();
        } catch (Exception exception) {
            //expecting interupted
            Log.w(TAG, "Got exception", exception);
            Assert.assertTrue(exception instanceof InterruptedException);
            Assert.assertEquals("Test", exception.getMessage());
        }
    }

    @Test
    public void testRuntimeException() {
        try {
            sampleService.testRuntimeException();
        } catch (Exception exception) {
            //expecting runtimeexception
            Log.w(TAG, "Got exception", exception);
            Assert.assertTrue(exception instanceof RuntimeException);
            Assert.assertEquals("Test", exception.getMessage());
        }
    }

    @Test
    public void testRuntimeExceptionForOneWay() {
        boolean gotException = false;
        try {
            sampleService.testOnewayThrowsException(0);
        } catch (Exception exception) {
            Log.w(TAG, "Got exception", exception);
            Assert.assertEquals("TestOneWay", exception.getCause().getMessage());
            gotException = true;
        }
        Assert.assertTrue(gotException);
    }


    @Test
    public void testProxyIdentity() throws RemoteException {

        final int[] resultCount = {0,0};
        final String[] expectedEcho = {""};

        ISampleServiceListener listener1 = new ISampleServiceListener() {
            @Override
            public void onEcho(String echo) {
                Log.i(TAG, "Echo from listener 1 " + echo);
                resultCount[0] ++;
                Assert.assertEquals(expectedEcho[0], echo);
            }
        };

        ISampleServiceListener listener2 = new ISampleServiceListener() {
            @Override
            public void onEcho(String echo) {
                Log.i(TAG, "Echo from listener 2 " + echo);
                resultCount[1] ++;
                Assert.assertEquals(expectedEcho[0], echo);
            }
        };


        expectedEcho[0] = "1";

        int result = sampleService.registerListener(listener1);

        Log.i(TAG, "Listener id " + result);

        Assert.assertEquals(result, listener1.hashCode());
        Assert.assertEquals(resultCount[0], 1);
        Assert.assertEquals(resultCount[1], 0);



        result = sampleService.registerListener(listener1);

        Log.i(TAG, "Listener id same again " + result);

        Assert.assertEquals(result, -1);
        Assert.assertEquals(resultCount[0], 2);
        Assert.assertEquals(resultCount[1], 0);

        expectedEcho[0] = "2";

        result = sampleService.registerListener(listener2);

        Log.i(TAG, "Listener id new  " + result);

        Assert.assertEquals(result, listener2.hashCode());
        Assert.assertEquals(resultCount[0], 3);
        Assert.assertEquals(resultCount[1], 1);


        Assert.assertTrue(sampleService.unRegisterListener(listener1));
        Assert.assertFalse(sampleService.unRegisterListener(listener1));
        Assert.assertTrue(sampleService.unRegisterListener(listener2));

    }


}

