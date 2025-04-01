//package cryptoac.mm.mysql
//
//import cryptoac.OutcomeCode
//import cryptoac.Parameters
//import cryptoac.Service
//import cryptoac.TestUtilities.Companion.dir
//import cryptoac.TestUtilities.Companion.resetMMServiceABACMySQL
//import cryptoac.mm.*
//import cryptoac.mm.MMServiceCACABACTest
//import cryptoac.tuple.User
//import cryptoac.runCommand
//import org.junit.jupiter.api.*
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//internal class MMServiceCACABACMySQLTest : MMServiceCACABACTest() {
//
//    override val mm = MMFactory.getMM(
//        Parameters.mmServiceABACMySQLParameters
//    ) as MMServiceCACABAC
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
//        processDocker = "./startCryptoAC_ALL.sh \"cryptoac_mysql\"".runCommand(
//            workingDir = dir,
//            endStrings = hashSetOf("port: 3306  MySQL Community Server - GPL")
//        )
//    }
//
//    @BeforeEach
//    override fun setUp() {
//        super.setUp()
//        assert(mm.addAttribute(Parameters.adminAttribute, false) == OutcomeCode.CODE_000_SUCCESS)
//        assert(mm.addUsersAttributes((hashSetOf(Parameters.adminUserAttribute))) == OutcomeCode.CODE_000_SUCCESS)
//    }
//
//    @AfterEach
//    override fun tearDown() {
//        super.tearDown()
//        resetMMServiceABACMySQL()
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
//        val userMM = MMServiceCACRBACMySQL(addUserResult.serviceParameters as MMServiceMySQLParameters)
//        return userMM
//    }
//}