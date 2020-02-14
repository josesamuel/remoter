package util.remoter.remoterclient

import android.annotation.TargetApi
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import util.remoter.service.*
import java.io.IOException


/**
 * Sample service impl
 */
class KotlinNonSuspendServiceImpl : ISampleNonSuspendKotlinService {

    companion object {
        const val TAG = "KotlinServiceImpl"
    }

    override fun testVarArg(vararg string: String?)  : Int {
        var totalSize = 0
        string.forEach {
            Log.v(TAG, "testVarArg $it")
            totalSize ++
        }
        return totalSize
    }

    override fun testBoolean1(a: Boolean, arrayIn: BooleanArray, arrayOut: BooleanArray, arrayInOut: BooleanArray): Boolean {
        Log.v(TAG, "testBoolean1 $a")
        return a
    }

    override fun testBoolean2(a: Boolean, arrayIn: BooleanArray?, arrayOut: BooleanArray?, arrayInOut: BooleanArray?): BooleanArray {
        Log.v(TAG, "testBoolean2 $a")
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        val result = booleanArrayOf(a)
        return result
    }

    override fun testBoolean3(a: Boolean, arrayIn: BooleanArray?, arrayOut: BooleanArray?, arrayInOut: BooleanArray?): BooleanArray? {
        Log.v(TAG, "testBoolean3 $a")
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override fun testByte1(a: Byte, arrayIn: ByteArray, arrayOut: ByteArray, arrayInOut: ByteArray): Byte {
        Log.v(TAG, "testByte1 $a")
        return a
    }

    override fun testByte2(a: Byte, arrayIn: ByteArray?, arrayOut: ByteArray?, arrayInOut: ByteArray?): ByteArray {
        val result = byteArrayOf(a)
        return result
    }

    override fun testByte3(a: Byte, arrayIn: ByteArray?, arrayOut: ByteArray?, arrayInOut: ByteArray?): ByteArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override fun testChar1(a: Char, arrayIn: CharArray, arrayOut: CharArray, arrayInOut: CharArray): Char {
        return a
    }

    override fun testChar2(a: Char, arrayIn: CharArray?, arrayOut: CharArray?, arrayInOut: CharArray?): CharArray {
        return charArrayOf(a)
    }

    override fun testChar3(a: Char, arrayIn: CharArray?, arrayOut: CharArray?, arrayInOut: CharArray?): CharArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override fun testInt1(a: Int, arrayIn: IntArray, arrayOut: IntArray, arrayInOut: IntArray): Int {
        return a
    }

    override fun testInt2(a: Int, arrayIn: IntArray?, arrayOut: IntArray?, arrayInOut: IntArray?): IntArray {
        return intArrayOf(a)
    }

    override fun testInt3(a: Int, arrayIn: IntArray?, arrayOut: IntArray?, arrayInOut: IntArray?): IntArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override fun testLong1(a: Long, arrayIn: LongArray, arrayOut: LongArray, arrayInOut: LongArray): Long {
        return a
    }

    override fun testLong2(a: Long, arrayIn: LongArray?, arrayOut: LongArray?, arrayInOut: LongArray?): LongArray {

        return longArrayOf(a)
    }

    override fun testLong3(a: Long, arrayIn: LongArray?, arrayOut: LongArray?, arrayInOut: LongArray?): LongArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override fun testFloat1(a: Float, arrayIn: FloatArray, arrayOut: FloatArray, arrayInOut: FloatArray): Float {
        return a
    }

    override fun testFloat2(a: Float, arrayIn: FloatArray?, arrayOut: FloatArray?, arrayInOut: FloatArray?): FloatArray {
        return floatArrayOf(a)
    }

    override fun testFloat3(a: Float, arrayIn: FloatArray?, arrayOut: FloatArray?, arrayInOut: FloatArray?): FloatArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override fun testDouble1(a: Double, arrayIn: DoubleArray, arrayOut: DoubleArray, arrayInOut: DoubleArray): Double {
        return a
    }

    override fun testDouble2(a: Double, arrayIn: DoubleArray?, arrayOut: DoubleArray?, arrayInOut: DoubleArray?): DoubleArray {
        return doubleArrayOf(a)
    }

    override fun testDouble3(a: Double, arrayIn: DoubleArray?, arrayOut: DoubleArray?, arrayInOut: DoubleArray?): DoubleArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override fun testCharSequence1(a: CharSequence): CharSequence {
        return a
    }

    override fun testCharSequence2(a: CharSequence?): CharSequence? {
        return a
    }

    override fun testString1(a: String, data: Array<String>, result: Array<String>, reply: Array<String>): String {

        return a
    }

    override fun testString2(a: String?, data: Array<String?>?, result: Array<String?>?, reply: Array<String?>?): Array<String?>? {
        if (reply != null && reply.isNotEmpty()) {
            reply[0] = a
        }
        return data
    }

    override fun testList1(inList: MutableList<String>, listOut: MutableList<String>, inOutList: MutableList<String>): MutableList<String> {
        return inList
    }

    override fun testList2(inList: MutableList<String?>?, listOut: MutableList<String?>?, inOutList: MutableList<String?>?): MutableList<String?>? {
        if (inOutList != null) {
            inOutList.clear()
            if (inList != null) {
                inOutList.addAll(inList)
            }
        }
        return inList
    }

    override fun testMap1(inMap: MutableMap<String, Int>, outMap: MutableMap<String, Int>, inOutMap: MutableMap<String, Int>): MutableMap<String, Int> {
        return inMap
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun testMap2(inMap: MutableMap<String?, Int?>?, outMap: MutableMap<String?, Int?>?, inOutMap: MutableMap<String?, Int?>?): MutableMap<String?, Int?>? {
        if (inOutMap != null) {
            inOutMap.clear()
            inMap?.forEach { t, u ->
                inOutMap[t] = u
            }
            Log.v(TAG, "testMap2 inout $inOutMap")
        }
        return inMap
    }

    override fun testMap3(inMap: MutableMap<String?, Int>?, outMap: MutableMap<String, Int>?, inOutMap: MutableMap<String, Int>?): MutableMap<String, Int?>? {
        return mutableMapOf()
    }

    override fun testParcelable1(inParcelable: FooParcelable<String>, parcelableOut: FooParcelable<String>, parcelableInOut: FooParcelable<String>): FooParcelable<String> {
        return inParcelable
    }

    override fun testParcelable2(inParcelable: FooParcelable<String?>?, parcelableOut: FooParcelable<String?>?, parcelableInOut: FooParcelable<String?>?): FooParcelable<String?>? {
        if (parcelableInOut != null) {
            if (inParcelable != null) {
                parcelableInOut.intValue = inParcelable.intValue
            }
        }
        return inParcelable
    }

    override fun testParcelableArray1(arrayIn: Array<FooParcelable<String>>, arrayOut: Array<FooParcelable<String>>, arrayInOut: Array<FooParcelable<String>>): Array<FooParcelable<String>> {
        return arrayIn
    }

    override fun testParcelableArray2(arrayIn: Array<FooParcelable<String?>?>?, arrayOut: Array<FooParcelable<String?>?>?, arrayInOut: Array<FooParcelable<String?>?>?): Array<FooParcelable<String?>?>? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = arrayIn!![0]
        }

        return arrayIn
    }

