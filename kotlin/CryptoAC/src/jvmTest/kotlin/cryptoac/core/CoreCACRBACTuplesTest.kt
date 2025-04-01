package cryptoac.core

import cryptoac.OutcomeCode
import cryptoac.Parameters
import cryptoac.TestUtilities
import cryptoac.TestUtilities.Companion.dir
import cryptoac.dm.local.DMLocal
import cryptoac.dm.local.DMServiceLocalParameters
import cryptoac.mm.local.MMServiceCACRBACLocal
import cryptoac.mm.local.MMServiceRBACLocalParameters
// import cryptoac.core.files.CoreCACRBACryptoAC
import cryptoac.runCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoreCACRBACTuplesTest : CoreCACRBACTest() {

    // TODO automatically test both with and without OPA
    val parameters = Parameters.adminCoreCACRBACCryptoACParametersNoOPA // Parameters.adminCoreCACRBACCryptoACParametersWithOPA
    override val core: CoreCACRBACTuples =
        CoreFactory.getCore(parameters) as CoreCACRBACTuples
    private var processDocker: Process? = null

    // TODO replicate relevant tests for both combined and traditional enforcement (I know, it's a lot of work...)

    @BeforeAll
    override fun setUpAll() {
//        "./cleanAllAndBuild.sh".runCommand(dir, hashSetOf("built_all_end_of_script"))
//        processDocker = "./startCryptoAC_ALL.sh \"cryptoac_mysql cryptoac_proxy cryptoac_rm cryptoac_dm cryptoac_opa\"".runCommand(
//            workingDir = dir,
//            endStrings = hashSetOf(
//                "port: 3306  MySQL Community Server - GPL",
//                "Routes were registered, CryptoAC is up",
//                "OPA is running"
//            )
//        )
        core.initCore()
    }

    @BeforeEach
    override fun setUp() {
        assert(core.configureServices() == OutcomeCode.CODE_000_SUCCESS)
    }

    @AfterEach
    override fun tearDown() {
        core.dm = DMLocal(DMServiceLocalParameters())
        core.mm.init()
//        TestUtilities.resetDMServiceRBACCryptoAC()
        // TestUtilities.resetMMServiceRBACMySQL()
        if (parameters.acServiceParameters != null) {
            TestUtilities.resetACServiceRBACOPA()
        }
    }

    @AfterAll
    override fun tearDownAll() {
        // processDocker!!.destroy()
//        Runtime.getRuntime().exec("kill -SIGINT ${processDocker!!.pid()}")
//        "./cleanAll.sh".runCommand(
//            workingDir = dir,
//            endStrings = hashSetOf("clean_all_end_of_script")
//        )
    }

    /** Before executing each block, commit the MM status */
    override fun myRun(core: Core?, block: () -> Unit) {
        val mmServiceRBAC = core?.let { (core as CoreCACRBACTuples).mm } ?: this.core.mm
        TestUtilities.assertUnLockAndLock(mmServiceRBAC)
        try {
            block.invoke()
        } catch (e: AssertionError) {
            e.printStackTrace()
        }
        TestUtilities.assertUnLockAndLock(mmServiceRBAC)
    }

    /** Before executing each blocking block, commit the MM status */
    override fun myRunBlocking(core: Core?, block: suspend CoroutineScope.() -> Unit) {
        val mmServiceRBAC = core?.let { (core as CoreCACRBACTuples).mm } ?: this.core.mm
        TestUtilities.assertUnLockAndLock(mmServiceRBAC)
        try {
            runBlocking {
                block.invoke(this)
            }
        } catch (e: AssertionError) {
            e.printStackTrace()
        }
        TestUtilities.assertUnLockAndLock(mmServiceRBAC)
    }

}