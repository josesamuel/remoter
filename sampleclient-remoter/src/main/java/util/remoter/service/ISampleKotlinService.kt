package util.remoter.service

import remoter.annotations.*
import java.io.IOException

@Remoter
interface ISampleKotlinService {


    //vararg is not supported for suspend. Either use array or nonsuspend
    //suspend fun testVarArg(vararg string: String?)

    //If return is nullable for suspend function, specify [NullableType]

    fun testVarArg(vararg string: String?) : Int

    suspend fun testBoolean1(a: Boolean, @ParamIn arrayIn: BooleanArray, @ParamOut arrayOut: BooleanArray, arrayInOut: BooleanArray): Boolean
    suspend fun testBoolean2(a: Boolean, @ParamIn arrayIn: BooleanArray?, @ParamOut arrayOut: BooleanArray?, arrayInOut: BooleanArray?): BooleanArray
    @NullableType
    suspend fun testBoolean3(a: Boolean, @ParamIn arrayIn: BooleanArray?, @ParamOut arrayOut: BooleanArray?, arrayInOut: BooleanArray?): BooleanArray?


    suspend fun testByte1(a: Byte, @ParamIn arrayIn: ByteArray, @ParamOut arrayOut: ByteArray, arrayInOut: ByteArray): Byte
    suspend fun testByte2(a: Byte, @ParamIn arrayIn: ByteArray?, @ParamOut arrayOut: ByteArray?, arrayInOut: ByteArray?): ByteArray
    @NullableType
    suspend fun testByte3(a: Byte, @ParamIn arrayIn: ByteArray?, @ParamOut arrayOut: ByteArray?, arrayInOut: ByteArray?): ByteArray?


    suspend fun testChar1(a: Char, @ParamIn arrayIn: CharArray, @ParamOut arrayOut: CharArray, arrayInOut: CharArray): Char
    suspend fun testChar2(a: Char, @ParamIn arrayIn: CharArray?, @ParamOut arrayOut: CharArray?, arrayInOut: CharArray?): CharArray
    @NullableType
    suspend fun testChar3(a: Char, @ParamIn arrayIn: CharArray?, @ParamOut arrayOut: CharArray?, arrayInOut: CharArray?): CharArray?


    suspend fun testInt1(a: Int, @ParamIn arrayIn: IntArray, @ParamOut arrayOut: IntArray, arrayInOut: IntArray): Int
    suspend fun testInt2(a: Int, @ParamIn arrayIn: IntArray?, @ParamOut arrayOut: IntArray?, arrayInOut: IntArray?): IntArray
    @NullableType
    suspend fun testInt3(a: Int, @ParamIn arrayIn: IntArray?, @ParamOut arrayOut: IntArray?, arrayInOut: IntArray?): IntArray?


    suspend fun testLong1(a: Long, @ParamIn arrayIn: LongArray, @ParamOut arrayOut: LongArray, arrayInOut: LongArray): Long
    suspend fun testLong2(a: Long, @ParamIn arrayIn: LongArray?, @ParamOut arrayOut: LongArray?, arrayInOut: LongArray?): LongArray
    @NullableType
    suspend fun testLong3(a: Long, @ParamIn arrayIn: LongArray?, @ParamOut arrayOut: LongArray?, arrayInOut: LongArray?): LongArray?


    suspend fun testFloat1(a: Float, @ParamIn arrayIn: FloatArray, @ParamOut arrayOut: FloatArray, arrayInOut: FloatArray): Float
    suspend fun testFloat2(a: Float, @ParamIn arrayIn: FloatArray?, @ParamOut arrayOut: FloatArray?, arrayInOut: FloatArray?): FloatArray
    @NullableType
    suspend fun testFloat3(a: Float, @ParamIn arrayIn: FloatArray?, @ParamOut arrayOut: FloatArray?, arrayInOut: FloatArray?): FloatArray?


