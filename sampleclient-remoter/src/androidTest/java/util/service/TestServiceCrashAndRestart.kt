package util.service

import android.support.test.InstrumentationRegistry
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Test
import remoter.RemoterProxy
import util.remoter.service.ITimeService
import util.remoter.service.ITimeService_Proxy
import util.remoter.service.TIME_SERVICE_INTENT

/**
 * Test that the [ITimeService_Proxy]  using the kotlin coroutine can automatically connect and handle
 * service disconnections
 */
class TestServiceCrashAndRestart {

    //Simply create the service with the Proxy and the intent to connect
    private val timeService: ITimeService by lazy {
        ITimeService_Proxy(
            InstrumentationRegistry.getContext(),
            TIME_SERVICE_INTENT
        )
    }


    @After
    fun tearDown() {
        (timeService as RemoterProxy).destroyProxy()
    }


    @Test
    fun testCrashRestart() {
        runBlocking {
            var time = timeService.getTime()
            Log.v("Test", "Got time $time")
            Assert.assertNotNull(time)

            try {
                timeService.simulateServiceCrash()
                Assert.fail("Should have thrown exception")
            } catch (ignored: Exception) {
                Log.v("Test", "Caught expected exception")
            }
            delay(150)
            Log.v("Test", "Testing service restart after crash ")
            time = timeService.getTime()
            Log.v("Test", "Got time $time")
            Assert.assertNotNull(time)

            try {
                timeService.simulateServiceCrash()
                Assert.fail("Should have thrown exception")
            } catch (ignored: Exception) {
                Log.v("Test", "Caught expected exception")
            }
            delay(150)
            Log.v("Test", "Testing service restart after crash ")
            time = timeService.getTime()
            Log.v("Test", "Got time $time")
            Assert.assertNotNull(time)
        }
    }


}