package util.service

import android.support.test.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import remoter.RemoterProxy
import remoter.builder.ServiceConnector
import util.remoter.service.*
import java.io.IOException
import java.util.concurrent.CountDownLatch

private const val SERVICE_INTENT = "util.remoter.remoterclient.SampleKotlinService"

class TestKotlinSuspendService {

    private lateinit var service: ISampleKotlinService

    @Before
    fun setup() {
        service = ISampleKotlinService_Proxy(InstrumentationRegistry.getContext(), SERVICE_INTENT)
    }

    @After
    fun tearDown() {
        (service as RemoterProxy).destroyProxy()
        ServiceConnector.of(InstrumentationRegistry.getContext(), SERVICE_INTENT).disconnect()
    }

    @Test
    fun testBoolean() {
        runBlocking {
            var input: BooleanArray? = null
            var output: BooleanArray? = null
            var inputOutput: BooleanArray? = null
            var result = service.testBoolean3(true, input, output, inputOutput)
            Assert.assertNull(result)


            input = booleanArrayOf(true, false)
            output = booleanArrayOf(false)
            inputOutput = booleanArrayOf(false)
            result = service.testBoolean3(true, input, output, inputOutput)
            Assert.assertArrayEquals(input, result)
            Assert.assertEquals(true, inputOutput[0])

            input = booleanArrayOf(true, false)
            output = booleanArrayOf(false)
            inputOutput = booleanArrayOf(false)

            val result2:Boolean = service.testBoolean1(true, input, output, inputOutput)
            Assert.assertTrue(result2)
            Assert.assertEquals(true, output[0])
            Assert.assertEquals(true, inputOutput[0])


        }
    }


    @Test
    fun testByte() {
        runBlocking {
            var input: ByteArray? = null
            var output: ByteArray? = null
            var inputOutput: ByteArray? = null
            var result = service.testByte3(1, input, output, inputOutput)
            Assert.assertNull(result)


            input = byteArrayOf(1, 2)
            output = byteArrayOf(5)
            inputOutput = byteArrayOf(6)
            result = service.testByte3(2, input, output, inputOutput)
            Assert.assertArrayEquals(input, result)
            Assert.assertEquals(2.toByte(), inputOutput[0])

            input = byteArrayOf(1, 2)
            output = byteArrayOf(5)
            inputOutput = byteArrayOf(6)
            val result2:Byte = service.testByte1(2, input, output, inputOutput)
            Assert.assertEquals(2.toByte(), result2)
            Assert.assertEquals(2.toByte(), output[0])
            Assert.assertEquals(2.toByte(), inputOutput[0])

        }
    }

    @Test
    fun testInt() {
        runBlocking {
            var input: IntArray? = null
            var output: IntArray? = null
            var inputOutput: IntArray? = null
            var result = service.testInt3(1, input, output, inputOutput)
            Assert.assertNull(result)


            input = intArrayOf(1, 2)
            output = intArrayOf(5)
            inputOutput = intArrayOf(6)
            result = service.testInt3(2, input, output, inputOutput)
            Assert.assertArrayEquals(input, result)
            Assert.assertEquals(2, inputOutput[0])


            input = intArrayOf(1, 2)
            output = intArrayOf(5)
            inputOutput = intArrayOf(6)
            val result2 = service.testInt1(2, input, output, inputOutput)
            Assert.assertEquals(2, result2)
            Assert.assertEquals(2, inputOutput[0])
            Assert.assertEquals(2, output[0])
        }
    }

    @Test
    fun testLong() {
        runBlocking {
            var input: LongArray? = null
            var output: LongArray? = null
            var inputOutput: LongArray? = null
            var result = service.testLong3(1, input, output, inputOutput)
            Assert.assertNull(result)


            input = longArrayOf(1, 2)
            output = longArrayOf(5)
            inputOutput = longArrayOf(6)
            result = service.testLong3(2, input, output, inputOutput)
            Assert.assertArrayEquals(input, result)
            Assert.assertEquals(2.toLong(), inputOutput[0])


            input = longArrayOf(1, 2)
            output = longArrayOf(5)
            inputOutput = longArrayOf(6)
            val result2 = service.testLong1(2, input, output, inputOutput)
            Assert.assertEquals(2.toLong(), result2)
            Assert.assertEquals(2.toLong(), inputOutput[0])
            Assert.assertEquals(2.toLong(), output[0])
        }
    }


