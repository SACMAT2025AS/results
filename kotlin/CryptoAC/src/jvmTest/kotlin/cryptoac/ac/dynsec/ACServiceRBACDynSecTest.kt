package cryptoac.ac.dynsec

import cryptoac.*
import cryptoac.OutcomeCode.*
import cryptoac.TestUtilities.Companion.assertUnlock
import cryptoac.TestUtilities.Companion.assertUnlockAndLock
import cryptoac.TestUtilities.Companion.resetACServiceRBACDynSEC
import cryptoac.ac.ACFactory
import cryptoac.ac.ACServiceRBACTest
import cryptoac.tuple.TupleStatus
import cryptoac.tuple.Operation
import cryptoac.tuple.User
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ACServiceRBACDynSecTest : ACServiceRBACTest() {

    override val acRBAC = ACFactory.getAC(
        Parameters.acServiceRBACDynSecParameters
    )!! as ACServiceRBACDynSec
    override val ac = acRBAC
    override val service: Service = ac

    private var processDocker: Process? = null



    @BeforeAll
    override fun setUpAll() {
        "./cleanAllAndBuild.sh".runCommand(TestUtilities.dir, hashSetOf("built_all_end_of_script"))
        processDocker = "./startCryptoAC_ALL.sh \"cryptoac_mosquitto_dynsec\"".runCommand(
            workingDir = TestUtilities.dir,
            endStrings = hashSetOf("mosquitto version")
        )
    }

    @BeforeEach
    override fun setUp() {
        super.setUp()
        assert(acRBAC.configure() == CODE_000_SUCCESS)
    }

    @AfterEach
    override fun tearDown() {
        assertUnlock(service)
        resetACServiceRBACDynSEC(acRBAC)
        service.deinit()
        assert(service.locks == 0)
    }

    @AfterAll
    override fun tearDownAll() {
        processDocker!!.destroy()
        Runtime.getRuntime().exec("kill -SIGINT ${processDocker!!.pid()}")
        "./cleanAll.sh".runCommand(
            workingDir = TestUtilities.dir,
            endStrings = hashSetOf("clean_all_end_of_script")
        )
    }



    @Test
    override fun add_admin_once_works() {
        /** Add admin is not implemented */
    }

    @Test
    override fun add_admin_with_wrong_or_blank_name_or_twice_fails() {
        /** Add admin is not implemented */
    }

    @Test
    override fun init_user_once_works() {
        /** Init user is not implemented */
    }

    @Test
    override fun init_user_not_existing_or_already_initialized_or_deleted_fail() {
        /** Init user is not implemented */
    }

    @Test
    override fun add_user_twice_or_with_admin_or_blank_name_or_previously_deleted_fail() {
        /** add user twice */
        run {
            assert(acRBAC.addUser(
                newUser = Parameters.aliceUser
            ).code == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC)

            assert(acRBAC.addUser(
                newUser = Parameters.aliceUser
            ).code == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC, unlockCode = CODE_001_USER_ALREADY_EXISTS)
        }

        /** add user with admin or blank name */
        myRun {
            assert(acRBAC.addUser(
                Parameters.adminUser
            ).code == CODE_001_USER_ALREADY_EXISTS)

            assert(acRBAC.addUser(
                newUser = User(" ")
            ).code == CODE_020_INVALID_PARAMETER)
        }

        /** add user previously deleted */
        myRun {
            /** DynSec does not keep the list of deleted users */
            //addAndInitUser(Parameters.bobUser)
            //assert(acRBAC.deleteUser(Parameters.bobUser.name) == CODE_000_SUCCESS)
            //assertUnlockAndLock(acRBAC)
            //assert(acRBAC.addUser(Parameters.bobUser).code == CODE_000_SUCCESS)
            //assertUnlockAndLock(acRBAC, unlockCode = CODE_013_USER_WAS_DELETED)
        }
    }

    override fun get_not_existing_incomplete_operational_and_deleted_user_by_name_works() {
        val incompleteUser = Parameters.aliceUser
        service.addUser(incompleteUser)
        val operationalUser = Parameters.bobUser
        addAndInitUser(operationalUser)
        val deletedUser = Parameters.carlUser
        addAndInitUser(deletedUser)
        assert(service.deleteUser(deletedUser.name) == CODE_000_SUCCESS)

        /** get not-existing user by name */
        myRun {
            assert(service.getUsers(username = "non-existing").isEmpty())
        }

        /** get incomplete user by name */
        myRun {
            val incompleteUserByName = service.getUsers(username = incompleteUser.name)
            assert(incompleteUserByName.size == 1)
        }

        /** get operational user by name */
        myRun {
            val operationalUserByName = service.getUsers(username = operationalUser.name)
            assert(operationalUserByName.size == 1)
            /**
             * Do not check that the token is the same, unfortunately DynSec does
             * not have the possibility to add metadata to users
             */
            //assert(operationalUserByName.firstOrNull()!!.token == operationalUser.token)
        }

        /** get deleted user by name */
        myRun {
            val deletedUserByName = service.getUsers(username = deletedUser.name, status = TupleStatus.DELETED)
            assert(deletedUserByName.size == 1)
            assert(deletedUserByName.firstOrNull()!!.name == deletedUser.name)
        }
    }

    @Test
    override fun delete_non_existing_and_deleted_users_by_name_or_blank_name_fails() {

        /** delete user twice */
        run {
            assert(acRBAC.addUser(
                newUser = Parameters.aliceUser
            ).code == CODE_000_SUCCESS)

            assert(acRBAC.deleteUser(
                username = Parameters.aliceUser.name
            ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC)

            assert(acRBAC.deleteUser(
                username = Parameters.aliceUser.name
            ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC, unlockCode = CODE_004_USER_NOT_FOUND)
        }

        /** delete user not existing */
        run {
            assert(acRBAC.deleteUser(
                username = Parameters.aliceUser.name
            ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC, unlockCode = CODE_004_USER_NOT_FOUND)
        }

        /** delete user with blank username */
        runBlocking {
            assert(ac.deleteUser(
                username = " "
            ) == CODE_020_INVALID_PARAMETER)
        }
    }

    @Test
    override fun add_role_twice_or_with_blank_name_fail() {
        val roleName = "roleTest1"

        /** add role twice */
        runBlocking {
            assert(
                acRBAC.addRole(
                    roleName = roleName
                ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC)

            assert(
                acRBAC.addRole(
                    roleName = roleName
                ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC, unlockCode = CODE_002_ROLE_ALREADY_EXISTS)
        }

        /** add role with blank name */
        runBlocking {
            assert(
                acRBAC.addRole(
                    roleName = ""
                ) == CODE_020_INVALID_PARAMETER)
        }

        /** Cleanup */
        assert(
            acRBAC.deleteRole(
                roleName = roleName
            ) == CODE_000_SUCCESS)
    }

    @Test
    override fun delete_role_twice_or_with_blank_role_name_fail() {
        val roleName = "roleTest1"

        /** delete role twice */
        runBlocking {
            assert(
                acRBAC.addRole(
                    roleName = roleName
                ) == CODE_000_SUCCESS)

            assert(
                acRBAC.deleteRole(
                    roleName = roleName
                ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC)

            assert(
                acRBAC.deleteRole(
                    roleName = roleName
                ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC, unlockCode = CODE_005_ROLE_NOT_FOUND)
        }

        /** delete role with blank name */
        runBlocking {
            assert(
                acRBAC.deleteRole(
                    roleName = ""
                ) == CODE_020_INVALID_PARAMETER)
        }
    }

    @Test
    override fun add_resource_once_works() {
        /** Add resource is not implemented */
    }

    @Test
    override fun add_resource_twice_or_with_blank_name_fail() {
        /** Add resource is not implemented */
    }

    @Test
    override fun delete_resource_works() {
        /** Add resource (thus, delete resource) is not implemented */
    }

    @Test
    override fun delete_resource_twice_or_with_blank_resource_name_fail() {
        /** Add resource (thus, delete resource) is not implemented */
    }

    @Test
    override fun assign_user_to_role_twice_or_non_existing_user_to_role_or_non_existing_role_or_blank_name_fail() {
        val username = "userTest1"
        val roleName = "roleTest1"

        /** assign user to role twice */
        runBlocking {
            assert(acRBAC.addUser(
                newUser = User(username)
            ).code == CODE_000_SUCCESS)

            assert(
                acRBAC.addRole(
                    roleName = roleName
                ) == CODE_000_SUCCESS)

            assert(
                acRBAC.assignUserToRole(
                    username = username,
                    roleName = roleName
                ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC)

            assert(
                acRBAC.assignUserToRole(
                    username = username,
                    roleName = roleName
                ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC, unlockCode = CODE_062_UR_ASSIGNMENTS_ALREADY_EXISTS)
        }

        /** assign user to non-existing role */
        runBlocking {
            assert(
                acRBAC.assignUserToRole(
                    username = username,
                    roleName = "non-existing-role"
                ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC, unlockCode = CODE_005_ROLE_NOT_FOUND)
        }

        /** assign non-existing user to role */
        runBlocking {
            assert(
                acRBAC.assignUserToRole(
                    username = "non-existing-user",
                    roleName = roleName
                ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC, unlockCode = CODE_004_USER_NOT_FOUND)
        }

        /** assign user to role with blank name */
        runBlocking {
            assert(
                acRBAC.assignUserToRole(
                    username = username,
                    roleName = " "
                ) == CODE_020_INVALID_PARAMETER)
        }

        /** assign user with blank name to role */
        runBlocking {
            assert(
                acRBAC.assignUserToRole(
                    username = " ",
                    roleName = roleName
                ) == CODE_020_INVALID_PARAMETER)
        }

        /** Cleanup */
        assert(
            acRBAC.deleteUser(
                username = username
            ) == CODE_000_SUCCESS)
        assertUnlockAndLock(acRBAC)

        assert(
            acRBAC.deleteRole(
                roleName = roleName
            ) == CODE_000_SUCCESS)
    }

    @Test
    override fun assign_permission_to_role_twice_or_non_existing_role_or_non_existing_resource_or_blank_name_fail() {
        val roleName = "roleTest1"
        val resourceName = "resourceTest1"

        /** assign permission to role twice */
        runBlocking {
            assert(
                acRBAC.addRole(
                    roleName = roleName
                ) == CODE_000_SUCCESS)

            assert(
                acRBAC.addResource(
                    resourceName = resourceName
                ) == CODE_000_SUCCESS)

            assert(
                acRBAC.assignPermissionToRole(
                    roleName = roleName,
                    operation = Operation.READWRITE,
                    resourceName = resourceName
                ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC)

            assert(
                acRBAC.assignPermissionToRole(
                    roleName = roleName,
                    operation = Operation.READWRITE,
                    resourceName = resourceName
                ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC, unlockCode = CODE_063_PA_ASSIGNMENTS_ALREADY_EXISTS)
        }

        /** assign permission to role on non-existing resource */
        runBlocking {
            /** As DynSec does not have state of resources (i.e., topics), this works */
        }

        /** assign permission to non-existing role on resource */
        runBlocking {
            assert(
                acRBAC.assignPermissionToRole(
                    roleName = "non-existing-role",
                    operation = Operation.READWRITE,
                    resourceName = resourceName
                ) == CODE_000_SUCCESS)
            assertUnlockAndLock(acRBAC, unlockCode = CODE_005_ROLE_NOT_FOUND)
        }

        /** assign permission to role with blank name on resource */
        runBlocking {
            assert(
                acRBAC.assignPermissionToRole(
                    roleName = " ",
                    operation = Operation.READWRITE,
                    resourceName = resourceName
                ) == CODE_020_INVALID_PARAMETER)
        }

        /** assign permission to role on resource with blank name */
        runBlocking {
            assert(
                acRBAC.assignPermissionToRole(
                    roleName = roleName,
                    operation = Operation.READWRITE,
                    resourceName = " "
                ) == CODE_020_INVALID_PARAMETER)
        }

        /** Cleanup */
        assert(
            acRBAC.deleteRole(
                roleName = roleName
            ) == CODE_000_SUCCESS)
        assertUnlockAndLock(acRBAC)

        assert(
            acRBAC.deleteResource(
                resourceName = resourceName
            ) == CODE_000_SUCCESS)
    }

    @Test
    override fun check_authorized_user_can_do_works() {
        /** Is user allowed is not implemented */
    }

    @Test
    override fun check_not_authorized_user_can_do_fails() {
        /** Is user allowed is not implemented */
    }



    /** In this implementation, set the callback of the MQTT client */
    override fun addUser(user: User): Service {
        return super.addUser(user).apply {
            (this as ACServiceRBACDynSec).client.setCallback(acRBAC)
        }
    }
}