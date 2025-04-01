//package cryptoac.dm.mqtt
//
//import cryptoac.Constants
//import cryptoac.OutcomeCode
//import cryptoac.OutcomeCode.*
//import cryptoac.core.CoreParameters
//import cryptoac.core.mqtt.CryptoACMQTTClient
//import cryptoac.core.myJson
//import cryptoac.dm.DMServiceABAC
//import cryptoac.dm.DMServiceRBAC
//import cryptoac.code.CodeResource
//import cryptoac.code.CodeBoolean
//import cryptoac.code.CodeServiceParameters
//import cryptoac.core.mqtt.AdminMqttMessageCryptoAC
//import cryptoac.core.mqtt.MqttMessageCryptoAC
//import cryptoac.core.mqtt.MqttMessageCryptoACType.*
//import cryptoac.core.mqtt.UserMqttMessageCryptoAC
//import cryptoac.tuple.TupleStatus
//import cryptoac.tuple.Resource
//import cryptoac.tuple.User
//import kotlinx.coroutines.runBlocking
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import kotlinx.serialization.decodeFromString
//import kotlinx.serialization.encodeToString
//import mu.KotlinLogging
//import org.eclipse.paho.mqttv5.client.IMqttToken
//import org.eclipse.paho.mqttv5.client.MqttCallback
//import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
//import org.eclipse.paho.mqttv5.common.MqttException
//import org.eclipse.paho.mqttv5.common.MqttMessage
//import org.eclipse.paho.mqttv5.common.packet.MqttProperties
//import java.io.InputStream
//
//private val logger = KotlinLogging.logger {}
//
///**
// * Class implementing the methods for invoking the APIs of an
// * MQTT broker configured with the given [dmServiceParameters]
// * and using the given MQTT [client]. Note that this class can act as
// * MqttCallback as well by simply storing received MQTT messages in the
// * [topicsAndMessages] hash map
// */
//class DMServiceMQTT(
//    private val dmServiceParameters: DMServiceMQTTParameters,
//    var client: CryptoACMQTTClient
//) : DMServiceRBAC, DMServiceABAC, MqttCallback {
//
//    override var locks = 0
//
//    /** A map of subscribed topics with the messages */
//    val topicsAndMessages: HashMap<String, MutableList<String>> = hashMapOf()
//
//    /** Mutex to synchronize the message arrived procedure */
//    private val messageArrivedMutex = Mutex()
//
//
//
//    override fun alreadyConfigured(): CodeBoolean {
//        // TODO to implement
//        return CodeBoolean()
//    }
//
//    /**
//     * In this implementation, no configuration is needed,
//     * therefore subsequent invocations are allowed
//     */
//    override fun configure(
//        parameters: CoreParameters?
//    ): OutcomeCode {
//        logger.info { "No configuration needed for DM MQTT" }
//        return CODE_000_SUCCESS
//    }
//
//    override fun init() {
//        logger.info { "No action required to initialize the DM MQTT service" }
//    }
//
//    /**
//     * In this implementation, unsubscribe to all topics
//     * and disconnect from the MQTT broker
//     */
//    override fun deinit() {
//        try {
//            if (client.isConnected) {
//                client.unsubscribe("#")
//                client.disconnect()
//            }
//        } catch (e: MqttException) {
//            logger.warn { "Exception while de-initializing MQTT client (${e.message})" }
//            logger.warn { e }
//            client.disconnectForcibly()
//        }
//    }
//
//
//
//    override fun addAdmin(
//        newAdmin: User
//    ): OutcomeCode {
//        /** Guard clauses */
//        if (newAdmin.name != Constants.ADMIN) {
//            logger.warn {
//                "Admin user has name ${newAdmin.name}" +
//                ", but admin name should be ${Constants.ADMIN}"
//            }
//            return CODE_036_ADMIN_NAME
//        }
//
//        logger.info { "No action required to add the admin" }
//        return CODE_000_SUCCESS
//    }
//
//    override fun initUser(
//        user: User
//    ): OutcomeCode {
//        logger.info { "No action required to initialize user" }
//        return CODE_000_SUCCESS
//    }
//
//    /**
//     * In this implementation, just return success.
//     * In fact, it is possible to add users to the
//     * Mosquitto broker only when using the DynSec
//     * plugin, hence when using the same broker as
//     * AC (in other words, the user is added to the
//     * DynSec plugin of the broker by the AC service)
//     */
//    override fun addUser(
//        newUser: User
//    ): CodeServiceParameters {
//        val username = newUser.name
//
//        /** Guard clauses */
//        if (username.isBlank() ) {
//            logger.warn { "Username is blank" }
//            return CodeServiceParameters(CODE_020_INVALID_PARAMETER)
//        }
//
//        logger.info { "No action required to add user" }
//
//        return CodeServiceParameters(
//            serviceParameters = DMServiceMQTTParameters(
//                username = username,
//                password = "noPasswordForMQTTAsDM",
//                port = dmServiceParameters.port,
//                url = dmServiceParameters.url,
//                tls = dmServiceParameters.tls,
//            )
//        )
//    }
//
//    override fun getUsers(
//        username: String?,
//        status: TupleStatus?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int
//    ): HashSet<User> {
//        TODO("Not yet implemented")
//    }
//
//    /**
//     * In this implementation, just return success.
//     * In fact, it is possible to delete users from
//     * the Mosquitto broker only when using the DynSec
//     * plugin, hence when using the same broker as
//     * AC (in other words, the user is deleted from the
//     * DynSec plugin of the broker by the AC service)
//     */
//    override fun deleteUser(
//        username: String
//    ): OutcomeCode {
//
//        /** Guard clauses */
//        if (username.isBlank()) {
//            logger.warn { "Username is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//        if (username == Constants.ADMIN) {
//            logger.warn { "Cannot delete the ${Constants.ADMIN} user" }
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }
//
//
//        logger.info { "No action required to delete user" }
//        return CODE_000_SUCCESS
//    }
//
//
//
//    /**
//     * In this implementation, create a topic with as name
//     * the name of the new resource, serialize the [newResource]
//     * and send it as a retained message, and finally send the
//     * [resourceContent] as a normal MQTT message. if given
//     */
//    override fun addResource(
//        newResource: Resource,
//        resourceContent: InputStream
//    ): OutcomeCode {
//
//        /** Guard clauses */
//        if (newResource.name.isBlank()) {
//            logger.warn { "Resource name is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//        if (newResource.versionNumber != 1) {
//            logger.warn { "Resource version number is not 1 (${newResource.versionNumber})" }
//            return CODE_020_INVALID_PARAMETER
//        }
//
//
//        logger.info {"Creating a topic with name ${newResource.name} in the MQTT broker"}
//
//        /** Publish the message containing the resource */
//        val adminMqttMessageCryptoAC = MqttMessageCryptoAC(
//            type = RESOURCE_UPDATE,
//            content = myJson.encodeToString(
//                AdminMqttMessageCryptoAC(
//                    resource = newResource
//                )
//            ).toByteArray()
//        )
//        val code = client.publishDMMessage(
//            topic = newResource.name,
//            mqttMessageCryptoAC = adminMqttMessageCryptoAC,
//            qos = 2,
//        )
//        if (code != CODE_000_SUCCESS) {
//            return code
//        }
//
//        /** Publish the (optional) resource content */
//        val optionalMessage = resourceContent.readAllBytes()
//        return if (optionalMessage.isNotEmpty()) {
//
//            val userMqttMessageCryptoAC = MqttMessageCryptoAC(
//                type = RESOURCE_CONTENT,
//                content = myJson.encodeToString(
//                    UserMqttMessageCryptoAC(
//                        resourceContent = optionalMessage,
//                        resourceVersionNumber = newResource.versionNumber
//                    )
//                ).toByteArray()
//            )
//            // TODO if this second publish fails, how to rollback?
//            client.publishDMMessage(
//                topic = newResource.name,
//                mqttMessageCryptoAC = userMqttMessageCryptoAC,
//                // TODO allow to select the QoS
//                qos = 1,
//            )
//        } else {
//            CODE_000_SUCCESS
//        }
//    }
//
//
//    /**
//     * In this implementation, subscribe to the topic [resourceName].
//     * [resourceVersionNumber] is not used
//     */
//    override fun readResource(
//        resourceName: String,
//        resourceVersionNumber: Int
//    ): CodeResource {
//
//        /** Guard clauses */
//        if (resourceName.isBlank()) {
//            logger.warn { "Resource name is blank" }
//            return CodeResource(CODE_020_INVALID_PARAMETER)
//        }
//
//        logger.info { "Subscribing to the topic $resourceName" }
//        return CodeResource(code = client.mySubscribe(
//            topicFilter = resourceName,
//            qos = 2,
//            isTheDM = true,
//        ))
//    }
//
//    /**
//     * In this implementation, send the [resourceContent] as a
//     * message in the topic with the name of the [updatedResource]
//     */
//    override fun writeResource(
//        updatedResource: Resource,
//        resourceContent: InputStream
//    ): OutcomeCode {
//
//        /** Guard clauses */
//        if (updatedResource.name.isBlank()) {
//            logger.warn { "Resource name is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//
//
//        logger.info {(
//            "Sending a message in the topic with name "
//            + "${updatedResource.name} in the MQTT broker"
//        )}
//
//        val userMqttMessageCryptoAC = MqttMessageCryptoAC(
//            type = RESOURCE_CONTENT,
//            content = myJson.encodeToString(
//                UserMqttMessageCryptoAC(
//                    resourceContent = resourceContent.readAllBytes(),
//                    resourceVersionNumber = updatedResource.versionNumber
//                )
//            ).toByteArray()
//        )
//        return client.publishDMMessage(
//            topic = updatedResource.name,
//            mqttMessageCryptoAC = userMqttMessageCryptoAC,
//            // TODO allow to select the QoS
//            qos = 1,
//        )
//    }
//
//    /**
//     * In this implementation, remove the retained message from the topic
//     * with name [resourceName]. [resourceVersionNumber]
//     */
//    override fun deleteResource(
//        resourceName: String,
//        resourceVersionNumber: Int
//    ): OutcomeCode {
//
//        /** Guard clauses */
//        if (resourceName.isBlank()) {
//            logger.warn { "Resource name is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//
//        logger.info { "Deleting topic $resourceName from the MQTT broker" }
//
//        val adminMqttMessageCryptoAC = MqttMessageCryptoAC(
//            type = RESOURCE_DELETE,
//            content = "".toByteArray()
//        )
//        return client.publishDMMessage(
//            topic = resourceName,
//            mqttMessageCryptoAC = adminMqttMessageCryptoAC,
//            qos = 2,
//        )
//    }
//
//
//
//    /**
//     * In this implementation, check whether the MQTT client is already
//     * connected. If not, check whether the MQTT client is already connecting
//     * (because, e.g., it was disconnected from the broker). If so, block
//     * until the client gets connected. Otherwise, simply connect the client.
//     * Remember that "client.connect()" is a blocking method that returns
//     * once connect completes
//     */
//    override fun lock(): OutcomeCode {
//        return if (locks == 0) {
//            logger.info { "Locking the status of the DM" }
//            try {
//                client.connectSync()
//                locks++
//                CODE_000_SUCCESS
//            } catch (e: MqttException) {
//                if (e.message?.contains("Not authorized") == true) {
//                    logger.warn { "DM MQTT - access denied for user" }
//                    CODE_059_ACCESS_DENIED_TO_DM
//                } else if (e.message?.contains("Unable to connect to server") == true) {
//                    logger.warn { "DM MQTT - connection timeout" }
//                    CODE_044_DM_CONNECTION_TIMEOUT
//                } else {
//                    throw e
//                }
//            }
//        } else if (locks > 0) {
//            locks++
//            logger.debug { "Increment lock number to $locks" }
//            CODE_000_SUCCESS
//        } else {
//            logger.warn { "Lock number is negative ($locks)" }
//            CODE_031_LOCK_CALLED_IN_INCONSISTENT_STATUS
//        }
//    }
//
//    /**
//     * In this implementation, TODO
//     */
//    override fun rollback(): OutcomeCode {
//        return if (locks == 1) {
//            logger.info { "Rollback the status of the DM" }
//            locks--
//            // TODO
//            CODE_000_SUCCESS
//        } else if (locks > 1) {
//            locks--
//            logger.debug { "Decrement lock number to $locks" }
//            CODE_000_SUCCESS
//        } else {
//            logger.warn { "DM rollback number is zero or negative ($locks)" }
//            CODE_033_ROLLBACK_CALLED_IN_INCONSISTENT_STATUS
//        }
//    }
//
//    /** In this implementation, TODO */
//    override fun unlock(): OutcomeCode {
//        return if (locks == 1) {
//            logger.info { "Unlock the status of the DM" }
//            locks--
//            // TODO
//            CODE_000_SUCCESS
//        } else if (locks > 1) {
//            locks--
//            logger.debug { "Decrement lock number to $locks" }
//            CODE_000_SUCCESS
//        } else {
//            logger.warn { "DM unlock number is zero or negative ($locks)" }
//            CODE_032_UNLOCK_CALLED_IN_INCONSISTENT_STATUS
//        }
//    }
//
//
//
//    /**
//     * In this implementation, store the message in the
//     * [topicsAndMessages] hash map as an [EncryptedMQTTMessage]
//     */
//    override fun messageArrived(
//        topic: String,
//        message: MqttMessage
//    ) {
//        /**
//         * Synchronize to avoid interference due to
//         * the receival of multiple MQTT messages
//         */
//        runBlocking {
//            messageArrivedMutex.withLock {
//                logger.info { "Topic $topic (user ${dmServiceParameters.username}): new message" }
//                topicsAndMessages.putIfAbsent(topic, mutableListOf())
//                val mqttMessageCryptoAC = myJson.decodeFromString<MqttMessageCryptoAC>(String(message.payload))
//                val messagePayload = when (mqttMessageCryptoAC.type) {
//                    RESOURCE_UPDATE -> {
//                        val resource = myJson.decodeFromString<AdminMqttMessageCryptoAC>(
//                            String(mqttMessageCryptoAC.content)
//                        ).resource
//                        myJson.encodeToString(resource)
//                    }
//                    RESOURCE_DELETE -> {
//                        client.unsubscribe(topic)
//                        "Topic $topic was deleted"
//                    }
//
//                    RESOURCE_CONTENT -> {
//                        val userMqttMessageCryptoAC = myJson.decodeFromString<UserMqttMessageCryptoAC>(
//                            String(mqttMessageCryptoAC.content)
//                        )
//                        String(userMqttMessageCryptoAC.resourceContent)
//                    }
//                }
//
//                if (messagePayload.isBlank()) {
//                    topicsAndMessages[topic]!!.add("")
//                } else {
//                    topicsAndMessages[topic]!!.add(messagePayload)
//                }
//
//            }
//        }
//    }
//
//    /** In this implementation, just log the event */
//    override fun disconnected(
//        disconnectResponse: MqttDisconnectResponse?
//    ) {
//        logger.warn {
//            "MQTT client for ${dmServiceParameters.username} was disconnected: " +
//                    disconnectResponse.toString()
//        }
//    }
//
//    /** In this implementation, just log the event */
//    override fun authPacketArrived(
//        reasonCode: Int,
//        properties: MqttProperties?
//    ) {
//        logger.debug { "authPacketArrived" }
//    }
//
//    /** In this implementation, just log the event */
//    override fun connectComplete(
//        reconnect: Boolean,
//        serverURI: String?
//    ) {
//        logger.debug { "connectComplete (reconnect $reconnect, serverURI $serverURI)" }
//    }
//
//    /** In this implementation, just log the event */
//    override fun deliveryComplete(
//        token: IMqttToken?
//    ) {
//        logger.debug { "deliveryComplete" }
//    }
//
//    /** In this implementation, just log the event */
//    override fun mqttErrorOccurred(
//        exception: MqttException?
//    ) {
//        logger.warn { "mqttErrorOccurred: $exception" }
//    }
//}
