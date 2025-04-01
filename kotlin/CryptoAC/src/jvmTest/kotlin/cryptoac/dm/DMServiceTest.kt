package cryptoac.dm

import cryptoac.*
import cryptoac.OutcomeCode.*
import cryptoac.ServiceTest
import cryptoac.tuple.Resource
import cryptoac.tuple.Enforcement
import cryptoac.tuple.User
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*

internal abstract class DMServiceTest : ServiceTest() {

    abstract val dm: DMService



    @BeforeEach
    override fun setUp() {
        super.setUp()
        assert(dm.configure() == CODE_000_SUCCESS)
    }



    @Test
    open fun add_resource_once_works() {
        val emptyResource = Resource(
            name = "empty",
            // enforcement = Enforcement.COMBINED
        )
        val newResource = Resource(
            name = "exam",
            // enforcement = Enforcement.COMBINED
        )
        val newResourceContent = "exam content"

        /** add resource */
        run {
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = newResourceContent.inputStream()
                ) == CODE_000_SUCCESS)

            assert(
                dm.addResource(
                    newResource = emptyResource,
                    resourceContent = "".inputStream()
                ) == CODE_000_SUCCESS)

            assert(
                dm.deleteResource(
                    resourceName = newResource.name,
                    // resourceVersionNumber = 1
                ) == CODE_000_SUCCESS)
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "another exam content".inputStream()
                ) == CODE_000_SUCCESS)
        }

        /** Cleanup */
        assert(
            dm.deleteResource(
                resourceName = newResource.name,
                // resourceVersionNumber = 1
            ) == CODE_000_SUCCESS
        )

        assert(
            dm.deleteResource(
                resourceName = emptyResource.name,
                // resourceVersionNumber = 1
            ) == CODE_000_SUCCESS
        )
    }

    @Test
    open fun add_resource_twice_or_with_blank_name_fail() {
        val newResource = Resource(
            name = "exam",
            // enforcement = Enforcement.COMBINED
        )
        val blankResource = Resource(
            name = "",
            // enforcement = Enforcement.COMBINED
        )
        val newResourceContent1 = "exam content"
        val newResourceContent2 = "second exam content"

        /** add resource twice */
        run {
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = newResourceContent1.inputStream()
                ) == CODE_000_SUCCESS)
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
            dm.deleteResource(
                resourceName = newResource.name,
                // resourceVersionNumber = 1
            ) == CODE_000_SUCCESS
        )
    }

    @Test
    open fun read_resource_works() {
        val newResource = Resource(
            name = "exam",
            // enforcement = Enforcement.COMBINED
        )
        val resourceContent = "exam content"

        /** read resource */
        run {
            assert(dm
                .addResource(
                    newResource = newResource,
                    resourceContent = resourceContent.inputStream()
                ) == CODE_000_SUCCESS)
            dm.readResource(
                resourceName = newResource.name,
                // resourceVersionNumber = 1
            ).apply {
                assert(code == CODE_000_SUCCESS)
                assert(stream!!.readAllBytes().contentEquals(resourceContent.toByteArray()))
            }
        }

        /** Cleanup */
        assert(
            dm.deleteResource(
                resourceName = newResource.name,
                // resourceVersionNumber = 1
            ) == CODE_000_SUCCESS
        )
    }

    @Test
    open fun read_non_existing_or_deleted_resource_or_with_blank_name_fail() {
        /** read non-existing resource */
        run {
            dm.readResource(
                resourceName = "non-existing",
                // resourceVersionNumber = 1
            ).apply {
                assert(code == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND)
                assert(stream == null)
            }
        }

        /** read deleted resource */
        run {
            val newResource = Resource(
                name = "exam",
                // enforcement = Enforcement.COMBINED
            )

            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "exam content".inputStream()
                ) == CODE_000_SUCCESS)

            assert(
                dm.deleteResource(
                    resourceName = newResource.name,
                    // resourceVersionNumber = 1
                ) == CODE_000_SUCCESS)

            dm.readResource(
                resourceName = "non-existing",
                // resourceVersionNumber = 1
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
                    // resourceVersionNumber = 1
                ).code == OutcomeCode.CODE_020_INVALID_PARAMETER)
        }
    }

    @Test
    open fun write_resource_works() {
        val newResource = Resource(
            name = "exam",
            // enforcement = Enforcement.COMBINED
        )
        val otherNewResource = Resource(
            name = "test",
            // enforcement = Enforcement.COMBINED
        )

        /** write resource */
        run {
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "exam content".inputStream()
                ) == CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = newResource,
                    resourceContent = "updated resource content".inputStream()
                ) == CODE_000_SUCCESS)
            dm.readResource(
                resourceName = newResource.name,
                // resourceVersionNumber = 1
            ).apply {
                assert(code == CODE_000_SUCCESS)
                assert(stream!!.readAllBytes().contentEquals("updated resource content".toByteArray()))
            }

            assert(
                dm.addResource(
                    newResource = otherNewResource,
                    resourceContent = "".inputStream()
                ) == CODE_000_SUCCESS)
            assert(
                dm.writeResource(
                    updatedResource = otherNewResource,
                    resourceContent = "".inputStream()
                ) == CODE_000_SUCCESS)
            dm.readResource(
                resourceName = otherNewResource.name,
                // resourceVersionNumber = 1
            ).apply {
                assert(code == CODE_000_SUCCESS)
                assert(stream!!.readAllBytes().contentEquals("".toByteArray()))
            }
        }

        /** Cleanup */
        assert(
            dm.deleteResource(
                resourceName = newResource.name,
                // resourceVersionNumber = 1
            ) == CODE_000_SUCCESS
        )

        assert(
            dm.deleteResource(
                resourceName = otherNewResource.name,
                // resourceVersionNumber = 1
            ) == CODE_000_SUCCESS
        )
    }

    @Test
    open fun write_non_existing_or_deleted_resource_or_with_blank_name_fail() {
        /** write non-existing resource */
        run {
            val nonExisting = Resource(
                name = "non-existing",
                // enforcement = Enforcement.COMBINED
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
                // enforcement = Enforcement.COMBINED
            )
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "exam content".inputStream()
                ) == CODE_000_SUCCESS)
            assert(
                dm.deleteResource(
                    resourceName = newResource.name,
                    // resourceVersionNumber = 1
                ) == CODE_000_SUCCESS)
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
                // enforcement = Enforcement.COMBINED
            )

            assert(
                dm.writeResource(
                    updatedResource = emptyResource,
                    resourceContent = "empty resource content".inputStream()
                ) == OutcomeCode.CODE_020_INVALID_PARAMETER)
        }
    }

    @Test
    open fun delete_resource_once_works() {
        /** delete resource */
        run {
            val newResource = Resource(
                name = "exam",
                //  enforcement = Enforcement.COMBINED
            )
            assert(
                dm.addResource(
                    newResource = newResource,
                    resourceContent = "exam content".inputStream()
                ) == CODE_000_SUCCESS)
            assert(
                dm.deleteResource(
                    resourceName = newResource.name,
                    // resourceVersionNumber = 1
                ) == CODE_000_SUCCESS)
        }
    }

    @Test
    open fun delete_non_existing_or_deleted_resource_or_with_blank_name_fail() {
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
                ) == CODE_000_SUCCESS)
            assert(
                dm.deleteResource(
                    resourceName = newResource.name,
//                    resourceVersionNumber = 1
                ) == CODE_000_SUCCESS)
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



    override fun addUser(user: User): Service {
        val addUserResult = dm.addUser(user)
        assert(addUserResult.code == CODE_000_SUCCESS)
        val userDM = DMFactory.getDM(
            dmParameters = addUserResult.serviceParameters as DMServiceParameters
        )
        return userDM
    }
}