    @Test
    fun testFloat() {
        runBlocking {
            var input: FloatArray? = null
            var output: FloatArray? = null
            var inputOutput: FloatArray? = null
            var result = service.testFloat3(1f, input, output, inputOutput)
            Assert.assertNull(result)


            input = floatArrayOf(1f, 2f)
            output = floatArrayOf(5f)
            inputOutput = floatArrayOf(6f)
            result = service.testFloat3(2f, input, output, inputOutput)
            //Assert.assertArrayEquals(input, result)
            Assert.assertEquals(2f, inputOutput[0])

            input = floatArrayOf(1f, 2f)
            output = floatArrayOf(5f)
            inputOutput = floatArrayOf(6f)
            val result2 = service.testFloat1(2f, input, output, inputOutput)

            Assert.assertEquals(2f, result2)
            Assert.assertEquals(2f, inputOutput[0])
            Assert.assertEquals(2f, output[0])

        }
    }


    @Test
    fun testCharSeq() {
        runBlocking {
            var input: CharSequence? = null
            var result = service.testCharSequence2(input)
            Assert.assertEquals(input, result)


            input = "Hello"
            result = service.testCharSequence2(input)
            Assert.assertEquals(input, result)

        }
    }


    @Test
    fun testString() {
        runBlocking {
            var input: Array<String?>? = null
            var output: Array<String?>? = null
            var inputOutput: Array<String?>? = null

            var result = service.testString2("Hello", input, output, inputOutput)
            Assert.assertNull(result)


            input = arrayOf("1", "2")
            output = arrayOf("3", "4")
            inputOutput = arrayOf("5")

            result = service.testString2("Hello", input, output, inputOutput)
            Assert.assertArrayEquals(input, result)
            Assert.assertEquals("Hello", inputOutput[0])

            val input2 = arrayOf("1", "2")
            val output2 = arrayOf("3", "4")
            val inputOutput2 = arrayOf("5")

            val result2 = service.testString1("Hello", input2, output2, inputOutput2)
            Assert.assertEquals("Hello", result2)
            Assert.assertEquals("Hello", inputOutput2[0])
            Assert.assertEquals("Hello", output2[0])

        }
    }


    @Test
    fun testList() {
        runBlocking {
            var input: MutableList<String?>? = null
            var output: MutableList<String?>? = null
            var inputOutput: MutableList<String?>? = null

            var result = service.testList2(input, output, inputOutput)
            Assert.assertNull(result)


            input = mutableListOf("1", "2")
            output = mutableListOf("3", "4")
            inputOutput = mutableListOf("5")

            result = service.testList2(input, output, inputOutput)
            Assert.assertEquals(input, result)
            Assert.assertEquals(input, inputOutput)


            val input2 = mutableListOf("1", "2")
            val output2 = mutableListOf("3", "4")
            val inputOutput2 = mutableListOf("5")

            val result2 = service.testList1(input2, output2, inputOutput2)

            Assert.assertEquals(input2, result2)
            Assert.assertEquals(output2[0], input2[0])
            Assert.assertEquals(inputOutput2[0], input2[0])

        }
    }


    @Test
    fun testMap() {
        runBlocking {
            var input: MutableMap<String?, Int?>? = null
            var output: MutableMap<String?, Int?>? = null
            var inputOutput: MutableMap<String?, Int?>? = null

            var result = service.testMap2(input, output, inputOutput)
            Assert.assertNull(result)


            input = mutableMapOf("1" to 1, "2" to 2)
            output = mutableMapOf("3" to 3, "4" to 4)
            inputOutput = mutableMapOf("5" to 5)

            result = service.testMap2(input, output, inputOutput)
            Assert.assertEquals(input, result)
            Assert.assertEquals(input, inputOutput)


            val input2 = mutableMapOf("1" to 1, "2" to 2)
            val output2 = mutableMapOf("3" to 3, "4" to 4)
            val inputOutput2 = mutableMapOf("5" to 5)

            val result2 = service.testMap1(input2, output2, inputOutput2)
            Assert.assertEquals(input2, result2)
            Assert.assertEquals(input2, output2)
            Assert.assertEquals(input2, inputOutput)
        }
    }


    @Test
    fun testParcellable() {
        runBlocking {
            var input: FooParcelable<String?>? = null
            var output: FooParcelable<String?>? = null
            var inputOutput: FooParcelable<String?>? = null

            var result = service.testParcelable2(input, output, inputOutput)
            Assert.assertNull(result)


            input = FooParcelable("1", 1)
            output = FooParcelable("2", 2)
            inputOutput = FooParcelable("3", 3)

            result = service.testParcelable2(input, output, inputOutput)
            Assert.assertEquals(input.intValue, result?.intValue)
            Assert.assertEquals(input.stringValue, result?.stringValue)
            Assert.assertEquals(input.intValue, inputOutput.intValue)

            val input2 = FooParcelable<String>("1", 1)
            val output2 = FooParcelable<String>("2", 2)
            val inputOutput2 = FooParcelable<String>("3", 3)

            val result2 = service.testParcelable1(input2, output2, inputOutput2)
            Assert.assertEquals(input2.stringValue, result2.stringValue)
            Assert.assertEquals(input2.stringValue, output2.stringValue)
            Assert.assertEquals(input2.stringValue, inputOutput2.stringValue)


        }
    }


