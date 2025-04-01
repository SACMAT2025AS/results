//package benchmarks
//
//import benchmarks.TestUtilities.Companion.RandomNamesState
//import benchmarks.TestUtilities.Companion.addAndInitUser
//import benchmarks.TestUtilities.Companion.assertSuccessOrThrow
//import benchmarks.TestUtilities.Companion.coreCACRBACMQTTNoACNoTLS
//import cryptoac.generateRandomString
//import cryptoac.inputStream
//import cryptoac.model.tuple.Operation
//import cryptoac.model.tuple.Enforcement.COMBINED
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
//open class CoreCACRBACMQTTBenchmarkInitAdmin {
//
//    @TearDown(Level.Iteration)
//    fun tearDownTrial() {
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Benchmark
//    fun initAdmin() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 1000, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkAddUser {
//
//    private var usernames = RandomNamesState()
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACNoTLS.dm)
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Benchmark
//    fun addUser() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addUser(usernames.getNextName()).code)
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 1000, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkAddRole {
//
//    private var roleNames  = RandomNamesState()
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACNoTLS.dm)
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Benchmark
//    fun addRole() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(roleNames.getNextName()))
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 1000, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkAddResource {
//
//    private var resourceNames = RandomNamesState()
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACNoTLS.dm)
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Benchmark
//    fun addResource() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addResource(
//            resourceNames.getNextName().replace("+", ""),
//            generateRandomString(1024).inputStream(),
//            COMBINED
//        ))
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 1000, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkDeleteResource {
//
//    private var resourceNamesAlreadyPresent = RandomNamesState()
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        for (i in 0 until (100 + 1000)) {
//            assertSuccessOrThrow(
//                coreCACRBACMQTTNoACNoTLS.addResource(
//                    resourceNamesAlreadyPresent.getNextName().replace("+", ""),
//                    "none".inputStream(),
//                    COMBINED
//                )
//            )
//        }
//        resourceNamesAlreadyPresent.reset()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACNoTLS.dm)
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Benchmark
//    fun deleteResource() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.deleteResource(
//            resourceNamesAlreadyPresent.getNextName().replace("+", ""),
//        ))
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 1000, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkAssignUserToRole {
//
//    var usernamesAlreadyPresent = RandomNamesState()
//    var roleNamesAlreadyPresent  = RandomNamesState()
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        for (i in 0 until (100 + 1000)) {
//            assertSuccessOrThrow(addAndInitUser(coreCACRBACMQTTNoACNoTLS, usernamesAlreadyPresent.getNextName()))
//            assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(roleNamesAlreadyPresent.getNextName()))
//        }
//        usernamesAlreadyPresent.reset()
//        roleNamesAlreadyPresent.reset()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACNoTLS.dm)
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Benchmark
//    fun assignUserToRole() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignUserToRole(
//            usernamesAlreadyPresent.getNextName(),
//            roleNamesAlreadyPresent.getNextName(),
//        ))
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 1000, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkAssignNewPermissionToRole {
//
//    private var roleNamesAlreadyPresent  = RandomNamesState()
//    private var resourceNamesAlreadyPresent = RandomNamesState()
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        for (i in 0 until (100 + 1000)) {
//            assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(roleNamesAlreadyPresent.getNextName()))
//            assertSuccessOrThrow(
//                coreCACRBACMQTTNoACNoTLS.addResource(
//                    resourceNamesAlreadyPresent.getNextName().replace("+", ""),
//                    "none".inputStream(),
//                    COMBINED
//                )
//            )
//        }
//        roleNamesAlreadyPresent.reset()
//        resourceNamesAlreadyPresent.reset()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACNoTLS.dm)
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Benchmark
//    fun assignNewPermissionToRole() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignPermissionToRole(
//            roleNamesAlreadyPresent.getNextName(),
//            resourceNamesAlreadyPresent.getNextName().replace("+", ""),
//            Operation.READ
//        ))
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 1000, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkAssignExistingPermissionToRole {
//
//    private var roleNamesAlreadyPresent  = RandomNamesState()
//    private var resourceNamesAlreadyPresent = RandomNamesState()
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        for (i in 0 until (100 + 1000)) {
//            val currentRoleName = roleNamesAlreadyPresent.getNextName()
//            val currentResourceName = resourceNamesAlreadyPresent.getNextName().replace("+", "")
//            assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(currentRoleName))
//            assertSuccessOrThrow(
//                coreCACRBACMQTTNoACNoTLS.addResource(
//                    currentResourceName.replace("+", ""),
//                    "none".inputStream(),
//                    COMBINED
//                )
//            )
//            assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignPermissionToRole(
//                currentRoleName,
//                currentResourceName,
//                Operation.READ
//            ))
//        }
//        roleNamesAlreadyPresent.reset()
//        resourceNamesAlreadyPresent.reset()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACNoTLS.dm)
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Benchmark
//    fun assignExistingPermissionToRole() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignPermissionToRole(
//            roleNamesAlreadyPresent.getNextName(),
//            resourceNamesAlreadyPresent.getNextName().replace("+", ""),
//            Operation.READWRITE
//        ))
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 1000, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkRevokeUserFromRole {
//
//    private var usernamesAlreadyPresent = RandomNamesState()
//    private var roleNamesAlreadyPresent  = RandomNamesState()
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        for (i in 0 until (100 + 1000)) {
//            val currentUserName = usernamesAlreadyPresent.getNextName()
//            val currentRoleName = roleNamesAlreadyPresent.getNextName()
//            assertSuccessOrThrow(addAndInitUser(coreCACRBACMQTTNoACNoTLS, currentUserName))
//            assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(currentRoleName))
//            assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignUserToRole(
//                currentUserName,
//                currentRoleName,
//            ))
//        }
//        usernamesAlreadyPresent.reset()
//        roleNamesAlreadyPresent.reset()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACNoTLS.dm)
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Benchmark
//    fun revokeUserFromRole() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.revokeUserFromRole(
//            usernamesAlreadyPresent.getNextName(),
//            roleNamesAlreadyPresent.getNextName(),
//        ))
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 1000, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkRevokeOnePermissionFromRole {
//
//    private var roleNamesAlreadyPresent  = RandomNamesState()
//    private var resourceNamesAlreadyPresent = RandomNamesState()
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        for (i in 0 until (100 + 1000)) {
//            val currentRoleName = roleNamesAlreadyPresent.getNextName()
//            val currentResourceName = resourceNamesAlreadyPresent.getNextName().replace("+", "")
//            assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(currentRoleName))
//            assertSuccessOrThrow(
//                coreCACRBACMQTTNoACNoTLS.addResource(
//                    currentResourceName.replace("+", ""),
//                    "none".inputStream(),
//                    COMBINED
//                )
//            )
//            assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignPermissionToRole(
//                currentRoleName,
//                currentResourceName,
//                Operation.READWRITE
//            ))
//        }
//        roleNamesAlreadyPresent.reset()
//        resourceNamesAlreadyPresent.reset()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACNoTLS.dm)
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Benchmark
//    fun revokeOnePermissionFromRole() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.revokePermissionFromRole(
//            roleNamesAlreadyPresent.getNextName(),
//            resourceNamesAlreadyPresent.getNextName().replace("+", ""),
//            Operation.WRITE
//        ))
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 1000, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkRevokeAllPermissionsFromRole {
//
//    private var roleNamesAlreadyPresent  = RandomNamesState()
//    private var resourceNamesAlreadyPresent = RandomNamesState()
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        for (i in 0 until (100 + 1000)) {
//            val currentRoleName = roleNamesAlreadyPresent.getNextName()
//            val currentResourceName = resourceNamesAlreadyPresent.getNextName().replace("+", "")
//            assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(currentRoleName))
//            assertSuccessOrThrow(
//                coreCACRBACMQTTNoACNoTLS.addResource(
//                    currentResourceName.replace("+", ""),
//                    "none".inputStream(),
//                    COMBINED
//                )
//            )
//            assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignPermissionToRole(
//                currentRoleName,
//                currentResourceName,
//                Operation.READWRITE
//            ))
//        }
//        roleNamesAlreadyPresent.reset()
//        resourceNamesAlreadyPresent.reset()
//    }
//
//    @TearDown(Level.Trial)
//    fun tearDownTrial() {
//        coreCACRBACMQTTNoACNoTLS.subscribedTopicsKeysAndMessages.clear()
//        TestUtilities.resetDMServiceRBACMQTT(coreCACRBACMQTTNoACNoTLS.dm)
//        TestUtilities.resetMMServiceRBACRedis()
//    }
//
//    @Benchmark
//    fun revokeAllPermissionsFromRole() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.revokePermissionFromRole(
//            roleNamesAlreadyPresent.getNextName(),
//            resourceNamesAlreadyPresent.getNextName().replace("+", ""),
//            Operation.READWRITE
//        ))
//    }
//}
//
//
//
//// ===== ===== ===== BELOW, BENCHMARK FOR REVOKEP ACTION ===== ===== =====
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 0, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 101, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkRevokeAllPermissionsFromRoleIterateThroughParameters {
//
//    private var roleNamesAlreadyPresent  = RandomNamesState()
//    private var roleName = "roleNameUnderTest"
//    private var resourceName = "resourceNameUnderTest"
//
//    private var step = 5
//    private var iteration = 0
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        println("setUpTrial")
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(roleName))
//        assertSuccessOrThrow(
//            coreCACRBACMQTTNoACNoTLS.addResource(
//                resourceName,
//                "none".inputStream(),
//                COMBINED
//            )
//        )
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignPermissionToRole(
//            roleName,
//            resourceName,
//            Operation.READWRITE
//        ))
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
//        if (iteration != 0) {
//            for (i in 0 until step) {
//                val currentRoleName = roleNamesAlreadyPresent.getNextName()
//                assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(currentRoleName))
//                assertSuccessOrThrow(
//                    coreCACRBACMQTTNoACNoTLS.assignPermissionToRole(
//                        currentRoleName,
//                        resourceName,
//                        Operation.READWRITE
//                    )
//                )
//            }
//        }
//    }
//
//    @TearDown(Level.Iteration)
//    fun tearDownIteration() {
//        println("tearDownTrial: concluded iteration number $iteration")
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignPermissionToRole(
//            roleName,
//            resourceName,
//            Operation.READWRITE
//        ))
//        iteration++
//    }
//
//    @Benchmark
//    fun revokeAllPermissionsFromRole() {
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.revokePermissionFromRole(
//            roleName,
//            resourceName,
//            Operation.READWRITE
//        ))
//    }
//}
//
//
//
//// ===== ===== ===== BELOW, BENCHMARK FOR REVOKEU ACTION ===== ===== =====
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 0, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 101, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkRevokeUserFromRoleIterateThroughParameter1 {
//
//    private var usernamesAlreadyPresent  = RandomNamesState()
//    private var username = "usernameUnderTest"
//    private var roleName = "roleNameUnderTest"
//
//    private var step = 5
//    private var iteration = 0
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        println("setUpTrial")
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        assertSuccessOrThrow(addAndInitUser(coreCACRBACMQTTNoACNoTLS, username))
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(roleName))
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignUserToRole(
//            username,
//            roleName,
//        ))
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
//        if (iteration != 0) {
//            for (i in 0 until step) {
//                val currentUsername = usernamesAlreadyPresent.getNextName()
//                assertSuccessOrThrow(addAndInitUser(coreCACRBACMQTTNoACNoTLS, currentUsername))
//                assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignUserToRole(
//                    currentUsername,
//                    roleName,
//                ))
//            }
//        }
//    }
//
//    @TearDown(Level.Iteration)
//    fun tearDownIteration() {
//        println("tearDownTrial: concluded iteration number $iteration")
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignUserToRole(
//            username,
//            roleName,
//        ))
//        iteration++
//    }
//
//    @Benchmark
//    fun revokeUserFromRole() {
//        assertSuccessOrThrow(
//            coreCACRBACMQTTNoACNoTLS.revokeUserFromRole(
//                username,
//                roleName,
//            )
//        )
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 0, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 101, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkRevokeUserFromRoleIterateThroughParameter2 {
//
//    private var username = "usernameUnderTest"
//    private var roleName = "roleNameUnderTest"
//    private var resourceNamesAlreadyPresent  = RandomNamesState()
//
//    private var step = 5
//    private var iteration = 0
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        println("setUpTrial")
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        assertSuccessOrThrow(addAndInitUser(coreCACRBACMQTTNoACNoTLS, username))
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(roleName))
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignUserToRole(
//            username,
//            roleName,
//        ))
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
//        if (iteration != 0) {
//            for (i in 0 until step) {
//                val currentResourceName = resourceNamesAlreadyPresent.getNextName().replace("+", "")
//                assertSuccessOrThrow(
//                    coreCACRBACMQTTNoACNoTLS.addResource(
//                        currentResourceName,
//                        "none".inputStream(),
//                        COMBINED
//                    )
//                )
//                assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignPermissionToRole(
//                    roleName,
//                    currentResourceName,
//                    Operation.READWRITE
//                ))
//            }
//        }
//    }
//
//    @TearDown(Level.Iteration)
//    fun tearDownIteration() {
//        println("tearDownTrial: concluded iteration number $iteration")
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignUserToRole(
//            username,
//            roleName,
//        ))
//        iteration++
//    }
//
//    @Benchmark
//    fun revokeUserFromRole() {
//        assertSuccessOrThrow(
//            coreCACRBACMQTTNoACNoTLS.revokeUserFromRole(
//                username,
//                roleName,
//            )
//        )
//    }
//}
//
//
//
//@State(Scope.Benchmark)
//@Threads(1)
//@Fork(1)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@Warmup(iterations = 0, time = 1, timeUnit = TimeUnit.SECONDS)
//@Measurement(iterations = 101, time = 1, timeUnit = TimeUnit.SECONDS)
//open class CoreCACRBACMQTTBenchmarkRevokeUserFromRoleIterateThroughParameter3 {
//
//    private var username = "usernameUnderTest"
//    private var roleName = "roleNameUnderTest"
//    private var resourceName = "resourceNameUnderTest"
//    private var roleNamesAlreadyPresent  = RandomNamesState()
//
//    private var step = 5
//    private var iteration = 0
//
//    @Setup(Level.Trial)
//    fun setUpTrial() {
//        println("setUpTrial")
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.configureServices())
//        coreCACRBACMQTTNoACNoTLS.initCore()
//        assertSuccessOrThrow(addAndInitUser(coreCACRBACMQTTNoACNoTLS, username))
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(roleName))
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignUserToRole(
//            username,
//            roleName,
//        ))
//        assertSuccessOrThrow(
//            coreCACRBACMQTTNoACNoTLS.addResource(
//                resourceName,
//                "none".inputStream(),
//                COMBINED
//            )
//        )
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignPermissionToRole(
//            roleName,
//            resourceName,
//            Operation.READWRITE
//        ))
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
//        if (iteration != 0) {
//            for (i in 0 until step) {
//                val currentRoleName = roleNamesAlreadyPresent.getNextName()
//                assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.addRole(currentRoleName))
//                assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignPermissionToRole(
//                    currentRoleName,
//                    resourceName,
//                    Operation.READWRITE
//                ))
//            }
//        }
//    }
//
//    @TearDown(Level.Iteration)
//    fun tearDownIteration() {
//        println("tearDownTrial: concluded iteration number $iteration")
//        assertSuccessOrThrow(coreCACRBACMQTTNoACNoTLS.assignUserToRole(
//            username,
//            roleName,
//        ))
//        iteration++
//    }
//
//    @Benchmark
//    fun revokeUserFromRole() {
//        assertSuccessOrThrow(
//            coreCACRBACMQTTNoACNoTLS.revokeUserFromRole(
//                username,
//                roleName,
//            )
//        )
//    }
//}
