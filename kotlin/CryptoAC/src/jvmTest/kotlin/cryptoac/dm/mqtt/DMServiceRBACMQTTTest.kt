//package cryptoac.dm.mqtt
//
//import cryptoac.*
//import cryptoac.OutcomeCode.*
//import cryptoac.TestUtilities.Companion.dir
//import cryptoac.core.mqtt.CryptoACMQTTClient
//import cryptoac.core.myJson
//import cryptoac.dm.*
//import cryptoac.dm.DMServiceRBACTest
//import cryptoac.tuple.Resource
//import cryptoac.tuple.Enforcement
//import cryptoac.tuple.User
//import kotlinx.coroutines.runBlocking
//import kotlinx.serialization.encodeToString
//import org.junit.jupiter.api.*
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//internal class DMServiceRBACMQTTTest : DMServiceRBACTest() {
//
//    override var dm = DMFactory.getDM(
//        Parameters.dmServiceMQTTNoACParameters
//    ) as DMServiceMQTT
//    override val dmRBAC: DMServiceRBAC = dm
//    override val service: Service = dm
//
//    private var processDocker: Process? = null
//
//
//
//    @BeforeAll
//    override fun setUpAll() {
//        "./cleanAllAndBuild.sh".runCommand(dir, hashSetOf("built_all_end_of_script"))
//        processDocker = "./startCryptoAC_ALL.sh \"cryptoac_mosquitto_no_dynsec\"".runCommand(
//            workingDir = dir,
//            endStrings = hashSetOf("mosquitto version")
//        )
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
//    override fun add_admin_once_works() {
//        /** Add admin is not implemented */
//    }
//
//    @Test
//    override fun add_admin_with_wrong_or_blank_name_or_twice_fails() {
//        /** Add admin is not implemented */
//    }
//
//    @Test
//    override fun init_user_once_works() {
//        /** Init user is not implemented */
//    }
//
//    @Test
//    override fun init_user_not_existing_or_already_initialized_or_deleted_fail() {
//        /** Init user is not implemented */
//    }
//
//    @Test
//    override fun add_user_once_works() {
//        /** Add user is not implemented */
//    }
//
//    @Test
//    override fun add_user_twice_or_with_admin_or_blank_name_or_previously_deleted_fail() {
//        /** Add user is not implemented */
//    }
//
//    override fun get_not_existing_incomplete_operational_and_deleted_user_by_name_works() {
//        /** Add user is not implemented */
//    }
//
//    @Test
//    override fun get_all_users_works() {
//        /** Add user is not implemented */
//    }
//
//    @Test
//    override fun delete_incomplete_and_operational_users_by_name_works() {
//        /** Delete user is not implemented */
//    }
//
//    @Test
//    override fun delete_non_existing_and_deleted_users_by_name_or_blank_name_fails() {
//        /** Delete user is not implemented */
//    }
//
//    @Test
//    override fun add_resource_once_works() {
//        val newTopic = Resource(
//            name = "exam",
//            enforcement = Enforcement.COMBINED
//        )
//
//        /** add resource once */
//        runBlocking {
//            assert(
//                dm.addResource(
//                    newResource = newTopic,
//                    resourceContent = "".inputStream()
//                ) == CODE_000_SUCCESS
//            )
//            assert(
//                dm.readResource(
//                    resourceName = newTopic.name,
//                    resourceVersionNumber = 1
//                ).code == CODE_000_SUCCESS
//            )
//            assert(waitForCondition { (dm.topicsAndMessages[newTopic.name]?.size ?: 0) == 1 })
//            assert(dm.topicsAndMessages[newTopic.name]!!.first() == myJson.encodeToString(newTopic))
//            dm.topicsAndMessages[newTopic.name]!!.clear()
//        }
//
//        /** Cleanup */
//        runBlocking {
//            assert(dm.deleteResource(
//                resourceName = newTopic.name,
//                resourceVersionNumber = 1
//            ) == CODE_000_SUCCESS)
//            assert(waitForCondition { (dm.topicsAndMessages[newTopic.name]?.size ?: 0) == 1 })
//            dm.topicsAndMessages[newTopic.name]!!.clear()
//        }
//    }
//
//    @Test
//    override fun add_resource_twice_or_with_blank_name_fail() {
//        val blankResource = Resource(
//            name = "",
//            enforcement = Enforcement.COMBINED
//        )
//        val newResourceContent1 = "exam content"
//
//        /** add resource twice */
//        run {
//            /** Creating a topic twice is fine */
//        }
//
//        /** add resource with blank name */
//        runBlocking {
//            assert(
//                dm.addResource(
//                    newResource = blankResource,
//                    resourceContent = newResourceContent1.inputStream()
//                ) == CODE_020_INVALID_PARAMETER
//            )
//        }
//    }
//
//    @Test
//    override fun read_resource_works() {
//        val newTopic = Resource(
//            name = "exam",
//            enforcement = Enforcement.COMBINED
//        )
//
//        /** read resource */
//        runBlocking {
//            val newTopicResourceContent = "First message for topic ${newTopic.name}"
//            assert(dm.addResource(
//                newResource = newTopic,
//                resourceContent = newTopicResourceContent.inputStream()
//            ) == CODE_000_SUCCESS
//            )
//            assert(dm.readResource(
//                resourceName = newTopic.name,
//                resourceVersionNumber = 1
//            ).code == CODE_000_SUCCESS
//            )
//            assert(waitForCondition { (dm.topicsAndMessages[newTopic.name]?.size ?: 0) == 1 })
//            assert(dm.topicsAndMessages[newTopic.name]!!.first() == myJson.encodeToString(newTopic))
//            dm.topicsAndMessages[newTopic.name]!!.clear()
//        }
//
//        /** Cleanup */
//        runBlocking {
//            assert(
//                dm.deleteResource(
//                    resourceName = newTopic.name,
//                    resourceVersionNumber = 1
//                ) == CODE_000_SUCCESS
//            )
//            assert(waitForCondition { (dm.topicsAndMessages[newTopic.name]?.size ?: 0) == 1 })
//            dm.topicsAndMessages[newTopic.name]!!.clear()
//        }
//    }
//
//    @Test
//    override fun read_non_existing_or_deleted_resource_or_with_blank_name_fail() {
//        /** read non-existing resource */
//        run {
//            /**
//             * MQTT clients can subscribe to non-existing or deleted
//             * topics (the broker will just create the given topic)
//             */
//        }
//
//        /** read deleted resource */
//        run {
//            /**
//             * MQTT clients can subscribe to non-existing or deleted
//             * topics (the broker will just create the given topic)
//             */
//        }
//
//        /** read resource with blank name */
//        runBlocking {
//            assert(
//                dm.readResource(
//                    resourceName = " ",
//                    resourceVersionNumber = 1
//                ).code == CODE_020_INVALID_PARAMETER
//            )
//        }
//    }
//
//    @Test
//    override fun write_resource_works() {
//        val emptyTopic = Resource(
//            name = "empty",
//            enforcement = Enforcement.COMBINED
//        )
//        val newTopic = Resource(
//            name = "exam",
//            enforcement = Enforcement.COMBINED
//        )
//
//        /** write resource once */
//        runBlocking {
//            val newTopicResourceContent = "First message for topic ${newTopic.name}"
//            assert(
//                dm.addResource(
//                    newResource = newTopic,
//                    resourceContent = newTopicResourceContent.inputStream()
//                ) == CODE_000_SUCCESS
//            )
//            assert(
//                dm.readResource(
//                    resourceName = newTopic.name,
//                    resourceVersionNumber = 1
//                ).code == CODE_000_SUCCESS
//            )
//            assert(waitForCondition { (dm.topicsAndMessages[newTopic.name]?.size ?: 0) == 1 })
//            assert(dm.topicsAndMessages[newTopic.name]!!.first() == myJson.encodeToString(newTopic))
//            dm.topicsAndMessages[newTopic.name]!!.clear()
//
//            val newTopicContentWrite = "write new message for topic ${newTopic.name}"
//            assert(
//                dm.writeResource(
//                    updatedResource = Resource(
//                        name = newTopic.name,
//                        enforcement = Enforcement.COMBINED
//                    ),
//                    resourceContent = newTopicContentWrite.inputStream()
//                ) == CODE_000_SUCCESS
//            )
//            assert(waitForCondition { (dm.topicsAndMessages[newTopic.name]?.size ?: 0) == 1 })
//            assert(dm.topicsAndMessages[newTopic.name]!!.first() == newTopicContentWrite)
//            dm.topicsAndMessages[newTopic.name]!!.clear()
//        }
//
//        /** write empty resource */
//        runBlocking {
//            assert(
//                dm.addResource(
//                    newResource = emptyTopic,
//                    resourceContent = "".inputStream()
//                ) == CODE_000_SUCCESS
//            )
//            assert(
//                dm.readResource(
//                    resourceName = emptyTopic.name,
//                    resourceVersionNumber = 1
//                ).code == CODE_000_SUCCESS
//            )
//            assert(waitForCondition { (dm.topicsAndMessages[emptyTopic.name]?.size ?: 0) == 1 })
//            assert(dm.topicsAndMessages[emptyTopic.name]!!.first() == myJson.encodeToString(emptyTopic))
//            dm.topicsAndMessages[emptyTopic.name]!!.clear()
//
//            assert(waitForCondition { (dm.topicsAndMessages[emptyTopic.name]?.size ?: 0) == 0 })
//            val emptyTopicContentWrite = ""
//            assert(dm.writeResource(
//                updatedResource = Resource(
//                    name = emptyTopic.name,
//                    enforcement = Enforcement.COMBINED
//                ),
//                resourceContent = emptyTopicContentWrite.inputStream()
//            ) == CODE_000_SUCCESS
//            )
//            assert(waitForCondition { (dm.topicsAndMessages[emptyTopic.name]?.size ?: 0) == 1 })
//            assert(dm.topicsAndMessages[emptyTopic.name]!!.first() == emptyTopicContentWrite)
//            dm.topicsAndMessages[emptyTopic.name]!!.clear()
//        }
//
//        /** Cleanup */
//        runBlocking {
//            assert(
//                dm.deleteResource(
//                    resourceName = newTopic.name,
//                    resourceVersionNumber = 1
//                ) == CODE_000_SUCCESS
//            )
//            assert(
//                dm.deleteResource(
//                    resourceName = emptyTopic.name,
//                    resourceVersionNumber = 1
//                ) == CODE_000_SUCCESS
//            )
//            assert(waitForCondition { (dm.topicsAndMessages[newTopic.name]?.size ?: 0) == 1 })
//            assert(waitForCondition { (dm.topicsAndMessages[emptyTopic.name]?.size ?: 0) == 1 })
//            dm.topicsAndMessages[newTopic.name]!!.clear()
//            dm.topicsAndMessages[emptyTopic.name]!!.clear()
//        }
//    }
//
//    @Test
//    override fun write_non_existing_or_deleted_resource_or_with_blank_name_fail() {
//        /** write non-existing resource */
//        run {
//            /**
//             * MQTT clients can publish to non-existing or deleted
//             * topics (the broker will just create the given topic)
//             */
//        }
//
//        /** write deleted resource */
//        run {
//            /**
//             * MQTT clients can publish to non-existing or deleted
//             * topics (the broker will just create the given topic)
//             */
//        }
//
//        /** write resource with blank name */
//        runBlocking {
//            val emptyResource = Resource(
//                name = "",
//                enforcement = Enforcement.COMBINED
//            )
//
//            assert(
//                dm.writeResource(
//                    updatedResource = emptyResource,
//                    resourceContent = "empty resource content".inputStream()
//                ) == CODE_020_INVALID_PARAMETER
//            )
//        }
//    }
//
//    @Test
//    override fun delete_resource_once_works() {
//        val newTopic = Resource(
//            name = "exam",
//            enforcement = Enforcement.COMBINED
//        )
//
//        /** delete resource once */
//        runBlocking {
//            val newTopicResourceContent = "First message for topic ${newTopic.name}"
//            assert(
//                dm.addResource(
//                    newResource = newTopic,
//                    resourceContent = newTopicResourceContent.inputStream()
//                ) == CODE_000_SUCCESS
//            )
//            assert(
//                dm.readResource(
//                    resourceName = newTopic.name,
//                    resourceVersionNumber = 1
//                ).code == CODE_000_SUCCESS
//            )
//            assert(waitForCondition { (dm.topicsAndMessages[newTopic.name]?.size ?: 0) == 1 })
//            assert(dm.topicsAndMessages[newTopic.name]!!.first() == myJson.encodeToString(newTopic))
//            dm.topicsAndMessages[newTopic.name]!!.clear()
//            assert(
//                dm.deleteResource(
//                    resourceName = newTopic.name,
//                    resourceVersionNumber = 1
//                ) == CODE_000_SUCCESS
//            )
//            assert(waitForCondition { (dm.topicsAndMessages[newTopic.name]?.size ?: 0) == 1 })
//            dm.topicsAndMessages[newTopic.name]!!.clear()
//        }
//    }
//
//    @Test
//    override fun delete_non_existing_or_deleted_resource_or_with_blank_name_fail() {
//        /** delete non-existing resource */
//        run {
//            /**
//             * The lifecycle of topics is determined by
//             * retained messages and subscribed users
//             */
//        }
//
//        /** delete non-existing resource */
//        run {
//            /**
//             * The lifecycle of topics is determined by
//             * retained messages and subscribed users
//             */
//        }
//
//        /** delete resource with blank name */
//        runBlocking {
//            assert(
//                dm.deleteResource(
//                    resourceName = " ",
//                    resourceVersionNumber = 1
//                ) == CODE_020_INVALID_PARAMETER
//            )
//        }
//    }
//
//
//
//    /** In this implementation, set the callback of the MQTT client */
//    override fun addUser(user: User): Service {
//        return super.addUser(user).apply {
//            (this as DMServiceMQTT).client.setCallback(dm)
//        }
//    }
//}
