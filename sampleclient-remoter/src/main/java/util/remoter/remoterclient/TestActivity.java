package util.remoter.remoterclient;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import remoter.RemoterProxy;
import util.remoter.service.ISampleService;
import util.remoter.service.ISampleServiceListener;
import util.remoter.service.ISampleService_Proxy;

import static util.remoter.remoterclient.ServiceIntents.INTENT_REMOTER_SERVICE;

/**
 * A sample client to validate there are no memory leaks while sending the stub across remote process.
 *
 * Perform register/unregister multiple times and run the profiler.
 *
 * Once GC is done on both client and server, the profiler should show only active listeners. Any unregistered
 * listeners should have been collected
 */
public class TestActivity extends Activity {

    private static final String TAG = "Test";

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "Service connected 1");
            sampleService = new ISampleService_Proxy(service);
            Log.v(TAG, "Service connected " + sampleService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private ISampleService sampleService;
    private ISampleServiceListener sampleServiceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);

        findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Register " + sampleService);
                if (sampleService != null) {
                    sampleServiceListener = new SampleListener();
                    Log.v(TAG, "Registering " + sampleServiceListener.hashCode());
                    int result = sampleService.registerListener(sampleServiceListener);
                    Log.v(TAG, "Register result " + result);
                }
            }
        });

        findViewById(R.id.unregister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Un Register " + sampleService);
                if (sampleService != null) {
                    boolean result = sampleService.unRegisterListener(sampleServiceListener);
                    //clear the stub
                    ((RemoterProxy)sampleService).destroyStub(sampleServiceListener);
                    Log.v(TAG, "UnRegister result " + result);
                }
            }
        });

        Intent remoterServiceIntent = new Intent(INTENT_REMOTER_SERVICE);
        remoterServiceIntent.setClassName("util.remoter.remoterservice", INTENT_REMOTER_SERVICE);

        Log.v(TAG, "Connecting with service");

        boolean result = bindService(remoterServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        //startService(remoterServiceIntent);
        Log.v(TAG, "Connecting with service  result " + result);

    }

    static class SampleListener implements ISampleServiceListener {
        @Override
        public void onEcho(String echo) {
            Log.v(TAG, "Listener call " + echo);
        }
    }


}