    suspend fun testDouble1(a: Double, @ParamIn arrayIn: DoubleArray, @ParamOut arrayOut: DoubleArray, arrayInOut: DoubleArray): Double
    suspend fun testDouble2(a: Double, @ParamIn arrayIn: DoubleArray?, @ParamOut arrayOut: DoubleArray?, arrayInOut: DoubleArray?): DoubleArray
    @NullableType
    suspend fun testDouble3(a: Double, @ParamIn arrayIn: DoubleArray?, @ParamOut arrayOut: DoubleArray?, arrayInOut: DoubleArray?): DoubleArray?


    fun testCharSequence1(a: CharSequence): CharSequence
    @NullableType
    suspend fun testCharSequence2(a: CharSequence?): CharSequence?

    suspend fun testString1(a: String, @ParamIn data: Array<String>, @ParamOut result: Array<String>, reply: Array<String>): String

    /**
     * Using [NullableType] to specify that the type 'String' in the array is nullable Array<String?>
     */
    @NullableType
    suspend fun testString2(a: String?, @ParamIn @NullableType data: Array<String?>?, @ParamOut @NullableType result: Array<String?>?, @NullableType reply: Array<String?>?): Array<String?>?


    //List should be explicity Mutable
    suspend fun testList1(@ParamIn inList: MutableList<String>, @ParamOut listOut: MutableList<String>, inOutList: MutableList<String>): MutableList<String>

    @NullableType
    suspend fun testList2(@ParamIn @NullableType inList: MutableList<String?>?, @ParamOut @NullableType listOut: MutableList<String?>?, @NullableType inOutList: MutableList<String?>?): MutableList<String?>?


    suspend fun testMap1(@ParamIn inMap: MutableMap<String, Int>, @ParamOut outMap: MutableMap<String, Int>, inOutMap: MutableMap<String, Int>): MutableMap<String, Int>
    @NullableType(nullableIndexes = [0, 1])
    suspend fun testMap2(@NullableType(nullableIndexes = [0, 1]) @ParamIn inMap: MutableMap<String?, Int?>?, @NullableType(nullableIndexes = [0, 1]) @ParamOut outMap: MutableMap<String?, Int?>?,
                         @NullableType(nullableIndexes = [0, 1]) inOutMap: MutableMap<String?, Int?>?): MutableMap<String?, Int?>?

    @NullableType(nullableIndexes = [1])
    suspend fun testMap3(@ParamIn @NullableType inMap: MutableMap<String?, Int>?, @ParamOut outMap: MutableMap<String, Int>?,
                         inOutMap: MutableMap<String, Int>?): MutableMap<String, Int?>?


    suspend fun testParcelable1(@ParamIn inParcelable: FooParcelable<String>, @ParamOut parcelableOut: FooParcelable<String>, parcelableInOut: FooParcelable<String>): FooParcelable<String>
    @NullableType
    suspend fun testParcelable2(@ParamIn @NullableType inParcelable: FooParcelable<String?>?, @ParamOut @NullableType parcelableOut: FooParcelable<String?>?, @NullableType parcelableInOut: FooParcelable<String?>?): FooParcelable<String?>?


    suspend fun testParcelableArray1(@ParamIn arrayIn: Array<FooParcelable<String>>, @ParamOut arrayOut: Array<FooParcelable<String>>, arrayInOut: Array<FooParcelable<String>>): Array<FooParcelable<String>>

    @NullableType
    suspend fun testParcelableArray2(@ParamIn @NullableType arrayIn: Array<FooParcelable<String?>?>?, @ParamOut @NullableType arrayOut: Array<FooParcelable<String?>?>?, @NullableType arrayInOut: Array<FooParcelable<String?>?>?): Array<FooParcelable<String?>?>?


    suspend fun testSimpleParcelable1(@ParamIn inParcelable: SimpleParcelable, @ParamOut parcelableOut: SimpleParcelable, parcelableInOut: SimpleParcelable): SimpleParcelable

