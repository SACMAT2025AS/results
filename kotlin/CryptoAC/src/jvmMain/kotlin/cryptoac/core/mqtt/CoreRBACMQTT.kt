//package cryptoac.core.mqtt
//
//import cryptoac.*
//import cryptoac.ac.ACServiceRBAC
//import cryptoac.code.*
//import cryptoac.core.*
//import cryptoac.crypto.*
//import cryptoac.dm.*
//import cryptoac.mm.MMServiceCACRBAC
//import cryptoac.rm.RMServiceRBAC
//import cryptoac.tuple.Enforcement
//import cryptoac.tuple.Operation
//import cryptoac.tuple.TupleStatus
//import cryptoac.tuple.User
//import io.ktor.websocket.*
//import mu.KotlinLogging
//import org.eclipse.paho.mqttv5.client.*
//import org.eclipse.paho.mqttv5.common.MqttException
//import org.eclipse.paho.mqttv5.common.MqttMessage
//import org.eclipse.paho.mqttv5.common.packet.MqttProperties
//import java.io.InputStream
//
//private val logger = KotlinLogging.logger {}
//
///**
// * The CoreCACRBACMQTT implements a role-based CAC scheme
// * with hybrid cryptography for IoT (MQTT) scenarios.
// * It requires an MM and a DM service, while AC is optional
// * It receives the [coreParameters] and uses the [cryptoPKE] and
// * [cryptoSKE] objects to perform cryptographic computations
// */
//class CoreCACRBACMQTT(
//    override val cryptoPKE: CryptoPKE,
//    override val cryptoSKE: CryptoSKE,
//    override val coreParameters: CoreParametersRBAC,
//) : CoreCACRBACTuples(cryptoPKE, cryptoSKE, coreParameters), MqttCallback {
//
//
//    override val mm: MMServiceCACRBAC
//        get() = TODO("Not yet implemented")
//    override val rm: RMServiceRBAC?
//        get() = TODO("Not yet implemented")
//    override val dm: DMServiceRBAC
//        get() = TODO("Not yet implemented")
//
////    override val ac: ACServiceRBAC?
////        get() = TODO("Not yet implemented")
//
//    override fun addRole(roleName: String): OutcomeCode {
//        TODO("Not yet implemented")
//    }
//
//    override fun deleteRole(roleName: String): OutcomeCode {
//        TODO("Not yet implemented")
//    }
//
//    override fun addResource(
//        resourceName: String,
//        resourceContent: InputStream,
//        enforcement: Enforcement
//    ): OutcomeCode {
//        TODO("Not yet implemented")
//    }
//
//    override fun deleteResource(resourceName: String): OutcomeCode {
//        TODO("Not yet implemented")
//    }
//
//    override fun assignUserToRole(username: String, roleName: String): OutcomeCode {
//        TODO("Not yet implemented")
//    }
//
//    override fun revokeUserFromRole(username: String, roleName: String): OutcomeCode {
//        TODO("Not yet implemented")
//    }
//
//    override fun assignPermissionToRole(roleName: String, resourceName: String, operation: Operation): OutcomeCode {
//        TODO("Not yet implemented")
//    }
//
//    override fun revokePermissionFromRole(roleName: String, resourceName: String, operation: Operation): OutcomeCode {
//        TODO("Not yet implemented")
//    }
//
//    override fun readResource(resourceName: String): CodeResource {
//        TODO("Not yet implemented")
//    }
//
//    override fun writeResource(resourceName: String, resourceContent: InputStream): OutcomeCode {
//        TODO("Not yet implemented")
//    }
//
//    override fun getRoles(): CodeRoles {
//        TODO("Not yet implemented")
//    }
//
//    override fun getResources(): CodeResources {
//        TODO("Not yet implemented")
//    }
//
//    override fun getUsersRoles(username: String?, roleName: String?): CodeUsersRoles {
//        TODO("Not yet implemented")
//    }
//
//    override fun getRolesPermissions(username: String?, roleName: String?, resourceName: String?): CodeRolesPermissions {
//        TODO("Not yet implemented")
//    }
//    override val user: User
//        get() = TODO("Not yet implemented")
//
//    override fun configureServices(): OutcomeCode {
//        TODO("Not yet implemented")
//    }
//
//    override fun addUser(username: String): CodeCoreParameters {
//        TODO("Not yet implemented")
//    }
//
//    override fun deleteUser(username: String): OutcomeCode {
//        TODO("Not yet implemented")
//    }
//
//    override fun getUsers(
//        statuses: Array<TupleStatus>
//    ): CodeUsers {
//        TODO("Not yet implemented")
//    }
//
//    override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun mqttErrorOccurred(exception: MqttException?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun messageArrived(topic: String?, message: MqttMessage?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun deliveryComplete(token: IMqttToken?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
//        TODO("Not yet implemented")
//    }
//
//
//
////
////    /** The MM service */
////    override val mm: MMServiceCACRBAC = MMFactory.getMM(
////        mmParameters = coreParameters.mmServiceParameters
////    ) as MMServiceCACRBAC
////
////    /** The DM service */
////    override val dm: DMServiceMQTT = DMFactory.getDM(
////        dmParameters = coreParameters.dmServiceParameters
////    ) as DMServiceMQTT
////
////    /** The RM service (not needed for MQTT) */
////    override val rm: RMServiceRBAC? = null
////
////    /** The AC service */
////    override val ac: ACServiceRBACDynSec? = coreParameters.acServiceParameters?.let {
////        ACFactory.getAC(acParameters = it)
////    } as ACServiceRBACDynSec?
////
////    /** The user in the [coreParameters] */
////    override val user: User = coreParameters.user
////
////    /** Asymmetric encryption key pair */
////    override val asymEncKeyPair: KeyPairCryptoAC = cryptoPKE.recreateAsymKeyPair(
////        asymPublicKeyBytes = user.asymEncKeys?.public?.decodeBase64()!!,
////        asymPrivateKeyBytes = user.asymEncKeys.private.decodeBase64(),
////        type = AsymKeysType.ENC
////    )
////
////    /** Asymmetric signature key pair */
////    override val asymSigKeyPair: KeyPairCryptoAC = cryptoPKE.recreateAsymKeyPair(
////        asymPublicKeyBytes = user.asymSigKeys?.public?.decodeBase64()!!,
////        asymPrivateKeyBytes = user.asymSigKeys.private.decodeBase64(),
////        type = AsymKeysType.SIG
////    )
////
//    /** The web socket for sending MQTT messages */
//    var wss: DefaultWebSocketSession? = null
////
////    /** Mutex to synchronize the message pub/sub procedure */
////    private val messageArrivedMutex = Mutex()
////
////    // TODO shouldn't we clear the hash map below sometimes (e.g., when disconnect/deinit)?
////    /** A map of subscribed topics with the cached key and messages to send to the client */
////    val subscribedTopicsKeysAndMessages = hashMapOf<String, SymmetricKeysAndCachedMessages?>()
////
////    init {
////        val acParameters = coreParameters.acServiceParameters as ACServiceRBACDynSecParameters?
////        if (acParameters != null) {
////            val dmParameters = coreParameters.dmServiceParameters as DMServiceMQTTParameters
////            require(((acParameters.tls == dmParameters.tls)
////                    || (acParameters.url == dmParameters.url)
////                    || (acParameters.port == dmParameters.port)
////                    || (acParameters.username == dmParameters.username)
////                    )
////            ) { "The same Mosquitto MQTT broker must be used as both DM and AC" }
////            ac!!.client.setCallback(this)
////            dm.client = ac.client
////        } else {
////            dm.client.setCallback(this)
////        }
////    }
////
////    /**
////     * This function is invoked each time the core object
////     * is destroyed, and it should contain the code to
////     * de-initialize the core (e.g., possibly disconnect
////     * remote services like MQTT brokers, databases, wipe
////     * cryptographic secrets, etc.)
////     *
////     * In this implementation, clear the [subscribedTopicsKeysAndMessages] and
////     * also flush and close the wss
////     */
////    override fun deinitCore() {
////        super.deinitCore()
////        // TODO remember to zero the keys stored in subscribedTopicsKeysAndMessages
////        subscribedTopicsKeysAndMessages.clear()
////        runBlocking {
////            wss?.flush()
////            wss?.close()
////        }
////    }
////
////
////
////    /**
////     * Delete the resource with the matching [resourceName] from
////     * the policy, and delete all the related permissions. Finally,
////     * return the outcome code
////     *
////     * In this implementation, invoke the super method and
////     * then unsubscribe from the topic
////     */
////    override fun deleteResource(
////        resourceName: String
////    ): OutcomeCode {
////
////        /** Lock the status of the services */
////        var code = startOfMethod()
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /** Invoke the super method */
////        code = super.deleteResource(resourceName)
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Unsubscribe from the topic */
////        dm.client.unsubscribe(resourceName)
////        return endOfMethod(code = CODE_000_SUCCESS)
////    }
////
////    /**
////     * Revoke the user [username] from the
////     * role [roleName] from the policy.
////     * Finally, return the outcome code
////     */
////    override fun revokeUserFromRole(
////        username: String,
////        roleName: String
////    ): OutcomeCode {
////        return super.revokeUserFromRole(username, roleName)
////        // TODO BISOGNEREBBE COPIARE IL CODICE DALLA SUPERCLASSE
////        //  AGGIUNGENDO PERO' ANCHE L'INVIO DI UN NUOVO RETAINED
////        //  MESSAGE PER OGNI RISORSA AFFETTA DALL'OPERAZIONE: FACCIAMOLO
////        //  DOPO AVER UNIFORMATO IL CORE RBAC A QUELLO ABE-BASED, I.E.,
////        //  PERMETTENDO PIÙ PERMISSION TUPLE PER OGNI RISORSA
////    }
////
////    /**
////     * Revoke the [operation] from the role [roleName] over
////     * the resource [resourceName] in the policy. Finally,
////     * return the outcome code
////     */
////    override fun revokePermissionFromRole(
////        roleName: String,
////        resourceName: String,
////        operation: Operation
////    ): OutcomeCode {
////        return super.revokePermissionFromRole(roleName, resourceName, operation)
////        // TODO BISOGNEREBBE COPIARE IL CODICE DALLA SUPERCLASSE
////        //  AGGIUNGENDO PERO' ANCHE L'INVIO DI UN NUOVO RETAINED
////        //  MESSAGE PER LA RISORSA AFFETTA DALL'OPERAZIONE: FACCIAMOLO
////        //  DOPO AVER UNIFORMATO IL CORE RBAC A QUELLO ABE-BASED, I.E.,
////        //  PERMETTENDO PIÙ PERMISSION TUPLE PER OGNI RISORSA
////    }
////
////    /**
////     * Download, decrypt and check the signature of
////     * the content of the resource [resourceName]
////     * and return it along with the outcome code (if an
////     * error occurred, the content of the resource will
////     * be null)
////     *
////     * In this implementation, the first invocation of
////     * this function allows subscribing to the topic with
////     * the given [resourceName], while subsequent invocations
////     * allow retrieving the latest messages -- i.e., the messages
////     * still to be downloaded by the user -- published in the
////     * topic with the given [resourceName]
////     */
////    override fun readResource(resourceName: String): CodeResource {
////        logger.info {
////            "User ${user.name} is asking to subscribe " +
////            "or read messages published to topic $resourceName"
////        }
////
////        /** Guard clauses */
////        if (resourceName.isBlank()) {
////            logger.warn { "Topic name is blank" }
////            return CodeResource(CODE_020_INVALID_PARAMETER)
////        }
////
////        /** Lock the status of the services */
////        val code = startOfMethod(mmLock = false, acLock = false)
////        if (code != CODE_000_SUCCESS) {
////            return CodeResource(code)
////        }
////
////        // TODO here we should also match MQTT topic wildcards (i.e., '+' and '#')
////        // TODO we should test this behaviour (i.e., the download of MQTT messages through the READ RESOURCE api)
////        return if (subscribedTopicsKeysAndMessages.containsKey(resourceName)) {
////            val stringMessages = myJson.encodeToString(
////                value = subscribedTopicsKeysAndMessages[resourceName]!!.messages
////            ).inputStream()
////            subscribedTopicsKeysAndMessages[resourceName]!!.messages.clear()
////            CodeResource(
////                code = endOfMethod(
////                    code = CODE_000_SUCCESS,
////                    mmLocked = false,
////                    acLocked = false
////                ),
////                stream = stringMessages
////            )
////        } else {
////            val readCode = dm.readResource(resourceName)
////            CodeResource(
////                code = endOfMethod(
////                    code = readCode.code,
////                    mmLocked = false,
////                    acLocked = false
////                ),
////                stream = myJson.encodeToString(
////                    value = listOf<DecryptedMQTTMessage>()
////                ).inputStream()
////            )
////        }
////    }
////
////    /**
////     * Encrypt, sign and upload the new [resourceContent]
////     * for the resource [resourceName]. Finally, return
////     * the outcome code
////     *
////     * In this implementation, check whether the client
////     * is subscribed to the topic with name [resourceName].
////     * If so, check the enforcement and eventually
////     * use the cached key to encrypt the message, then
////     * publish. Otherwise, fetch the enforcement
////     * and eventually the key from the MM.
////     */
////    override fun writeResource(
////        resourceName: String,
////        resourceContent: InputStream
////    ): OutcomeCode {
////        /**
////         * Synchronize to avoid interference
////         * with the messageArrived function
////         * and with the receival of different
////         * messages (e.g., two key update
////         * messages)
////         */
////        return runBlocking {
////            messageArrivedMutex.withLock {
////
////                logger.info { "Publishing a message in topic $resourceName by user ${user.name}" }
////
////                /** Guard clauses */
////                if (resourceName.isBlank()) {
////                    logger.warn { "Topic name is blank" }
////                    return@withLock CODE_020_INVALID_PARAMETER
////                }
////
////                /** Lock the status of the services */
////                val code = startOfMethod(acLock = false)
////                if (code != CODE_000_SUCCESS) {
////                    return@withLock code
////                }
////
////                val resource = mm.getResources(
////                    resourceName = resourceName,
////                    isAdmin = user.isAdmin
////                ).firstOrNull()
////                if (resource == null) {
////                    logger.warn {
////                        "Resource not found. Either user ${user.name} does not have " +
////                        "access to topic $resourceName or topic does not exist"
////                    }
////                    return@withLock endOfMethod(
////                        code = CODE_006_RESOURCE_NOT_FOUND,
////                        acLocked = false
////                    )
////                }
////
////                val enforcement = if (subscribedTopicsKeysAndMessages[resourceName]?.retainedMessage != null) {
////                    /**
////                     * It means that we are publishing to a topic while
////                     * being subscribed and after having received the
////                     * retained message
////                     */
////                    logger.debug { "Getting enforcement from cache" }
////                    latestSymKeyVersionNumber =
////                        subscribedTopicsKeysAndMessages[resourceName]!!
////                            .retainedMessage!!.latestSymKeyVersionNumber
////                    subscribedTopicsKeysAndMessages[resourceName]!!
////                        .retainedMessage!!.enforcement
////                } else {
////
////                    /**
////                     * It means that we are publishing to a topic without
////                     * being subscribed. Therefore, we have to check
////                     * every time whether the key of the topic is still
////                     * up-to-date
////                     */
////                    logger.debug { "Getting enforcement from MM" }
////                    resource.enforcement
////                }
////
////                return@withLock when (enforcement) {
////                    /** MQTT messages should not be encrypted, i.e., just publish it */
////                    Enforcement.TRADITIONAL -> {
////                        logger.debug { "Enforcement is TRADITIONAL, no need to encrypt" }
////                        endOfMethod(
////                            code = dm.writeResource(
////                                updatedResource = Resource(
////                                    name = resourceName,
////                                    enforcement = enforcement
////                                ),
////                                content = resourceContent
////                            ),
////                            acLocked = false
////                        )
////                    }
////                    /** MQTT messages should be encrypted, i.e., get the key to do it */
////                    Enforcement.COMBINED -> {
////                        logger.debug { "Enforcement is COMBINED, encrypt the message" }
////                        val codeAndKey = if (subscribedTopicsKeysAndMessages[resourceName]?.retainedMessage != null) {
////                            logger.debug { "Getting key from cache" }
////                            CodeSymmetricKey(
////                                key = subscribedTopicsKeysAndMessages[resourceName]!!.key,
////                            )
////                        } else {
////                            logger.debug { "Getting key from MM" }
////                            getEncSymmetricKey(resource = resource)
////                        }
////
////                        if (codeAndKey.code != CODE_000_SUCCESS) {
////                            endOfMethod(
////                                code = codeAndKey.code,
////                                acLocked = false
////                            )
////                        } else {
////                            val messageStream = cryptoSKE.encryptStream(
////                                encryptingKey = codeAndKey.key!!,
////                                stream = resourceContent
////                            )
////                            endOfMethod(
////                                code = dm.writeResource(
////                                    updatedResource = Resource(
////                                        name = resourceName,
////                                        enforcement = enforcement
////                                    ),
////                                    content = messageStream
////                                ),
////                                acLocked = false
////                            )
////                        }
////                    }
////                }
////            }
////        }
////    }
////
////
////
////    /**
////     * Given an MQTT [message] for a given [topic], send it to
////     * the client through WSS. If the client is not connected
////     * to the WSS, cache the message to send it later.
////     */
////        private fun cacheOrSendMessage(
////            topic: String,
////            message: DecryptedMQTTMessage
////        ) {
////        if (wss == null) {
////            logger.info {(
////                "User ${user.name} is not connected "
////                + "through WSS, caching the message"
////            )}
////            subscribedTopicsKeysAndMessages[topic]!!.messages.add(message)
////            // TODO we cached the message, now check for reconnection
////            //  from the WSS to then send to it the messages
////        } else {
////            runBlocking {
////                logger.info { "Sending the message to the client through WSS" }
////                wss!!.send(myJson.encodeToString(message))
////            }
////        }
////    }
////
////
////
////    // TODO log doc refactor
////    /**
////     * This method is called when a [message] arrives from the server in the [topic]
////     *
////     * This method is invoked synchronously by the MQTT client. An acknowledgment is
////     * not sent back to the server until this method returns cleanly
////     *
////     * If an implementation of this method throws an `Exception`, then
////     * the client will be shut down. When the client is next re-connected, any QoS 1
////     * or 2 messages will be redelivered by the server
////     *
////     * Any additional messages which arrive while an implementation of this method
////     * is running, will build up in memory, and will then back up on the network
////     *
////     * If an application needs to persist data, then it should ensure the data is
////     * persisted prior to returning from this method, as after returning from this
////     * method, the [message] is considered to have been delivered, and will not be
////     * reproducible
////     *
////     * It is possible to send a new message within an implementation of this
////     * callback (for example, a response to this [message]), but the implementation
////     * must not disconnect the client, as it will be impossible to send an
////     * acknowledgment for the [message] being processed, and a deadlock will occur
////     *
////     * In this implementation, TODO
////     */
////    override fun messageArrived(
////        topic: String,
////        message: MqttMessage
////    ) {
////            QUI ABBIAMO CAMBIATO LA GESTIONE DEI MESSAGGI MQTT, VEDI PER ESEMPIO COME E'
////            IMPLEMENTATA LA FUNZIONE "override fun messageArrived(" IN DMServiceMQTT
////        /**
////         * Synchronize to avoid interference due to
////         * the receival of multiple MQTT messages
////         */
////        runBlocking {
////            messageArrivedMutex.withLock {
////
////                try {
////                    logger.info {
////                        "Topic $topic (user ${user.name}): new message (" +
////                        (if (!message.isRetained) "not " else "") +
////                        "retained)"
////                    }
////
////                    if (topic == ACServiceRBACDynSec.dynsecTopicResponse) {
////                        ac?.responsesReceivedFromDynSec?.add(String(message.payload))
////                    } else {
////
////                        /**
////                         * The message is from the admin, and it contains
////                         * the (latest) version number of the symmetric key
////                         */
////                        if (message.isRetained) {
////
////                            // TODO verify the signature of the message and that the admin sent it
////                            if (message.payload.isEmpty()) {
////                                logger.info { "The payload is empty (perhaps the topic is being deleted?)" }
////                                dm.client.unsubscribe(topic)
////                            } else {
////                                val resource = myJson.decodeFromString<Resource>(String(message.payload))
////                                val versionNumber = resource.latestSymKeyVersionNumber
////                                val enforcement = resource.enforcement
////
////                                if (topic != resource.name) {
////                                    // TODO this is error (replay attack?)
////                                    logger.error {
////                                        """
////                                    this is error (replay attack)
////                                    """.trimIndent()
////                                    }
////                                    throw java.lang.Exception("sytedgew")
////                                    // TODO decide what to do (in any case, stop here the execution)
////                                }
////
////                                logger.info {
////                                    "Retained message (versionNumber " +
////                                    "$versionNumber, enforcement $enforcement)"
////                                }
////
////                                when (enforcement) {
////                                    Enforcement.COMBINED -> {
////                                        logger.info { "Enforcement is combined" }
////
////                                        /** Whether there is a new symmetric key for the resource */
////                                        var haveToFetchNewKey = false
////
////                                        /**
////                                         * This is not the first retained message for this
////                                         * topic that the user receives. Probably, it is
////                                         * an update of the symmetric key. We check that
////                                         * the enforcement is the same as before (i.e.,
////                                         * [Enforcement.COMBINED] and that the version
////                                         * number is greater than the previous one.
////                                         */
////                                        if (subscribedTopicsKeysAndMessages[topic]?.retainedMessage != null) {
////                                            logger.info { "Update of cached key" }
////
////                                            val cachedEnforcement = subscribedTopicsKeysAndMessages[topic]!!.retainedMessage!!.enforcement
////                                            val cachedVersionNumber = subscribedTopicsKeysAndMessages[topic]!!.retainedMessage!!.latestSymKeyVersionNumber
////
////                                            if (cachedEnforcement != Enforcement.COMBINED) {
////                                                // TODO this is wrong, unless we allow the admin to dynamically
////                                                //  change the security level of a topic
////                                                logger.error {
////                                                    """
////                                            this is wrong, unless we allow the admin to dynamically
////                                            change the security level of a topic
////                                            """.trimIndent()
////                                                }
////                                                throw java.lang.Exception("asdfg")
////                                                // TODO decide what to do (in any case, stop here the execution)
////                                            }
////
////                                            if (cachedVersionNumber < versionNumber) {
////                                                logger.warn {
////                                                    "Resource $topic: change version number from" +
////                                                    "$cachedVersionNumber to $versionNumber; getting new key"
////                                                }
////                                                // TODO zero (i.e., delete from memory) the secret key
////                                                subscribedTopicsKeysAndMessages[topic]!!.retainedMessage = resource
////                                                haveToFetchNewKey = true
////                                            } else if (cachedVersionNumber > versionNumber) {
////                                                // TODO this is wrong
////                                                logger.error {
////                                                    """
////                                            this is wrong
////                                            """.trimIndent()
////                                                }
////                                                throw java.lang.Exception("asdfghjntr")
////                                                // TODO decide what to do (in any case, stop here the execution)
////                                            } else {
////                                                /** This means that cachedVersionNumber == versionNumber */
////                                                // TODO this is warning (perhaps reconnect?)
////                                                logger.error {
////                                                    """
////                                            this is warning (perhaps reconnect)
////                                            """.trimIndent()
////                                                }
////                                                throw java.lang.Exception("aqwertgjtrs")
////                                                // TODO decide what to do (in any case, stop here the execution)
////                                            }
////
////                                            logger.info { "Notification of a new symmetric key" }
////                                        }
////                                        /**
////                                         * Probably, the user just subscribed to the topic. As
////                                         * such, add the topic in the [subscribedTopicsKeysAndMessages]
////                                         * variable and proceed to get the symmetric key
////                                         */
////                                        else {
////                                            logger.info { "Probably just subscribed, fetch the key" }
////                                            haveToFetchNewKey = true
////                                            if (subscribedTopicsKeysAndMessages[topic] == null) {
////                                                subscribedTopicsKeysAndMessages[topic] = SymmetricKeysAndCachedMessages(
////                                                    retainedMessage = resource
////                                                )
////                                            } else {
////                                                subscribedTopicsKeysAndMessages[topic]!!.retainedMessage = resource
////                                            }
////                                        }
////
////                                        /** If we need to fetch the new key */
////                                        if (haveToFetchNewKey) {
////
////                                            logger.info { "We need to fetch key with version number $versionNumber" }
////
////                                            /**
////                                             * Lock the MM. If an error occurs, send an
////                                             * error message with the code to the client
////                                             * TODO do what with the key or eventual new messages?
////                                             */
////                                            val lockCode = startOfMethod(
////                                                dmLock = false,
////                                                acLock = false
////                                            )
////                                            if (lockCode != CODE_000_SUCCESS) {
////                                                logger.warn { "Could not lock ($lockCode)" }
////                                                val lockMessage = DecryptedMQTTMessage(
////                                                    message = lockCode.toString(),
////                                                    topic = topic,
////                                                    error = true
////                                                )
////                                                cacheOrSendMessage(
////                                                    topic = topic,
////                                                    message = lockMessage
////                                                )
////                                            }
////
////                                            /**
////                                             * Get the key. If an error occurs, send an
////                                             * error message with the code to the client
////                                             * TODO do what with the key or eventual new messages?
////                                             */
////                                            val symKey = getSymmetricKey(
////                                                resourceName = topic,
////                                                symKeyVersionNumber = versionNumber
////                                            )
////                                            if (symKey.code != CODE_000_SUCCESS) {
////                                                logger.warn { "Error while retrieving the key (${symKey.code})" }
////
////                                                val errorMessage = DecryptedMQTTMessage(
////                                                    message = symKey.code.toString(),
////                                                    topic = topic,
////                                                    error = true
////                                                )
////                                                cacheOrSendMessage(
////                                                    topic = topic,
////                                                    message = errorMessage
////                                                )
////                                            } else {
////                                                logger.info {
////                                                    "Key fetch operation was successful"
////                                                }
////                                                subscribedTopicsKeysAndMessages[topic]!!.symKey = symKey.key
////                                                logger.info {
////                                                    "Key fetch operation was " +
////                                                            "successful; now decrypt " +
////                                                            "eventual messages left over"
////                                                }
////
////                                                val currentVersionNumber = subscribedTopicsKeysAndMessages[topic]!!.retainedMessage!!.latestSymKeyVersionNumber
////                                                logger.info {
////                                                    subscribedTopicsKeysAndMessages[topic]!!.messagesToDecrypt.size
////                                                    " messages to decrypt"
////                                                }
////                                                val iterator = subscribedTopicsKeysAndMessages[topic]!!.messagesToDecrypt.iterator()
////                                                while (iterator.hasNext()) {
////                                                    val messageToDecrypt = iterator.next()
////                                                    if (messageToDecrypt.symKeyVersionNumber < currentVersionNumber) {
////                                                        TODO()
////                                                    } else if (messageToDecrypt.symKeyVersionNumber == currentVersionNumber) {
////                                                        val messageContent = cryptoSKE.decryptStream(
////                                                            decryptingKey = symKey.key!!,
////                                                            stream = messageToDecrypt.resourceContent.inputStream()
////                                                        ).readAllBytes()
////                                                        val receivedMessage = DecryptedMQTTMessage(
////                                                            message = String(messageContent),
////                                                            topic = topic,
////                                                            error = false
////                                                        )
////                                                        cacheOrSendMessage(
////                                                            topic = topic,
////                                                            message = receivedMessage
////                                                        )
////                                                        iterator.remove()
////                                                    } else {
////                                                        /** "messageToDecrypt.symKeyVersionNumber > currentVersionNumber" */
////                                                        logger.warn {
////                                                            "Message to decrypt has version number " +
////                                                                    messageToDecrypt.symKeyVersionNumber +
////                                                                    " that is higher then version number of key" +
////                                                                    currentVersionNumber +
////                                                                    "; delay decryption"
////                                                        }
////                                                    }
////                                                }
////                                            }
////
////                                            /**
////                                             * Unlock the mm. If an error occurs, send an
////                                             * error message with the code to the client
////                                             * TODO do what with the key or eventual new messages?
////                                             */
////                                            val unlockCode = endOfMethod(
////                                                code = symKey.code,
////                                                dmLocked = false,
////                                                acLocked = false
////                                            )
////                                            if (unlockCode != CODE_000_SUCCESS) {
////                                                val unlockMessage = DecryptedMQTTMessage(
////                                                    message = unlockCode.toString(),
////                                                    topic = topic,
////                                                    error = true
////                                                )
////                                                cacheOrSendMessage(
////                                                    topic = topic,
////                                                    message = unlockMessage
////                                                )
////                                            } else {
////                                                logger.debug { "Unlock successful" }
////                                            }
////                                        } else {
////                                            logger.debug { "We already have key with version number $versionNumber" }
////                                        }
////                                    }
////
////                                    Enforcement.TRADITIONAL -> {
////                                        logger.info { "Enforcement is traditional" }
////
////                                        /**
////                                         * This is not the first retained message for this
////                                         * topic that the user receives. In a topic with
////                                         * no cryptographic protection, this should not happen
////                                         */
////                                        // TODO not unless we allow the admin to dynamically
////                                        //  change the security level of a topic
////                                        if (subscribedTopicsKeysAndMessages[topic]?.retainedMessage != null) {
////                                            logger.error {
////                                                """
////                                        This is not the first retained message for this
////                                        topic that the user receives. In a topic with
////                                        no cryptographic protection, this should not happen
////                                        """.trimIndent()
////                                            }
////                                        }
////                                        /** Probably, the user just subscribed to the topic */
////                                        else {
////                                            if (subscribedTopicsKeysAndMessages[topic] == null) {
////                                                subscribedTopicsKeysAndMessages[topic] = SymmetricKeysAndCachedMessages(
////                                                    retainedMessage = resource
////                                                )
////                                            } else {
////                                                subscribedTopicsKeysAndMessages[topic]!!.retainedMessage = resource
////                                            }
////                                        }
////                                    }
////                                }
////                            }
////                        } else {
////                            val encryptedMQTTMessage = myJson.decodeFromString<EncryptedMQTTMessage>(String(message.payload))
////                            if (subscribedTopicsKeysAndMessages[topic]?.retainedMessage != null) {
////
////                                val cachedKey = subscribedTopicsKeysAndMessages[topic]!!
////                                val cachedResource = cachedKey.retainedMessage!!
////                                val cachedSymKeyVersionNumber = cachedResource.latestSymKeyVersionNumber
////
////                                if (encryptedMQTTMessage.symKeyVersionNumber != cachedSymKeyVersionNumber) {
////                                    // TODO this is error
////                                    logger.error {
////                                        """
////                                    this is error (encryptedMQTTMessage.symKeyVersionNumber != cachedSymKeyVersionNumber)
////                                    (${encryptedMQTTMessage.symKeyVersionNumber}, $cachedSymKeyVersionNumber)
////                                    """.trimIndent()
////                                    }
////                                    throw java.lang.Exception("bhmkertsdc")
////                                    // TODO decide what to do (in any case, stop here the execution)
////                                }
////
////                                val messageContent = when (cachedResource.enforcement) {
////                                    Enforcement.TRADITIONAL -> {
////                                        logger.debug { "The message is not encrypted" }
////                                        encryptedMQTTMessage.resourceContent
////                                    }
////
////                                    Enforcement.COMBINED -> {
////                                        logger.debug { "The message is encrypted" }
////                                        if (cachedKey.key == null) {
////                                            // TODO key retrieval (see below)
////                                            throw java.lang.Exception(
////                                                """
////                                        it means that the last retrieval of the
////                                        key went wrong, but in the meantime a message
////                                        arrived. what to do? I'd say either cache it
////                                        and try to get the key again OR send it to
////                                        the user even if encrypted
////                                        """.trimIndent()
////                                            )
////                                        } else {
////                                            cryptoSKE.decryptStream(
////                                                decryptingKey = cachedKey.key!!,
////                                                stream = encryptedMQTTMessage.resourceContent.inputStream()
////                                            ).readAllBytes()
////                                        }
////                                    }
////                                }
////
////                                val receivedMessage = DecryptedMQTTMessage(
////                                    message = String(messageContent),
////                                    topic = topic,
////                                    error = false
////                                )
////                                cacheOrSendMessage(
////                                    topic = topic,
////                                    message = receivedMessage
////                                )
////                            } else {
////                                logger.warn {
////                                    "Probably just subscribed, received normal " +
////                                            "message before retained message; put the " +
////                                            "normal message in the queue and wait for " +
////                                            "the retained message."
////                                }
////                                if (subscribedTopicsKeysAndMessages.containsKey(topic)) {
////                                    logger.warn {
////                                        "This is message number " +
////                                                "${subscribedTopicsKeysAndMessages[
////                                                    topic
////                                                ]!!.messagesToDecrypt.size} " +
////                                                "that we receive before the retained message"
////                                    }
////                                    subscribedTopicsKeysAndMessages[
////                                        topic
////                                    ]!!.messagesToDecrypt.add(
////                                        encryptedMQTTMessage
////                                    )
////                                } else {
////                                    subscribedTopicsKeysAndMessages[topic] = SymmetricKeysAndCachedMessages(
////                                        messagesToDecrypt = mutableListOf(
////                                            encryptedMQTTMessage
////                                        ),
////                                    )
////                                }
////                            }
////                        }
////                    }
////                } catch (e: Exception) {
////                    logger.error { "Exception in messageArrived MQTT callback function: ${e.localizedMessage}" }
////                    logger.error { e } // TODO delete
////                    val exceptionMessage = DecryptedMQTTMessage(
////                        message = CODE_049_UNEXPECTED.toString(),
////                        topic = topic,
////                        error = true
////                    )
////                    cacheOrSendMessage(
////                        topic = topic,
////                        message = exceptionMessage
////                    )
////                } // TODO do try catch also in the other functions of the callbacks
////            }
////        }
////    }
////
////    /**
////     * This method is called when the server gracefully disconnects
////     * from the client by sending a [disconnectResponse] packet, or
////     * when the TCP connection is lost due to a network issue or if
////     * the client encounters an error
////     *
////     * In this implementation, try to reconnect to the broker only
////     * if the error code is TODO
////     */
////    override fun disconnected(
////        disconnectResponse: MqttDisconnectResponse?
////    ) {
////        logger.warn {
////            "MQTT client for ${user.name} was disconnected: " +
////            disconnectResponse.toString()
////        }
////    }
////
////    /**
////     * Called when an AUTH packet is received by the client. The [reasonCode]
////     * can be Success (0), Continue authentication (24) or Re-authenticate (25),
////     * while the [properties] are the MqttProperties to be sent, containing the
////     * Authentication Method, Authentication Data and any required User
////     * Defined Properties
////     *
////     * In this implementation, just log the event
////     */
////    override fun authPacketArrived(
////        reasonCode: Int,
////        properties: MqttProperties?
////    ) {
////        logger.debug { "authPacketArrived" }
////    }
////
////    /**
////     * Called when the connection to the server is completed successfully.
////     * The [reconnect] value is true if the connection was the result of
////     * automatic reconnect. The [serverURI] is the URI of the server that
////     * the connection was made to
////     *
////     * In this implementation, just log the event
////     */
////    override fun connectComplete(
////        reconnect: Boolean,
////        serverURI: String?
////    ) {
////        logger.debug { "connectComplete (reconnect $reconnect, serverURI $serverURI)" }
////    }
////
////    /**
////     * Called when delivery for a message has been completed, and all
////     * acknowledgments have been received. For QoS 0 messages it is called once the
////     * message has been handed to the network for delivery. For QoS 1 it is called
////     * when PUBACK is received and for QoS 2 when PUBCOMP is received. The [token]
////     * will be the same [token] as that returned when the message was published
////     *
////     * In this implementation, just log the event
////     */
////    override fun deliveryComplete(
////        token: IMqttToken?
////    ) {
////        logger.debug { "deliveryComplete" }
////    }
////
////    /**
////     * This method is called when an [exception] is thrown within the MQTT client. The
////     * reasons for this may vary, from malformed packets, to protocol errors or even
////     * bugs within the MQTT client itself. This callback surfaces those errors to
////     * the application so that it may decide how best to deal with them
////     *
////     * For example, The MQTT server may have sent a publish message with an invalid
////     * topic alias, the MQTTv5 specification suggests that the client should
////     * disconnect from the broker with the appropriate return code, however this is
////     * completely up to the application itself
////     *
////     * In this implementation, just log the event
////     */
////    override fun mqttErrorOccurred(
////        exception: MqttException?
////    ) {
////        logger.warn { "mqttErrorOccurred: $exception" }
////    }
//}
