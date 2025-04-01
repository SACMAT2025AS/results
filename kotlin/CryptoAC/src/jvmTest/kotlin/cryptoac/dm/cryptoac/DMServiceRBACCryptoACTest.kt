package cryptoac.dm.cryptoac

import cryptoac.*
import cryptoac.Parameters.adminCoreCACRBACCryptoACParametersNoOPA
import cryptoac.TestUtilities.Companion.assertLock
import cryptoac.TestUtilities.Companion.dir
import cryptoac.dm.DMFactory
import cryptoac.dm.DMServiceRBACTest
import cryptoac.tuple.Enforcement
import cryptoac.tuple.Resource
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DMServiceRBACCryptoACTest : DMServiceRBACTest() {

    override val dmRBAC = DMFactory.getDM(
        Parameters.dmServiceCryptoACParameters
    ) as DMServiceCryptoAC
    override val dm = dmRBAC

    override val service: Service = dm

    private var processDocker: Process? = null



    @BeforeAll
    override fun setUpAll() {
        "./cleanAllAndBuild.sh".runCommand(dir, hashSetOf("built_all_end_of_script"))
        processDocker = "./startCryptoAC_ALL.sh \"cryptoac_dm\"".runCommand(
            workingDir = dir,
            endStrings = hashSetOf("Routes were registered, CryptoAC is up")
        )
    }

    @BeforeEach
    override fun setUp() {
        assert(dm.locks == 0)
        dm.init()
        assertLock(dm)
        assert(dm.configure(adminCoreCACRBACCryptoACParametersNoOPA) == OutcomeCode.CODE_000_SUCCESS)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        /** the DM is resetted directly in the tests */
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
        /** Add user is not implemented, hence get user returns nothing */
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
    override fun add_resource_once_works() {
        val emptyResource = Resource(
            name = "empty",
//            enforcement = Enforcement.COMBINED
        )
        val newResource = Resource(
            name = "exam",
//            enforcement = Enforcement.COMBINED
        )
        val newResourceContent = "exam content"

        /** add resource */
        run {
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = newResourceContent.inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = newResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)

            assert(
                dm.addResource(
                    newResource = emptyResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = emptyResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)

            assert(
                dm.deleteResource(
                    resourceName = newResource.name,
//                    resourceVersionNumber = 1
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "another exam content".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = newResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
        }

        /** Cleanup */
        assert(
            dm.deleteResource(
                resourceName = newResource.name,
//                resourceVersionNumber = 1
            ) == OutcomeCode.CODE_000_SUCCESS)

        assert(
            dm.deleteResource(
                resourceName = emptyResource.name,
//                resourceVersionNumber = 1
            ) == OutcomeCode.CODE_000_SUCCESS)
    }

    @Test
    override fun add_resource_twice_or_with_blank_name_fail() {
        val newResource = Resource(
            name = "exam",
//            enforcement = Enforcement.COMBINED
        )
        val blankResource = Resource(
            name = "",
//            enforcement = Enforcement.COMBINED
        )
        val newResourceContent1 = "exam content"
        val newResourceContent2 = "second exam content"

        /** add resource twice */
        run {
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = newResourceContent1.inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = newResourceContent2.inputStream()
                ) == OutcomeCode.CODE_003_RESOURCE_ALREADY_EXISTS)
        }

        /** add resource with blank name */
        runBlocking {
            assert(
                dm.addResource(
                    newResource = blankResource,
                    resourceContent = newResourceContent1.inputStream()
                ) == OutcomeCode.CODE_020_INVALID_PARAMETER
            )
        }

        /** Cleanup */
        assert(
            dm.writeResource(
                updatedResource = newResource,
                resourceContent = "".inputStream()
            ) == OutcomeCode.CODE_000_SUCCESS)
        assert(
            dm.deleteResource(
                resourceName = newResource.name,
//                resourceVersionNumber = 1
            ) == OutcomeCode.CODE_000_SUCCESS)
    }

    @Test
    override fun read_resource_works() {
        val newResource = Resource(
            name = "exam",
//            enforcement = Enforcement.COMBINED
        )
        val resourceContent = "exam content"

        /** read resource */
        run {
            assert(dm
                .addResource(
                    newResource = newResource,
                    resourceContent = resourceContent.inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = newResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            dm.readResource(
                resourceName = newResource.name,
//                resourceVersionNumber = 1
            ).apply {
                assert(code == OutcomeCode.CODE_000_SUCCESS)
                assert(stream!!.readAllBytes().contentEquals(resourceContent.toByteArray()))
            }
        }

        /** Cleanup */
        assert(
            dm.deleteResource(
                resourceName = newResource.name,
//                resourceVersionNumber = 1
            ) == OutcomeCode.CODE_000_SUCCESS)
    }

    @Test
    override fun read_non_existing_or_deleted_resource_or_with_blank_name_fail() {
        /** read non-existing resource */
        run {
            dm.readResource(
                resourceName = "non-existing",
//                resourceVersionNumber = 1
            ).apply {
                assert(code == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND)
                assert(stream == null)
            }
        }

        /** read deleted resource */
        run {
            val newResource = Resource(
                name = "exam",
//                enforcement = Enforcement.COMBINED
            )

            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "exam content".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = newResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.deleteResource(
                    resourceName = newResource.name,
//                    resourceVersionNumber = 1
                ) == OutcomeCode.CODE_000_SUCCESS)

            dm.readResource(
                resourceName = "non-existing",
//                resourceVersionNumber = 1
            ).apply {
                assert(code == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND)
                assert(stream == null)
            }
        }

        /** read resource with blank name */
        runBlocking {
            assert(
                dm.readResource(
                    resourceName = " ",
//                    resourceVersionNumber = 1
                ).code == OutcomeCode.CODE_020_INVALID_PARAMETER)
        }
    }

    @Test
    override fun write_resource_works() {
        val newResource = Resource(
            name = "exam",
//            enforcement = Enforcement.COMBINED
        )
        val otherNewResource = Resource(
            name = "test",
//            enforcement = Enforcement.COMBINED
        )

        /** write resource */
        run {
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "exam content".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = newResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)

            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "updated resource content".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = newResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)

            dm.readResource(
                resourceName = newResource.name,
//                resourceVersionNumber = 1
            ).apply {
                assert(code == OutcomeCode.CODE_000_SUCCESS)
                assert(stream!!.readAllBytes().contentEquals("updated resource content".toByteArray()))
            }

            assert(
                dm.addResource(
                    newResource = otherNewResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = otherNewResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.addResource(
                    newResource = otherNewResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = otherNewResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            dm.readResource(
                resourceName = otherNewResource.name,
//                resourceVersionNumber = 1
            ).apply {
                assert(code == OutcomeCode.CODE_000_SUCCESS)
                assert(stream!!.readAllBytes().contentEquals("".toByteArray()))
            }
        }

        /** Cleanup */
        assert(
            dm.deleteResource(
                resourceName = newResource.name,
//                resourceVersionNumber = 1
            ) == OutcomeCode.CODE_000_SUCCESS)

        assert(
            dm.deleteResource(
                resourceName = otherNewResource.name,
//                resourceVersionNumber = 1
            ) == OutcomeCode.CODE_000_SUCCESS)
    }

    @Test
    override fun write_non_existing_or_deleted_resource_or_with_blank_name_fail() {
        /** write non-existing resource */
        run {
            val nonExisting = Resource(
                name = "non-existing",
//                enforcement = Enforcement.COMBINED
            )
            assert(
                dm.writeResource(
                    updatedResource = nonExisting,
                    resourceContent = "non-existing".inputStream()
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND)
        }

        /** write deleted resource */
        run {
            val newResource = Resource(
                name = "exam",
//                enforcement = Enforcement.COMBINED
            )
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "exam content".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = newResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.deleteResource(
                    resourceName = newResource.name,
//                    resourceVersionNumber = 1
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = newResource,
                    resourceContent = "updated exam content".inputStream()
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND)
        }

        /** write resource with blank name */
        runBlocking {
            val emptyResource = Resource(
                name = "",
//                enforcement = Enforcement.COMBINED
            )

            assert(
                dm.writeResource(
                    updatedResource = emptyResource,
                    resourceContent = "empty resource content".inputStream()
                ) == OutcomeCode.CODE_020_INVALID_PARAMETER)
        }
    }

    @Test
    override fun delete_resource_once_works() {
        /** delete resource */
        run {
            val newResource = Resource(
                name = "exam",
//                enforcement = Enforcement.COMBINED
            )
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "exam content".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = newResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.deleteResource(
                    resourceName = newResource.name,
//                    resourceVersionNumber = 1
                ) == OutcomeCode.CODE_000_SUCCESS)
        }
    }

    @Test
    override fun delete_non_existing_or_deleted_resource_or_with_blank_name_fail() {
        /** delete non-existing resource */
        run {
            assert(
                dm.deleteResource(
                    resourceName = "non-existing",
//                    resourceVersionNumber = 1
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND)
        }

        /** delete non-existing resource */
        run {
            val newResource = Resource(
                name = "exam",
//                enforcement = Enforcement.COMBINED
            )
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "exam content".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = newResource,
                    resourceContent = "".inputStream()
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.deleteResource(
                    resourceName = newResource.name,
//                    resourceVersionNumber = 1
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                dm.deleteResource(
                    resourceName = newResource.name,
//                    resourceVersionNumber = 1
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND)
        }

        /** delete resource with blank name */
        runBlocking {
            assert(
                dm.deleteResource(
                    resourceName = " ",
//                    resourceVersionNumber = 1
                ) == OutcomeCode.CODE_020_INVALID_PARAMETER)
        }
    }
}
