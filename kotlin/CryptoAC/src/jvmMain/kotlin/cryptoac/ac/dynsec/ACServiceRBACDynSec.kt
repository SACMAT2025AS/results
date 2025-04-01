package cryptoac.ac.dynsec

import cryptoac.Constants.ADMIN
import cryptoac.OutcomeCode
import cryptoac.OutcomeCode.*
import cryptoac.ac.ACServiceRBAC
import cryptoac.core.CoreParameters
import cryptoac.core.mqtt.CryptoACMQTTClient
import cryptoac.core.myJson
import cryptoac.encodeBase64
import cryptoac.code.CodeBoolean
import cryptoac.code.CodeServiceParameters
import cryptoac.code.CodeStrings
import cryptoac.tuple.TupleStatus
import cryptoac.tuple.Operation
import cryptoac.tuple.User
import cryptoac.waitForCondition
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashSet

private val logger = KotlinLogging.logger {}

/**
 * Class implementing the methods for invoking the APIs of the
 * Mosquitto DynSec plugin configured with the given [acServiceParameters]
 * and using the given MQTT [client] for RB-CAC. Note that this class can
 * act as MqttCallback as well, but it will only handle those MQTT messages
 * related to the DynSec plugin (i.e., the [dynsecTopicResponse] topic).
 */
