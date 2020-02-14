package util.remoter.service

import remoter.annotations.*
import java.io.IOException

@Remoter
interface ISampleNonSuspendKotlinService {


    fun testVarArg(vararg string: String?) : Int

    fun testBoolean1(a: Boolean, @ParamIn arrayIn: BooleanArray, @ParamOut arrayOut: BooleanArray, arrayInOut: BooleanArray): Boolean
    fun testBoolean2(a: Boolean, @ParamIn arrayIn: BooleanArray?, @ParamOut arrayOut: BooleanArray?, arrayInOut: BooleanArray?): BooleanArray

    fun testBoolean3(a: Boolean, @ParamIn arrayIn: BooleanArray?, @ParamOut arrayOut: BooleanArray?, arrayInOut: BooleanArray?): BooleanArray?


    fun testByte1(a: Byte, @ParamIn arrayIn: ByteArray, @ParamOut arrayOut: ByteArray, arrayInOut: ByteArray): Byte
    fun testByte2(a: Byte, @ParamIn arrayIn: ByteArray?, @ParamOut arrayOut: ByteArray?, arrayInOut: ByteArray?): ByteArray
    @NullableType
    fun testByte3(a: Byte, @ParamIn arrayIn: ByteArray?, @ParamOut arrayOut: ByteArray?, arrayInOut: ByteArray?): ByteArray?


    fun testChar1(a: Char, @ParamIn arrayIn: CharArray, @ParamOut arrayOut: CharArray, arrayInOut: CharArray): Char
    fun testChar2(a: Char, @ParamIn arrayIn: CharArray?, @ParamOut arrayOut: CharArray?, arrayInOut: CharArray?): CharArray
    @NullableType
    fun testChar3(a: Char, @ParamIn arrayIn: CharArray?, @ParamOut arrayOut: CharArray?, arrayInOut: CharArray?): CharArray?


    fun testInt1(a: Int, @ParamIn arrayIn: IntArray, @ParamOut arrayOut: IntArray, arrayInOut: IntArray): Int
    fun testInt2(a: Int, @ParamIn arrayIn: IntArray?, @ParamOut arrayOut: IntArray?, arrayInOut: IntArray?): IntArray
    @NullableType
    fun testInt3(a: Int, @ParamIn arrayIn: IntArray?, @ParamOut arrayOut: IntArray?, arrayInOut: IntArray?): IntArray?


    fun testLong1(a: Long, @ParamIn arrayIn: LongArray, @ParamOut arrayOut: LongArray, arrayInOut: LongArray): Long
    fun testLong2(a: Long, @ParamIn arrayIn: LongArray?, @ParamOut arrayOut: LongArray?, arrayInOut: LongArray?): LongArray
    @NullableType
    fun testLong3(a: Long, @ParamIn arrayIn: LongArray?, @ParamOut arrayOut: LongArray?, arrayInOut: LongArray?): LongArray?


    fun testFloat1(a: Float, @ParamIn arrayIn: FloatArray, @ParamOut arrayOut: FloatArray, arrayInOut: FloatArray): Float
    fun testFloat2(a: Float, @ParamIn arrayIn: FloatArray?, @ParamOut arrayOut: FloatArray?, arrayInOut: FloatArray?): FloatArray
    @NullableType
    fun testFloat3(a: Float, @ParamIn arrayIn: FloatArray?, @ParamOut arrayOut: FloatArray?, arrayInOut: FloatArray?): FloatArray?


    fun testDouble1(a: Double, @ParamIn arrayIn: DoubleArray, @ParamOut arrayOut: DoubleArray, arrayInOut: DoubleArray): Double
    fun testDouble2(a: Double, @ParamIn arrayIn: DoubleArray?, @ParamOut arrayOut: DoubleArray?, arrayInOut: DoubleArray?): DoubleArray
    @NullableType
    fun testDouble3(a: Double, @ParamIn arrayIn: DoubleArray?, @ParamOut arrayOut: DoubleArray?, arrayInOut: DoubleArray?): DoubleArray?


    fun testCharSequence1(a: CharSequence): CharSequence
    @NullableType
    fun testCharSequence2(a: CharSequence?): CharSequence?

    fun testString1(a: String, @ParamIn data: Array<String>, @ParamOut result: Array<String>, reply: Array<String>): String

    /**
     * Using [NullableType] to specify that the type 'String' in the array is nullable Array<String?>
     */
    @NullableType
    fun testString2(a: String?, @ParamIn @NullableType data: Array<String?>?, @ParamOut @NullableType result: Array<String?>?, @NullableType reply: Array<String?>?): Array<String?>?


    //List should be explicity Mutable
    fun testList1(@ParamIn inList: MutableList<String>, @ParamOut listOut: MutableList<String>, inOutList: MutableList<String>): MutableList<String>

    @NullableType
    fun testList2(@ParamIn @NullableType inList: MutableList<String?>?, @ParamOut @NullableType listOut: MutableList<String?>?, @NullableType inOutList: MutableList<String?>?): MutableList<String?>?


