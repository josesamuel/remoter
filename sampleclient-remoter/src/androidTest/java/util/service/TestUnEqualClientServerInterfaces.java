package util.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import util.remoter.remoterclient.TestActivity;
import util.remoter.service.ISampleService;
import util.remoter.service.ISampleService_Proxy;
import util.remoter.service.IServiceInterfaceWithLesserMethods;
import util.remoter.service.IServiceInterfaceWithMoreMethods;

import static util.remoter.remoterclient.ServiceIntents.INTENT_REMOTER_SERVICE;
import static util.remoter.remoterclient.ServiceIntents.INTENT_REMOTER_TEST_ACTIVITY;


/**
 * Tests Remoter client -  Remoter server
 */
public class TestUnEqualClientServerInterfaces {

    private static final String TAG = TestUnEqualClientServerInterfaces.class.getSimpleName();
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
    public void testLessInterfaceAtServer() {
        IServiceInterfaceWithLesserMethods testInterface = sampleService.getTestInterfaceWithLesserServerMethods();
        Assert.assertNotNull(testInterface);
        Assert.assertEquals("Hello", testInterface.echo("Hello"));
        try {
            String result = testInterface.echo1("Hello");
            Log.v(TAG, "Result from non existent method at server " + result);
            Assert.fail("Should have thrown exception");
        } catch (Exception ex) {
            Log.w(TAG, "Expected exception", ex);
        }

        IServiceInterfaceWithLesserMethods testInterface2 = sampleService.getTestInterfaceWithLesserServerMethods();
        Assert.assertNotNull(testInterface2);
        Assert.assertEquals("Hello", testInterface2.echo("Hello"));
        try {
            String result = testInterface2.echo1("Hello");
            Log.v(TAG, "Result from non existent method at server " + result);
            Assert.fail("Should have thrown exception");
        } catch (Exception ex) {
            Log.w(TAG, "Expected exception", ex);
        }
    }

    @Test
    public void testMoreInterfaceAtServer() {
        IServiceInterfaceWithMoreMethods testInterface = sampleService.getTestInterfaceWithMoreServerMethods();
        Assert.assertNotNull(testInterface);
        Assert.assertEquals("Hello", testInterface.echo("Hello"));


        IServiceInterfaceWithMoreMethods testInterface2 = sampleService.getTestInterfaceWithMoreServerMethods();
        Assert.assertNotNull(testInterface2);
        Assert.assertEquals("Hello", testInterface2.echo("Hello"));

    }


}

