package util.remoter.remoterclient

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import remoter.RemoterProxy
import remoter.builder.ServiceConnector
//import remoter.builder.ServiceConnector
import util.remoter.service.*

/**
 * A sample client to validate there are no memory leaks while sending the stub across remote process.
 *
 * Perform register/unregister multiple times and run the profiler.
 *
 * Once GC is done on both client and server, the profiler should show only active listeners. Any unregistered
 * listeners should have been collected
 */
class TestActivity : Activity() {


    lateinit var service: ISampleKotlinService
    private val listener = object: ISampleKotlinServiceListener {
        override suspend fun onEcho(echo: String?) {
            Log.v(TAG, "Callback on onEcho $echo")
        }
    }


    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.v(TAG, "Service connected 1")
            sampleService = ISampleService_Proxy(service)
            Log.v(TAG, "Service connected $sampleService")
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }
    private var sampleService: ISampleService? = null
    private var sampleServiceListener: ISampleServiceListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        service = ISampleKotlinService_Proxy(ServiceConnector.of(this.applicationContext, "util.remoter.remoterclient.SampleKotlinService"))

        setContentView(R.layout.layout)
        findViewById<View>(R.id.register).setOnClickListener {
            Log.v(TAG, "Register $sampleService")
            if (sampleService != null) {
                sampleServiceListener = SampleListener()
                Log.v(TAG, "Registering " + sampleServiceListener.hashCode())
                val result = sampleService!!.registerListener(sampleServiceListener)
                Log.v(TAG, "Register result $result")
            }


            GlobalScope.launch {
                Log.v(TAG, "Echo result $service ${service.testEcho(System.currentTimeMillis().toString(), null)}")
                Log.v(TAG, " ${service.registerListener(listener)}")
            }
        }
        findViewById<View>(R.id.unregister).setOnClickListener {
            Log.v(TAG, "Un Register $sampleService")
            if (sampleService != null) {
                val result = sampleService!!.unRegisterListener(sampleServiceListener)
                //clear the stub
                (sampleService as RemoterProxy).destroyStub(sampleServiceListener)
                Log.v(TAG, "UnRegister result $result")
            }

            GlobalScope.launch {
                Log.v(TAG, " unregister result of suspend ${service.unRegisterListener(listener)}")
                ServiceConnector.of(applicationContext, "util.remoter.remoterclient.SampleKotlinService").disconnect()
            }

        }
        val remoterServiceIntent = Intent(ServiceIntents.INTENT_REMOTER_SERVICE)
        remoterServiceIntent.setClassName("util.remoter.remoterservice", ServiceIntents.INTENT_REMOTER_SERVICE)
        Log.v(TAG, "Connecting with service")
        val result = bindService(remoterServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        //startService(remoterServiceIntent);
        Log.v(TAG, "Connecting with service  result $result")


    }

    internal class SampleListener : ISampleServiceListener {
        override fun onEcho(echo: String) {
            Log.v(TAG, "Listener call $echo")
        }
    }

    companion object {
        private const val TAG = "Test"
    }
}