    override fun testSimpleParcelable1(inParcelable: SimpleParcelable, parcelableOut: SimpleParcelable, parcelableInOut: SimpleParcelable): SimpleParcelable {
        return inParcelable
    }

    override fun testSimpleParcelable2(inParcelable: SimpleParcelable?, parcelableOut: SimpleParcelable?, parcelableInOut: SimpleParcelable?): SimpleParcelable? {
        Log.v(TAG, "testSimpleParcelable2 $inParcelable , $parcelableOut , $parcelableInOut")
        if (parcelableInOut != null) {
            if (inParcelable != null) {
                parcelableInOut.intValue = inParcelable.intValue
            }
        }
        Log.v(TAG, "testSimpleParcelable2 returning $inParcelable $parcelableInOut")
        return inParcelable
    }

    override fun testSimpleParcelableArray1(arrayIn: Array<SimpleParcelable>, arrayOut: Array<SimpleParcelable>, arrayInOut: Array<SimpleParcelable>): Array<SimpleParcelable> {
        return arrayIn
    }

    override fun testSimpleParcelableArray2(arrayIn: Array<SimpleParcelable?>?, arrayOut: Array<SimpleParcelable?>?, arrayInOut: Array<SimpleParcelable?>?): Array<SimpleParcelable?>? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = arrayIn!![0]
        }

        return arrayIn
    }

    override fun testParcel1(customData: CustomData, customData2: CustomData, customData3: CustomData): CustomData {
        return customData
    }

    override fun testParcel2(customData: CustomData?, customData2: CustomData?, customData3: CustomData?): CustomData? {
        Log.v(TAG, "testParcel2 inout ${customData3?.data} ${customData3?.intData}")
        return customData
    }

    override fun testParcelArray1(customData: Array<CustomData>, customData2: Array<CustomData>, customData3: Array<CustomData>): Array<CustomData> {
        return customData
    }

    override fun testParcelArray2(customData: Array<CustomData?>?, customData2: Array<CustomData?>?, customData3: Array<CustomData?>?): Array<CustomData?>? {
        if (customData3?.isNotEmpty() == true && customData?.isNotEmpty() == true) {
            customData3[0] = customData[0]
            Log.v(TAG, "testParcelArray2 inout ${customData3[0]?.intData} ${customData3[0]?.data}")
        }


        return customData
    }

    override fun testParcelList1(customData1: MutableList<CustomData>, customData2: MutableList<CustomData>, customData3: MutableList<CustomData>): MutableList<CustomData> {
        return customData1
    }

    override fun testParcelList2(customData1: MutableList<CustomData?>?, customData2: MutableList<CustomData?>?, customData3: MutableList<CustomData?>?): MutableList<CustomData?>? {

        if (customData3 != null) {
            customData3.clear()
            if (customData1 != null) {
                customData3.addAll(customData1)
            }
        }
        return customData1
    }

    override fun testEcho(string: String?, listener: ISampleKotlinServiceListener?): String? {
        runBlocking {
            listener?.onEcho(string)
        }
        return string
    }

    override fun testOneway0(a: Int): Int {
        return a
    }

    override fun testOneway1(a: Int) {
    }

    override fun testException(): Int {
        throw IOException("IOException thrown")
    }

    override fun testRuntimeException(): Int {
        throw RuntimeException("Runtime exception")
    }

    override fun getBinder1(binder1: IExtE, binderArray: Array<IExtE>): IExtE {
        binder1.echoLong(1)
        return binder1
    }

    override fun getBinder2(binder1: IExtE?, binderArray: Array<IExtE?>?): Array<IExtE?>? {
        return binderArray
    }

    override fun getTemplateRemoter1(): ITest<String, CustomData, CustomData> {
        return ITest { param1, param2 -> CustomData().also { it.data = "input $param1" } }
    }

    override fun getTemplateRemoter2(): ITest<String?, CustomData, CustomData?>? {
        return ITest { param1, param2 -> CustomData().also { it.data = "input $param1" } }
    }

    private val listeners = mutableListOf<ISampleKotlinServiceListener>()
    override fun registerListener(listener: ISampleKotlinServiceListener?): Int {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener)
            return 1
        }
        return 0
    }

    override fun unRegisterListener(listener: ISampleKotlinServiceListener?): Boolean {
        if (listener != null && listeners.contains(listener)) {
            listeners.remove(listener)
            return true
        }
        return false
    }

    override fun testOnewayThrowsException(a: Int) {
        throw RuntimeException("Exception")
    }

}