    @Test
    fun testParcellable2() {
        runBlocking {
            var input: SimpleParcelable? = null
            var output: SimpleParcelable? = null
            var inputOutput: SimpleParcelable? = null

            var result = service.testSimpleParcelable2(input, output, inputOutput)
            Assert.assertNull(result)


            input = SimpleParcelable("100", 100)
            output = SimpleParcelable("200", 200)
            inputOutput = SimpleParcelable("300", 300)

            result = service.testSimpleParcelable2(input, output, inputOutput)
            Assert.assertEquals(input.intValue, result?.intValue)
            Assert.assertEquals(input.stringValue, result?.stringValue)
            Assert.assertEquals(input.intValue, inputOutput.intValue)


            input = SimpleParcelable("100", 100)
            output = SimpleParcelable("200", 200)
            inputOutput = SimpleParcelable("300", 300)

            result = service.testSimpleParcelable1(input, output, inputOutput)
            Assert.assertEquals(input.stringValue, result.stringValue)
            Assert.assertEquals(input.stringValue, output.stringValue)
            Assert.assertEquals(input.stringValue, inputOutput.stringValue)

        }
    }

    @Test
    fun testParcellableArray() {
        runBlocking {
            var input: Array<FooParcelable<String?>?>? = null
            var output: Array<FooParcelable<String?>?>? = null
            var inputOutput: Array<FooParcelable<String?>?>? = null

            var result = service.testParcelableArray2(input, output, inputOutput)
            Assert.assertNull(result)


            input = arrayOf(FooParcelable("100", 100))
            output = arrayOf(FooParcelable("200", 200))
            inputOutput = arrayOf(FooParcelable("300", 300))

            result = service.testParcelableArray2(input, output, inputOutput)

            Assert.assertEquals(input[0]?.intValue, result!![0]?.intValue)
            Assert.assertEquals(input[0]?.stringValue, result!![0]?.stringValue)
            Assert.assertEquals(input[0]?.intValue, inputOutput!![0]?.intValue)
            Assert.assertEquals(input[0]?.stringValue, inputOutput!![0]?.stringValue)


            val input2 = arrayOf(FooParcelable<String>("100", 100))
            val output2 = arrayOf(FooParcelable<String>("200", 200))
            val inputOutput2 = arrayOf(FooParcelable<String>("300", 300))

            val result2 = service.testParcelableArray1(input2, output2, inputOutput2)

            Assert.assertEquals(input2[0].intValue, result2[0].intValue)
            Assert.assertEquals(input2[0].intValue, output2[0].intValue)
            Assert.assertEquals(input2[0].intValue, inputOutput2[0].intValue)

        }
    }


    @Test
    fun testParcellableArray2() {
        runBlocking {
            var input: Array<SimpleParcelable?>? = null
            var output: Array<SimpleParcelable?>? = null
            var inputOutput: Array<SimpleParcelable?>? = null

            var result = service.testSimpleParcelableArray2(input, output, inputOutput)
            Assert.assertNull(result)


            input = arrayOf(SimpleParcelable("100", 100))
            output = arrayOf(SimpleParcelable("200", 200))
            inputOutput = arrayOf(SimpleParcelable("300", 300))

            result = service.testSimpleParcelableArray2(input, output, inputOutput)

            Assert.assertEquals(input[0]?.intValue, result!![0]?.intValue)
            Assert.assertEquals(input[0]?.stringValue, result!![0]?.stringValue)
            Assert.assertEquals(input[0]?.intValue, inputOutput!![0]?.intValue)
            Assert.assertEquals(input[0]?.stringValue, inputOutput!![0]?.stringValue)


            val input2 = arrayOf(SimpleParcelable("100", 100))
            val output2 = arrayOf(SimpleParcelable("200", 200))
            val inputOutput2 = arrayOf(SimpleParcelable("300", 300))

            val result2 = service.testSimpleParcelableArray1(input2, output2, inputOutput2)

            Assert.assertEquals(input2[0].intValue, result2[0].intValue)
            Assert.assertEquals(input2[0].intValue, output2[0].intValue)
            Assert.assertEquals(input2[0].intValue, inputOutput2[0].intValue)

        }
    }


