package cryptoac.core

import cryptoac.Constants
import cryptoac.OutcomeCode
import cryptoac.TestUtilities
import cryptoac.inputStream
import cryptoac.prolog.NewParameters
import cryptoac.tuple.Enforcement
import cryptoac.tuple.Operation
import cryptoac.tuple.TupleStatus
import kotlinx.coroutines.CoroutineScope
import org.eclipse.jetty.http.HttpStatus.Code
import org.junit.jupiter.api.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import kotlin.system.exitProcess
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class CoreTest {

    abstract val core: Core

    // TODO find a way to test core objects with all supported services
    //  e.g., test CoreMQTT with MySQL and CoreCloud with Redis. Remember
    //  that JUnit5 does not have easy support for parametrized test classes

    @BeforeAll
    abstract fun setUpAll()

    @BeforeEach
    abstract fun setUp()

    @AfterEach
    abstract fun tearDown()

    @AfterAll
    abstract fun tearDownAll()



    @Test
    fun `configure services once works`() {
        /** Services are already configured in the setUp function */
    }

//    @Test
//    fun `configure services twice fails`() {
//        assert(core.configureServices() == OutcomeCode.CODE_077_SERVICE_ALREADY_CONFIGURED)
//    }

    @Test
    fun `init user of existing user works`() {
        val aliceCore = TestUtilities.addUser(core, "alice")
        assert(aliceCore.initUser() == OutcomeCode.CODE_000_SUCCESS)
    }

    @Test
    fun `init user twice fails`() {
        val bobCore = TestUtilities.addUser(core, "bob")
        assert(bobCore.initUser() == OutcomeCode.CODE_000_SUCCESS)
        assert(bobCore.initUser() == OutcomeCode.CODE_052_USER_ALREADY_INITIALIZED)
    }

    @Test
    fun `add user of non-existing user works`() {
        /** add user of non-existing user */
        run {
            val alice = "alice"
            assert(core.addUser(alice).code == OutcomeCode.CODE_000_SUCCESS)

            val users = core.getUsers(
                statuses = arrayOf(TupleStatus.INCOMPLETE, TupleStatus.OPERATIONAL)
            ).users!!.filter { it.name == alice }
            assert(users.size == 1)
            assert(users.filter { it.name == alice }.size == 1)
            val aliceUser = users.first { it.name == alice }
            assert(!aliceUser.isAdmin)
            assert(aliceUser.status == TupleStatus.INCOMPLETE)
        }
    }

    @Test
    fun `add user of blank, admin, existing (incomplete or operational) or deleted user fails`() {
        core.configureServices()
        /** add user with blank username */
        run {
            assert(core.addUser("").code == OutcomeCode.CODE_020_INVALID_PARAMETER)
            assert(core.addUser("    ").code == OutcomeCode.CODE_020_INVALID_PARAMETER)
        }

        /** add user with admin username */
        run {
            assert(core.addUser(Constants.ADMIN).code == OutcomeCode.CODE_001_USER_ALREADY_EXISTS)
        }

        /** add user of existing (incomplete) user */
        run {
            assert(core.addUser("alice").code == OutcomeCode.CODE_000_SUCCESS)
            assert(core.addUser("alice").code == OutcomeCode.CODE_001_USER_ALREADY_EXISTS)
        }

        /** add user of existing (operational) user */
        run {
            addAndInitUser(core, "bob")
            assert(core.addUser("bob").code == OutcomeCode.CODE_001_USER_ALREADY_EXISTS)
        }

        /** add user of deleted user */
        run {
            addAndInitUser(core, "carl")
            assert(core.deleteUser("carl") == OutcomeCode.CODE_000_SUCCESS)
            assert(core.addUser("carl").code == OutcomeCode.CODE_013_USER_WAS_DELETED)
        }
    }

    @Test
    fun `delete user of existing (incomplete or operational) user works`() {
        /** delete user of existing (incomplete) user */
        run {
            assert(core.addUser("alice").code == OutcomeCode.CODE_000_SUCCESS)
            assert(core.deleteUser("alice") == OutcomeCode.CODE_000_SUCCESS)
        }

        /** delete user of existing (operational) user */
        run {
            addAndInitUser(core, "bob")
            assert(core.deleteUser("bob") == OutcomeCode.CODE_000_SUCCESS)
        }
    }

    @Test
    fun prolog_test() {
        val cac: CoreCACRBACTuples =
            CoreFactory.getCore(NewParameters.adminCoreCACRBACCryptoACParametersNoOPA) as CoreCACRBACTuples

        val file = File("prolog/instructions.txt")
        val input = BufferedReader(FileReader(file))

        val outputFile = File("prolog/final.txt")
        val output = PrintWriter(FileWriter(outputFile))

        val users = HashMap<String, CoreCACRBACTuples>()
        users["admin"] = cac

        while(input.ready()) {
            val line = input.readLine()
            output.println(line)

            if(!line.startsWith("[CryptoAC]")) {
                continue
            }

            val toks = line.split(" ")

            println("Executing $line...")

            val start = System.currentTimeMillis()
            val code = when(toks[1]) {
                // init system
                "initAdmC" -> cac.configureServices()

                // users
                "addUserC" -> {
//                    val params = cac.addUser(toks[2])
//                    if(params.code != OutcomeCode.CODE_000_SUCCESS) {
//                        params.code
//                    }
                    val core = addAndInitUser(cac, toks[2]) as CoreCACRBACTuples
                    users[toks[2]] = core
                    OutcomeCode.CODE_000_SUCCESS
                }
                "initUserC" -> OutcomeCode.CODE_000_SUCCESS // users[toks[2]]!!.initUser()
                "deleteUserC" -> cac.deleteUser(toks[2])

                // roles
                "addRoleC" -> cac.addRole(toks[2])
                "deleteRoleC" -> cac.deleteRole(toks[2])

                // resource
                "addResourceC" -> cac.addResource(toks[2], "default".inputStream(), Enforcement.COMBINED)
                "deleteResourceC" -> {
                    var code = cac.deleteResource(toks[2])
                    if(code == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND || code == OutcomeCode.CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND){
                        code = OutcomeCode.CODE_000_SUCCESS
                    }
                    code
                }
                "prepareReencryptionC" -> cac.rotateResourceKey(toks[2])
                "reencryptResourceC" -> cac.eagerReencryption(toks[2])

                // user-roles
                "assignUserToRoleC" -> cac.assignUserToRole(toks[2], toks[3])
                "revokeUserFromRoleC" -> cac.revokeUserFromRole(toks[2], toks[3])
                "rotateRoleKeyUserRoleC" -> cac.rotateRoleKeyUserRoles(toks[2])
                "rotateRoleKeyPermissionsC" -> cac.rotateRoleKeyPermissions(toks[2])

                // role-permissions
                "assignPermissionToRoleC" -> cac.assignPermissionToRole(toks[2], toks[4], Operation.fromString(toks[3])!!)
                "revokePermissionFromRoleC" -> {
                    var code = cac.revokePermissionFromRole(toks[2], toks[4], Operation.fromString(toks[3])!!)
                    if(code == OutcomeCode.CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND || code == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND) {
                        code = OutcomeCode.CODE_000_SUCCESS
                    }
                    code
                }

                // IO
                "readResourceC" -> {
                    val result = users[toks[2]]!!.readResource(toks[3])
                    result.code
                }
                "writeResourceC" -> users[toks[2]]!!.writeResource(toks[3], "content".inputStream())

                // other -> error
                else -> OutcomeCode.CODE_049_UNEXPECTED
            }
            val end = System.currentTimeMillis()

            output.println("% Employed time: ${end - start}")
            println("$code in ${end - start}")

            assertEquals(OutcomeCode.CODE_000_SUCCESS, code)

//            if(code != OutcomeCode.CODE_000_SUCCESS) {
//                exitProcess(1)
//            }
        }
        output.flush()
    }

    @Test
    fun `delete user of blank, admin, non-existing or deleted user fails`() {
        /** delete user of blank user */
        run {
            assert(core.deleteUser("") == OutcomeCode.CODE_020_INVALID_PARAMETER)
            assert(core.deleteUser("   ") == OutcomeCode.CODE_020_INVALID_PARAMETER)
        }

        /** delete user of admin user */
        run {
            assert(core.deleteUser(Constants.ADMIN) == OutcomeCode.CODE_022_ADMIN_CANNOT_BE_MODIFIED)
        }

        /** delete user of non-existing user */
        run {
            assert(core.deleteUser("alice") == OutcomeCode.CODE_004_USER_NOT_FOUND)
        }

        /** delete user of deleted user */
        run {
            addAndInitUser(core, "alice")
            assert(core.deleteUser("alice") == OutcomeCode.CODE_000_SUCCESS)
            // assert(core.deleteUser("alice") == OutcomeCode.CODE_013_USER_WAS_DELETED)
        }
    }

    @Test
    fun `get user of incomplete or operational or deleted user works`() {
        assert(core.addUser("incomplete").code == OutcomeCode.CODE_000_SUCCESS)
        addAndInitUser(core, "operational")
        addAndInitUser(core, "deleted")
        assert(core.deleteUser("deleted") == OutcomeCode.CODE_000_SUCCESS)

        /** get user of incomplete user */
        run {
            assert(core.getUsers(
                arrayOf(TupleStatus.INCOMPLETE)
            ).users!!.first { it.status == TupleStatus.INCOMPLETE }.name == "incomplete")
        }

        /** get user of operational user */
        run {
            assert(core.getUsers(
                arrayOf(TupleStatus.OPERATIONAL)
            ).users!!.filter { it.status == TupleStatus.OPERATIONAL }.size == 2)
        }

        /** get user of deleted user */
        run {
            assert(core.getUsers(
                arrayOf(TupleStatus.CACHED)
            ).users!!.first { it.status == TupleStatus.CACHED }.name == "deleted")
        }
    }

    @Test
    fun `get user of non-existing fails`() {

        /** get user of non-existing user */
        run {
            assert(core.getUsers(
                arrayOf(TupleStatus.OPERATIONAL)
            ).users!!.none { it.name == "not-existing" })
        }
    }



    open fun addAndInitUser(core: Core, username: String): Core {
        val userCore = TestUtilities.addUser(core, username)
        assert(userCore.initUser() == OutcomeCode.CODE_000_SUCCESS)
        return userCore
    }

    abstract fun myRun(core: Core? = null, block: () -> Unit)

    abstract fun myRunBlocking(core: Core? = null, block: suspend CoroutineScope.() -> Unit)
}
