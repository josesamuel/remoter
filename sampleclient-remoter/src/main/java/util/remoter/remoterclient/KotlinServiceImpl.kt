package util.remoter.remoterclient

import android.annotation.TargetApi
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import util.remoter.service.*
import java.io.IOException


/**
 * Sample service impl
 */
class KotlinServiceImpl : ISampleKotlinService {

    private val nonSuspendService = KotlinNonSuspendServiceImpl()
    private val nonSuspendService2 = KotlinNonSuspendServiceImpl2()

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

    override suspend fun testBoolean1(a: Boolean, arrayIn: BooleanArray, arrayOut: BooleanArray, arrayInOut: BooleanArray): Boolean {
        Log.v(TAG, "testBoolean1 $a")
        arrayInOut[0] = a
        arrayOut[0] = a
        return a
    }

    override suspend fun testBoolean2(a: Boolean, arrayIn: BooleanArray?, arrayOut: BooleanArray?, arrayInOut: BooleanArray?): BooleanArray {
        Log.v(TAG, "testBoolean2 $a")
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        val result = booleanArrayOf(a)
        return result
    }

    override suspend fun testBoolean3(a: Boolean, arrayIn: BooleanArray?, arrayOut: BooleanArray?, arrayInOut: BooleanArray?): BooleanArray? {
        Log.v(TAG, "testBoolean3 $a")
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override suspend fun testByte1(a: Byte, arrayIn: ByteArray, arrayOut: ByteArray, arrayInOut: ByteArray): Byte {
        Log.v(TAG, "testByte1 $a")
        arrayInOut[0] = a
        arrayOut[0] = a

        return a
    }

    override suspend fun testByte2(a: Byte, arrayIn: ByteArray?, arrayOut: ByteArray?, arrayInOut: ByteArray?): ByteArray {
        val result = byteArrayOf(a)
        return result
    }

    override suspend fun testByte3(a: Byte, arrayIn: ByteArray?, arrayOut: ByteArray?, arrayInOut: ByteArray?): ByteArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override suspend fun testChar1(a: Char, arrayIn: CharArray, arrayOut: CharArray, arrayInOut: CharArray): Char {
        arrayInOut[0] = a
        arrayOut[0] = a

        return a
    }

    override suspend fun testChar2(a: Char, arrayIn: CharArray?, arrayOut: CharArray?, arrayInOut: CharArray?): CharArray {
        return charArrayOf(a)
    }

    override suspend fun testChar3(a: Char, arrayIn: CharArray?, arrayOut: CharArray?, arrayInOut: CharArray?): CharArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override suspend fun testInt1(a: Int, arrayIn: IntArray, arrayOut: IntArray, arrayInOut: IntArray): Int {
        arrayInOut[0] = a
        arrayOut[0] = a

        return a
    }

    override suspend fun testInt2(a: Int, arrayIn: IntArray?, arrayOut: IntArray?, arrayInOut: IntArray?): IntArray {
        return intArrayOf(a)
    }

    override suspend fun testInt3(a: Int, arrayIn: IntArray?, arrayOut: IntArray?, arrayInOut: IntArray?): IntArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override suspend fun testLong1(a: Long, arrayIn: LongArray, arrayOut: LongArray, arrayInOut: LongArray): Long {
        arrayInOut[0] = a
        arrayOut[0] = a

        return a
    }

    override suspend fun testLong2(a: Long, arrayIn: LongArray?, arrayOut: LongArray?, arrayInOut: LongArray?): LongArray {

        return longArrayOf(a)
    }

    override suspend fun testLong3(a: Long, arrayIn: LongArray?, arrayOut: LongArray?, arrayInOut: LongArray?): LongArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override suspend fun testFloat1(a: Float, arrayIn: FloatArray, arrayOut: FloatArray, arrayInOut: FloatArray): Float {
        arrayInOut[0] = a
        arrayOut[0] = a

        return a
    }

    override suspend fun testFloat2(a: Float, arrayIn: FloatArray?, arrayOut: FloatArray?, arrayInOut: FloatArray?): FloatArray {
        return floatArrayOf(a)
    }

    override suspend fun testFloat3(a: Float, arrayIn: FloatArray?, arrayOut: FloatArray?, arrayInOut: FloatArray?): FloatArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override suspend fun testDouble1(a: Double, arrayIn: DoubleArray, arrayOut: DoubleArray, arrayInOut: DoubleArray): Double {
        arrayInOut[0] = a
        arrayOut[0] = a

        return a
    }

    override suspend fun testDouble2(a: Double, arrayIn: DoubleArray?, arrayOut: DoubleArray?, arrayInOut: DoubleArray?): DoubleArray {
        return doubleArrayOf(a)
    }

    override suspend fun testDouble3(a: Double, arrayIn: DoubleArray?, arrayOut: DoubleArray?, arrayInOut: DoubleArray?): DoubleArray? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = a
        }
        return arrayIn
    }

    override fun testCharSequence1(a: CharSequence): CharSequence {
        return a
    }

    override suspend fun testCharSequence2(a: CharSequence?): CharSequence? {
        return a
    }

    override suspend fun testString1(a: String, data: Array<String>, result: Array<String>, reply: Array<String>): String {
        result[0] = a
        reply[0] = a

        return a
    }

    override suspend fun testString2(a: String?, data: Array<String?>?, result: Array<String?>?, reply: Array<String?>?): Array<String?>? {
        if (reply != null && reply.isNotEmpty()) {
            reply[0] = a
        }
        return data
    }

    override suspend fun testList1(inList: MutableList<String>, listOut: MutableList<String>, inOutList: MutableList<String>): MutableList<String> {
        inOutList[0] = inList[0]
        listOut.add(inList[0])

        return inList
    }

    override suspend fun testList2(inList: MutableList<String?>?, listOut: MutableList<String?>?, inOutList: MutableList<String?>?): MutableList<String?>? {
        if (inOutList != null) {
            inOutList.clear()
            if (inList != null) {
                inOutList.addAll(inList)
            }
        }
        return inList
    }

    override suspend fun testMap1(inMap: MutableMap<String, Int>, outMap: MutableMap<String, Int>, inOutMap: MutableMap<String, Int>): MutableMap<String, Int> {
        outMap.clear()
        inOutMap.clear()
        outMap.putAll(inMap)
        inOutMap.putAll(inMap)

        return inMap
    }

    @TargetApi(Build.VERSION_CODES.N)
    override suspend fun testMap2(inMap: MutableMap<String?, Int?>?, outMap: MutableMap<String?, Int?>?, inOutMap: MutableMap<String?, Int?>?): MutableMap<String?, Int?>? {
        if (inOutMap != null) {
            inOutMap.clear()
            inMap?.forEach { t, u ->
                inOutMap[t] = u
            }
            Log.v(TAG, "testMap2 inout $inOutMap")
        }
        return inMap
    }

    override suspend fun testMap3(inMap: MutableMap<String?, Int>?, outMap: MutableMap<String, Int>?, inOutMap: MutableMap<String, Int>?): MutableMap<String, Int?>? {
        return mutableMapOf()
    }

    override suspend fun testParcelable1(inParcelable: FooParcelable<String>, parcelableOut: FooParcelable<String>, parcelableInOut: FooParcelable<String>): FooParcelable<String> {
        parcelableOut.stringValue = inParcelable.stringValue
        parcelableInOut.stringValue = inParcelable.stringValue
        return inParcelable
    }

    override suspend fun testParcelable2(inParcelable: FooParcelable<String?>?, parcelableOut: FooParcelable<String?>?, parcelableInOut: FooParcelable<String?>?): FooParcelable<String?>? {
        if (parcelableInOut != null) {
            if (inParcelable != null) {
                parcelableInOut.intValue = inParcelable.intValue
            }
        }
        return inParcelable
    }

    override suspend fun testParcelableArray1(arrayIn: Array<FooParcelable<String>>, arrayOut: Array<FooParcelable<String>>, arrayInOut: Array<FooParcelable<String>>): Array<FooParcelable<String>> {

        arrayOut[0] = arrayIn[0]
        arrayInOut[0] = arrayIn[0]
        return arrayIn
    }

    override suspend fun testParcelableArray2(arrayIn: Array<FooParcelable<String?>?>?, arrayOut: Array<FooParcelable<String?>?>?, arrayInOut: Array<FooParcelable<String?>?>?): Array<FooParcelable<String?>?>? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = arrayIn!![0]
        }

        return arrayIn
    }

    override suspend fun testSimpleParcelable1(inParcelable: SimpleParcelable, parcelableOut: SimpleParcelable, parcelableInOut: SimpleParcelable): SimpleParcelable {
        parcelableOut.stringValue = inParcelable.stringValue
        parcelableInOut.stringValue = inParcelable.stringValue

        return inParcelable
    }

    override suspend fun testSimpleParcelable2(inParcelable: SimpleParcelable?, parcelableOut: SimpleParcelable?, parcelableInOut: SimpleParcelable?): SimpleParcelable? {
        Log.v(TAG, "testSimpleParcelable2 $inParcelable , $parcelableOut , $parcelableInOut")
        if (parcelableInOut != null) {
            if (inParcelable != null) {
                parcelableInOut.intValue = inParcelable.intValue
            }
        }
        Log.v(TAG, "testSimpleParcelable2 returning $inParcelable $parcelableInOut")
        return inParcelable
    }

    override suspend fun testSimpleParcelableArray1(arrayIn: Array<SimpleParcelable>, arrayOut: Array<SimpleParcelable>, arrayInOut: Array<SimpleParcelable>): Array<SimpleParcelable> {
        arrayOut[0] = arrayIn[0]
        arrayInOut[0] = arrayIn[0]

        return arrayIn
    }

    override suspend fun testSimpleParcelableArray2(arrayIn: Array<SimpleParcelable?>?, arrayOut: Array<SimpleParcelable?>?, arrayInOut: Array<SimpleParcelable?>?): Array<SimpleParcelable?>? {
        if (arrayInOut != null && arrayInOut.isNotEmpty()) {
            arrayInOut[0] = arrayIn!![0]
        }

        return arrayIn
    }


    override suspend fun testParcel1(customData: CustomData, customData2: CustomData, customData3: CustomData): CustomData {
        return customData
    }

    override suspend fun testParcel2(customData: CustomData?, customData2: CustomData?, customData3: CustomData?): CustomData? {
        Log.v(TAG, "testParcel2 inout ${customData3?.data} ${customData3?.intData}")
        return customData
    }

    override suspend fun testParcelArray1(customData: Array<CustomData>, customData2: Array<CustomData>, customData3: Array<CustomData>): Array<CustomData> {
        customData2[0] = customData[0]
        customData3[0] = customData[0]
        return customData
    }

    override suspend fun testParcelArray2(customData: Array<CustomData?>?, customData2: Array<CustomData?>?, customData3: Array<CustomData?>?): Array<CustomData?>? {
        if (customData3?.isNotEmpty() == true && customData?.isNotEmpty() == true) {
            customData3[0] = customData[0]
        }

        return customData
    }

    override suspend fun testParcelList1(customData1: MutableList<CustomData>, customData2: MutableList<CustomData>, customData3: MutableList<CustomData>): MutableList<CustomData> {
        customData2.add(customData1[0])
        customData3[0] = customData1[0]

        return customData1
    }

    override suspend fun testParcelList2(customData1: MutableList<CustomData?>?, customData2: MutableList<CustomData?>?, customData3: MutableList<CustomData?>?): MutableList<CustomData?>? {

        if (customData3 != null) {
            customData3.clear()
            if (customData1 != null) {
                customData3.addAll(customData1)
            }
        }
        return customData1
    }

    override suspend fun testEcho(string: String?, listener: ISampleKotlinServiceListener?): String? {
        listener?.onEcho(string)
        return string
    }

    override suspend fun testOneway0(a: Int): Int {
        return a
    }

    override suspend fun testOneway1(a: Int) {
    }

    override suspend fun testException(): Int {
        throw IOException("IOException thrown")
    }

    override suspend fun testRuntimeException(): Int {
        throw RuntimeException("Runtime exception")
    }

    override suspend fun getBinder1(binder1: IExtE, binderArray: Array<IExtE>): IExtE {
        binder1.echoLong(1)
        return binder1
    }

    override suspend fun getBinder2(binder1: IExtE?, binderArray: Array<IExtE?>?): Array<IExtE?>? {
        return binderArray
    }

    override suspend fun getTemplateRemoter1(): ITest<String, CustomData, CustomData> {
        return ITest { param1, param2 -> CustomData().also { it.data = "input $param1" } }
    }

    override suspend fun getTemplateRemoter2(): ITest<String?, CustomData, CustomData?>? {
        return ITest { param1, param2 -> CustomData().also { it.data = "input $param1" } }
    }

    private val listeners = mutableListOf<ISampleKotlinServiceListener>()
    override suspend fun registerListener(listener: ISampleKotlinServiceListener?): Int {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener)
            return 1
        }
        return 0
    }

    override suspend fun unRegisterListener(listener: ISampleKotlinServiceListener?): Boolean {
        if (listener != null && listeners.contains(listener)) {
            listeners.remove(listener)
            return true
        }
        return false
    }

    override suspend fun testOnewayThrowsException(a: Int) {
        throw RuntimeException("Exception")
    }

    override suspend fun testSuspend() {
    }

    override suspend fun testSuspend2() = withContext(Dispatchers.IO) {
        100
    }

    override suspend fun testSuspend3(a: Int, b: String): MutableMap<Int?, CustomData?>? = withContext(Dispatchers.IO) {
        mutableMapOf<Int?, CustomData?>()
    }

    override suspend fun testSuspend4(a: Int, b: String): MutableMap<Int, CustomData> {
        return mutableMapOf<Int, CustomData>()
    }

    override fun testSuspend5(a: Int, b: String): MutableMap<Int?, CustomData?>? {
        return mutableMapOf()
    }

    override fun testSuspend6(a: Int, b: String): MutableMap<Int, CustomData> {
        return mutableMapOf()
    }

    override fun getNonSuspendInterface(): ISampleNonSuspendKotlinService {
        return nonSuspendService
    }

    override fun getNonSuspendInterface2(): ISampleNonSuspendKotlinService2 {
        return nonSuspendService2
    }

}