package util.service;

import android.content.ComponentName;
import android.content.Context;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import remoter.RemoterProxy;
import remoter.RemoterProxyListener;
import util.remoter.remoterservice.TestActivity;
import util.remoter.service.FooParcelable;
import util.remoter.service.ISampleService;
import util.remoter.service.ISampleServiceListener;
import util.remoter.service.ISampleServiceListener_Stub;
import util.remoter.service.ISampleService_Proxy;

import static util.remoter.remoterservice.ServiceIntents.INTENT_AIDL_SERVICE;
import static util.remoter.remoterservice.ServiceIntents.INTENT_REMOTER_TEST_ACTIVITY;


/**
 * Tests Remoter client -  AIDL server
 * <p>
 * Requires sampleservice-aidl to be installed
 */
public class RemoterClientToAidlServerTest {

    private static final String TAG = RemoterClientToAidlServerTest.class.getSimpleName();
    private Object objectLock = new Object();
    private ISampleService sampleService;


    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            sampleService = new ISampleService_Proxy(iBinder);
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
            Intent remoterServiceIntent = new Intent(INTENT_AIDL_SERVICE);
            remoterServiceIntent.setClassName("util.remoter.aidlservice", INTENT_AIDL_SERVICE);

            //mActivityRule.getActivity().startService(remoterServiceIntent);
            mActivityRule.getActivity().bindService(remoterServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

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
        Log.i(TAG, "Out Result " + inOutList);

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

        ISampleServiceListener_Stub.setStubProxyCheck(false);

        sampleService.testEcho(message, listener);
        Assert.assertEquals(1, callBack.size());
    }

    @Test
    public void testException() {
        try {
            sampleService.testException();
        } catch (Exception exception) {
            //expecting interupted
            Log.w(TAG, "Got exception", exception);
            Assert.assertTrue(exception instanceof RuntimeException);
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


}

