//package cryptoac.mm.redis
//
//import cryptoac.OutcomeCode
//import cryptoac.Parameters
//import cryptoac.Service
//import cryptoac.TestUtilities.Companion.dir
//import cryptoac.TestUtilities.Companion.resetMMServiceRBACRedis
//import cryptoac.mm.MMFactory
//import cryptoac.mm.MMServiceCACRBAC
//import cryptoac.mm.MMServiceCACRBACTest
//import cryptoac.tuple.User
//import cryptoac.runCommand
//import org.junit.jupiter.api.*
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//internal class MMServiceRBACPredicatesRedisCACRBACTest : MMServiceCACRBACTest() {
//
//    override val mm = MMFactory.getMM(
//        Parameters.mmServiceRBACRedisParameters
//    ) as MMServiceCACRBAC
//
//    override val service: Service = mm
//
//    private var processDocker: Process? = null
//
//
//
//    @BeforeAll
//    override fun setUpAll() {
//        "./cleanAllAndBuild.sh".runCommand(dir, hashSetOf("built_all_end_of_script"))
//        processDocker = "./startCryptoAC_ALL.sh \"cryptoac_redis\"".runCommand(
//            workingDir = dir,
//            endStrings = hashSetOf("Server initialized")
//        )
//    }
//
//    @BeforeEach
//    override fun setUp() {
//        super.setUp()
//        assert(mm.addRole(Parameters.adminRole) == OutcomeCode.CODE_000_SUCCESS)
//        assert(mm.addUsersRoles(hashSetOf(Parameters.adminUserRole)) == OutcomeCode.CODE_000_SUCCESS)
//    }
//
//    @AfterEach
//    override fun tearDown() {
//        super.tearDown()
//        resetMMServiceRBACRedis()
//    }
//
//    @AfterAll
//    override fun tearDownAll() {
//        processDocker!!.destroy()
//        Runtime.getRuntime().exec("kill -SIGINT ${processDocker!!.pid()}")
//        "./cleanAll.sh".runCommand(
//            workingDir = dir,
//            endStrings = hashSetOf("clean_all_end_of_script")
//        )
//    }
//
//
//
//    override fun addUser(user: User): Service {
//        val addUserResult = mm.addUser(user)
//        assert(addUserResult.code == OutcomeCode.CODE_000_SUCCESS)
//        val userMM = MMServiceCACRBACRedis(addUserResult.serviceParameters as MMServiceRedisParameters)
//        return userMM
//    }
//}