    fun testMap1(@ParamIn inMap: MutableMap<String, Int>, @ParamOut outMap: MutableMap<String, Int>, inOutMap: MutableMap<String, Int>): MutableMap<String, Int>
    @NullableType(nullableIndexes = [0, 1])
    fun testMap2(@NullableType(nullableIndexes = [0, 1]) @ParamIn inMap: MutableMap<String?, Int?>?, @NullableType(nullableIndexes = [0, 1]) @ParamOut outMap: MutableMap<String?, Int?>?,
                         @NullableType(nullableIndexes = [0, 1]) inOutMap: MutableMap<String?, Int?>?): MutableMap<String?, Int?>?

    @NullableType(nullableIndexes = [1])
    fun testMap3(@ParamIn @NullableType inMap: MutableMap<String?, Int>?, @ParamOut outMap: MutableMap<String, Int>?,
                         inOutMap: MutableMap<String, Int>?): MutableMap<String, Int?>?


    fun testParcelable1(@ParamIn inParcelable: FooParcelable<String>, @ParamOut parcelableOut: FooParcelable<String>, parcelableInOut: FooParcelable<String>): FooParcelable<String>
    @NullableType
    fun testParcelable2(@ParamIn @NullableType inParcelable: FooParcelable<String?>?, @ParamOut @NullableType parcelableOut: FooParcelable<String?>?, @NullableType parcelableInOut: FooParcelable<String?>?): FooParcelable<String?>?


    fun testParcelableArray1(@ParamIn arrayIn: Array<FooParcelable<String>>, @ParamOut arrayOut: Array<FooParcelable<String>>, arrayInOut: Array<FooParcelable<String>>): Array<FooParcelable<String>>

    @NullableType
    fun testParcelableArray2(@ParamIn @NullableType arrayIn: Array<FooParcelable<String?>?>?, @ParamOut @NullableType arrayOut: Array<FooParcelable<String?>?>?, @NullableType arrayInOut: Array<FooParcelable<String?>?>?): Array<FooParcelable<String?>?>?


    fun testSimpleParcelable1(@ParamIn inParcelable: SimpleParcelable, @ParamOut parcelableOut: SimpleParcelable, parcelableInOut: SimpleParcelable): SimpleParcelable

    @NullableType
    fun testSimpleParcelable2(@ParamIn inParcelable: SimpleParcelable?, @ParamOut parcelableOut: SimpleParcelable?, parcelableInOut: SimpleParcelable?): SimpleParcelable?


    fun testSimpleParcelableArray1(@ParamIn arrayIn: Array<SimpleParcelable>, @ParamOut arrayOut: Array<SimpleParcelable>, arrayInOut: Array<SimpleParcelable>): Array<SimpleParcelable>
    @NullableType
    fun testSimpleParcelableArray2(@ParamIn @NullableType arrayIn: Array<SimpleParcelable?>?, @ParamOut @NullableType arrayOut: Array<SimpleParcelable?>?, @NullableType arrayInOut: Array<SimpleParcelable?>?): Array<SimpleParcelable?>?


    fun testParcel1(@ParamIn customData: CustomData, @ParamOut customData2: CustomData, customData3: CustomData): CustomData
    @NullableType
    fun testParcel2(@ParamIn customData: CustomData?, @ParamOut customData2: CustomData?, customData3: CustomData?): CustomData?


    fun testParcelArray1(@ParamIn customData: Array<CustomData>, @ParamOut customData2: Array<CustomData>, customData3: Array<CustomData>): Array<CustomData>
    @NullableType
    fun testParcelArray2(@ParamIn @NullableType customData: Array<CustomData?>?, @ParamOut @NullableType customData2: Array<CustomData?>?, @NullableType customData3: Array<CustomData?>?): Array<CustomData?>?


    fun testParcelList1(@ParamIn customData1: MutableList<CustomData>, @ParamOut customData2: MutableList<CustomData>, customData3: MutableList<CustomData>): MutableList<CustomData>

    @NullableType
    fun testParcelList2(@ParamIn @NullableType customData1: MutableList<CustomData?>?, @ParamOut @NullableType customData2: MutableList<CustomData?>?, @NullableType customData3: MutableList<CustomData?>?): MutableList<CustomData?>?


    @NullableType
    fun testEcho(string: String?, listener: ISampleKotlinServiceListener?) : String?

    @Oneway
    fun testOneway0(a: Int): Int

    @Oneway
    fun testOneway1(a: Int)

    @Throws(IOException::class, InterruptedException::class)
    fun testException(): Int

    fun testRuntimeException(): Int


    fun getBinder1(binder1: IExtE, binderArray: Array<IExtE>): IExtE
    @NullableType
    fun getBinder2(binder1: IExtE?, @NullableType binderArray: Array<IExtE?>?): Array<IExtE?>?


    fun getTemplateRemoter1(): ITest<String, CustomData, CustomData>

    @NullableType(nullableIndexes = [0, 2])
    fun getTemplateRemoter2(): ITest<String?, CustomData, CustomData?>?

    fun registerListener(listener: ISampleKotlinServiceListener?): Int
    fun unRegisterListener(listener: ISampleKotlinServiceListener?): Boolean

    @Oneway
    fun testOnewayThrowsException(a: Int)

}