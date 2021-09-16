package util.remoter.service

import remoter.annotations.Remoter

/**Intent to connect for this service*/
const val TIME_SERVICE_INTENT = "util.remoter.service.ITimeService"

/**
 * A sample interface in kotlin that uses
 */
@Remoter
interface ITimeService {

    /**
     * Returns current time
     */
    suspend fun getTime(): Long

    /**
     * Simulate a service side crash
     */
    suspend fun simulateServiceCrash(): Int

}