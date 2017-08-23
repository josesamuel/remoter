package util.remoter.aidlservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


/**
 * Service that exposes impl for the aidl
 */
public class SampleService extends Service {

    private static final String TAG = SampleService.class.getSimpleName();
    private IBinder serviceImpl = new SampleServiceImpl();

    public SampleService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Service Create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceImpl;
    }
}