class ACServiceRBACDynSec(
    private val acServiceParameters: ACServiceRBACDynSecParameters,
    var client: CryptoACMQTTClient
): ACServiceRBAC, MqttCallback {

    override var locks = 0

    /**
     * A list containing the commands
     * to send (i.e., commit) when unlocking
     */
    private val commands = mutableListOf<String>()

    /**
     * A list containing the responses
     * expected to be received from
     * DynSec after sending the [commands]
     */
    private val responses = mutableListOf<String>()

    /**
     * The topic where to send
     * commands for the DynSec plugin
     */
    private val dynsecTopic = "\$CONTROL/dynamic-security/v1"

    /**
     * Messages received after sending
     * a command to the DynSec plugin
     */
    private val responsesReceivedFromDynSec = mutableListOf<String>()

    /** Mutex to synchronize the message arrived procedure */
    private val messageArrivedMutex = Mutex()

    companion object {

        /**
         * The topic where to receive
         * responses from the DynSec plugin
         */
        const val dynsecTopicResponse = "\$CONTROL/dynamic-security/v1/response"
    }



    /**
     * In this implementation, TODO
     */
    override fun alreadyConfigured(): CodeBoolean {
        TODO("to implement")
    }

    /**
     * In this implementation, no configuration is needed,
     * therefore subsequent invocations are allowed
     */
    override fun configure(
        parameters: CoreParameters?
    ): OutcomeCode {
        logger.info { "No configuration needed for RBAC DYNSEC" }
        return CODE_000_SUCCESS
    }

    /**
     * In this implementation, subscribe to the topic in
     * which DynSec sends responses (if the user is admin)
     */
    override fun init() {
        val initCode = if (acServiceParameters.username == ADMIN) {
            client.mySubscribe(
                topicFilter = dynsecTopicResponse,
                qos = 2,
                isTheDM = false,
            )
        } else {
            CODE_000_SUCCESS
        }
        if (initCode != CODE_000_SUCCESS) {
            val message = "Could not initialize ACServiceRBACDynSec (code $initCode)}"
            logger.error { message }
            throw IllegalStateException(message)
        }
    }

    /**
     * In this implementation, clear the lists of [commands] and
     * [responses], unsubscribe from all topics and disconnect from
     * the MQTT broker
     */
    override fun deinit() {
        try {
            commands.clear()
            responses.clear()
            if (client.isConnected) {
                client.unsubscribe("#")
                client.disconnect()
            }
        } catch (e: MqttException) {
            logger.warn { "Exception while de-initializing MQTT client (${e.message})" }
            logger.warn { e }
            client.disconnectForcibly()
        }
    }

    /**
     * In this implementation, just check that the admin name
     * is the expected one; the admin as user, as role and the
     * assignment between the admin user and the admin role
     * were already created in the DynSec plugin
     * through the mosquitto.conf file
     */
    override fun addAdmin(
        newAdmin: User
    ): OutcomeCode {
        /** Guard clauses */
        if (newAdmin.name != ADMIN) {
            logger.warn {
                "Admin user has name ${newAdmin.name}" +
                ", but admin name should be $ADMIN"
            }
            return CODE_036_ADMIN_NAME
        }

        return CODE_000_SUCCESS
    }

    override fun initUser(
        user: User
    ): OutcomeCode {
        logger.info { "No action required to initialize user" }
        return CODE_000_SUCCESS
    }

    /** In this implementation, add the [newUser] to U */
    override fun addUser(
        newUser: User
    ): CodeServiceParameters {
        val username = newUser.name

        /** Guard clauses */
        if (username.isBlank() ) {
            logger.warn { "Username is blank" }
            return CodeServiceParameters(CODE_020_INVALID_PARAMETER)
        }
        if (username == ADMIN) {
            /** Mosquitto.conf already creates the admin user */
            return CodeServiceParameters(CODE_001_USER_ALREADY_EXISTS)
        }


        logger.info { "Adding the user $username in Mosquitto DynSec" }

        /** TODO check password generation */
        val passwordBytes = ByteArray(20)
        Random().nextBytes(passwordBytes)
        val newPassword = passwordBytes.encodeBase64()

        commands.add(
            getCreateClientCommand(
                username = username,
                password = newPassword
            )
        )
        responses.add("createClient")

        return CodeServiceParameters(
            serviceParameters = ACServiceRBACDynSecParameters(
                username = username,
                password = newPassword,
                port = acServiceParameters.port,
                url = acServiceParameters.url,
                tls = acServiceParameters.tls
            )
        )
    }

    override fun getUsers(
        username: String?,
        status: TupleStatus?,
        isAdmin: Boolean,
        offset: Int,
        limit: Int
    ): HashSet<User> {
        // TODO rivedere l'implementazione (e.g., non usa i parametri formali forniti) e il valore di ritorno di questa funzione
        val getUsersCommand = """
                {
                    "command": "listClients"
                }
            """.trimIndent()
        sendDynsecCommand(listOf(getUsersCommand))
        val users = hashSetOf<User>()
        runBlocking {
            if (waitForCondition { responsesReceivedFromDynSec.size == 1 }) {
                val dynsecResponse = responsesReceivedFromDynSec.first()
                val dynsecResponseJson: DynSecListUsersResponse = myJson.decodeFromString(dynsecResponse)
                for (user in dynsecResponseJson.responses.first().data.clients) {
                    if (username == null || username == user)
                        users.add(User(name = user))
                }
                responsesReceivedFromDynSec.clear()
            } else {
                // TODO something is wrong, mosquitto broker is not reachable?
            }
        }
        return users
    }

    /**
     * In this implementation, delete [username] from U
     * and [username]'s assignments from UR
     */
    override fun deleteUser(
        username: String
    ): OutcomeCode {

        /** Guard clauses */
        if (username.isBlank()) {
            logger.warn { "Username is blank" }
            return CODE_020_INVALID_PARAMETER
        }
        if (username == ADMIN) {
            logger.warn { "Cannot delete the $ADMIN user" }
            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
        }


        commands.add(
            getDeleteClientCommand(
                username = username
            )
        )
        responses.add("deleteClient")
        return CODE_000_SUCCESS
    }



    override fun addRole(
        roleName: String
    ): OutcomeCode {

        /** Guard clauses */
        if (roleName.isBlank()) {
            logger.warn { "Role name is blank" }
            return CODE_020_INVALID_PARAMETER
        }
        /** Mosquitto.conf already creates the admin role */
        if (roleName == ADMIN) {
            return CODE_000_SUCCESS
        }

        commands.add(
            getCreateRoleCommand(
                roleName = roleName
            )
        )
        responses.add("createRole")
        return CODE_000_SUCCESS
    }

    override fun deleteRole(
        roleName: String
    ): OutcomeCode {

        /** Guard clauses */
        if (roleName.isBlank()) {
            logger.warn { "Role name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        commands.add(
            getDeleteRoleCommand(
                roleName = roleName
            )
        )
        responses.add("deleteRole")
        return CODE_000_SUCCESS
    }

    /**
     * In this implementation, there is no
     * need to add [resourceName] to P, as
     * the DynSec plugin does not have the
     * concept of resource (it has topics)
     */
    override fun addResource(
        resourceName: String
    ): OutcomeCode {
        return CODE_000_SUCCESS
    }

    /**
     * In this implementation, unfortunately,
     * the DynSec plugin of Mosquitto does
     * not allow to remove the ACL permission
     * for a topic without specifying the role.
     * Therefore, we need to get all roles,
     * check whether the role has any permission
     * on [resourceName] and then revoke that
     * permission
     */
    override fun deleteResource(
        resourceName: String
    ): OutcomeCode {

        /** Guard clauses */
        if (resourceName.isBlank()) {
            logger.warn { "Resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        val roles = getRoles()
        for (roleName in roles) {
            val rolePermissions = getRolePermissionsOnTopic(
                roleName = roleName,
                topicName = resourceName
            )
            if (rolePermissions.code != CODE_000_SUCCESS) {
                return rolePermissions.code
            } else if (rolePermissions.acls!!.isNotEmpty()) {
                val revokeCode = revokePermissionFromRole(
                    roleName = roleName,
                    resourceName = resourceName
                )
                if (
                    revokeCode != CODE_000_SUCCESS &&
                    revokeCode != CODE_042_PA_ASSIGNMENTS_NOT_FOUND
                    ) {
                    return revokeCode
                }
            }
        }

        return CODE_000_SUCCESS
    }

    override fun assignUserToRole(
        username: String,
        roleName: String
    ): OutcomeCode {
        logger.info { "Adding UR assignment $username-$roleName" }

        /** Guard clauses */
        if (username.isBlank() || roleName.isBlank()) {
            logger.warn { "User or role name is blank" }
            return CODE_020_INVALID_PARAMETER
        }
        /**
         * Mosquitto.conf already assigns
         * the admin user to the admin role
         */
        if (username == ADMIN && roleName == ADMIN) {
            return CODE_000_SUCCESS
        }
        /**
         * Mosquitto.conf already assigns
         * all permission to the admin user
         */
        if (username == ADMIN) {
            return CODE_000_SUCCESS
        }

        commands.add(
            getAddClientRoleCommand(
                username = username,
                roleName = roleName
            )
        )
        responses.add("addClientRole")
        return CODE_000_SUCCESS
    }

    override fun revokeUserFromRole(
        username: String,
        roleName: String
    ): OutcomeCode {
        logger.info { "Revoking UR assignment $username-$roleName" }

        /** Guard clauses */
        if (username.isBlank() || roleName.isBlank()) {
            logger.warn { "User or role name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        val existingAssignments = getUserAssignments(
            username = username
        )
        return if (existingAssignments.code != CODE_000_SUCCESS) {
            existingAssignments.code
        } else if (!existingAssignments.strings!!.contains(roleName)) {
            CODE_041_UR_ASSIGNMENTS_NOT_FOUND
        } else {
            commands.add(
                getRemoveClientRoleCommand(
                    username = username,
                    roleName = roleName
                )
            )
            responses.add("removeClientRole")
            CODE_000_SUCCESS
        }
    }

    override fun assignPermissionToRole(
        roleName: String,
        operation: Operation,
        resourceName: String
    ): OutcomeCode {
        logger.info { "Adding PA assignment $roleName-$operation-$resourceName" }

        /** Guard clauses */
        if (roleName.isBlank() || resourceName.isBlank()) {
            logger.warn { "Role or resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }
        /**
         * Mosquitto.conf already assigns
         * all permission to the admin role
         */
        if (roleName == ADMIN) {
            return CODE_000_SUCCESS
        }


        val permissions = computeACLTypesFromPermission(operation)
        permissions.forEach { aclType ->
            commands.add(
                getAddRoleACLCommand(
                    roleName = roleName,
                    dynSecAclType = aclType,
                    topicName = resourceName
                )
            )
            responses.add("addRoleACL")
        }
        return CODE_000_SUCCESS
    }

    override fun revokePermissionFromRole(
        roleName: String,
        resourceName: String
    ): OutcomeCode {
        logger.info { "Revoking PA assignment $roleName-$resourceName" }

        /** Guard clauses */
        if (roleName.isBlank() || resourceName.isBlank()) {
            logger.warn { "Role or resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        return removeRoleACL(
            roleName = roleName,
            resourceName = resourceName,
            permissionToKeep = null
        )
    }

    override fun updatePermissionToRole(
        roleName: String,
        newOperation: Operation,
        resourceName: String
    ): OutcomeCode {
        logger.info { "Update PA assignment $roleName-$newOperation-$resourceName" }

        /** Guard clauses */
        if (roleName.isBlank() || resourceName.isBlank()) {
            logger.warn { "Role or resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        return removeRoleACL(
            roleName = roleName,
            resourceName = resourceName,
            permissionToKeep = newOperation
        )
    }

    override fun canDo(
        username: String,
        operation: Operation,
        resourceName: String
    ): OutcomeCode = canDo(
            username = username,
            roleName = null,
            operation = operation,
            resourceName = resourceName
        )

    override fun canDo(
        username: String,
        roleName: String?,
        operation: Operation,
        resourceName: String,
    ): OutcomeCode {
        TODO("Not yet implemented")
        // TODO to implement, but not really needed.
        //  If implement, implement also tests in ACServiceRBACDynSecTest
    }



    /**
     * In this implementation, check whether the lists of [commands] and [responses]
     * are both empty. Then, check also whether the MQTT client is already
     * connected. If not, check whether the MQTT client is already connecting
     * (because, e.g., it was disconnected from the broker). If so, block
     * until the client gets connected. Otherwise, simply connect the client.
     * Remember that "client.connect()" is a blocking method that (should)
     * return only once the connection procedure completes
     */
    override fun lock(): OutcomeCode {
        return if (locks == 0) {
            logger.info { "Locking the status of the AC" }
            try {
                if (commands.isNotEmpty() || responses.isNotEmpty()) {
                    logger.warn { "A lock has been set but not released" }
                    CODE_031_LOCK_CALLED_IN_INCONSISTENT_STATUS
                } else {
                    locks++
                    client.connectSync()
                    CODE_000_SUCCESS
                }
            } catch (e: MqttException) {
                if (e.message?.contains("Not authorized") == true) {
                    logger.warn { "AC Mosquitto - access denied for user" }
                    CODE_063_ACCESS_DENIED_TO_AC
                } else if (e.message?.contains("Unable to connect to server") == true) {
                    logger.warn { "AC Mosquitto - connection timeout" }
                    CODE_047_AC_CONNECTION_TIMEOUT
                } else {
                    throw e
                }
            }
        } else if (locks > 0) {
            locks++
            logger.debug { "Increment lock number to $locks" }
            CODE_000_SUCCESS
        } else {
            logger.warn { "Lock number is negative ($locks)" }
            CODE_031_LOCK_CALLED_IN_INCONSISTENT_STATUS
        }
    }

    /**
     * In this implementation, clear the
     * lists of [commands] and [responses]
     */
    override fun rollback(): OutcomeCode {
        return if (locks == 1) {
            logger.info { "Rollback the status of the AC" }
            locks--
            commands.clear()
            responses.clear()
            CODE_000_SUCCESS
        } else if (locks > 1) {
            locks--
            logger.debug { "Decrement lock number to $locks" }
            CODE_000_SUCCESS
        } else {
            logger.warn { "AC rollback number is zero or negative ($locks)" }
            CODE_033_ROLLBACK_CALLED_IN_INCONSISTENT_STATUS
        }
    }

    /**
     * In this implementation, send the list of commands to the broker,
     * clear the list of [commands] and check that the expected [responses]
     * are received, then clear also the list of [responses]
     */
    override fun unlock(): OutcomeCode {
        return if (locks == 1) {
            logger.info { "Unlock the status of the AC" }
            locks--

            if (commands.isNotEmpty()) {
                val publishCode = sendDynsecCommand(commands)
                if (publishCode != CODE_000_SUCCESS) {
                    logger.warn { "Publish DynSec commands got error code $publishCode" }
                    commands.clear()
                    responses.clear()
                    return publishCode
                }

                /** Wait to receive the response from the DynSec plugin */
                val responsesCode = runBlocking error@{
                    if (waitForCondition { responsesReceivedFromDynSec.size == 1 }) {
                        /** A DynSec response is like:
                         * {
                         *     "responses":[
                         *         {
                         *             "command":"createRole"
                         *         },
                         *         {
                         *             "command":"createClient"
                         *         },
                         *         {
                         *             ...
                         *         }
                         *     ]
                         * }
                         */
                        val dynsecResponse = responsesReceivedFromDynSec.first()
                        val dynsecResponseJson: DynSecGenericResponse = myJson.decodeFromString(dynsecResponse)
                        for (response in dynsecResponseJson.responses) {
                            if (response.error != null) {
                                logger.warn {
                                    "AC Unlock failed (command \"${response.command}\" " +
                                    "has error message \"${response.error}\")"
                                }
                                val errorCode = getCodeFromError(
                                    command = response.command,
                                    error = response.error
                                )
                                // TODO some command went wrong
                                // TODO how to rollback?
                                return@error if (errorCode == CODE_049_UNEXPECTED) {
                                    CODE_034_UNLOCK_FAILED
                                } else {
                                    errorCode
                                }
                            } else if (responses[0] != response.command) {
                                logger.warn {
                                    "AC Unlock failed (expected response " +
                                    "${responses[0]}, received ${response.command})"
                                }
                                // TODO some command went wrong
                                // TODO how to rollback?
                                return@error CODE_034_UNLOCK_FAILED
                            } else {
                                logger.debug { "Found command ${response.command}, moving to the next one" }
                                responses.removeAt(0)
                            }
                        }

                        if (responses.isNotEmpty()) {
                            // TODO some command went wrong
                            // TODO how to rollback?
                            logger.warn {
                                "AC Unlock failed, set of responses is not empty " +
                                "but instead it contains the following responses:"
                            }
                            responses.forEachIndexed { index, response ->
                                logger.warn { "${index + 1}: \"$response\" " }
                            }
                            return@error CODE_034_UNLOCK_FAILED
                        } else {
                            return@error CODE_000_SUCCESS
                        }
                    } else {
                        // TODO something is wrong, mosquitto broker is not reachable?
                        // TODO how to rollback?
                        logger.warn { "Publish DynSec commands did not receive DynSec response" }
                        return@error CODE_034_UNLOCK_FAILED
                    }
                }
                commands.clear()
                responses.clear()
                responsesReceivedFromDynSec.clear()
                responsesCode
            } else {
                logger.info { "No commands to send" }
                CODE_000_SUCCESS
            }
        } else if (locks > 1) {
            locks--
            logger.debug { "Decrement lock number to $locks" }
            CODE_000_SUCCESS
        } else {
            logger.warn { "AC unlock number is zero or negative ($locks)" }
            CODE_032_UNLOCK_CALLED_IN_INCONSISTENT_STATUS
        }
    }



    /**
     * Remove all ACLs of [roleName] on [resourceName]
     * and keep only those corresponding to the given
     * [permissionToKeep]. If [permissionToKeep] is null,
     * remove all ACLs of [roleName] on [resourceName]
     */
    private fun removeRoleACL(
        roleName: String,
        resourceName: String,
        permissionToKeep: Operation?
    ): OutcomeCode {
        val existingPermissions = getRolePermissionsOnTopic(
            roleName = roleName,
            topicName = resourceName
        )
        return if (existingPermissions.code != CODE_000_SUCCESS) {
            existingPermissions.code
        } else if (existingPermissions.acls!!.isEmpty()) {
            CODE_042_PA_ASSIGNMENTS_NOT_FOUND
        } else {
            val newPermissions = permissionToKeep?.let { computeACLTypesFromPermission(it) } ?: listOf()
            val diffPermissionsToRemove = existingPermissions.acls.filterNot {
                newPermissions.contains(it)
            }
            val diffPermissionsToAssign = newPermissions.filterNot {
                existingPermissions.acls.contains(it)
            }
            diffPermissionsToRemove.forEach { aclType ->
                commands.add(
                    getRemoveRoleACLCommand(
                        roleName = roleName,
                        dynSecAclType = aclType,
                        topicName = resourceName
                    )
                )
                responses.add("removeRoleACL")
            }
            diffPermissionsToAssign.forEach { aclType ->
                commands.add(
                    getAddRoleACLCommand(
                        roleName = roleName,
                        dynSecAclType = aclType,
                        topicName = resourceName
                    )
                )
                responses.add("addRoleACL")
            }
            CODE_000_SUCCESS
        }
    }

    /** Return the set of roles in DynSec */
    fun getRoles(): HashSet<String> {
        val getRolesCommand = """
                {
                    "command": "listRoles"
                }
            """.trimIndent()
        sendDynsecCommand(listOf(getRolesCommand))

        val roles = hashSetOf<String>()
        runBlocking {
            if (waitForCondition { responsesReceivedFromDynSec.size == 1 }) {
                val dynsecResponse = responsesReceivedFromDynSec.first()
                val dynsecResponseJson: DynSecListRolesResponse = myJson.decodeFromString(dynsecResponse)
                for (role in dynsecResponseJson.responses.first().data.roles) {
                    roles.add(role)
                }
                responsesReceivedFromDynSec.clear()
            } else {
                // TODO something is wrong, mosquitto broker is not reachable?
            }
        }
        return roles
    }

    /**
     * Return the list of permissions
     * that [roleName] has on [topicName]
     */
    private fun getRolePermissionsOnTopic(
        roleName: String,
        topicName: String
    ): CodeDynSecAclType {
        val getACLsCommand = """
                {
                    "command": "getRole",
                    "rolename": "$roleName"
                }
            """.trimIndent()
        sendDynsecCommand(listOf(getACLsCommand))

        val rolePermissions = mutableListOf<DynSecAclType>()
        return runBlocking {
            if (waitForCondition { responsesReceivedFromDynSec.size == 1 }) {
                val dynsecResponse = responsesReceivedFromDynSec.first()
                val dynsecResponseJson: DynSecGetRoleResponse = myJson.decodeFromString(dynsecResponse)
                val dynsecResponseRole = dynsecResponseJson.responses.first()
                responsesReceivedFromDynSec.clear()
                if (dynsecResponseRole.error != null) {
                    CodeDynSecAclType(code = getCodeFromError(
                        command = "getRole",
                        error = dynsecResponseRole.error
                    ))
                } else {
                    for (acl in dynsecResponseRole.data!!.role.acls) {
                        if (acl.topic == topicName) {
                            rolePermissions.add(DynSecAclType.valueOf(acl.acltype))
                        }
                    }
                    CodeDynSecAclType(acls = rolePermissions)
                }

            } else {
                // TODO something is wrong, mosquitto broker is not reachable?
                throw Exception("TODO")
            }
        }
    }

    /**
     * Return the list of roles
     * assigned to [username]
     */
    private fun getUserAssignments(
        username: String,
    ): CodeStrings {
        val getACLsCommand = """
                {
                    "command": "getClient",
                    "username": "$username"
                }
            """.trimIndent()
        sendDynsecCommand(listOf(getACLsCommand))

        val userAssignments = hashSetOf<String>()
        return runBlocking {
            if (waitForCondition { responsesReceivedFromDynSec.size == 1 }) {
                val dynsecResponse = responsesReceivedFromDynSec.first()
                val dynsecResponseJson: DynSecGetClientResponse = myJson.decodeFromString(dynsecResponse)
                val dynsecResponseClient = dynsecResponseJson.responses.first()
                responsesReceivedFromDynSec.clear()
                if (dynsecResponseClient.error != null) {
                    CodeStrings(code = getCodeFromError(
                        command = "getClient",
                        error = dynsecResponseClient.error
                    ))
                } else {
                    for (role in dynsecResponseClient.data!!.client.roles) {
                        userAssignments.add(role.rolename)
                    }
                    CodeStrings(strings = userAssignments)
                }
            } else {
                // TODO something is wrong, mosquitto broker is not reachable?
                throw Exception("TODO")
            }
        }
    }

    /**
     * Get an outcome code corresponding
     * to the [error] message for the
     * given [command]
     */
    private fun getCodeFromError(
        command: String,
        error: String
    ): OutcomeCode {
        return when (error) {
            "Client already exists" -> CODE_001_USER_ALREADY_EXISTS
            "Role already exists" -> CODE_002_ROLE_ALREADY_EXISTS
            "Client not found" -> CODE_004_USER_NOT_FOUND
            "Role not found" -> CODE_005_ROLE_NOT_FOUND
            "ACL with this topic already exists" -> CODE_063_PA_ASSIGNMENTS_ALREADY_EXISTS
            "ACL not found" -> CODE_042_PA_ASSIGNMENTS_NOT_FOUND
            "Internal error" -> {
                if (command == "addClientRole") {
                    CODE_062_UR_ASSIGNMENTS_ALREADY_EXISTS
                } else {
                    CODE_084_UNSUPPORTED_DYNSEC_ERROR
                }
            }
            else -> {
                CODE_084_UNSUPPORTED_DYNSEC_ERROR
            }
        }
    }

    /**
     * Return the command for creating a client
     * with the given [username] and [password]
     */
    private fun getCreateClientCommand(
        username: String,
        password: String? = null
    ): String {
        logger.info { "Creating client $username in the MQTT Mosquitto broker" }

        return """
            {
                "command": "createClient", 
                "username": "$username"
                ${if (password != null) {
                    """, "password": "$password""""
                } else { 
                    ""
                }}
            }     
        """.trimIndent()
    }

    /**
     * Return the command for deleting the client
     * with the given [username]
     */
    private fun getDeleteClientCommand(
        username: String
    ): String {
        logger.info { "Deleting client $username from the MQTT Mosquitto broker" }
        return """
            {
                "command": "deleteClient", 
                "username": "$username"
            }     
        """.trimIndent()
    }

    /**
     * Return the command for creating a role
     * with the given [roleName]
     */
    private fun getCreateRoleCommand(
        roleName: String
    ): String {
        logger.info { "Creating role $roleName in the MQTT Mosquitto broker" }
        return """
            {
                "command": "createRole",
                "rolename": "$roleName"
            }
        """.trimIndent()
    }

    /**
     * Return the command for adding the client
     * [username] to the role [roleName]
     */
    private fun getAddClientRoleCommand(
        username: String,
        roleName: String
    ): String {
        logger.info { "Adding client $username to role $roleName in the MQTT Mosquitto broker" }
        return """
            {
                "command": "addClientRole",
                "username": "$username",
                "rolename": "$roleName"
            }
        """.trimIndent()
    }

    /**
     * Return the command for deleting the role
     * with the given [roleName]
     */
    private fun getDeleteRoleCommand(
        roleName: String
    ): String {
        logger.info { "Deleting role $roleName from the MQTT Mosquitto broker" }
        return """
            {
                "command": "deleteRole",
                "rolename": "$roleName"
            }
        """.trimIndent()
    }

    /**
     * Return the command for removing the client
     * [username] form the role [roleName]
     */
    private fun getRemoveClientRoleCommand(
        username: String,
        roleName: String
    ): String {
        logger.info { "Removing client $username from role $roleName in the MQTT Mosquitto broker" }
        return """
            {
                "command": "removeClientRole",
                "username": "$username",
                "rolename": "$roleName"
            }
        """.trimIndent()
    }

    /**
     * Return the command for adding the [dynSecAclType]
     * permission to the role [roleName] over the
     * topic [topicName]
     */
    private fun getAddRoleACLCommand(
        roleName: String,
        dynSecAclType: DynSecAclType,
        topicName: String
    ): String {
        logger.info {(
            "Adding to role $roleName ACL $dynSecAclType for "
            + "topic $topicName in the MQTT Mosquitto broker"
        )}
        return """
            {
                "command": "addRoleACL",
                "rolename": "$roleName",
                "acltype": "$dynSecAclType",
                "topic": "$topicName",
                "priority": -1,
                "allow": true
            }
        """.trimIndent()
    }

    /**
     * Return the command for removing the [dynSecAclType]
     * permission from the role [roleName] over the
     * topic [topicName]
     */
    private fun getRemoveRoleACLCommand(
        roleName: String,
        dynSecAclType: DynSecAclType,
        topicName: String
    ): String {
        logger.info {(
            "Removing role $roleName ACL $dynSecAclType for "
            + "topic $topicName in the MQTT Mosquitto broker"
        )}
        return """
            {
                "command": "removeRoleACL",
                "rolename": "$roleName",
                "acltype": "$dynSecAclType",
                "topic": "$topicName"
            }
        """.trimIndent()
    }

    /**
     * Send the list of [commands] to the DYNSEC plugin
     * and return the outcome code. Remember that DYNSEC
     * disconnects all MQTT clients involved in an update
     * of the AC policy. For instance, if you assign a
     * client to a role, that client will be disconnected
     * with an administrative action (RC 152).
     * Related issues:
     * - https://github.com/eclipse/mosquitto/issues/2474 (CLOSED)
     */
    private fun sendDynsecCommand(
        commands: List<String>
    ): OutcomeCode {
        return if (commands.isNotEmpty()) {
            logger.info { "Sending ${commands.size} dynsec commands to the MQTT Mosquitto broker" }
            val commandsBuilder = StringBuilder("""{ "commands": [""")
            var prefix = ""
            commands.forEach {
                commandsBuilder.append(prefix).append(it)
                prefix = ","
            }
            commandsBuilder.append("""] }""")
            logger.debug { "Sending to the MQTT Mosquitto broker command: $commandsBuilder " }
            val message = commandsBuilder.toString()
            if (message.contains("\"\"")) {
                CODE_019_MISSING_PARAMETERS
            } else {
                val messageMQTT = MqttMessage(message.toByteArray())
                messageMQTT.qos = 2

                try {
                    client.publishACMessage(
                        topic = dynsecTopic,
                        message = messageMQTT
                    )
                } catch (e: MqttException) {
                    if (e.message?.contains("Disconnect RC: 152") == true) {
                        logger.info { "The broker disconnected the client after DYNSEC update" }
                        CODE_000_SUCCESS
                    } else {
                        throw e
                    }
                }
            }
        } else {
            logger.warn{
                "Asked to send dynsec commands to the MQTT " +
                "Mosquitto broker but no commands were given"
            }
            CODE_000_SUCCESS
        }
    }

    /**
     * Map the given CryptoAC [operation]
     * to the Mosquitto DYNSEC permissions
     */
    private fun computeACLTypesFromPermission(
        operation: Operation
    ) = when (operation) {
            Operation.READ -> listOf(
                DynSecAclType.publishClientReceive,
                DynSecAclType.subscribePattern,
                DynSecAclType.unsubscribePattern
            )
            Operation.READWRITE -> listOf(
                DynSecAclType.publishClientReceive,
                DynSecAclType.publishClientSend,
                DynSecAclType.subscribePattern,
                DynSecAclType.unsubscribePattern
            )
            Operation.WRITE -> listOf(
                DynSecAclType.publishClientSend,
                DynSecAclType.subscribePattern,
                DynSecAclType.unsubscribePattern
            )
        }



    /**
     * In this implementation, add the message to [responsesReceivedFromDynSec].
     * If a message is received from a topic different than [dynsecTopicResponse],
     * throw an [IllegalStateException]
     */
    override fun messageArrived(
        topic: String,
        message: MqttMessage
    ) {
        /**
         * Synchronize to avoid interference due to
         * the receival of multiple MQTT messages
         */
        runBlocking {
            messageArrivedMutex.withLock {
                logger.info { "Topic $topic (user ${acServiceParameters.username}): new message" }
                if (topic == dynsecTopicResponse) {
                    responsesReceivedFromDynSec.add(String(message.payload))
                } else {
                    val eMessage = "Received message from topic different than $dynsecTopicResponse"
                    logger.error { eMessage }
                    throw IllegalStateException(eMessage)
                }
            }
        }
    }

    /** In this implementation, just log the event */
    override fun disconnected(
        disconnectResponse: MqttDisconnectResponse?
    ) {
        logger.warn {
            "MQTT client for ${acServiceParameters.username} was disconnected: " +
                    disconnectResponse.toString()
        }
    }

    /** In this implementation, just log the event */
    override fun authPacketArrived(
        reasonCode: Int,
        properties: MqttProperties?
    ) {
        logger.debug { "authPacketArrived" }
    }

    /** In this implementation, just log the event */
    override fun connectComplete(
        reconnect: Boolean,
        serverURI: String?
    ) {
        logger.debug { "connectComplete (reconnect $reconnect, serverURI $serverURI)" }
    }

    /** In this implementation, just log the event */
    override fun deliveryComplete(
        token: IMqttToken?
    ) {
        logger.debug { "deliveryComplete" }
    }

    /** In this implementation, just log the event */
    override fun mqttErrorOccurred(
        exception: MqttException?
    ) {
        logger.warn { "mqttErrorOccurred: $exception" }
    }
}