    @Test
    fun testParcel() {
        runBlocking {
            var input: CustomData? = null
            var output: CustomData? = null
            var inputOutput: CustomData? = null

            var result = service.testParcel2(input, output, inputOutput)
            Assert.assertNull(result)


            input = CustomData("100", 100)
            output = CustomData("200", 200)
            inputOutput = CustomData("300", 300)

            result = service.testParcel2(input, output, inputOutput)
            Assert.assertEquals(input.intData, result?.intData)
            Assert.assertEquals(input.data, result?.data)

            result = service.testParcel1(input, output, inputOutput)
            Assert.assertEquals(input.intData, result?.intData)
            Assert.assertEquals(input.data, result?.data)


        }
    }


    @Test
    fun testParcelArray() {
        runBlocking {
            var input: Array<CustomData?>? = null
            var output: Array<CustomData?>? = null
            var inputOutput: Array<CustomData?>? = null

            var result = service.testParcelArray2(input, output, inputOutput)
            Assert.assertNull(result)


            input = arrayOf(CustomData("100", 100))
            output = arrayOf(CustomData("200", 200))
            inputOutput = arrayOf(CustomData("300", 300))

            result = service.testParcelArray2(input, output, inputOutput)

            Assert.assertEquals(input[0]?.intData, result!![0]?.intData)
            Assert.assertEquals(input[0]?.data, result!![0]?.data)
            Assert.assertEquals(input[0]?.intData, inputOutput!![0]?.intData)
            Assert.assertEquals(input[0]?.data, inputOutput!![0]?.data)

            val input2 = arrayOf(CustomData("100", 100))
            val output2 = arrayOf(CustomData("200", 200))
            val inputOutput2 = arrayOf(CustomData("300", 300))

            val result2 = service.testParcelArray1(input2, output2, inputOutput2)

            Assert.assertEquals(input2[0].intData, result2[0].intData)
            Assert.assertEquals(input2[0].intData, output2[0].intData)
            Assert.assertEquals(input2[0].intData, inputOutput2[0].intData)


        }
    }


    @Test
    fun testParcelList() {
        runBlocking {
            var input: MutableList<CustomData?>? = null
            var output: MutableList<CustomData?>? = null
            var inputOutput: MutableList<CustomData?>? = null

            var result = service.testParcelList2(input, output, inputOutput)
            Assert.assertNull(result)


            input = mutableListOf(CustomData("100", 100))
            output = mutableListOf(CustomData("200", 200))
            inputOutput = mutableListOf(CustomData("300", 300))

            result = service.testParcelList2(input, output, inputOutput)
            Assert.assertEquals(input[0]?.data, result!![0]?.data)
            Assert.assertEquals(input[0]?.intData, result!![0]?.intData)
            Assert.assertEquals(input[0]?.data, inputOutput!![0]?.data)
            Assert.assertEquals(input[0]?.intData, inputOutput!![0]?.intData)


            val input2 = mutableListOf(CustomData("100", 100))
            val output2 = mutableListOf(CustomData("200", 200))
            val inputOutput2 = mutableListOf(CustomData("300", 300))

            val result2 = service.testParcelList1(input2, output2, inputOutput2)
            Assert.assertEquals(input2[0].data, result2[0].data)
            Assert.assertEquals(input2[0].data, output2[0].data)
            Assert.assertEquals(input2[0].data, inputOutput2[0].data)
        }
    }

    @Test
    fun testRemoter() {
        runBlocking {
            var result = service.testEcho(null, null)
            Assert.assertNull(result)

            result = service.testEcho("Hello", null)
            Assert.assertEquals("Hello", result)

            val lock = CountDownLatch(1)
            result = service.testEcho("Hello", object : ISampleKotlinServiceListener{
                override suspend fun onEcho(echo: String?) {
                    Assert.assertEquals("Hello", result)
                    lock.countDown()
                }

            })
            Assert.assertEquals("Hello", result)
            lock.await()
        }
    }


    @Test(expected = IOException::class)
    fun testException() {
        runBlocking {
            service.testException()
       }
    }


    @Test(expected = RuntimeException::class)
    fun testRuntimeException() {
        runBlocking {
            service.testRuntimeException()
        }
    }

    @Test
    fun testVarArg() {
        Assert.assertEquals(0, service.testVarArg())
        Assert.assertEquals(1, service.testVarArg("a"))
        Assert.assertEquals(1, service.testVarArg(null))
        Assert.assertEquals(2, service.testVarArg(null, "a"))
    }
}