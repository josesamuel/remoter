package util.remoter.remoterservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import util.remoter.service.ISampleService;
import util.remoter.service.ISampleService_Stub;


/**
 * Service that exposes impl for the remoter way
 */
public class SampleService extends Service {

    private static final String TAG = SampleService.class.getSimpleName();
    private ISampleService serviceImpl = new SampleServiceImpl();

    public SampleService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Service Create");

        //For testing with aidl clients, turn check off
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ISampleService_Stub.setStubProxyCheck(intent.getBooleanExtra("enable", true));
            }
        }, new IntentFilter("remoter.test.ProxyStubCheck"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ISampleService_Stub(serviceImpl);
    }
}
