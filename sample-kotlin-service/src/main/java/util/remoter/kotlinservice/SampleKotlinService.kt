package util.remoter.kotlinservice

import android.app.Service
import android.content.Intent
import android.util.Log
import util.remoter.service.ITimeService
import util.remoter.service.ITimeService_Stub


/**
 * Sample service that exposes [ITimeService]
 */
class SampleKotlinService : Service() {

    companion object {
        const val TAG = "TimeServiceImpl"
    }

    private val binder by lazy {
        ITimeService_Stub(TimeServiceImpl())
    }

    override fun onBind(intent: Intent?) = binder

    override fun onCreate() {
        super.onCreate()
        Log.v(TAG, "onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(TAG, "onDestroy")
    }

}