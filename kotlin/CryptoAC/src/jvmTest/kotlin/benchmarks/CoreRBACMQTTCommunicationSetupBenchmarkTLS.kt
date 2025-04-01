//package benchmarks
//
//import benchmarks.TestUtilities.Companion.assertSuccessOrThrow
//import benchmarks.TestUtilities.Companion.coreCACRBACMQTTNoACWithTLS
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
//open class CoreCACRBACMQTTCommunicationSetupBenchmarkTLS {
//
//    private var iteration = 0
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        println("setUpTrial")
//        assertSuccessOrThrow(coreCACRBACMQTTNoACWithTLS.configureServices())
//        coreCACRBACMQTTNoACWithTLS.initCore()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        println("tearDownTrial")
//        coreCACRBACMQTTNoACWithTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACWithTLS.dm)
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
//        coreCACRBACMQTTNoACWithTLS.dm.deinit()
//        iteration++
//    }
//
//    @Benchmark
//    fun connectToTheBroker() {
//        coreCACRBACMQTTNoACWithTLS.dm.client.connectSync()
//    }
//}
