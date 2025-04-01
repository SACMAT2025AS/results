package cryptoac.rm.cryptoac

import cryptoac.*
import cryptoac.Constants.ADMIN
import cryptoac.TestUtilities.Companion.assertLock
import cryptoac.TestUtilities.Companion.assertUnlock
import cryptoac.TestUtilities.Companion.dir
import cryptoac.ac.ACFactory
import cryptoac.ac.opa.ACServiceRBACOPA
import cryptoac.dm.DMFactory
import cryptoac.tuple.Resource
import cryptoac.tuple.Enforcement
import cryptoac.mm.MMFactory
import cryptoac.mm.MMServiceCACRBAC
import cryptoac.tuple.User
import cryptoac.rm.RMFactory
import cryptoac.rm.RMService
import cryptoac.rm.RMServiceRBACTest
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RMServiceRBACCryptoACTest : RMServiceRBACTest() {

    override val rmRBAC = RMFactory.getRM(
        Parameters.rmServiceRBACCryptoACParameters
    )!! as RMServiceRBACCryptoAC
    override val dm = DMFactory.getDM(
        Parameters.dmServiceCryptoACParameters
    )
    override val rm = rmRBAC

    private val mm: MMServiceCACRBAC = MMFactory.getMM(
        Parameters.mmServiceRBACMySQLParameters
    ) as MMServiceCACRBAC
    private val ac: ACServiceRBACOPA = ACFactory.getAC(
        Parameters.acServiceRBACOPAParameters
    ) as ACServiceRBACOPA
    private var processDocker: Process? = null

    override val service: Service = rm



    @BeforeAll
    override fun setUpAll() {
        "./cleanAllAndBuild.sh".runCommand(dir, hashSetOf("built_all_end_of_script"))
        processDocker = "./startCryptoAC_ALL.sh \"cryptoac_rm cryptoac_dm cryptoac_mysql cryptoac_opa\"".runCommand(
            workingDir = dir,
            endStrings = hashSetOf(
                "port: 3306  MySQL Community Server - GPL",
                "Routes were registered, CryptoAC is up",
                "OPA is running"
            )
        )
    }

    @BeforeEach
    override fun setUp() {
        super.setUp()
        assertLock(ac)
        assert(ac.configure() == OutcomeCode.CODE_000_SUCCESS)
        assert(ac.addAdmin(Parameters.adminUser) == OutcomeCode.CODE_000_SUCCESS)
        assert(ac.assignUserToRole(Parameters.adminUser.name, Parameters.adminUser.name) == OutcomeCode.CODE_000_SUCCESS)
        assertUnlock(ac)
        assertLock(mm)
        assert(mm.configure() == OutcomeCode.CODE_000_SUCCESS)
        assert(mm.addAdmin(Parameters.adminUser) == OutcomeCode.CODE_000_SUCCESS)
        assert(mm.addRole(Parameters.adminRole) == OutcomeCode.CODE_000_SUCCESS)
        assert(mm.addUsersRoles(hashSetOf(Parameters.adminUserRole)) == OutcomeCode.CODE_000_SUCCESS)
        assertUnlock(mm)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
//        TestUtilities.resetMMServiceRBACMySQL()
        TestUtilities.resetACServiceRBACOPA()
    }

    @AfterAll
    override fun tearDownAll() {
        processDocker!!.destroy()
        Runtime.getRuntime().exec("kill -SIGINT ${processDocker!!.pid()}")
        "./cleanAll.sh".runCommand(
            workingDir = dir,
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
    override fun add_user_once_works() {
        /** Add user is not implemented */
    }

    @Test
    override fun add_user_twice_or_with_admin_or_blank_name_or_previously_deleted_fail() {
        /** Add user is not implemented */
    }

    @Test
    override fun get_not_existing_incomplete_operational_and_deleted_user_by_name_works() {
        /** Add user is not implemented yet, hence get user returns nothing */
    }

    @Test
    override fun get_all_users_works() {
        /** Add user is not implemented yet, hence get user returns nothing */
    }

    @Test
    override fun delete_incomplete_and_operational_users_by_name_works() {
        /** Delete user is not implemented */
    }

    @Test
    override fun delete_non_existing_and_deleted_users_by_name_or_blank_name_fails() {
        /** Delete user is not implemented */
    }

    @Test
    override fun delete_the_admin_user_by_name_fails() {
        /** Delete user is not implemented */
    }



    @Test
    override fun check_add_resource_once_works() {
        val newResource = Resource(
            name = "exam",
//            enforcement = Enforcement.COMBINED
        )

        /** check add resource once */
        run {
            assert(dm.addResource(
                newResource = newResource,
                resourceContent = "exam content".inputStream()
            ) == OutcomeCode.CODE_000_SUCCESS)
            val addResourceRequest = createAddResourceRequest(
                resourceName = newResource.name,
                roleName = ADMIN
            )
            assert(
                rm.checkAddResource(
                    newResource = addResourceRequest.resource,
                    adminRolePermission = addResourceRequest.rolePermission
                ) == OutcomeCode.CODE_000_SUCCESS)
        }

        /** Cleanup */
        assert(dm.deleteResource(
            resourceName = newResource.name,
//            resourceVersionNumber = 1
        ) == OutcomeCode.CODE_000_SUCCESS)
    }

    @Test
    override fun check_add_resource_twice_non_existing_or_deleted_resource_fail() {
        val newResource = Resource(
            name = "exam",
//            enforcement = Enforcement.COMBINED
        )
        val addResourceRequest = createAddResourceRequest(newResource.name, ADMIN)

        /** check add resource twice */
        run {
            assert(dm.addResource(
                newResource = newResource,
                resourceContent = "exam content".inputStream()
            ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                rm.checkAddResource(
                    newResource = addResourceRequest.resource,
                    adminRolePermission = addResourceRequest.rolePermission
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                rm.checkAddResource(
                    newResource = addResourceRequest.resource,
                    adminRolePermission = addResourceRequest.rolePermission
                ) == OutcomeCode.CODE_003_RESOURCE_ALREADY_EXISTS
            )
        }

        /** check non-existing resource */
        run {
            val nonExistingResourceRequest = createAddResourceRequest(
                resourceName = "non-existing",
                roleName = ADMIN
            )
            assert(
                rm.checkAddResource(
                    newResource = nonExistingResourceRequest.resource,
                    adminRolePermission = nonExistingResourceRequest.rolePermission
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** check deleted resource */
        run {
            assert(dm.deleteResource(
                resourceName = newResource.name,
//                resourceVersionNumber = 1
            ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                rm.checkAddResource(
                    newResource = addResourceRequest.resource,
                    adminRolePermission = addResourceRequest.rolePermission
                ) == OutcomeCode.CODE_003_RESOURCE_ALREADY_EXISTS
            )
        }
    }

    @Test
    override fun check_write_resource_once_works() {
        val newResource = Resource(
            name = "exam",
//            enforcement = Enforcement.COMBINED
        )
        val addResourceRequest = createAddResourceRequest(newResource.name, ADMIN)
        assert(dm.addResource(
            newResource = newResource,
            resourceContent = "exam content".inputStream()
        ) == OutcomeCode.CODE_000_SUCCESS)
        assert(rm.checkAddResource(
            newResource = addResourceRequest.resource,
            adminRolePermission = addResourceRequest.rolePermission
        ) == OutcomeCode.CODE_000_SUCCESS)

        /** check write resource once */
        run {
            val updatedResource = Resource(
                name = "exam",
//                enforcement = Enforcement.COMBINED
            )
            assert(dm.addResource(
                newResource = updatedResource,
                resourceContent = "updated exam content".inputStream()
            ) == OutcomeCode.CODE_000_SUCCESS)

            val writeResourceRequest = createWriteResourceRequest(
                resourceName = newResource.name,
                resourceToken = addResourceRequest.resource.token,
                symKeyVersionNumber = 1
            )
            assert(rm.checkWriteResource(
                roleName = ADMIN,
                symKeyVersionNumber = writeResourceRequest.resource.versionNumber,
                newResource = writeResourceRequest.resource
            ) == OutcomeCode.CODE_000_SUCCESS)
        }

        /** Cleanup */
        assert(dm.deleteResource(
            resourceName = newResource.name,
//            resourceVersionNumber = 1
        ) == OutcomeCode.CODE_000_SUCCESS)
    }

    @Test
    override fun check_write_resource_twice_non_existing_or_deleted_resource_fail() {
        val newResource = Resource(name = "exam")
        val addResourceRequest = createAddResourceRequest(newResource.name, ADMIN)
        assert(dm.addResource(
            newResource = newResource,
            resourceContent = "exam content".inputStream()
        ) == OutcomeCode.CODE_000_SUCCESS)
        assert(
            rm.checkAddResource(
                addResourceRequest.resource, addResourceRequest.rolePermission
            ) == OutcomeCode.CODE_000_SUCCESS
        )

        val updatedResource = Resource(name = "exam")
        assert(dm.addResource(
            newResource = updatedResource,
            resourceContent = "updated exam content".inputStream()
        ) == OutcomeCode.CODE_000_SUCCESS)
        val writeResourceRequest = createWriteResourceRequest(
            resourceName = newResource.name,
            resourceToken = addResourceRequest.resource.token,
            symKeyVersionNumber = 1
        )
        assert(
            rm.checkWriteResource(
                ADMIN,
                writeResourceRequest.resource.versionNumber,
                writeResourceRequest.resource
            ) == OutcomeCode.CODE_000_SUCCESS
        )

        /** check write resource twice */
        run {
            assert(
                rm.checkWriteResource(
                    roleName = ADMIN,
                    symKeyVersionNumber = writeResourceRequest.resource.versionNumber,
                    newResource = writeResourceRequest.resource
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** check write non-existing resource */
        run {
            val nonExistingWriteResourceRequest = createWriteResourceRequest(
                resourceName = "non-existing",
                resourceToken = "non-existing",
                symKeyVersionNumber = 1
            )
            assert(
                rm.checkWriteResource(
                    ADMIN,
                    nonExistingWriteResourceRequest.resource.versionNumber,
                    nonExistingWriteResourceRequest.resource
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** check write deleted resource */
        run {
            assert(dm.deleteResource(
                resourceName = newResource.name,
//                resourceVersionNumber = 1
            ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                rm.checkWriteResource(
                    ADMIN,
                    writeResourceRequest.resource.versionNumber,
                    writeResourceRequest.resource
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND
            )
        }
    }



    override fun addAndInitUser(user: User): RMService {
        val addUserResult = rm.addUser(user)
        assert(addUserResult.code == OutcomeCode.CODE_000_SUCCESS)
        val userRM = RMServiceRBACCryptoAC(addUserResult.serviceParameters as RMServiceRBACCryptoACParameters)
        userRM.initUser(user)
        return userRM
    }
}
