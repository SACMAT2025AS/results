//package benchmarks
//
//import benchmarks.TestUtilities.Companion.assertSuccessOrThrow
//import benchmarks.TestUtilities.Companion.assertTrueOrThrow
//import benchmarks.TestUtilities.Companion.coreCACRBACMQTTNoACNoTLS
//import cryptoac.inputStream
//import cryptoac.model.tuple.Enforcement.COMBINED
//import cryptoac.model.tuple.Resource
//import org.openjdk.jmh.annotations.*
//import java.util.*
//import java.util.concurrent.TimeUnit
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 1000, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTCommunicationSetupBenchmarkCryptoAC {
//
//    private val resourceName = "resourceTestUnderTest"
//    private var resource: Resource? = null
//    private var iteration = 0
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        println("setUpTrial")
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        assertSuccessOrThrow(
//            coreCACRBACMQTTNoACNoTLS.addResource(
//                resourceName,
//                "none".inputStream(),
//                COMBINED
//            )
//        )
//        val resourceCode = coreCACRBACMQTTNoACNoTLS.getResources()
//        assertSuccessOrThrow(resourceCode.code)
//        resource = resourceCode.resources!!.first()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        println("tearDownTrial")
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACNoTLS.dm)
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Setup(Level.Iteration)
//    fun setUpIteration() {
//        println("setUpIteration: started iteration number $iteration")
//    }
//
//    @TearDown(Level.Iteration)
//    fun tearDownIteration() {
//        println("tearDownTrial: concluded iteration number $iteration")
//        coreCACRBACMQTTNoACNoTLS.deinitCore()
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        iteration++
//    }
//
//    @Benchmark
//    fun connectToTheBroker() {
//        assertTrueOrThrow(coreCACRBACMQTTNoACNoTLS.dm.client.connectSync())
//        coreCACRBACMQTTNoACNoTLS.mm.lock()
//        coreCACRBACMQTTNoACNoTLS.getSymmetricKey(
//            resource = resource!!,
//            encryptingKey =  true
//        )
//        coreCACRBACMQTTNoACNoTLS.mm.unlock()
//    }
//}
