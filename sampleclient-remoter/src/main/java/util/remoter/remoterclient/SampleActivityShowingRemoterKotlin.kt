package util.remoter.remoterclient

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import remoter.RemoterProxy
import util.remoter.service.ITimeService
import util.remoter.service.ITimeService_Proxy
import util.remoter.service.TIME_SERVICE_INTENT

/**
 * Sample activity that shows how to connect to a service that exposes a @Remoter interface defined in Kotlin
 */
class SampleActivityShowingRemoterKotlin : Activity() {

    //Simply create the service with the Proxy and the intent to connect
    private val timeService: ITimeService by lazy { ITimeService_Proxy(this, TIME_SERVICE_INTENT) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.layout_sample)

        findViewById<View>(R.id.time).setOnClickListener {
            Log.v(TAG, "Time")
            GlobalScope.launch {
                val time = timeService.getTime()
                Log.v(TAG, "Time result $time")
                findViewById<TextView>(R.id.resultView).text = "Response : $time"
            }
        }

        findViewById<View>(R.id.simulateCrash).setOnClickListener {
            Log.v(TAG, "simulateCrash")
            GlobalScope.launch {
                try {
                    val time = timeService.simulateServiceCrash()
                } catch (exception: Exception) {
                    Log.w(TAG, "Expected Exception ", exception)
                    findViewById<TextView>(R.id.resultView).text =
                        "Got expected exception ${exception.message}"
                }
            }
        }

    }

    override fun onDestroy() {
        //Clears the proxy, disconnects from service as needed
        (timeService as RemoterProxy).destroyProxy()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Sample"
    }
}