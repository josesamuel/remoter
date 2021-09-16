package util.remoter.kotlinservice

import android.util.Log
import util.remoter.service.ITimeService
import kotlin.system.exitProcess


/**
 * Implementation of [ITimeService]
 */
class TimeServiceImpl : ITimeService {


    companion object {
        const val TAG = "TimeServiceImpl"
    }

    override suspend fun getTime(): Long {
        val time = System.currentTimeMillis()
        Log.v(TAG, "getTime returning :$time")
        return time
    }

    override suspend fun simulateServiceCrash(): Int {
        Log.v(TAG, "simulateServiceCrash")
        exitProcess(0)
    }

}