    @NullableType
    suspend fun testSimpleParcelable2(@ParamIn inParcelable: SimpleParcelable?, @ParamOut parcelableOut: SimpleParcelable?, parcelableInOut: SimpleParcelable?): SimpleParcelable?


    suspend fun testSimpleParcelableArray1(@ParamIn arrayIn: Array<SimpleParcelable>, @ParamOut arrayOut: Array<SimpleParcelable>, arrayInOut: Array<SimpleParcelable>): Array<SimpleParcelable>
    @NullableType
    suspend fun testSimpleParcelableArray2(@ParamIn @NullableType arrayIn: Array<SimpleParcelable?>?, @ParamOut @NullableType arrayOut: Array<SimpleParcelable?>?, @NullableType arrayInOut: Array<SimpleParcelable?>?): Array<SimpleParcelable?>?


    suspend fun testParcel1(@ParamIn customData: CustomData, @ParamOut customData2: CustomData, customData3: CustomData): CustomData
    @NullableType
    suspend fun testParcel2(@ParamIn customData: CustomData?, @ParamOut customData2: CustomData?, customData3: CustomData?): CustomData?


    suspend fun testParcelArray1(@ParamIn customData: Array<CustomData>, @ParamOut customData2: Array<CustomData>, customData3: Array<CustomData>): Array<CustomData>
    @NullableType
    suspend fun testParcelArray2(@ParamIn @NullableType customData: Array<CustomData?>?, @ParamOut @NullableType customData2: Array<CustomData?>?, @NullableType customData3: Array<CustomData?>?): Array<CustomData?>?


    suspend fun testParcelList1(@ParamIn customData1: MutableList<CustomData>, @ParamOut customData2: MutableList<CustomData>, customData3: MutableList<CustomData>): MutableList<CustomData>

    @NullableType
    suspend fun testParcelList2(@ParamIn @NullableType customData1: MutableList<CustomData?>?, @ParamOut @NullableType customData2: MutableList<CustomData?>?, @NullableType customData3: MutableList<CustomData?>?): MutableList<CustomData?>?


    @NullableType
    suspend fun testEcho(string: String?, listener: ISampleKotlinServiceListener?) : String?

    @Oneway
    suspend fun testOneway0(a: Int): Int

    @Oneway
    suspend fun testOneway1(a: Int)

    @Throws(IOException::class, InterruptedException::class)
    suspend fun testException(): Int

    suspend fun testRuntimeException(): Int


    suspend fun getBinder1(binder1: IExtE, binderArray: Array<IExtE>): IExtE
    @NullableType
    suspend fun getBinder2(binder1: IExtE?, @NullableType binderArray: Array<IExtE?>?): Array<IExtE?>?


    suspend fun getTemplateRemoter1(): ITest<String, CustomData, CustomData>

    @NullableType(nullableIndexes = [0, 2])
    suspend fun getTemplateRemoter2(): ITest<String?, CustomData, CustomData?>?

    suspend fun registerListener(listener: ISampleKotlinServiceListener?): Int
    suspend fun unRegisterListener(listener: ISampleKotlinServiceListener?): Boolean

    @Oneway
    suspend fun testOnewayThrowsException(a: Int)


    suspend fun testSuspend()
    suspend fun testSuspend2(): Int

    @NullableType(nullableIndexes = [0, 1])
    suspend fun testSuspend3(a: Int, b: String): MutableMap<Int?, CustomData?>?

    suspend fun testSuspend4(a: Int, b: String): MutableMap<Int, CustomData>

    @NullableType(nullableIndexes = [0, 1])
    fun testSuspend5(a: Int, b: String): MutableMap<Int?, CustomData?>?

    fun testSuspend6(a: Int, b: String): MutableMap<Int, CustomData>

    fun getNonSuspendInterface() : ISampleNonSuspendKotlinService

    fun getNonSuspendInterface2() : ISampleNonSuspendKotlinService2

    fun getExtE() : IExtE

    suspend fun getExtESuspend() : IExtE

}