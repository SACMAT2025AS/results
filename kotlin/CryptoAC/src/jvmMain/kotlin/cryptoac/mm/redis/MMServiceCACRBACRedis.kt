//package cryptoac.mm.redis
//
//import cryptoac.Constants.ADMIN
//import cryptoac.OutcomeCode
//import cryptoac.OutcomeCode.*
//import cryptoac.core.CoreParameters
//import cryptoac.crypto.AsymKeysType
//import cryptoac.crypto.EncryptedAsymKeys
//import cryptoac.crypto.EncryptedSymKey
//import cryptoac.decodeBase64
//import cryptoac.encodeBase64
//import cryptoac.mm.MMServiceCACRBAC
//import cryptoac.mm.MMType
//import cryptoac.code.CodeServiceParameters
//import cryptoac.code.*
//import cryptoac.tuple.*
//import mu.KotlinLogging
//import redis.clients.jedis.*
//import redis.clients.jedis.exceptions.JedisAccessControlException
//import redis.clients.jedis.exceptions.JedisConnectionException
//import java.security.PublicKey
//import java.util.*
//import kotlin.collections.HashMap
//import kotlin.collections.HashSet
//
//private val logger = KotlinLogging.logger {}
//
//
///**
// * Class implementing the methods for invoking the APIs of a Redis
// * datastore configured with the given [mmRedisServiceParameters]
// * for RB-CAC
// *
// * Currently, Redis does not support multiple keys pointing to the
// * same value (see https://github.com/redis/redis/issues/2668)
// *
// * To regulate accesses to the metadata (e.g., the access control policy, the
// * identifiers of other users/roles/resources, ...), Redis 7.0 (the most recent
// * version at the moment, 03/2022) allows defining Access Control Lists (ACLs).
// * However, these ACLs do not have support for roles or groups. As such, each user
// * has to be assigned permissions on what commands she can execute and what
// * keys she can access individually. Therefore, we need to find the right
// * balance between the ACL complexity and the possibility that users can
// * "guess" the right key to access metadata they could not (keeping in mind that
// * this would only be a privacy problem, not a security problem). In the former
// * case, we would assign each user to all keys she can access, possibly resulting
// * in an explosion of the ACL size due to the number of keys in Redis. In the
// * latter case, we would assign each user to a small number of top-level keys
// * which (like in a hierarchy) implicitly allow access to a large number of sub-keys.
// * Still, users would need extra information (e.g., the role token) to derive the
// * actual key (e.g., the sub-key is composed as <top level key>:<role token>)
// * to fetch the data from.
// * We chose the second approach, so we keep the ACL simple and do not have to update
// * it every time we change the policy. In detail, we have some top-level keys that
// * are then complemented with specific information (e.g., a user token or a role name).
// * We have the following keys that contain extra information to identify other keys:
// * - [setOfUsersKey]: set of username:userToken of users in Redis (ADMIN ONLY)
// * - [setOfDeletedUsersKey]: set of username:userToken of deleted users in Redis (ADMIN ONLY)
// * - [setOfRolesKey]: set of roleName:roleToken of roles in Redis (ADMIN ONLY)
// * - [setOfDeletedRolesKey]: set of roleName:roleToken of deleted roles in Redis (ADMIN ONLY)
// * - [setOfResourcesKey]: set of resourceName:resourceToken of resources in Redis (ADMIN ONLY)
// * - [setOfDeletedResourcesKey]: set of resourceName:resourceToken of deleted resources in Redis (ADMIN ONLY)
// * - [userRoleListKeyPrefix]: top-level key prefix for extra information to identify the keys of user-role assignments:
// *   - [byUserAndRoleNameKeyPrefix]: set of, for each permission assigned to the user, the role name plus the token (ADMIN ONLY)
// *   - [byRoleNameKeyPrefix]: set of, for each user assigned to the role, the username plus the token (ADMIN ONLY)
// *   - [byUsernameKeyPrefix]: set of, for each role assigned to the user, the role name plus the token (ADMIN ONLY)
// *   - [byUserTokenKeyPrefix]: set of, for each role assigned to the user, the role name plus the token (USER WITH TOKEN CAN ACCESS)
// * - [rolePermissionListKeyPrefix]: top-level key prefix for extra information to identify the keys of role-permission assignments:
// *   - [byRoleAndResourceNameKeyPrefix]: set of, for each permission assigned to the role, the resource name plus the token (ADMIN ONLY)
// *   - [byResourceNameKeyPrefix]: set of, for each permission assigned to the resource, the role name plus the token (ADMIN ONLY)
// *   - [byRoleNameKeyPrefix]: set of, for each permission assigned to the role, the resource name plus the token (ADMIN ONLY)
// *   - [byRoleTokenKeyPrefix]: set of, for each permission assigned to the role, the resource name plus the token (USERS CAN ACCESS)
// *
// * Then, we save metadata on objects (i.e users, roles, resources, user-role assignments, role-permission assignments) under the following keys:
// * - [userObjectPrefix] + [byUserTokenKeyPrefix]: hashset of user (name, token, sig pub key, enc pub key, version number, isAdmin, status) (USERS CAN ACCESS)
// * - [roleObjectPrefix] + [byRoleTokenKeyPrefix]: hashset of role (name, token, sig pub key, enc pub key, version number, status) (USERS CAN ACCESS)
// * - [resourceObjectPrefix] + [byResourceTokenKeyPrefix]: hashset of resource (name, token, version number, threshold number, status, enforcement, encrypted sym key) (USERS CAN ACCESS)
// * - [userRoleObjectPrefix] + [byUserAndRoleTokenKeyPrefix]: hashset of user-role assignment (username, role name, encrypted sig key, encrypted ver key, encrypted enc key, encrypted dec key, user version number, role version number, signature) (USERS CAN ACCESS)
// * - [rolePermissionObjectPrefix] + [byRoleAndResourceTokenKeyPrefix]: hashset of role-permission assignment (role name, resource name, role token, resource token, encrypted sym key, role version number, resource version number, operation, signer, signature) (USERS CAN ACCESS)
// */
//class MMServiceCACRBACRedis(
//    private val mmRedisServiceParameters: MMServiceRedisParameters
//) : MMServiceCACRBAC {
//
//    // TODO protection from stored XSS attacks? sanitize user inputs?
//
//    override var locks = 0
//
//    /** The transaction connection to the Redis database */
//    private var transaction: Jedis? = null
//
//    /**
//     * The connection for modifying the
//     * Redis database during a transaction
//     */
//    private var jTransaction: Transaction? = null
//
//    /**
//     * The connection for querying the
//     * Redis database during a transaction
//     */
//    private var jQuery: Jedis? = null
//
//    /**
//     * Whether the transaction contains
//     * any command to execute (thus we need
//     * to finalize the transaction during
//     * the unlock phase)
//     */
//    private var transactionToExec: Boolean = false
//
//    // TODO add "listOfKeysToAdd" and all related mechanisms, like for the other cache
//
//    /**
//     * This is a list of keys that are going to be deleted in the current
//     * transaction (i.e., between a lock and an unlock/rollback operation.
//     * We need to keep this information, e.g., when deleting and then
//     * adding keys in the same transaction (otherwise we get errors like
//     * "this key already exists")
//     */
//    private var listOfKeysToDelete: MutableList<String> = mutableListOf()
//
//    /**
//     * This is a list of keys that are going to be renamed in the current
//     * transaction (i.e., between a lock and an unlock/rollback operation.
//     * We need to keep this information, e.g., when renaming a key and
//     * then fetching data from that key in the same transaction (otherwise
//     * we get errors like "this key does not exist"). Note that the key of
//     * the hashmap is the renamed key, the value is the original key
//     */
//    private var listOfKeysRenamed: HashMap<String, String> = hashMapOf()
//
//    /**
//     * This is a list of names unit elements that are going to
//     * be inserted in Redis in the current transaction (i.e., between a
//     * lock and an unlock/rollback operation. We need to remember this
//     * information when first inserting the elements and then checking
//     * their existence in the same transaction
//     */
//    private var listOfElementsToAdd: MutableList<String> = mutableListOf()
//
//    /**
//     * This is a map holding, for each key, an object represented as a map
//     * that is going to be added in the transaction (i.e., between a lock
//     * and an unlock/rollback operation. We need to keep this information,
//     * e.g., when adding and then looking for objects in the same transaction
//     * (otherwise we get errors like "this key does not exist"). Besides,
//     * this works as a cache to improve performance
//     */
//    private var mapOfValuesToAdd: HashMap<String, HashMap<String, String>> = hashMapOf()
//
//    /**
//     * This is a map holding, for each key, a set of strings that are going to
//     * be added in the transaction (i.e., between a lock and an unlock/rollback
//     * operation. We need to keep this information, e.g., when adding and then
//     * looking for objects in the same transaction (otherwise we get errors like
//     * "this key does not exist"). Besides, this works as a cache to improve
//     * performance
//     */
//    private var mapOfListsToAdd: HashMap<String, HashSet<String>> = hashMapOf()
//
//    /**
//     * This is a map holding, for each key, a set of strings that are going to
//     * be removed in the transaction (i.e., between a lock and an unlock/rollback
//     * operation. We need to keep this information, e.g., when removing and then
//     * checking the existence of delete values in the same transaction (otherwise,
//     * a value could seem to be present while instead it is going to be deleted)
//     */
//    private var mapOfListsToRemove: HashMap<String, HashSet<String>> = hashMapOf()
//
//    /** The threadsafe pool of network connections toward Redis */
//    private val pool = JedisPool(
//        JedisPoolConfig(),
//        mmRedisServiceParameters.url,
//        mmRedisServiceParameters.port
//    )
//
//    /**
//     * The username with which connect to Redis.
//     * "default" is the Redis default username
//     */
//    private val usernameRedis = if (mmRedisServiceParameters.username == ADMIN) {
//        "default"
//    } else {
//        mmRedisServiceParameters.username
//    }
//
//    private val usernameField = "u"
//    private val roleNameField = "rn"
//    private val isAdminField = "uia"
//    private val userTokenField = "ut"
//    private val roleTokenField = "rt"
//    private val asymEncPublicKeyField = "aepk"
//    private val asymSigPublicKeyField = "aspk"
//    private val resourceNameField = "fn"
//    private val resourceTokenField = "ft"
//    private val encryptedSymKeyField = "sk"
//    private val resourceVersionNumberField = "skvn"
//    private val operationField = "o"
//    private val signerTokenField = "sto"
//    private val reEncryptionThresholdNumberField = "retn"
//    private val enforcementField = "e"
//    private val statusField = "sta"
//    private val userVersionNumberField = "uvn"
//    private val roleVersionNumberField = "rvn"
//    private val encryptedAsymEncPublicKeyField = "eaepuk"
//    private val encryptedAsymEncPrivateKeyField = "eaeprk"
//    private val encryptedAsymSigPublicKeyField = "easpuk"
//    private val encryptedAsymSigPrivateKeyField = "easprk"
//    private val signatureField = "si"
//
//    /**
//     * Key to enable check-and-set (CAS) behavior
//     * to Redis transactions, i.e., we get the
//     * value of the key, execute the transaction
//     * only if the value has not changed in the
//     * meantime and finally change the value of
//     * the key ourselves
//     */
//    private val lockUnlockRollbackKey = "lurk" // TODO il watch potremmo farlo più fine-grained?
//
//    /**
//     * Key for the set of (incomplete or operational)
//     * users. Each element is composed of the username
//     * and the user token
//     */
//    private val setOfUsersKey = "iou"
//
//    /**
//     * Key for the set of (deleted) users. Each element
//     * is composed of the username and the user token
//     */
//    private val setOfDeletedUsersKey = "du"
//
//    /**
//     * Key for the set of (operational) roles. Each element
//     * is composed of the role name and the role token
//     */
//    private val setOfRolesKey = "or"
//
//    /**
//     * Key for the set of (deleted) roles. Each element
//     * is composed of the role name and the role token
//     */
//    private val setOfDeletedRolesKey = "dr"
//
//    /**
//     * Key for the set of (operational) resources. Each element
//     * is composed of the resource name and the resource token
//     */
//    private val setOfResourcesKey = "of"
//
//    /**
//     * Key for the set of (deleted) resources. Each element
//     * is composed of the resource name and the resource token
//     */
//    private val setOfDeletedResourcesKey = "df"
//
//    /** Delimited character for both keys and values */
//    private val dl = ":"
//
//    /** Prefix for user objects */
//    private val userObjectPrefix = "uo$dl"
//
//    /** Prefix for role objects */
//    private val roleObjectPrefix = "ro$dl"
//
//    /** Prefix for resource objects */
//    private val resourceObjectPrefix = "fo$dl"
//
//    /** Prefix for the keys of user-role assignments */
//    private val userRoleObjectPrefix = "rt$dl"
//
//    /** Prefix for the keys of lists of user-role assignments */
//    private val userRoleListKeyPrefix = "rtl$dl"
//
//    /** Prefix for the keys of role-permission assignments */
//    private val rolePermissionObjectPrefix = "pt$dl"
//
//    /** Prefix for the keys of lists of role-permission assignments */
//    private val rolePermissionListKeyPrefix = "ptl$dl"
//
//    /** Prefix for secondary indexes based on usernames */
//    private val byUsernameKeyPrefix = "bu$dl"
//
//    /** Prefix for secondary indexes based on user and role names */
//    private val byUserAndRoleNameKeyPrefix = "bur$dl"
//
//    /** Prefix for secondary indexes based on user and role tokens */
//    private val byUserAndRoleTokenKeyPrefix = "burt$dl"
//
//    /** Prefix for secondary indexes based on role names */
//    private val byRoleNameKeyPrefix = "br$dl"
//
//    /** Prefix for secondary indexes based on user tokens */
//    private val byUserTokenKeyPrefix = "but$dl"
//
//    /** Prefix for secondary indexes based on role tokens */
//    private val byRoleTokenKeyPrefix = "brt$dl"
//
//    /** Prefix for secondary indexes based on resource tokens */
//    private val byResourceTokenKeyPrefix = "bft$dl"
//
//    /** Prefix for secondary indexes based on role and resource names */
//    private val byRoleAndResourceNameKeyPrefix = "brf$dl"
//
//    /** Prefix for secondary indexes based on role and resource names */
//    private val byRoleAndResourceTokenKeyPrefix = "brff$dl"
//
//    /** Prefix for secondary indexes based on resource names */
//    private val byResourceNameKeyPrefix = "bf$dl"
//
//
//
//    /**
//     * In this implementation, check if
//     * the key [setOfUsersKey] (which gets
//     * added to Redis when the admin is
//     * initialized) is present
//     */
//    override fun alreadyConfigured(): CodeBoolean {
//        TODO("Not yet implemented")
//    }
//
//    /**
//     * In this implementation, no configuration is needed,
//     * therefore subsequent invocations are allowed
//     */
//    override fun configure(parameters: CoreParameters?): OutcomeCode {
//        logger.info { "No configuration needed for RBAC Redis" }
//        return CODE_000_SUCCESS
//    }
//
//    override fun init() {
//        logger.info { "No action required to initialize the MM RBAC Redis service" }
//    }
//
//    override fun deinit() {
//        logger.info { "No action required to de-initialize the MM RBAC Redis service" }
//    }
//
//    /**
//     * In this implementation, add the admin by adding
//     * the (key of the) user in the list of operational
//     * users. Finally, add the public keys of the user
//     * as an object with as key the token of the user,
//     * and the user as an object with as key the username
//     */
//    override fun addAdmin(newAdmin: User): OutcomeCode {
//        logger.info { "Adding the admin in the MM" }
//
//        /** Guard clauses */
//        if (newAdmin.name != ADMIN) {
//            logger.warn { "Admin user has name ${newAdmin.name}, but admin name should be $ADMIN" }
//            return CODE_036_ADMIN_NAME
//        }
//        if (getUsers(username = ADMIN).isNotEmpty()) {
//            logger.warn { "Admin $ADMIN already initialized" }
//            return CODE_035_ADMIN_ALREADY_INITIALIZED
//        }
//
//        val adminKeyByToken = "$userObjectPrefix$byUserTokenKeyPrefix${newAdmin.token}"
//        val asymEncPublicKeyEncoded = newAdmin.asymEncKeys!!.public
//        val asymSigPublicKeyEncoded = newAdmin.asymSigKeys!!.public
//
//        transactionToExec = true
//
//        /** Add the admin as user in the metadata */
//        logger.debug { "Adding the admin as user" }
//        val adminNameAndToken = concatenateNameAndToken(newAdmin.name, newAdmin.token)
//        val adminObject = hashMapOf(
//            usernameField to newAdmin.name,
//            userTokenField to newAdmin.token,
//            userVersionNumberField to "1",
//            asymEncPublicKeyField to asymEncPublicKeyEncoded,
//            asymSigPublicKeyField to asymSigPublicKeyEncoded,
//            statusField to TupleStatus.OPERATIONAL.toString(),
//            isAdminField to newAdmin.isAdmin.toString(),
//        )
//        jTransaction!!.saddCache(setOfUsersKey, adminNameAndToken)
//
//        jTransaction!!.hsetCache(adminKeyByToken, adminObject)
//        listOfElementsToAdd.add(ADMIN)
//
//        return CODE_000_SUCCESS
//    }
//
//    /**
//     * In this implementation, move the (key of the) user
//     * from the list of incomplete users to the list of
//     * operational users. Finally, update the public keys
//     * and the token of the user as an object with as key
//     * the name of the user, and save the user as an object
//     * with as key the token of the user
//     */
//    override fun initUser(
//        user: User
//    ): OutcomeCode {
//        val username = user.name
//        val userToken = if (mmRedisServiceParameters.username != username)
//            user.token
//        else
//            mmRedisServiceParameters.token
//
//
//        logger.info { "Initializing user $username with token $userToken in the metadata" }
//
//        val userKeyByToken = "$userObjectPrefix$byUserTokenKeyPrefix$userToken"
//
//        return when (getStatus(token = userToken, type = RBACElementType.USER)) {
//            null -> {
//                logger.warn { "User ${user.name} does not exist in the metadata" }
//                CODE_004_USER_NOT_FOUND
//            }
//            TupleStatus.DELETED -> {
//                logger.warn { "User $username was previously deleted" }
//                CODE_013_USER_WAS_DELETED
//            }
//            TupleStatus.OPERATIONAL -> {
//                logger.warn { "User $username already initialized" }
//                CODE_052_USER_ALREADY_INITIALIZED
//            }
//            TupleStatus.INCOMPLETE -> {
//                val asymEncPublicKeyEncoded = user.asymEncKeys!!.public
//                val asymSigPublicKeyEncoded = user.asymSigKeys!!.public
//
//                transactionToExec = true
//                val userObject = hashMapOf(
//                    userTokenField to userToken,
//                    asymEncPublicKeyField to asymEncPublicKeyEncoded,
//                    asymSigPublicKeyField to asymSigPublicKeyEncoded,
//                    statusField to TupleStatus.OPERATIONAL.toString(),
//                )
//                jTransaction!!.hsetCache(userKeyByToken, userObject)
//                listOfElementsToAdd.add(username)
//
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    /**
//     * In this implementation, the user's asymmetric
//     * encryption and signing public keys and token
//     * will be set by the user him/herself later on
//     * (initUser function)
//     */
//    override fun addUser(
//        newUser: User
//    ): CodeServiceParameters {
//        val username = newUser.name
//        val userToken = newUser.token
//
//        /** Guard clauses */
//        if (username.isBlank()) {
//            logger.warn { "Username is blank" }
//            return CodeServiceParameters(code = CODE_020_INVALID_PARAMETER)
//        }
//
//
//        logger.info {
//            "Adding the user $username with token " +
//            "$userToken in the metadata and as Redis user"
//        }
//
//        val userKeyByToken = "$userObjectPrefix$byUserTokenKeyPrefix$userToken"
//
//        return when (getStatus(name = username, type = RBACElementType.USER)) {
//            null -> {
//                transactionToExec = true
//                val userNameAndToken = concatenateNameAndToken(username, userToken)
//                val userObject = hashMapOf(
//                    usernameField to username,
//                    userTokenField to userToken,
//                    userVersionNumberField to "1",
//                    statusField to TupleStatus.INCOMPLETE.toString(),
//                    isAdminField to newUser.isAdmin.toString()
//                )
//                jTransaction!!.saddCache(setOfUsersKey, userNameAndToken)
//
//                jTransaction!!.hsetCache(userKeyByToken, userObject)
//                listOfElementsToAdd.add(username)
//
//                /** TODO check password generation */
//                val passwordBytes = ByteArray(20)
//                Random().nextBytes(passwordBytes)
//                val newPassword = passwordBytes.encodeBase64()
//
//                val success = jQuery!!.aclSetUser(
//                    username,
//                    "on",
//                    "+multi",
//                    "+exec",
//                    "+ping",
//                    "+discard",
//                    "(+WATCH ~$lockUnlockRollbackKey)",
//                    "(+UNWATCH ~$lockUnlockRollbackKey)",
//                    "(+INCR ~$lockUnlockRollbackKey)",
//                    "(+SMEMBERS ~$userRoleListKeyPrefix$byUserTokenKeyPrefix$userToken)",
//                    "(+SMEMBERS ~$rolePermissionListKeyPrefix$byRoleTokenKeyPrefix*)",
//                    "(+HGETALL ~$userObjectPrefix$byUserTokenKeyPrefix*)",
//                    "(+HSET ~$userObjectPrefix$byUserTokenKeyPrefix$userToken)",
//                    "(+HGETALL ~$roleObjectPrefix$byRoleTokenKeyPrefix*)",
//                    "(+HGETALL ~$resourceObjectPrefix$byResourceTokenKeyPrefix*)",
//                    "(+HGETALL ~$userRoleObjectPrefix$byUserAndRoleTokenKeyPrefix*)",
//                    "(+HGETALL ~$rolePermissionObjectPrefix$byRoleAndResourceTokenKeyPrefix*)",
//                    "(+HGET ~$userObjectPrefix$byUserTokenKeyPrefix*)",
//                    "(+HGET ~$roleObjectPrefix$byRoleTokenKeyPrefix*)",
//                    "(+HGET ~$resourceObjectPrefix$byResourceTokenKeyPrefix*)",
//                    "(+HGET ~$userRoleObjectPrefix$byUserAndRoleTokenKeyPrefix*)",
//                    "(+HGET ~$rolePermissionObjectPrefix$byRoleAndResourceTokenKeyPrefix*)",
//                    ">$newPassword"
//                ) == "OK"
//
//
//                if (success) {
//                    CodeServiceParameters(
//                        serviceParameters = MMServiceRedisParameters(
//                            username = username,
//                            password = newPassword,
//                            port = mmRedisServiceParameters.port,
//                            url = mmRedisServiceParameters.url,
//                            token = userToken,
//                            mmType = MMType.RBAC_REDIS
//                        )
//                    )
//                } else {
//                    logger.warn { "Could not create user $username in Redis" }
//                    CodeServiceParameters(CODE_054_CREATE_USER_MM)
//                }
//            }
//            TupleStatus.DELETED -> {
//                logger.warn { "User $username was previously deleted" }
//                CodeServiceParameters(CODE_013_USER_WAS_DELETED)
//            }
//            else -> {
//                logger.warn { "User $username already present in the metadata" }
//                CodeServiceParameters(CODE_001_USER_ALREADY_EXISTS)
//            }
//        }
//    }
//
//    /**
//     * In this implementation, change the status of the
//     * [username] to deleted, move the key to the list
//     * of deleted users and delete the user at Redis level
//     */
//    override fun deleteUser(username: String): OutcomeCode {
//
//        /** Guard Clauses */
//        if (username.isBlank()) {
//            logger.warn { "Username is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//        if (username == ADMIN) {
//            logger.warn { "Cannot delete the $ADMIN user" }
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }
//
//
//        logger.info { "Deleting user $username" }
//
//        return when (getStatus(name = username, type = RBACElementType.USER)) {
//            null -> {
//                CODE_004_USER_NOT_FOUND
//            }
//            TupleStatus.DELETED -> {
//                CODE_013_USER_WAS_DELETED
//            }
//            else -> {
//                val userToken = getToken(
//                    name = username,
//                    type = RBACElementType.USER
//                )!!
//                val elementNameAndToken = concatenateNameAndToken(username, userToken)
//                val userKeyByToken = "$userObjectPrefix$byUserTokenKeyPrefix$userToken"
//                logger.info { "Deleting user with token $userToken" }
//
//                transactionToExec = true
//                jTransaction!!.sremCache(
//                    key = setOfUsersKey,
//                    value = elementNameAndToken
//                )
//
//                jTransaction!!.saddCache(
//                    key = setOfDeletedUsersKey,
//                    value = elementNameAndToken
//                )
//
//                jTransaction!!.hsetCache(
//                    key = userKeyByToken,
//                    field = statusField,
//                    value = TupleStatus.DELETED.toString()
//                )
//
//                logger.debug { "Deleting the user from the Redis database" }
//                return if (jQuery!!.aclDelUser(username) == 1L) {
//                    CODE_000_SUCCESS
//                } else {
//                    CODE_056_DELETE_USER_MM
//                }
//            }
//        }
//    }
//
//
//
//    /**
//
//     * In this implementation, add the role as
//     * an object, plus add the key of the role
//     * in the list of OPERATIONAL roles. Finally,
//     * add the public keys of the role and the version
//     * number with as key the token of the role
//     */
//    override fun addRole(newRole: Role): OutcomeCode {
//        val roleName = newRole.name
//        val roleToken = newRole.token
//
//        /** Guard clauses */
//        if (roleName.isBlank()) {
//            logger.warn { "Role name is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//
//
//        logger.info { "Adding the role $roleName in the metadata" }
//
//        val roleKeyByToken = "$roleObjectPrefix$byRoleTokenKeyPrefix$roleToken"
//
//        return when (getStatus(name = roleName, type = RBACElementType.ROLE)) {
//            null -> {
//                val asymEncPublicKeyEncoded = newRole.asymEncKeys!!.public
//                val asymSigPublicKeyEncoded = newRole.asymSigKeys!!.public
//
//                transactionToExec = true
//                val roleNameAndToken = concatenateNameAndToken(roleName, roleToken)
//                val roleObject = hashMapOf(
//                    roleNameField to roleName,
//                    roleTokenField to roleToken,
//                    asymEncPublicKeyField to asymEncPublicKeyEncoded,
//                    asymSigPublicKeyField to asymSigPublicKeyEncoded,
//                    roleVersionNumberField to newRole.versionNumber.toString(),
//                    statusField to newRole.status.toString(),
//                )
//                jTransaction!!.saddCache(setOfRolesKey, roleNameAndToken)
//                jTransaction!!.hsetCache(roleKeyByToken, roleObject)
//                listOfElementsToAdd.add(roleName)
//
//                CODE_000_SUCCESS
//            }
//
//            TupleStatus.DELETED -> {
//                logger.warn { "Role $roleName was previously deleted" }
//                CODE_014_ROLE_WAS_DELETED
//            }
//            else -> {
//                logger.warn { "Role $roleName already present in the metadata" }
//                CODE_002_ROLE_ALREADY_EXISTS
//            }
//        }
//    }
//
//    /**
//     * In this implementation, add the resource as
//     * an object, plus add the key of the resource
//     * in the list of OPERATIONAL resources. Finally,
//     * add the symmetric encryption key version
//     * number of the resource with as key the token
//     * of the resource
//     */
//    override fun addResource(newResource: Resource): OutcomeCode {
//        val resourceName = newResource.name
//        val resourceToken = newResource.token
//
//        /** Guard clauses */
//        if (resourceName.isBlank()) {
//            logger.warn { "Resource name is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//
//
//        logger.info { "Adding the resource $resourceName in the metadata" }
//
//        val resourceKeyByToken = "$resourceObjectPrefix$byResourceTokenKeyPrefix$resourceToken"
//
//        return when (getStatus(name = resourceName, type = RBACElementType.RESOURCE)) {
//            null -> {
//                transactionToExec = true
//                val resourceNameAndToken = concatenateNameAndToken(resourceName, resourceToken)
//                val resourceObject = hashMapOf(
//                    resourceNameField to resourceName,
//                    resourceTokenField to resourceToken,
//                    resourceVersionNumberField to newResource.versionNumber.toString(),
//                    reEncryptionThresholdNumberField to newResource.reEncryptionThresholdNumber.toString(),
//                    statusField to newResource.status.toString(),
//                    enforcementField to newResource.enforcement.toString(),
//                )
//                newResource.encryptedSymKey?.key?.encodeBase64()?.let {
//                    resourceObject.put(
//                        key = encryptedSymKeyField,
//                        value = it
//                    )
//                }
//                jTransaction!!.saddCache(setOfResourcesKey, resourceNameAndToken)
//
//                jTransaction!!.hsetCache(resourceKeyByToken, resourceObject)
//                listOfElementsToAdd.add(resourceName)
//
//                CODE_000_SUCCESS
//            }
//            TupleStatus.DELETED -> {
//                logger.warn { "Resource $resourceName was previously deleted" }
//                CODE_015_RESOURCE_WAS_DELETED
//            }
//            else -> {
//                logger.warn { "Resource $resourceName already present in the metadata" }
//                CODE_003_RESOURCE_ALREADY_EXISTS
//            }
//        }
//    }
//
//    /**
//     * In this implementation, add the (key of the) user-role assignment
//     * in two lists, holding the list of user-role assignment for a
//     * given username and the list of user-role assignment of a given
//     * role name, respectively. Finally, add the user-role assignment
//     * with as key the combination of the username and the role name
//     */
//    override fun addUsersRoles(newUsersRoles: HashSet<UserRole>): OutcomeCode {
//        val size = newUsersRoles.size
//        if (size == 0) {
//            logger.warn { "No user-role assignments given" }
//            return CODE_000_SUCCESS
//        }
//
//        logger.info { "Adding $size user-role assignments to the metadata (one per row below):" }
//        newUsersRoles.forEachIndexed { index, userRole ->
//            logger.info {
//                "${index + 1}: user ${userRole.username} " +
//                    "to role ${userRole.roleName}"
//            }
//        }
//
//        var code = CODE_000_SUCCESS
//        run error@{
//            newUsersRoles.forEach { newUserRole ->
//
//                val username = newUserRole.username
//                val roleName = newUserRole.roleName
//
//                /**
//                 * Check that involved users exist, are
//                 * not incomplete or were not deleted,
//                 * and that involved roles exist and
//                 * were not deleted
//                 */
//                code = if (listOfElementsToAdd.contains(username)) {
//                    CODE_000_SUCCESS
//                } else {
//                    when (
//                        getStatus(name = username, type = RBACElementType.USER)
//                    ) {
//                        null -> {
//                            logger.warn { "USer $username was not found" }
//                            CODE_004_USER_NOT_FOUND
//                        }
//                        TupleStatus.DELETED -> {
//                            logger.warn { "USer $username was previously deleted" }
//                            CODE_013_USER_WAS_DELETED
//                        }
//                        TupleStatus.INCOMPLETE -> {
//                            logger.warn { "USer $username is incomplete" }
//                            CODE_053_USER_IS_INCOMPLETE
//                        }
//                        else ->
//                            CODE_000_SUCCESS
//                    }
//                }
//                if (code != CODE_000_SUCCESS) {
//                    return@error
//                }
//
//                code = if (listOfElementsToAdd.contains(roleName)) {
//                    CODE_000_SUCCESS
//                } else {
//                    when (
//                        getStatus(name = roleName, type = RBACElementType.ROLE)
//                    ) {
//                        null -> {
//                            logger.warn { "Role $roleName was not found" }
//                            CODE_005_ROLE_NOT_FOUND
//                        }
//                        TupleStatus.DELETED -> {
//                            logger.warn { "Role $roleName was previously deleted" }
//                            CODE_014_ROLE_WAS_DELETED
//                        }
//                        else ->
//                            CODE_000_SUCCESS
//                    }
//                }
//                if (code != CODE_000_SUCCESS) {
//                    return@error
//                }
//
//                val userToken = getToken(username, RBACElementType.USER)
//                val roleToken = getToken(roleName, RBACElementType.ROLE)
//                val usernameAndToken = concatenateNameAndToken(username, userToken!!)
//                val roleNameAndToken = concatenateNameAndToken(roleName, roleToken!!)
//
//                val keyOfUsersRolesListByUserToken = "$userRoleListKeyPrefix$byUserTokenKeyPrefix$userToken"
//                val keyOfUsersRolesListByUser = "$userRoleListKeyPrefix$byUsernameKeyPrefix$username"
//                val keyOfUsersRolesListByRole = "$userRoleListKeyPrefix$byRoleNameKeyPrefix$roleName"
//                val keyOfUsersRolesListByUserAndRole =
//                    "$userRoleListKeyPrefix$byUserAndRoleNameKeyPrefix$username$dl$roleName"
//
//                val userRoleKey = "$userRoleObjectPrefix$byUserAndRoleTokenKeyPrefix$userToken$dl$roleToken"
//                logger.debug { "Adding the user-role assignments of user $username and role $roleName" }
//
//                /**
//                 * If the datastore already contains the key of the user-role
//                 * assignment and the list of keys to delete does NOT contain
//                 * the key of the user-role assignment, or the map of keys and
//                 * values to add contain the key of the user-role assignment,
//                 * it means that the key of the user-role assignment already exists
//                 */
//                if (
//                    mapOfValuesToAdd.contains(userRoleKey) ||
//                    (!listOfKeysToDelete.contains(userRoleKey) &&
//                    jQuery!!.sismemberCache(keyOfUsersRolesListByUser, roleNameAndToken))
//                ) {
//                    code = CODE_010_USER_ROLE_ASSIGNMENT_ALREADY_EXISTS
//                    return@error
//                } else {
//                    logger.debug {
//                        "Adding the user-role assignment for user $username " +
//                            "and role $roleName with key $userRoleKey"
//                    }
//                    transactionToExec = true
//                    val userRoleObject = hashMapOf(
//                        usernameField to username,
//                        roleNameField to roleName,
//                        encryptedAsymEncPublicKeyField to newUserRole.encryptedAsymEncKeys!!.public.encodeBase64(),
//                        encryptedAsymEncPrivateKeyField to newUserRole.encryptedAsymEncKeys.private.encodeBase64(),
//                        encryptedAsymSigPublicKeyField to newUserRole.encryptedAsymSigKeys!!.public.encodeBase64(),
//                        encryptedAsymSigPrivateKeyField to newUserRole.encryptedAsymSigKeys.private.encodeBase64(),
//                        userVersionNumberField to newUserRole.userVersionNumber.toString(),
//                        roleVersionNumberField to newUserRole.roleVersionNumber.toString(),
//                        signatureField to newUserRole.signature!!.encodeBase64(),
//                    )
//
//                    jTransaction!!.saddCache(keyOfUsersRolesListByUserToken, roleNameAndToken)
//                    jTransaction!!.saddCache(keyOfUsersRolesListByUser, roleNameAndToken)
//                    jTransaction!!.saddCache(keyOfUsersRolesListByRole, usernameAndToken)
//                    jTransaction!!.saddCache(keyOfUsersRolesListByUserAndRole, roleNameAndToken)
//
//                    jTransaction!!.hsetCache(userRoleKey, userRoleObject)
//                }
//            }
//        }
//
//        return code
//    }
//
//    /**
//     * In this implementation, add the (key of the) role-permission assignment
//     * in three lists, holding the list of role-permission assignments for a
//     * given role name, the list of role-permission assignments of a given
//     * resource name and the list of role-permission assignments for a combination
//     * of a role and resource name, respectively. Finally, add the role-permission
//     * assignment with as key the combination of the role name, the resource name
//     * and the resource version number
//     */
//    override fun addRolesPermissions(newRolesPermissions: HashSet<RolePermission>): OutcomeCode {
//        val size = newRolesPermissions.size
//        if (size == 0) {
//            logger.warn { "No role-permission assignments given" }
//            return CODE_000_SUCCESS
//        }
//
//        logger.info { "Adding $size role-permission assignments to the metadata (one per row below):" }
//        newRolesPermissions.forEachIndexed { index, newRolePermission ->
//            logger.info {
//                "${index + 1}: role ${newRolePermission.roleName} to resource " +
//                "${newRolePermission.resourceName} with operation ${newRolePermission.operation} "
//            }
//        }
//
//        var code = CODE_000_SUCCESS
//        run error@{
//            newRolesPermissions.forEach { newRolePermission ->
//                val roleName = newRolePermission.roleName
//                val resourceName = newRolePermission.resourceName
//
//                /**
//                 * Check that involved roles exist and
//                 * were not deleted and that involved
//                 * resources exist and were not deleted
//                 */
//                code = if (listOfElementsToAdd.contains(roleName)) {
//                    CODE_000_SUCCESS
//                } else {
//                    when (
//                        getStatus(name = roleName, type = RBACElementType.ROLE)
//                    ) {
//                        null -> {
//                            logger.warn { "Role $roleName was not found" }
//                            CODE_005_ROLE_NOT_FOUND
//                        }
//                        TupleStatus.DELETED -> {
//                            logger.warn { "Role $roleName was previously deleted" }
//                            CODE_014_ROLE_WAS_DELETED
//                        }
//                        else ->
//                            CODE_000_SUCCESS
//                    }
//                }
//                if (code != CODE_000_SUCCESS) {
//                    return@error
//                }
//
//                code = if (listOfElementsToAdd.contains(resourceName)) {
//                    CODE_000_SUCCESS
//                } else {
//                    when (getStatus(name = resourceName, type = RBACElementType.RESOURCE)) {
//                        null -> {
//                            logger.warn { "Resource $resourceName was not found" }
//                            CODE_006_RESOURCE_NOT_FOUND
//                        }
//                        TupleStatus.DELETED -> {
//                            logger.warn { "Resource $resourceName was previously deleted" }
//                            CODE_015_RESOURCE_WAS_DELETED
//                        }
//                        else ->
//                            CODE_000_SUCCESS
//                    }
//                }
//                if (code != CODE_000_SUCCESS) {
//                    return@error
//                }
//
//                val resourceVersionNumber = newRolePermission.resourceVersionNumber
//                val roleToken = getToken(
//                    name = roleName,
//                    type = RBACElementType.ROLE
//                )
//                val resourceToken = getToken(
//                    name = resourceName,
//                    type = RBACElementType.RESOURCE
//                )
//                val roleNameAndToken = concatenateNameAndToken(roleName, roleToken!!)
//                val resourceNameAndToken = concatenateNameAndToken(resourceName, resourceToken!!)
//
//                val keyOfRolesPermissionsListByRoleName =
//                    rolePermissionListKeyPrefix + byRoleNameKeyPrefix + roleName
//                val keyOfRolesPermissionsListByRoleToken =
//                    rolePermissionListKeyPrefix + byRoleTokenKeyPrefix + roleToken
//                val keyOfRolesPermissionsListByResource =
//                    rolePermissionListKeyPrefix + byResourceNameKeyPrefix + resourceName
//                val keyOfRolesPermissionsListByRoleAndResource =
//                    rolePermissionListKeyPrefix + byRoleAndResourceNameKeyPrefix + roleName + dl + resourceName
//                val rolePermissionKey =
//                    rolePermissionObjectPrefix + byRoleAndResourceTokenKeyPrefix + roleToken + dl + resourceToken
//
//                /**
//                 * If the datastore already contains the key of the role-permission
//                 * assignment and the list of keys to delete does NOT contain the
//                 * key of the role-permission assignment, or the map of keys and
//                 * values to add contain the key of the role-permission assignment,
//                 * it means that the key of the role-permission assignment already
//                 * exists
//                 */
//                if (
//                    mapOfValuesToAdd.contains(rolePermissionKey) ||
//                    (!listOfKeysToDelete.contains(rolePermissionKey) &&
//                     jQuery!!.sismemberCache(keyOfRolesPermissionsListByRoleName, resourceNameAndToken)
//                    )
//                ) {
//                    logger.warn {
//                        "Role-permission assignment for role $roleName " +
//                                "and resource $resourceName already exists"
//                    }
//                    code = CODE_011_ROLE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS
//                    return@error
//                } else {
//                    logger.debug {
//                        "Adding the role-permission assignment for role $roleName " +
//                            "and resource $resourceName with key $rolePermissionKey"
//                    }
//                    transactionToExec = true
//                    val rolePermissionObject = hashMapOf(
//                        roleNameField to roleName,
//                        resourceNameField to resourceName,
//                        roleTokenField to newRolePermission.roleToken,
//                        resourceTokenField to newRolePermission.resourceToken,
//                        encryptedSymKeyField to newRolePermission.encryptedSymKey!!.key.encodeBase64(),
//                        roleVersionNumberField to newRolePermission.roleVersionNumber.toString(),
//                        resourceVersionNumberField to resourceVersionNumber.toString(),
//                        operationField to newRolePermission.operation.toString(),
//                        signerTokenField to newRolePermission.signer!!,
//                        signatureField to newRolePermission.signature!!.encodeBase64(),
//                    )
//
//                    jTransaction!!.saddCache(keyOfRolesPermissionsListByRoleName, resourceNameAndToken)
//                    jTransaction!!.saddCache(keyOfRolesPermissionsListByRoleToken, resourceNameAndToken)
//                    jTransaction!!.saddCache(keyOfRolesPermissionsListByResource, roleNameAndToken)
//                    jTransaction!!.saddCache(keyOfRolesPermissionsListByRoleAndResource, resourceNameAndToken)
//
//                    jTransaction!!.hsetCache(rolePermissionKey, rolePermissionObject)
//                }
//            }
//        }
//
//        return code
//    }
//
//    override fun getUsers(
//        username: String?,
//        status: TupleStatus?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int
//    ): HashSet<User> {
//        logger.info { "Getting data of users (offset $offset, limit $limit)" }
//
//        val users = HashSet<User>()
//        val usersToGet = hashSetOf<String>()
//
//        /**
//         * If the user is not the admin, we get user objects
//         * matching the token in the [mmRedisServiceParameters]
//         */
//        if (!isAdmin) {
//            usersToGet.add(
//                concatenateNameAndToken(
//                    mmRedisServiceParameters.username,
//                    mmRedisServiceParameters.token
//                )
//            )
//        } else {
//            /** Get all users depending on the [status] value */
//            if (username.isNullOrBlank()) {
//                if (status != null) {
//                    logger.info { "Filtering by matching status $status" }
//                }
//                if (status == null ||
//                    status == TupleStatus.INCOMPLETE ||
//                    status == TupleStatus.OPERATIONAL
//                ) {
//                    jQuery!!.smembersCache(setOfUsersKey)?.let { usersToGet.addAll(it) }
//                }
//                if (status == null || status == TupleStatus.DELETED) {
//                    jQuery!!.smembersCache(setOfDeletedUsersKey)?.let { usersToGet.addAll(it) }
//                }
//            }
//            /** Get the user with the [username] */
//            else {
//                logger.info { "Filtering by matching username $username" }
//                val token = getToken(username, RBACElementType.USER)
//                if (token != null) {
//                    usersToGet.add(concatenateNameAndToken(username, token))
//                }
//            }
//        }
//
//        logger.debug {
//            "Found ${usersToGet.size} users to " +
//                "fetch (users to return may be less " +
//                "if status is INCOMPLETE or OPERATIONAL)"
//        }
//
//        /** Get all users from the data collected */
//        usersToGet.forEach { userNameAndToken ->
//            val currentUsername = getNameFromElementNameAndToken(userNameAndToken)!!
//            val currentUserToken = getTokenFromElementNameAndToken(userNameAndToken)!!
//            val userKeyByToken =
//                "$userObjectPrefix$byUserTokenKeyPrefix${getTokenFromElementNameAndToken(userNameAndToken)}"
//
//            logger.debug { "Retrieving data of user with key $userKeyByToken" }
//            val userValues = jQuery!!.hgetAllCache(userKeyByToken)
//            val userStatus = TupleStatus.valueOf(userValues[statusField]!!)
//            if (status == null || userStatus == status) {
//                users.add(
//                    User(
//                        name = currentUsername,
//                        status = userStatus,
//                        versionNumber = userValues[userVersionNumberField]!!.toInt(),
//                        isAdmin = userValues[isAdminField]!!.toBooleanStrict(),
//                    ).apply {
//                        token = currentUserToken
//                    }
//                )
//            }
//        }
//
//        logger.debug { "Found ${users.size} users" }
//        return users
//    }
//
//    override fun getRoles(
//        roleName: String?,
//        status: TupleStatus?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int
//    ): HashSet<Role> {
//        logger.info { "Getting data of roles (offset $offset, limit $limit)" }
//
//        val roles = HashSet<Role>()
//        val rolesToGet = hashSetOf<String>()
//
//        /**
//         * If the user is not the admin, we get the set of names
//         * and tokens of the roles assigned to the users and, with
//         * those, get the role objects, possibly matching the given
//         * role name
//         */
//        if (!isAdmin) {
//            val rolesNameAndToken: HashSet<String> = getRolesNameAndTokenFromUserToken()
//
//            rolesNameAndToken.forEach { roleNameAndToken ->
//                val currentRoleName = getNameFromElementNameAndToken(roleNameAndToken)!!
//                val roleToken = getTokenFromElementNameAndToken(roleNameAndToken)!!
//                if (roleName == null || currentRoleName == roleName) {
//                    rolesToGet.add(concatenateNameAndToken(currentRoleName, roleToken))
//                }
//            }
//        } else {
//            /** Get all roles depending on the [status] value */
//            if (roleName.isNullOrBlank()) {
//                if (status != null) {
//                    logger.info { "Filtering by matching status $status" }
//                }
//                if (status == null || status == TupleStatus.OPERATIONAL) {
//                    jQuery!!.smembersCache(setOfRolesKey)?.let { rolesToGet.addAll(it) }
//                }
//                if (status == null || status == TupleStatus.DELETED) {
//                    jQuery!!.smembersCache(setOfDeletedRolesKey)?.let { rolesToGet.addAll(it) }
//                }
//            }
//            /** Get the role with the [roleName] */
//            else {
//                logger.info { "Filtering by matching role name $roleName" }
//                val token = getToken(roleName, RBACElementType.ROLE)
//                if (token != null) {
//                    rolesToGet.add(concatenateNameAndToken(roleName, token))
//                }
//            }
//        }
//
//        logger.debug { "Found ${rolesToGet.size} roles to fetch" }
//
//        /** Get all roles from the keys collected */
//        rolesToGet.forEach { roleNameAndToken ->
//            val currentRoleName = getNameFromElementNameAndToken(roleNameAndToken)!!
//            val currentRoleToken = getTokenFromElementNameAndToken(roleNameAndToken)!!
//            val roleKeyByToken =
//                "$roleObjectPrefix$byRoleTokenKeyPrefix${getTokenFromElementNameAndToken(roleNameAndToken)}"
//
//            logger.debug { "Retrieving data of role with key $roleKeyByToken" }
//            val roleValues = jQuery!!.hgetAllCache(roleKeyByToken)
//            val roleStatus = TupleStatus.valueOf(roleValues[statusField]!!)
//            if (status == null || status == roleStatus) {
//                roles.add(
//                    Role(
//                        name = currentRoleName,
//                        status = roleStatus,
//                        versionNumber = roleValues[roleVersionNumberField]!!.toInt(),
//                    ).apply {
//                        token = currentRoleToken
//                    }
//                )
//            }
//        }
//
//        logger.debug { "Found ${roles.size} roles" }
//        return roles
//    }
//
//    override fun getResources(
//        resourceName: String?,
//        status: TupleStatus?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int
//    ): HashSet<Resource> {
//        logger.info { "Getting data of resources (offset $offset, limit $limit)" }
//
//        val resources = HashSet<Resource>()
//        val resourcesToGet = hashSetOf<String>()
//
//        /**
//         * If the user is not the admin, we get the set of names
//         * and tokens of the roles assigned to the users and, with
//         * those, get the set of names and tokens of resources assigned
//         * to each role and, with those, get the resources object, possibly
//         * matching the given resources name
//         */
//        if (!isAdmin) {
//            val rolesNameAndToken: HashSet<String> = getRolesNameAndTokenFromUserToken()
//
//            rolesNameAndToken.forEach { roleNameAndToken ->
//                val roleToken = getTokenFromElementNameAndToken(roleNameAndToken)!!
//
//                val resourcesNameAndToken = getResourcesNameAndTokenFromRoleToken(roleToken)
//                resourcesNameAndToken.forEach { resourceNameAndToken ->
//                    val currentResourceName = getNameFromElementNameAndToken(resourceNameAndToken)!!
//                    val resourceToken = getTokenFromElementNameAndToken(resourceNameAndToken)!!
//                    if (resourceName == null || currentResourceName == resourceName) {
//                        resourcesToGet.add(concatenateNameAndToken(currentResourceName, resourceToken))
//                    }
//                }
//            }
//        } else {
//
//            /** Get all resources depending on the [status] value */
//            if (resourceName.isNullOrBlank()) {
//                if (status != null) {
//                    logger.info { "Filtering by matching status $status" }
//                }
//                if (status == null || status == TupleStatus.OPERATIONAL) {
//                    jQuery!!.smembersCache(setOfResourcesKey)?.let { resourcesToGet.addAll(it) }
//                }
//                if (status == null || status == TupleStatus.DELETED) {
//                    jQuery!!.smembersCache(setOfDeletedResourcesKey)?.let { resourcesToGet.addAll(it) }
//                }
//            }
//            /** Get the resource with the [resourceName] */
//            else {
//                logger.info { "Filtering by matching resource name $resourceName" }
//                val token = getToken(resourceName, RBACElementType.RESOURCE)
//                if (token != null) {
//                    resourcesToGet.add(concatenateNameAndToken(resourceName, token))
//                }
//            }
//        }
//
//        logger.debug { "Found ${resourcesToGet.size} resources to fetch" }
//
//        /** Get all resources from the keys collected */
//        resourcesToGet.forEach { resourceNameAndToken ->
//            val currentResourceName = getNameFromElementNameAndToken(resourceNameAndToken)!!
//            val currentResourceToken = getTokenFromElementNameAndToken(resourceNameAndToken)!!
//            val resourceKeyByToken =
//                "$resourceObjectPrefix$byResourceTokenKeyPrefix${getTokenFromElementNameAndToken(resourceNameAndToken)}"
//
//            logger.debug { "Retrieving data of resource with key $resourceKeyByToken" }
//            val resourceValues = jQuery!!.hgetAllCache(resourceKeyByToken)
//            val resourceStatus = TupleStatus.valueOf(resourceValues[statusField]!!)
//            if (status == null || status == resourceStatus) {
//                resources.add(
//                    Resource(
//                        name = currentResourceName,
//                        status = resourceStatus,
//                        versionNumber = resourceValues[resourceVersionNumberField]!!.toInt(),
//                        reEncryptionThresholdNumber = resourceValues[reEncryptionThresholdNumberField]!!.toInt(),
//                        enforcement = Enforcement.valueOf(resourceValues[enforcementField]!!),
//                        encryptedSymKey = resourceValues[encryptedSymKeyField]?.let {
//                            EncryptedSymKey(
//                                key = it.decodeBase64(),
//                            )
//                        },
//                    ).apply {
//                        token = currentResourceToken
//                    }
//                )
//            }
//        }
//
//        logger.debug { "Found ${resources.size} resources" }
//        return resources
//    }
//
//    override fun getUsersRoles(
//        username: String?,
//        roleName: String?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int
//    ): HashSet<UserRole> {
//        logger.info { "Getting data of user-role assignments (offset $offset, limit $limit)" }
//
//        val usersRoles = HashSet<UserRole>()
//        val givenUsername = !username.isNullOrBlank()
//        val givenRoleName = !roleName.isNullOrBlank()
//
//        val keysOfUsersRolesToGet = hashSetOf<String>()
//
//        /**
//         * If the user is not the admin, we get the set of names
//         * and tokens of the roles assigned to the users and, with
//         * those, get the user-role assignments of the user, possibly
//         * selecting only the user-role assignments matching the given
//         * role name
//         */
//        if (!isAdmin) {
//            val rolesNameAndToken: HashSet<String> = getRolesNameAndTokenFromUserToken()
//
//            val userToken = mmRedisServiceParameters.token
//            rolesNameAndToken.forEach { roleNameAndToken ->
//                val currentRoleName = getNameFromElementNameAndToken(roleNameAndToken)
//                val roleToken = getTokenFromElementNameAndToken(roleNameAndToken)
//                if (roleName == null || currentRoleName == roleName) {
//                    val userRoleKey = "$userRoleObjectPrefix$byUserAndRoleTokenKeyPrefix$userToken$dl$roleToken"
//                    keysOfUsersRolesToGet.add(userRoleKey)
//                }
//            }
//        } else {
//            /** Get the keys of all user-role assignments */
//            if (givenUsername && givenRoleName) {
//                logger.info { "Filtering by matching username $username and role name $roleName" }
//                val userToken = getToken(username!!, RBACElementType.USER)
//
//                val keyOfUsersRolesListByUserAndRole =
//                    "$userRoleListKeyPrefix$byUserAndRoleNameKeyPrefix$username$dl$roleName"
//                val roleNamesAndTokens = hashSetOf<String>()
//                jQuery!!.smembersCache(keyOfUsersRolesListByUserAndRole)?.let { roleNamesAndTokens.addAll(it) }
//
//                val size = roleNamesAndTokens.size
//                logger.debug { "Found $size user-role assignments to retrieve" }
//
//                if (size == 0) {
//                    when (getStatus(name = username, type = RBACElementType.USER)) {
//                        TupleStatus.OPERATIONAL -> when (getStatus(name = roleName, type = RBACElementType.ROLE)) {
//                            TupleStatus.OPERATIONAL -> CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND
//                            TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                            TupleStatus.INCOMPLETE, null -> CODE_005_ROLE_NOT_FOUND
//                        }
//                        TupleStatus.DELETED -> CODE_013_USER_WAS_DELETED
//                        TupleStatus.INCOMPLETE -> CODE_053_USER_IS_INCOMPLETE
//                        null -> CODE_004_USER_NOT_FOUND
//                    }
//                } else {
//                    roleNamesAndTokens.forEach { currentRoleNameAndToken ->
//                        val userRoleKey = userRoleObjectPrefix +
//                            byUserAndRoleTokenKeyPrefix +
//                            "$userToken" +
//                            dl +
//                            getTokenFromElementNameAndToken(currentRoleNameAndToken)
//                        keysOfUsersRolesToGet.add(userRoleKey)
//                    }
//                }
//            } else if (givenUsername) {
//                logger.info { "Filtering by matching username $username" }
//                val userToken = getToken(username!!, RBACElementType.USER)
//
//                val keyOfUsersRolesListByUser =
//                    "$userRoleListKeyPrefix$byUsernameKeyPrefix$username"
//                val rolesNameAndToken = hashSetOf<String>()
//                jQuery!!.smembersCache(keyOfUsersRolesListByUser)?.let { rolesNameAndToken.addAll(it) }
//
//                val size = rolesNameAndToken.size
//                logger.debug { "Found $size user-role assignments to retrieve" }
//
//                if (size == 0) {
//                    when (getStatus(name = username, type = RBACElementType.USER)) {
//                        TupleStatus.OPERATIONAL -> CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND
//                        TupleStatus.DELETED -> CODE_013_USER_WAS_DELETED
//                        TupleStatus.INCOMPLETE -> CODE_053_USER_IS_INCOMPLETE
//                        null -> CODE_004_USER_NOT_FOUND
//                    }
//                } else {
//                    rolesNameAndToken.forEach { roleNameAndToken ->
//                        val roleToken = getTokenFromElementNameAndToken(roleNameAndToken)
//                        val userRoleKey = "$userRoleObjectPrefix$byUserAndRoleTokenKeyPrefix$userToken$dl$roleToken"
//                        keysOfUsersRolesToGet.add(userRoleKey)
//                    }
//                }
//            } else if (givenRoleName) {
//                logger.info { "Filtering by matching role name $roleName" }
//                val roleToken = getToken(roleName!!, RBACElementType.ROLE)
//
//                val keyOfUsersRolesListByRole =
//                    "$userRoleListKeyPrefix$byRoleNameKeyPrefix$roleName"
//                val usersNameAndToken = hashSetOf<String>()
//                jQuery!!.smembersCache(keyOfUsersRolesListByRole)?.let { usersNameAndToken.addAll(it) }
//
//                val size = usersNameAndToken.size
//                logger.debug { "Found $size user-role assignments to retrieve" }
//
//                if (size == 0) {
//                    when (getStatus(name = roleName, type = RBACElementType.ROLE)) {
//                        TupleStatus.OPERATIONAL -> CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND
//                        TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                        TupleStatus.INCOMPLETE, null -> CODE_005_ROLE_NOT_FOUND
//                    }
//                } else {
//                    usersNameAndToken.forEach { userNameAndToken ->
//                        val userToken = getTokenFromElementNameAndToken(userNameAndToken)
//                        val userRoleKey = "$userRoleObjectPrefix$byUserAndRoleTokenKeyPrefix$userToken$dl$roleToken"
//                        keysOfUsersRolesToGet.add(userRoleKey)
//                    }
//                }
//            } else {
//                logger.info { "Not filtering by user or role name" }
//
//                logger.debug { "Getting all users, then all user-role assignments of all users" }
//                jQuery!!.smembersCache(setOfUsersKey)?.forEach { usernameAndToken ->
//                    val currentUsername = getNameFromElementNameAndToken(usernameAndToken)
//                    val currentUserToken = getTokenFromElementNameAndToken(usernameAndToken)
//                    val keyOfUsersRolesListByUser =
//                        "$userRoleListKeyPrefix$byUsernameKeyPrefix$currentUsername"
//                    jQuery!!.smembersCache(keyOfUsersRolesListByUser)?.forEach { roleNameAndToken ->
//                        val currentRoleToken = getTokenFromElementNameAndToken(roleNameAndToken)
//                        val userRoleKey = "$userRoleObjectPrefix$byUserAndRoleTokenKeyPrefix$currentUserToken$dl$currentRoleToken"
//                        keysOfUsersRolesToGet.add(userRoleKey)
//                    }
//                }
//            }
//        }
//
//        logger.debug { "Found ${keysOfUsersRolesToGet.size} user-role assignments to fetch" }
//
//        /** Get all user-role assignments from the keys collected */
//        keysOfUsersRolesToGet.forEach { userRoleKey ->
//            logger.debug { "Retrieving data of user-role assignment with key $userRoleKey" }
//            val userRoleValues = jQuery!!.hgetAllCache(userRoleKey)
//            usersRoles.add(
//                UserRole(
//                    username = userRoleValues[usernameField]!!,
//                    roleName = userRoleValues[roleNameField]!!,
//                    encryptedAsymEncKeys = EncryptedAsymKeys(
//                        public = userRoleValues[encryptedAsymEncPublicKeyField]!!.decodeBase64(),
//                        private = userRoleValues[encryptedAsymEncPrivateKeyField]!!.decodeBase64(),
//                        keyType = AsymKeysType.ENC,
//                    ),
//                    encryptedAsymSigKeys = EncryptedAsymKeys(
//                        public = userRoleValues[encryptedAsymSigPublicKeyField]!!.decodeBase64(),
//                        private = userRoleValues[encryptedAsymSigPrivateKeyField]!!.decodeBase64(),
//                        keyType = AsymKeysType.SIG,
//                    ),
//                    userVersionNumber = userRoleValues[userVersionNumberField]!!.toInt(),
//                    roleVersionNumber = userRoleValues[roleVersionNumberField]!!.toInt(),
//                ).apply {
//                    updateSignature(
//                        newSignature = userRoleValues[signatureField]!!.decodeBase64(),
//                        newSigner = ADMIN,
//                    )
//                }
//            )
//        }
//
//        logger.debug { "Found ${usersRoles.size} user-role assignments" }
//        return usersRoles
//    }
//
//    override fun getRolesPermissions(
//        roleName: String?,
//        resourceName: String?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int,
//    ): HashSet<RolePermission> {
//        logger.info { "Getting data of role-permission assignments (offset $offset, limit $limit)" }
//
//        val rolesPermissions = HashSet<RolePermission>()
//        val givenRoleName = !roleName.isNullOrBlank()
//        val givenResourceName = !resourceName.isNullOrBlank()
//
//        val keysOfRolesPermissionsToGet = hashSetOf<String>()
//
//        /**
//         * If the user is not the admin, we get the set of names
//         * and tokens of the roles assigned to the users and, with
//         * those, get the set of names and tokens of resources assigned
//         * to each role and, with those, get the role-permission assignments
//         * of the user, possibly selecting only the role-permission
//         * assignments matching the given role and resource name
//         */
//        if (!isAdmin) {
//            val rolesNameAndToken: HashSet<String> = getRolesNameAndTokenFromUserToken()
//
//            rolesNameAndToken.forEach { roleNameAndToken ->
//                val currentRoleName = getNameFromElementNameAndToken(roleNameAndToken)
//                val roleToken = getTokenFromElementNameAndToken(roleNameAndToken)!!
//                if (roleName == null || currentRoleName == roleName) {
//
//                    val resourcesNameAndToken = getResourcesNameAndTokenFromRoleToken(roleToken)
//                    resourcesNameAndToken.forEach { resourceNameAndToken ->
//                        val currentResourceName = getNameFromElementNameAndToken(resourceNameAndToken)
//                        val resourceToken = getTokenFromElementNameAndToken(resourceNameAndToken)
//                        if (resourceName == null || currentResourceName == resourceName) {
//                            val rolePermissionKey = rolePermissionObjectPrefix +
//                                byRoleAndResourceTokenKeyPrefix +
//                                roleToken +
//                                dl +
//                                resourceToken
//                            keysOfRolesPermissionsToGet.add(rolePermissionKey)
//                        }
//                    }
//                }
//            }
//        } else {
//            /** Get the keys of all role-permission assignments */
//            if (givenRoleName && givenResourceName) {
//                logger.info { "Filtering by matching role name $roleName and resource name $resourceName" }
//
//                val keyOfRolesPermissionsListByUserAndRole =
//                    "$rolePermissionListKeyPrefix$byRoleAndResourceNameKeyPrefix$roleName$dl$resourceName"
//                val resourcesNameAndToken = hashSetOf<String>()
//                jQuery!!.smembersCache(keyOfRolesPermissionsListByUserAndRole)?.let {
//                    resourcesNameAndToken.addAll(it)
//                }
//                val size = resourcesNameAndToken.size
//                logger.debug { "Found $size role-permission assignments to retrieve" }
//
//                if (size == 0) {
//                    when (getStatus(
//                        name = resourceName,
//                        type = RBACElementType.RESOURCE
//                    )) {
//                        TupleStatus.OPERATIONAL -> when (getStatus(
//                            name = roleName,
//                            type = RBACElementType.ROLE
//                        )) {
//                            TupleStatus.OPERATIONAL -> CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//                            TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                            TupleStatus.INCOMPLETE, null -> CODE_005_ROLE_NOT_FOUND
//                        }
//                        TupleStatus.DELETED -> CODE_015_RESOURCE_WAS_DELETED
//                        TupleStatus.INCOMPLETE, null -> CODE_006_RESOURCE_NOT_FOUND
//                    }
//                } else {
//                    resourcesNameAndToken.forEach { resourceNameAndToken ->
//                        val rolePermissionKey = rolePermissionObjectPrefix +
//                                byRoleAndResourceTokenKeyPrefix +
//                                getToken(roleName!!, RBACElementType.ROLE)!! + // TODO optimize get token once
//                                dl +
//                                getTokenFromElementNameAndToken(resourceNameAndToken)!!
//                        keysOfRolesPermissionsToGet.add(rolePermissionKey)
//                    }
//                }
//            } else if (givenRoleName) {
//                logger.info { "Filtering by matching role name $roleName" }
//
//                val keyOfRolesPermissionsListByRole =
//                    "$rolePermissionListKeyPrefix$byRoleNameKeyPrefix$roleName"
//                val resourcesNameAndToken = hashSetOf<String>()
//                jQuery!!.smembersCache(keyOfRolesPermissionsListByRole)?.let { resourcesNameAndToken.addAll(it) }
//                val size = resourcesNameAndToken.size
//                logger.debug { "Found $size role-permission assignments to retrieve" }
//
//                if (size == 0) {
//                    when (getStatus(
//                        name = roleName,
//                        type = RBACElementType.ROLE
//                    )) {
//                        TupleStatus.OPERATIONAL -> CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//                        TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                        TupleStatus.INCOMPLETE, null -> CODE_005_ROLE_NOT_FOUND
//                    }
//                } else {
//                    resourcesNameAndToken.forEach { resourceNameAndToken ->
//                        val rolePermissionKey = rolePermissionObjectPrefix +
//                            byRoleAndResourceTokenKeyPrefix +
//                            getToken(roleName!!, RBACElementType.ROLE)!! + // TODO optimize get token once
//                            dl +
//                            getTokenFromElementNameAndToken(resourceNameAndToken)!!
//                        keysOfRolesPermissionsToGet.add(rolePermissionKey)
//                    }
//                }
//            } else if (givenResourceName) {
//                logger.info { "Filtering by matching resource name $resourceName" }
//
//                val keyOfRolesPermissionsListByResource =
//                    "$rolePermissionListKeyPrefix$byResourceNameKeyPrefix$resourceName"
//                val rolesNameAndToken = hashSetOf<String>()
//                jQuery!!.smembersCache(keyOfRolesPermissionsListByResource)?.let { rolesNameAndToken.addAll(it) }
//                val size = rolesNameAndToken.size
//                logger.debug { "Found $size role-permission assignments to retrieve" }
//
//                if (size == 0) {
//                    when (getStatus(
//                        name = resourceName,
//                        type = RBACElementType.RESOURCE
//                    )) {
//                        TupleStatus.OPERATIONAL -> CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//                        TupleStatus.DELETED -> CODE_015_RESOURCE_WAS_DELETED
//                        TupleStatus.INCOMPLETE, null -> CODE_006_RESOURCE_NOT_FOUND
//                    }
//                } else {
//                    rolesNameAndToken.forEach { roleNameAndToken ->
//                        val rolePermissionKey = rolePermissionObjectPrefix +
//                            byRoleAndResourceTokenKeyPrefix +
//                            getTokenFromElementNameAndToken(roleNameAndToken)!! +
//                            dl +
//                            getToken(resourceName!!, RBACElementType.RESOURCE)!! // TODO can you optimize get token once?
//                        keysOfRolesPermissionsToGet.add(rolePermissionKey)
//                    }
//                }
//            } else {
//                logger.info { "Not filtering by role or resource name" }
//
//                logger.debug { "Getting all roles, then all role-permission assignments of all roles" }
//                jQuery!!.smembersCache(setOfRolesKey)?.forEach { roleNameAndToken ->
//                    val currentRoleName = getNameFromElementNameAndToken(roleNameAndToken)
//                    val currentRoleToken = getTokenFromElementNameAndToken(roleNameAndToken)
//                    val keyOfRolesPermissionsListByRole =
//                        "$rolePermissionListKeyPrefix$byRoleNameKeyPrefix$currentRoleName"
//                    jQuery!!.smembersCache(keyOfRolesPermissionsListByRole)?.forEach { resourceNameAndToken ->
//                        val currentResourceToken = getTokenFromElementNameAndToken(resourceNameAndToken)
//                        val rolePermissionKey = rolePermissionObjectPrefix +
//                                byRoleAndResourceTokenKeyPrefix +
//                                currentRoleToken +
//                                dl +
//                                currentResourceToken
//                        keysOfRolesPermissionsToGet.add(rolePermissionKey)
//                    }
//                }
//            }
//        }
//
//        logger.debug { "Found ${keysOfRolesPermissionsToGet.size} role-permission assignments to fetch" }
//
//        /** Get all role-permission assignments from the keys collected */
//        keysOfRolesPermissionsToGet.forEach { rolePermissionKey ->
//            logger.debug { "Retrieving data of role-permission assignment with key $rolePermissionKey" }
//            val rolePermissionValues = jQuery!!.hgetAllCache(rolePermissionKey)
//            rolesPermissions.add(
//                RolePermission(
//                    roleName = rolePermissionValues[roleNameField]!!,
//                    resourceName = rolePermissionValues[resourceNameField]!!,
//                    roleToken = rolePermissionValues[roleTokenField]!!,
//                    resourceToken = rolePermissionValues[resourceTokenField]!!,
//                    encryptedSymKey = EncryptedSymKey(
//                        key = rolePermissionValues[encryptedSymKeyField]!!.decodeBase64(),
//                    ),
//                    roleVersionNumber = rolePermissionValues[roleVersionNumberField]!!.toInt(),
//                    resourceVersionNumber = rolePermissionValues[resourceVersionNumberField]!!.toInt(),
//                    operation = Operation.valueOf(rolePermissionValues[operationField]!!),
//                ).apply {
//                    updateSignature(
//                        newSignature = rolePermissionValues[signatureField]!!.decodeBase64(),
//                        newSigner = rolePermissionValues[signerTokenField]!!,
//                    )
//                }
//            )
//        }
//
//        logger.debug { "Found ${rolesPermissions.size} role-permission assignments" }
//        return rolesPermissions
//    }
//
//    override fun getPublicKey(
//        name: String?,
//        token: String?,
//        elementType: RBACElementType,
//        asymKeyType: AsymKeysType
//    ): ByteArray? {
//        logger.info { "Getting public key of type $asymKeyType of an element of type $elementType" }
//
//
//        // TODO support invocations by non-admin users
//
//        val givenName = !name.isNullOrBlank()
//        val givenToken = !token.isNullOrBlank()
//        val tokenToUse =
//            if (!givenToken) {
//                if (givenName) {
//                    getToken(
//                        name!!,
//                        when (elementType) {
//                            RBACElementType.USER -> RBACElementType.USER
//                            RBACElementType.ROLE -> RBACElementType.ROLE
//                            RBACElementType.RESOURCE -> TODO("cannot be here")
//                        }
//                    )
//                } else {
//                    val message = "Neither name nor token given for query"
//                    logger.error { message }
//                    throw IllegalArgumentException(message)
//                }
//            } else {
//                token
//            }
//
//        val fieldOfKeyToGet = when (asymKeyType) {
//            AsymKeysType.ENC -> asymEncPublicKeyField
//            AsymKeysType.SIG -> asymSigPublicKeyField
//        }
//
//        val keyOfElement = when (elementType) {
//            RBACElementType.USER -> "$userObjectPrefix$byUserTokenKeyPrefix$tokenToUse"
//            RBACElementType.ROLE -> "$roleObjectPrefix$byRoleTokenKeyPrefix$tokenToUse"
//            RBACElementType.RESOURCE -> TODO("cannot be here")
//        }
//
//        val elementData = jQuery!!.hgetAllCache(keyOfElement)
//        return when (elementData[statusField]?.let { TupleStatus.valueOf(it) }) {
//            TupleStatus.OPERATIONAL -> elementData[fieldOfKeyToGet]?.decodeBase64()
//            TupleStatus.DELETED -> elementData[fieldOfKeyToGet]?.decodeBase64()
//            TupleStatus.INCOMPLETE -> null
//            null -> null
//        }
//    }
//
//    override fun getVersionNumber(
//        name: String?,
//        token: String?,
//        elementType: RBACElementType
//    ): Int? {
//        logger.info { "Getting version number of a $elementType" }
//
//        // TODO support invocations by non-admin users
//
//        val givenName = !name.isNullOrBlank()
//        val givenToken = !token.isNullOrBlank()
//        val tokenToUse =
//            if (!givenToken) {
//                if (givenName) {
//                    getToken(name!!, elementType)
//                } else {
//                    val message = "Neither name nor token given for query"
//                    logger.error { message }
//                    throw IllegalArgumentException(message)
//                }
//            } else {
//                token
//            }
//
//        val fieldOfVersionNumberToGet = when (elementType) {
//            RBACElementType.USER -> userVersionNumberField
//            RBACElementType.ROLE -> roleVersionNumberField
//            RBACElementType.RESOURCE -> resourceVersionNumberField
//        }
//
//        val keyOfElement = when (elementType) {
//            RBACElementType.USER -> "$userObjectPrefix$byUserTokenKeyPrefix$tokenToUse"
//            RBACElementType.ROLE -> "$roleObjectPrefix$byRoleTokenKeyPrefix$tokenToUse"
//            RBACElementType.RESOURCE -> "$resourceObjectPrefix$byResourceTokenKeyPrefix$tokenToUse"
//        }
//
//        val elementData = jQuery!!.hgetAllCache(keyOfElement)
//        return if (elementData[statusField]?.let { TupleStatus.valueOf(it) } == TupleStatus.OPERATIONAL) {
//            elementData[fieldOfVersionNumberToGet]?.toInt()
//        } else {
//            null
//        }
//    }
//
//    override fun getToken(
//        name: String,
//        type: RBACElementType
//    ): String? {
//        logger.debug { "Get the token of element $type with name $name" }
//
//        // TODO support invocations by non-admin users
//
//        val elementsNamesAndTokens = mutableSetOf<String>()
//        when (type) {
//            RBACElementType.USER -> jQuery!!.smembersCache(setOfUsersKey)
//            RBACElementType.ROLE -> jQuery!!.smembersCache(setOfRolesKey)
//            RBACElementType.RESOURCE -> jQuery!!.smembersCache(setOfResourcesKey)
//        }?.let { elementsNamesAndTokens.addAll(it) }
//        when (type) {
//            RBACElementType.USER -> jQuery!!.smembersCache(setOfDeletedUsersKey)
//            RBACElementType.ROLE -> jQuery!!.smembersCache(setOfDeletedRolesKey)
//            RBACElementType.RESOURCE -> jQuery!!.smembersCache(setOfDeletedResourcesKey)
//        }?.let { elementsNamesAndTokens.addAll(it) }
//
//        val element = elementsNamesAndTokens.firstOrNull { it.startsWith("$name$dl") }
//        return getTokenFromElementNameAndToken(element)
//    }
//
//    override fun getStatus(
//        name: String?,
//        token: String?,
//        type: RBACElementType
//    ): TupleStatus? {
//        logger.debug { "Getting the status of a $type" }
//
//        // TODO support invocations by non-admin users
//
//        val givenName = !name.isNullOrBlank()
//        val givenToken = !token.isNullOrBlank()
//        val tokenToUse =
//            if (!givenToken) {
//                if (givenName) {
//                    getToken(name!!, type)
//                } else {
//                    val message = "Neither name nor token given for query"
//                    logger.error { message }
//                    throw IllegalArgumentException(message)
//                }
//            } else {
//                token
//            }
//
//        return if (tokenToUse != null) {
//            val keyOfElement = "${
//            when (type) {
//                RBACElementType.USER -> "$userObjectPrefix$byUserTokenKeyPrefix"
//                RBACElementType.ROLE -> "$roleObjectPrefix$byRoleTokenKeyPrefix"
//                RBACElementType.RESOURCE -> "$resourceObjectPrefix$byResourceTokenKeyPrefix"
//            }
//            }$tokenToUse"
//            val status = jQuery!!.hgetCache(keyOfElement, statusField)?.let { TupleStatus.valueOf(it) }
//            logger.debug { "The status is $status" }
//            status
//        } else {
//            null
//        }
//    }
//
//    /**
//     * In this implementation, change the status of the [roleName]
//     * to deleted and move the key to the list of deleted roles
//     */
//    override fun deleteRole(roleName: String): OutcomeCode {
//
//        /** Guard Clauses */
//        if (roleName == ADMIN) {
//            logger.warn { "Cannot delete the $ADMIN role" }
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }
//        if (roleName.isBlank()) {
//            logger.warn { "Role name is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//
//
//        logger.info { "Deleting role $roleName" }
//
//        return when (getStatus(name = roleName, type = RBACElementType.ROLE)) {
//            null -> {
//                CODE_005_ROLE_NOT_FOUND
//            }
//            TupleStatus.DELETED -> {
//                CODE_014_ROLE_WAS_DELETED
//            }
//            else -> {
//                val roleToken = getToken(roleName, RBACElementType.ROLE)!!
//                val elementNameAndToken = concatenateNameAndToken(roleName, roleToken)
//                val roleKeyByToken = "$roleObjectPrefix$byRoleTokenKeyPrefix$roleToken"
//                logger.info { "Deleting role with key $roleKeyByToken" }
//
//                transactionToExec = true
//                jTransaction!!.sremCache(setOfRolesKey, elementNameAndToken)
//                jTransaction!!.saddCache(setOfDeletedRolesKey, elementNameAndToken)
//                jTransaction!!.hsetCache(roleKeyByToken, statusField, TupleStatus.DELETED.toString())
//
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    /**
//     * In this implementation, change the status of the [resourceName]
//     * to deleted and move the key to the list of deleted resources
//     */
//    override fun deleteResource(
//        resourceName: String
//    ): OutcomeCode {
//
//        /** Guard Clauses */
//        if (resourceName.isBlank()) {
//            logger.warn { "Resource name is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//
//
//        logger.info { "Deleting resource $resourceName" }
//
//        return when (getStatus(name = resourceName, type = RBACElementType.RESOURCE)) {
//            null -> {
//                CODE_006_RESOURCE_NOT_FOUND
//            }
//            TupleStatus.DELETED -> {
//                CODE_015_RESOURCE_WAS_DELETED
//            }
//            else -> {
//                val resourceToken = getToken(resourceName, RBACElementType.RESOURCE)!!
//                val elementNameAndToken = concatenateNameAndToken(resourceName, resourceToken)
//                val resourceKeyByToken = "$resourceObjectPrefix$byResourceTokenKeyPrefix$resourceToken"
//                logger.info { "Deleting resource with key $resourceKeyByToken" }
//
//                transactionToExec = true
//                jTransaction!!.sremCache(setOfResourcesKey, elementNameAndToken)
//                jTransaction!!.saddCache(setOfDeletedResourcesKey, elementNameAndToken)
//                jTransaction!!.hsetCache(resourceKeyByToken, statusField, TupleStatus.DELETED.toString())
//
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    /**
//     * In this implementation, delete the user-role assignments
//     * with the [roleName] and remove the related keys from the
//     * lists of user-role assignments related to the user and
//     * the role
//     */
//    override fun deleteUsersRoles(roleName: String): OutcomeCode {
//        logger.info { "Deleting user-role assignments for role name $roleName" }
//
//        /** Guard Clauses */
//        if (roleName == ADMIN) {
//            logger.warn { "Cannot delete the $ADMIN role" }
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }
//
//        /** Get all users assigned to the role */
//        val usersRolesKeyByRoleName = "$userRoleListKeyPrefix$byRoleNameKeyPrefix$roleName"
//        val usersNameAndToken = hashSetOf<String>()
//        jQuery!!.smembersCache(usersRolesKeyByRoleName)?.let { usersNameAndToken.addAll(it) }
//        val size = usersNameAndToken.size
//        logger.debug { "Found $size user-role assignments to delete" }
//
//        return if (size == 0) {
//            when (getStatus(name = roleName, type = RBACElementType.ROLE)) {
//                TupleStatus.OPERATIONAL -> CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND
//                TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                TupleStatus.INCOMPLETE, null -> CODE_005_ROLE_NOT_FOUND
//            }
//        } else {
//            transactionToExec = true
//            jTransaction!!.delListCache(usersRolesKeyByRoleName)
//            val roleToken = getToken(roleName, RBACElementType.ROLE)!!
//
//            /** Remove the user-role assignments from all users */
//            usersNameAndToken.forEach { userNameAndToken ->
//                val username = getNameFromElementNameAndToken(userNameAndToken)
//                val userToken = getTokenFromElementNameAndToken(userNameAndToken)
//                val roleNameAndToken = concatenateNameAndToken(roleName, roleToken)
//                val usersRolesKeyByUserToken = "$userRoleListKeyPrefix$byUserTokenKeyPrefix$userToken"
//                val usersRolesKeyByUsername = "$userRoleListKeyPrefix$byUsernameKeyPrefix$username"
//                val usersRolesKeyByUsernameAndRoleName =
//                    "$userRoleListKeyPrefix$byUserAndRoleNameKeyPrefix$username$dl$roleName"
//                val userRoleKey = "$userRoleObjectPrefix$byUserAndRoleTokenKeyPrefix$userToken$dl$roleToken"
//
//                logger.debug { "Deleting user-role assignment of user $username" }
//                jTransaction!!.sremCache(
//                    usersRolesKeyByUsername,
//                    roleNameAndToken
//                )
//                jTransaction!!.sremCache(
//                    usersRolesKeyByUserToken,
//                    roleNameAndToken
//                )
//                jTransaction!!.delListCache(usersRolesKeyByUsernameAndRoleName)
//                jTransaction!!.delSetCache(userRoleKey)
//            }
//
//            CODE_000_SUCCESS
//        }
//    }
//
//    /**
//     * In this implementation, delete the role-permission assignments related to
//     * the [roleName], if given, the role-permission assignments related to the
//     * [resourceName], if given, or the role-permission assignments related to
//     * both [roleName] and [resourceName]. Also, remove the related keys from
//     * the list of role-permission assignments related to the involved roles,
//     * involved users and combination of involved users and roles
//     */
//    override fun deleteRolesPermissions(
//        roleName: String?,
//        resourceName: String?,
//    ): OutcomeCode {
//        logger.info { "Deleting role-permission assignments" }
//
//        /** Guard Clauses */
//        if (roleName == ADMIN) {
//            logger.warn { "Cannot delete the $ADMIN role" }
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }
//        val givenRoleName = !roleName.isNullOrBlank()
//        val givenResourceName = !resourceName.isNullOrBlank()
//
//        return if (!givenRoleName && !givenResourceName) {
//            val message = "Neither role nor resource name given for query"
//            logger.error { message }
//            throw IllegalArgumentException(message)
//        } else if (givenRoleName && !givenResourceName) {
//
//            logger.info { "Filtering by role name $roleName" }
//
//            /** The role name was given. Get all resources assigned to the role */
//            val rolesPermissionsKeyByRoleName = "$rolePermissionListKeyPrefix$byRoleNameKeyPrefix$roleName"
//            val resourcesNameAndToken = hashSetOf<String>()
//            jQuery!!.smembersCache(rolesPermissionsKeyByRoleName)?.let { resourcesNameAndToken.addAll(it) }
//            val size = resourcesNameAndToken.size
//
//            logger.debug { "Found $size role-permission assignments to delete" }
//
//            if (size == 0) {
//                when (getStatus(
//                    name = roleName,
//                    type = RBACElementType.ROLE
//                )) {
//                    TupleStatus.OPERATIONAL -> CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//                    TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                    TupleStatus.INCOMPLETE, null -> CODE_005_ROLE_NOT_FOUND
//                }
//            } else {
//                transactionToExec = true
//                jTransaction!!.delListCache(rolesPermissionsKeyByRoleName)
//
//                val roleToken = getToken(roleName!!, RBACElementType.ROLE)!!
//                val rolesPermissionsKeyByRoleToken =
//                    "$rolePermissionListKeyPrefix$byRoleTokenKeyPrefix$roleToken"
//                jTransaction!!.delListCache(rolesPermissionsKeyByRoleToken)
//
//                /** Remove the role-permission assignments from all resources */
//                resourcesNameAndToken.forEach { resourceNameAndToken ->
//                    val currentResourceName = getNameFromElementNameAndToken(resourceNameAndToken)
//                    val resourceToken = getTokenFromElementNameAndToken(resourceNameAndToken)
//                    val roleNameAndToken = concatenateNameAndToken(roleName, roleToken)
//
//                    val rolesPermissionsKeyByResourceName =
//                        "$rolePermissionListKeyPrefix$byResourceNameKeyPrefix$currentResourceName"
//                    val rolesPermissionsKeyByRoleNameAndResourceName =
//                        "$rolePermissionListKeyPrefix$byRoleAndResourceNameKeyPrefix" +
//                            "$roleName$dl$currentResourceName"
//                    val rolePermissionKey = "$rolePermissionObjectPrefix$byRoleAndResourceTokenKeyPrefix" +
//                        "$roleToken$dl$resourceToken"
//
//                    logger.debug {
//                        "Deleting role-permission assignment of role $roleName " +
//                            "and resource $currentResourceName"
//                    }
//
//                    jTransaction!!.sremCache(
//                        rolesPermissionsKeyByResourceName,
//                        roleNameAndToken
//                    )
//                    jTransaction!!.delListCache(rolesPermissionsKeyByRoleNameAndResourceName)
//                    jTransaction!!.delSetCache(rolePermissionKey)
//                }
//
//                CODE_000_SUCCESS
//            }
//        } else if (!givenRoleName) {
//
//            logger.info { "Filtering by resource name $resourceName" }
//
//            /** The resource name was given. Get all roles assigned to the resource */
//            val rolesPermissionsKeyByResourceName =
//                rolePermissionListKeyPrefix + byResourceNameKeyPrefix + resourceName
//            val rolesNameAndToken = hashSetOf<String>()
//            jQuery!!.smembersCache(rolesPermissionsKeyByResourceName)?.let { rolesNameAndToken.addAll(it) }
//            val size = rolesNameAndToken.size
//
//            logger.debug { "Found $size role-permission assignments to delete" }
//
//            if (size == 0) {
//                when (getStatus(
//                    name = resourceName,
//                    type = RBACElementType.RESOURCE
//                )) {
//                    TupleStatus.OPERATIONAL -> CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//                    TupleStatus.DELETED -> CODE_015_RESOURCE_WAS_DELETED
//                    TupleStatus.INCOMPLETE, null -> CODE_006_RESOURCE_NOT_FOUND
//                }
//            } else {
//                transactionToExec = true
//                jTransaction!!.delListCache(rolesPermissionsKeyByResourceName)
//
//                val resourceToken = getToken(resourceName!!, RBACElementType.RESOURCE)!!
//
//                /** Remove the role-permission assignments from all roles */
//                rolesNameAndToken.forEach { roleNameAndToken ->
//                    val currentRoleName = getNameFromElementNameAndToken(roleNameAndToken)
//                    val roleToken = getTokenFromElementNameAndToken(roleNameAndToken)
//                    val resourceNameAndToken = concatenateNameAndToken(resourceName, resourceToken)
//
//                    val rolesPermissionsKeyByRoleName =
//                        "$rolePermissionListKeyPrefix$byRoleNameKeyPrefix$currentRoleName"
//                    val rolesPermissionsKeyByRoleToken =
//                        "$rolePermissionListKeyPrefix$byRoleTokenKeyPrefix$roleToken"
//                    val rolesPermissionsKeyByRoleNameAndResourceName =
//                        "$rolePermissionListKeyPrefix$byRoleAndResourceNameKeyPrefix" +
//                            "$currentRoleName$dl$resourceName"
//                    val rolePermissionKey = "$rolePermissionObjectPrefix$byRoleAndResourceTokenKeyPrefix" +
//                        "$roleToken$dl$resourceToken"
//
//                    logger.debug {
//                        "Deleting role-permission assignments of role $currentRoleName " +
//                            "and resource $resourceName"
//                    }
//
//                    jTransaction!!.sremCache(
//                        key = rolesPermissionsKeyByRoleName,
//                        value = resourceNameAndToken
//                    )
//                    jTransaction!!.sremCache(
//                        key = rolesPermissionsKeyByRoleToken,
//                        value = resourceNameAndToken
//                    )
//                    jTransaction!!.delListCache(rolesPermissionsKeyByRoleNameAndResourceName)
//                    jTransaction!!.delSetCache(rolePermissionKey)
//                }
//                CODE_000_SUCCESS
//            }
//        } else {
//
//            logger.info { "Filtering by role name $roleName and resource name $resourceName" }
//
//            /** Both the role and resource name were given */
//            val rolesPermissionsKeyByRoleNameAndResourceName =
//                "$rolePermissionListKeyPrefix$byRoleAndResourceNameKeyPrefix" +
//                "$roleName$dl$resourceName"
//            val resourceNamesAndTokens = hashSetOf<String>()
//            jQuery!!.smembersCache(rolesPermissionsKeyByRoleNameAndResourceName)?.let {
//                resourceNamesAndTokens.addAll(it)
//            }
//            val size = resourceNamesAndTokens.size
//
//            logger.debug { "Found $size role-permission assignments to delete" }
//
//            when (size) {
//                0 -> {
//                    when (getStatus(
//                        name = roleName,
//                        type = RBACElementType.ROLE
//                    )) {
//                        TupleStatus.OPERATIONAL -> {
//                            when (getStatus(
//                                name = resourceName,
//                                type = RBACElementType.RESOURCE
//                            )) {
//                                TupleStatus.OPERATIONAL -> CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//                                TupleStatus.DELETED -> CODE_015_RESOURCE_WAS_DELETED
//                                TupleStatus.INCOMPLETE, null -> CODE_006_RESOURCE_NOT_FOUND
//                            }
//                        }
//                        TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                        TupleStatus.INCOMPLETE, null -> CODE_005_ROLE_NOT_FOUND
//                    }
//                }
//                1 -> {
//                    transactionToExec = true
//                    jTransaction!!.delListCache(rolesPermissionsKeyByRoleNameAndResourceName)
//
//                    val roleToken = getToken(roleName!!, RBACElementType.ROLE)!!
//                    val resourceToken = getToken(resourceName!!, RBACElementType.RESOURCE)!!
//                    val roleNameAndToken = concatenateNameAndToken(roleName, roleToken)
//                    val resourceNameAndToken = concatenateNameAndToken(resourceName, resourceToken)
//
//                    val rolesPermissionsKeyByRoleName =
//                        "$rolePermissionListKeyPrefix$byRoleNameKeyPrefix$roleName"
//                    val rolesPermissionsKeyByRoleToken =
//                        "$rolePermissionListKeyPrefix$byRoleTokenKeyPrefix$roleToken"
//                    val rolesPermissionsKeyByResourceName =
//                        "$rolePermissionListKeyPrefix$byResourceNameKeyPrefix$resourceName"
//
//                    val rolePermissionKey = "$rolePermissionObjectPrefix$byRoleAndResourceTokenKeyPrefix" +
//                            "$roleToken$dl$resourceToken"
//
//                    logger.debug {
//                        "Deleting role-permission assignments of role $roleName " +
//                                "and resource $resourceName"
//                    }
//
//                    jTransaction!!.sremCache(
//                        rolesPermissionsKeyByRoleName,
//                        resourceNameAndToken
//                    )
//                    jTransaction!!.sremCache(
//                        rolesPermissionsKeyByRoleToken,
//                        resourceNameAndToken
//                    )
//                    jTransaction!!.sremCache(
//                        rolesPermissionsKeyByResourceName,
//                        roleNameAndToken
//                    )
//                    jTransaction!!.delSetCache(rolePermissionKey)
//
//                    CODE_000_SUCCESS
//                }
//                else -> {
//                    val message = "There is more than one role-permission assignment " +
//                            "for role $roleName and resource $resourceName"
//                    logger.error { message }
//                    throw IllegalStateException(message)
//                }
//            }
//        }
//    }
//
//    override fun updateRoleTokenAndVersionNumberAndAsymKeys(
//        roleName: String,
//        oldRoleVersionNumber: Int,
//        oldRoleToken: String,
//        newRoleToken: String,
//        newAsymEncPublicKey: PublicKey,
//        newAsymSigPublicKey: PublicKey
//    ): OutcomeCode {
//        logger.info { "Updating the token and the public keys of role $roleName" }
//
//        return when (val role = getRoles(roleName = roleName).firstOrNull()) {
//            null -> {
//                logger.warn { "Role was not found" }
//                CODE_005_ROLE_NOT_FOUND
//            }
//            else -> {
//                when (role.status) {
//                    TupleStatus.DELETED -> {
//                        logger.warn { "Role $roleName was previously deleted" }
//                        CODE_014_ROLE_WAS_DELETED
//                    }
//                    else -> {
//                        transactionToExec = true
//                        val roleKeyByToken = "$roleObjectPrefix$byRoleTokenKeyPrefix${role.token}"
//                        val updatedRoleObject = hashMapOf(
//                            roleTokenField to newRoleToken,
//                            asymEncPublicKeyField to newAsymEncPublicKey.encoded.encodeBase64(),
//                            asymSigPublicKeyField to newAsymSigPublicKey.encoded.encodeBase64(),
//                            roleVersionNumberField to (oldRoleVersionNumber + 1).toString(),
//                        )
//                        jTransaction!!.hsetCache(roleKeyByToken, updatedRoleObject)
//
//                        logger.debug { "Change the role's token in the set of roles" }
//                        val oldRoleNameAndToken = concatenateNameAndToken(roleName, oldRoleToken)
//                        val newRoleNameAndToken = concatenateNameAndToken(roleName, newRoleToken)
//                        jTransaction!!.sremCache(setOfRolesKey, oldRoleNameAndToken)
//                        jTransaction!!.saddCache(setOfRolesKey, newRoleNameAndToken)
//
//                        logger.debug { "Change the role's token in the list of user-role assignments" }
//                        val keyOfUsersRolesListByRoleName =
//                            userRoleListKeyPrefix + byRoleNameKeyPrefix + roleName
//                        jQuery!!.smembersCache(keyOfUsersRolesListByRoleName)?.forEach { usernameAndToken ->
//                            logger.debug { "Changing role token for username and token $usernameAndToken" }
//                            val currentUsername = getNameFromElementNameAndToken(usernameAndToken)
//                            val currentUserToken = getTokenFromElementNameAndToken(usernameAndToken)
//
//                            logger.debug { "Changing role token in list of user-role assignments by user token" }
//                            val listOfUsersRolesByUserToken =
//                                userRoleListKeyPrefix + byUserTokenKeyPrefix + currentUserToken
//                            jTransaction!!.sremCache(listOfUsersRolesByUserToken, oldRoleNameAndToken)
//
//                            jTransaction!!.saddCache(listOfUsersRolesByUserToken, newRoleNameAndToken)
//
//                            logger.debug { "Changing role token in list of user-role assignments by username" }
//                            val listOfUsersRolesByUsername =
//                                userRoleListKeyPrefix + byUsernameKeyPrefix + currentUsername
//                            jTransaction!!.sremCache(listOfUsersRolesByUsername, oldRoleNameAndToken)
//
//                            jTransaction!!.saddCache(listOfUsersRolesByUsername, newRoleNameAndToken)
//
//                            logger.debug { "Changing role token in list of user-role assignments by username and role name" }
//                            val listOfUsersRolesByUserAndRoleName =
//                                userRoleListKeyPrefix + byUserAndRoleNameKeyPrefix + currentUsername + dl + roleName
//                            jTransaction!!.sremCache(listOfUsersRolesByUserAndRoleName, oldRoleNameAndToken)
//                            jTransaction!!.saddCache(listOfUsersRolesByUserAndRoleName, newRoleNameAndToken)
//
//                            val oldUserRoleObjectKey =
//                                userRoleObjectPrefix + byUserAndRoleTokenKeyPrefix +
//                                currentUserToken + dl + oldRoleToken
//                            val newUserRoleObjectKey =
//                                userRoleObjectPrefix + byUserAndRoleTokenKeyPrefix +
//                                currentUserToken + dl + newRoleToken
//                            logger.debug {
//                                "Rename the key of the user-role assignment object from " +
//                                "$oldUserRoleObjectKey to $newUserRoleObjectKey"
//                            }
//                            jTransaction!!.renameCache(
//                                oldUserRoleObjectKey,
//                                newUserRoleObjectKey
//                            )
//                        }
//
//                        val oldRoleRolesPermissionsListKey =
//                            rolePermissionListKeyPrefix + byRoleTokenKeyPrefix + oldRoleToken
//                        val newRoleRolesPermissionsListKey =
//                            rolePermissionListKeyPrefix + byRoleTokenKeyPrefix + newRoleToken
//                        logger.debug {
//                            "Rename the key of role-permission assignments by role token from " +
//                            "$oldRoleRolesPermissionsListKey to $newRoleRolesPermissionsListKey"
//                        }
//                        jTransaction!!.renameCache(
//                            oldRoleRolesPermissionsListKey,
//                            newRoleRolesPermissionsListKey
//                        )
//
//                        logger.debug { "Change the role's token in the list of role-permission assignments" }
//                        val keyOfRolesPermissionsListByRoleName =
//                            rolePermissionListKeyPrefix + byRoleNameKeyPrefix + roleName
//                        jQuery!!.smembersCache(keyOfRolesPermissionsListByRoleName)?.forEach { resourceNameAndToken ->
//                            logger.debug { "Changing role token for resource name and token $resourceNameAndToken" }
//                            val currentResourceName = getNameFromElementNameAndToken(resourceNameAndToken)
//                            val currentResourceToken = getTokenFromElementNameAndToken(resourceNameAndToken)
//
//                            logger.debug { "Change the role's token in the list of role-permission assignments by resource name" }
//                            val listOfRolesPermissionsByResourceName =
//                                rolePermissionListKeyPrefix + byResourceNameKeyPrefix + currentResourceName
//                            jTransaction!!.sremCache(listOfRolesPermissionsByResourceName, oldRoleNameAndToken)
//                            jTransaction!!.saddCache(listOfRolesPermissionsByResourceName, newRoleNameAndToken)
//
//
//                            val oldRolePermissionObjectKey =
//                                rolePermissionObjectPrefix + byRoleAndResourceTokenKeyPrefix +
//                                oldRoleToken + dl + currentResourceToken
//                            val newRolePermissionObjectKey =
//                                rolePermissionObjectPrefix + byRoleAndResourceTokenKeyPrefix +
//                                newRoleToken + dl + currentResourceToken
//                            logger.debug {
//                                "Rename the key of the role-permission assignment object from " +
//                                "$oldRolePermissionObjectKey to $newRolePermissionObjectKey"
//                            }
//                            jTransaction!!.renameCache(
//                                oldRolePermissionObjectKey,
//                                newRolePermissionObjectKey
//                            )
//                        }
//
//                        val oldRoleObjectKey =
//                            roleObjectPrefix + byRoleTokenKeyPrefix + oldRoleToken
//                        val newRoleObjectKey =
//                            roleObjectPrefix + byRoleTokenKeyPrefix + newRoleToken
//                        logger.debug {
//                            "Rename the key of the role object from " +
//                            "$oldRoleObjectKey to $newRoleObjectKey"
//                        }
//                        jTransaction!!.renameCache(
//                            oldRoleObjectKey,
//                            newRoleObjectKey
//                        )
//
//                        CODE_000_SUCCESS
//                    }
//                }
//            }
//        }
//    }
//
//    override fun updateResourceVersionNumber(
//        updatedResource: Resource
//    ): OutcomeCode {
//        val resourceName = updatedResource.name
//        logger.info { "Updating the version number of $resourceName" }
//
//        return when (val resource = getResources(resourceName = resourceName).firstOrNull()) {
//            null -> {
//                logger.warn { "Resource was not found" }
//                CODE_006_RESOURCE_NOT_FOUND
//            }
//            else -> {
//                when (resource.status) {
//                    TupleStatus.DELETED -> {
//                        logger.warn { "Resource $resourceName was previously deleted" }
//                        CODE_015_RESOURCE_WAS_DELETED
//                    }
//                    else -> {
//                        transactionToExec = true
//                        val resourceKeyByToken = "$resourceObjectPrefix$byResourceTokenKeyPrefix${resource.token}"
//                        val updatedResourceObject = hashMapOf(
//                            resourceVersionNumberField to resource.versionNumber.toString(),
//                        )
//                        jTransaction!!.hsetCache(
//                            key = resourceKeyByToken,
//                            values = updatedResourceObject
//                        )
//                        CODE_000_SUCCESS
//                    }
//                }
//            }
//        }
//    }
//
//    override fun updateResourceTokenAndVersionNumber(
//        resourceName: String,
//        oldResourceToken: String,
//        newResourceToken: String,
//        newVersionNumber: Int,
//    ): OutcomeCode {
//        logger.info { "Updating the token and the version number of $resourceName" }
//
//        return when (val resource = getResources(resourceName = resourceName).firstOrNull()) {
//            null -> {
//                logger.warn { "Resource was not found" }
//                CODE_006_RESOURCE_NOT_FOUND
//            }
//            else -> {
//                when (resource.status) {
//                    TupleStatus.DELETED -> {
//                        logger.warn { "Resource $resourceName was previously deleted" }
//                        CODE_015_RESOURCE_WAS_DELETED
//                    }
//                    else -> {
//                        transactionToExec = true
//                        val oldResourceObjectKey =
//                            resourceObjectPrefix + byResourceTokenKeyPrefix + oldResourceToken
//                        val updatedResourceObject = hashMapOf(
//                            resourceTokenField to newResourceToken,
//                            resourceVersionNumberField to newVersionNumber.toString(),
//                        )
//                        jTransaction!!.hsetCache(
//                            key = oldResourceObjectKey,
//                            values = updatedResourceObject
//                        )
//
//                        logger.debug { "Change the resource's token in the set of resources" }
//                        val oldResourceNameAndToken = concatenateNameAndToken(resourceName, oldResourceToken)
//                        val newResourceNameAndToken = concatenateNameAndToken(resourceName, newResourceToken)
//                        jTransaction!!.sremCache(setOfResourcesKey, oldResourceNameAndToken)
//                        jTransaction!!.saddCache(setOfResourcesKey, newResourceNameAndToken)
//
//                        logger.debug { "Change the resource's token in the list of role-permission assignments" }
//                        val keyOfRolesPermissionsListByResourceName =
//                            rolePermissionListKeyPrefix + byResourceNameKeyPrefix + resourceName
//                        jQuery!!.smembersCache(keyOfRolesPermissionsListByResourceName)?.forEach { roleNameAndToken ->
//                            logger.debug { "Changing resource token for role name and token $roleNameAndToken" }
//                            val currentRoleName = getNameFromElementNameAndToken(roleNameAndToken)
//                            val currentRoleToken = getTokenFromElementNameAndToken(roleNameAndToken)
//
//                            logger.debug { "Changing resource token in list of role-permission assignments by role token" }
//                            val listOfRolesPermissionsByRoleToken =
//                                rolePermissionListKeyPrefix + byRoleTokenKeyPrefix + currentRoleToken
//                            jTransaction!!.sremCache(listOfRolesPermissionsByRoleToken, oldResourceNameAndToken)
//                            jTransaction!!.saddCache(listOfRolesPermissionsByRoleToken, newResourceNameAndToken)
//
//                            logger.debug { "Changing resource token in list of role-permission assignments by role name" }
//                            val listOfRolesPermissionsByRoleName=
//                                rolePermissionListKeyPrefix + byRoleNameKeyPrefix + currentRoleName
//                            jTransaction!!.sremCache(listOfRolesPermissionsByRoleName, oldResourceNameAndToken)
//                            jTransaction!!.saddCache(listOfRolesPermissionsByRoleName, newResourceNameAndToken)
//
//                            logger.debug { "Changing resource token in list of role-permission assignments by role and resource name" }
//                            val listOfRolesPermissionsByRoleAndResourceName =
//                                rolePermissionListKeyPrefix + byRoleAndResourceNameKeyPrefix + currentRoleName + dl + resourceName
//                            jTransaction!!.sremCache(listOfRolesPermissionsByRoleAndResourceName, oldResourceNameAndToken)
//                            jTransaction!!.saddCache(listOfRolesPermissionsByRoleAndResourceName, newResourceNameAndToken)
//
//                            val oldRolePermissionObjectKey =
//                                rolePermissionObjectPrefix + byRoleAndResourceTokenKeyPrefix +
//                                currentRoleToken + dl + oldResourceToken
//                            val newRolePermissionObjectKey =
//                                rolePermissionObjectPrefix + byRoleAndResourceTokenKeyPrefix +
//                                currentRoleToken + dl + newResourceToken
//                            logger.debug {
//                                "Rename the key of the role-permission assignment object from " +
//                                "$oldRolePermissionObjectKey to $newRolePermissionObjectKey"
//                            }
//                            jTransaction!!.renameCache(
//                                oldKey = oldRolePermissionObjectKey,
//                                newKey = newRolePermissionObjectKey
//                            )
//                        }
//
//                        val newResourceObjectKey =
//                            resourceObjectPrefix + byResourceTokenKeyPrefix + newResourceToken
//                        logger.debug {
//                            "Rename the key of the resource object from " +
//                            "$oldResourceObjectKey to $newResourceObjectKey"
//                        }
//                        jTransaction!!.renameCache(
//                            oldResourceObjectKey,
//                            newResourceObjectKey
//                        )
//
//                        CODE_000_SUCCESS
//                    }
//                }
//            }
//        }
//    }
//
//    override fun updateRolePermission(updatedRolePermission: RolePermission): OutcomeCode {
//        val roleName = updatedRolePermission.roleName
//        val resourceName = updatedRolePermission.resourceName
//        val roleToken = updatedRolePermission.roleToken
//        val resourceToken = updatedRolePermission.resourceToken
//        val rolePermissionKey = rolePermissionObjectPrefix +
//            "$byRoleAndResourceTokenKeyPrefix$roleToken$dl$resourceToken"
//        logger.info { "Updating the role-permission assignment of role $roleName and resource $resourceName" }
//
//        return if (jQuery!!.existsCache(rolePermissionKey)) {
//            transactionToExec = true
//            val updatedRolePermissionObject = hashMapOf(
//                operationField to updatedRolePermission.operation.toString(),
//                signerTokenField to updatedRolePermission.signer!!,
//                signatureField to updatedRolePermission.signature!!.encodeBase64(),
//            )
//            jTransaction!!.hsetCache(
//                key = rolePermissionKey,
//                values = updatedRolePermissionObject
//            )
//            CODE_000_SUCCESS
//        } else {
//            CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//        }
//    }
//
//
//
//    /**
//     * In this implementation, create a Redis transaction and another
//     * connection for querying the database
//     */
//    override fun lock(): OutcomeCode {
//        return if (locks == 0) {
//            logger.info { "Locking the status of the MM" }
//            try {
//                if (jTransaction == null && jQuery == null && transaction == null) {
//                    transaction = pool.resource
//                    try {
//                        transaction!!.auth(usernameRedis, mmRedisServiceParameters.password)
//                        transaction!!.watch(lockUnlockRollbackKey)
//                        jTransaction = transaction!!.multi()
//                        jQuery = pool.resource
//                        jQuery!!.auth(usernameRedis, mmRedisServiceParameters.password)
//                        transactionToExec = false
//                        locks++
//                        CODE_000_SUCCESS
//                    } catch (e: JedisAccessControlException) {
//                        if (e.message?.contains(
//                                "WRONGPASS invalid username-password pair or user is disabled"
//                            ) == true
//                        ) {
//                            logger.warn { "MM Redis - access denied for user $usernameRedis" }
//                            CODE_055_ACCESS_DENIED_TO_MM
//                        } else {
//                            throw e
//                        }
//                    }
//                } else {
//                    /** A lock has been set but not released */
//                    logger.warn { "A lock has been set but not released" }
//                    jTransaction?.discard()
//                    closeAndNullRedis()
//                    locks = 0
//                    CODE_031_LOCK_CALLED_IN_INCONSISTENT_STATUS
//                }
//            } catch (e: JedisConnectionException) {
//                closeAndNullRedis()
//                if ((e.message ?: "").contains("Failed to create socket")) {
//                    logger.warn { "MM Redis - connection timeout" }
//                    CODE_045_MM_CONNECTION_TIMEOUT
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
//    /** In this implementation, discard the transaction */
//    override fun rollback(): OutcomeCode {
//        return if (locks == 1) {
//            logger.info { "Rollback the status of the MM" }
//            logger.debug { "Clearing list of elements to add (size was ${listOfElementsToAdd.size})" }
//            logger.debug { "Clearing list of lists to add (size was ${mapOfListsToAdd.size})" }
//            logger.debug { "Clearing list of values pairs to add (size was ${mapOfValuesToAdd.size})" }
//            logger.debug { "Clearing list of keys to delete (size was ${listOfKeysToDelete.size})" }
//            logger.debug { "Clearing list of lists to remove (size was ${mapOfListsToRemove.size})" }
//            logger.debug { "Clearing list of renamed keys (size was ${listOfKeysRenamed.size})" }
//            listOfElementsToAdd.clear()
//            mapOfListsToAdd.clear()
//            mapOfValuesToAdd.clear()
//            listOfKeysToDelete.clear()
//            mapOfListsToRemove.clear()
//            listOfKeysRenamed.clear()
//            locks--
//            if (jTransaction != null && jQuery != null && transaction != null) {
//                if (transactionToExec) {
//                    transactionToExec = false
//                    jTransaction!!.discard()
//                }
//                closeAndNullRedis()
//                CODE_000_SUCCESS
//            } else {
//                /** The lock has already been released */
//                logger.warn { "The lock was released but the connection was not closed" }
//                jTransaction?.discard()
//                closeAndNullRedis()
//                CODE_033_ROLLBACK_CALLED_IN_INCONSISTENT_STATUS
//            }
//        } else if (locks > 1) {
//            locks--
//            logger.debug { "Decrement lock number to $locks}" }
//            CODE_000_SUCCESS
//        } else {
//            logger.warn { "MM rollback number is zero or negative ($locks)" }
//            CODE_033_ROLLBACK_CALLED_IN_INCONSISTENT_STATUS
//        }
//    }
//
//    /**
//     * In this implementation, exec the transaction and increment (i.e., change)
//     * the value corresponding to the [lockUnlockRollbackKey]
//     */
//    override fun unlock(): OutcomeCode {
//        return if (locks == 1) {
//            logger.info { "Unlocking the status of the MM" }
//            logger.debug { "Clearing list of elements to add (size was ${listOfElementsToAdd.size})" }
//            logger.debug { "Clearing list of lists to add (size was ${mapOfListsToAdd.size})" }
//            logger.debug { "Clearing list of values pairs to add (size was ${mapOfValuesToAdd.size})" }
//            logger.debug { "Clearing list of keys to delete (size was ${listOfKeysToDelete.size})" }
//            logger.debug { "Clearing list of lists to remove (size was ${mapOfListsToRemove.size})" }
//            logger.debug { "Clearing list of renamed keys (size was ${listOfKeysRenamed.size})" }
//            listOfElementsToAdd.clear()
//            mapOfListsToAdd.clear()
//            mapOfValuesToAdd.clear()
//            listOfKeysToDelete.clear()
//            mapOfListsToRemove.clear()
//            listOfKeysRenamed.clear()
//            locks--
//            if (jTransaction != null && jQuery != null && transaction != null) {
//                if (transactionToExec) {
//                    transactionToExec = false
//                    jTransaction!!.incr(lockUnlockRollbackKey)
//                    val responses = jTransaction!!.exec()
//                    closeAndNullRedis()
//                    if (responses == null) {
//                        logger.warn { "Could not execute the transaction" }
//                        CODE_034_UNLOCK_FAILED
//                    } else {
//                        CODE_000_SUCCESS
//                    }
//                } else {
//                    closeAndNullRedis()
//                    CODE_000_SUCCESS
//                }
//            } else {
//                /** The lock has already been released */
//                logger.warn { "The lock was released but the connection was not closed" }
//                jTransaction?.discard()
//                closeAndNullRedis()
//                CODE_032_UNLOCK_CALLED_IN_INCONSISTENT_STATUS
//            }
//        } else if (locks > 1) {
//            locks--
//            logger.debug { "Decrement lock number to $locks" }
//            CODE_000_SUCCESS
//        } else {
//            logger.warn { "MM unlock number is zero or negative ($locks)" }
//            CODE_032_UNLOCK_CALLED_IN_INCONSISTENT_STATUS
//        }
//    }
//
//
//
//    /**
//     * Close all connections toward
//     * Redis and null the references
//     */
//    private fun closeAndNullRedis() {
//        transaction?.close()
//        jTransaction?.close()
//        jQuery?.close()
//        transaction = null
//        jTransaction = null
//        jQuery = null
//    }
//
//    /**
//     * Get all names and tokens of roles that the
//     * user whose token is in the [mmRedisServiceParameters]
//     * is assigned to
//     */
//    private fun getRolesNameAndTokenFromUserToken(): HashSet<String> {
//        val keyOfUsersRolesListByUserToken =
//            userRoleListKeyPrefix +
//                byUserTokenKeyPrefix +
//                mmRedisServiceParameters.token
//        return jQuery!!.smembersCache(keyOfUsersRolesListByUserToken) as HashSet<String>
//    }
//
//    /**
//     * Get all names and tokens of resources that the
//     * role with the given [roleToken] is assigned to
//     */
//    private fun getResourcesNameAndTokenFromRoleToken(roleToken: String): HashSet<String> {
//        val keyOfRolesPermissionsListByRoleToken =
//            rolePermissionListKeyPrefix +
//                byRoleTokenKeyPrefix +
//                roleToken
//        return jQuery!!.smembersCache(keyOfRolesPermissionsListByRoleToken) as HashSet<String>
//    }
//
//    /** Concatenate the given [name] and [token] */
//    private fun concatenateNameAndToken(name: String, token: String) = name + dl + token
//
//    /**
//     * Split the given [element] name + dl + token
//     * string to return the name only
//     */
//    private fun getNameFromElementNameAndToken(element: String?) = element?.split(dl)?.get(0)
//
//    /**
//     * Split the given [element] name + dl + token
//     * string to return the token only
//     */
//    private fun getTokenFromElementNameAndToken(element: String?) = element?.split(dl)?.get(1)
//
//    /**
//     * Extend the 'sismemberCache' function to retrieve
//     * values from the cache first
//     */
//    private fun Jedis.sismemberCache(key: String, member: String): Boolean {
//        val keyToUse = checkIfKeyWasRenamed(key)
//
//        return if (listOfKeysToDelete.contains(key)) {
//            false
//        } else if (mapOfListsToAdd[key]?.contains(member) == true) {
//            true
//        } else if (mapOfListsToRemove[key]?.contains(member) == true) {
//            false
//        } else {
//            this.sismember(keyToUse, member)
//        }
//    }
//
//    /**
//     * Extend the 'exists' function to retrieve
//     * values from the cache first
//     */
//    private fun Jedis.existsCache(key: String): Boolean {
//        return if (listOfKeysToDelete.contains(key)) {
//            false
//        } else {
//            this.exists(key)
//        }
//    }
//
//    /**
//     * Extend the 'smembers' function to retrieve
//     * values from the cache first
//     */
//    private fun Jedis.smembersCache(key: String): MutableSet<String>? {
//        val keyToUse = checkIfKeyWasRenamed(key)
//
//        return if (listOfKeysToDelete.contains(key)) {
//            mutableSetOf()
//        } else {
//            this.smembers(keyToUse).apply {
//                mapOfListsToAdd[key]?.let { addAll(it) }
//                mapOfListsToRemove[key]?.let { removeAll(it) }
//            }
//        }
//    }
//
//    /**
//     * Extend the 'hgetAll' function to retrieve
//     * values from the cache first
//     */
//    private fun Jedis.hgetAllCache(key: String): Map<String, String> {
//        val keyToUse = checkIfKeyWasRenamed(key)
//        return if (listOfKeysToDelete.contains(key)) {
//            hashMapOf()
//        } else {
//            val mapFromRedis = this.hgetAll(keyToUse)
//            mapOfValuesToAdd[keyToUse]?.forEach { (field, value) ->
//                mapFromRedis[field] = value
//            }
//            return mapFromRedis
//        }
//    }
//
//    /**
//     * Extend the 'hget' function to retrieve
//     * values from the cache first
//     */
//    private fun Jedis.hgetCache(key: String, field: String): String? {
//        val keyToUse = checkIfKeyWasRenamed(key)
//
//        return if (listOfKeysToDelete.contains(key)) {
//            null
//        } else if (mapOfValuesToAdd.contains(key)) {
//            mapOfValuesToAdd[key]?.get(field) ?: this.hget(keyToUse, field)
//        } else {
//            this.hget(keyToUse, field)
//        }
//    }
//
//    /**
//     * Extend the 'rename' function to store
//     * values from the cache. In particular,
//     * we want to map the new key with the
//     * old one, as queries done to the Redis
//     * datastore need to use old keys until
//     * the transaction is completed
//     */
//    private fun Transaction.renameCache(oldKey: String, newKey: String) {
//        if (listOfKeysToDelete.contains(oldKey)) {
//            val message = "Cannot rename delete key"
//            logger.error { message }
//            throw IllegalStateException(message)
//        }
//
//        val oldestKeys = hashSetOf<String>()
//        listOfKeysRenamed.forEach { (key, value) ->
//            if (key == oldKey) {
//                oldestKeys.add(value)
//            }
//        }
//        listOfKeysRenamed.values.remove(oldKey)
//        if (oldestKeys.isEmpty()) {
//            listOfKeysRenamed[newKey] = oldKey
//        } else {
//            oldestKeys.forEach {
//                listOfKeysRenamed[newKey] = it
//            }
//        }
//
//        this.rename(oldKey, newKey)
//        listOfKeysToDelete.add(oldKey)
//    }
//
//    /**
//     * Extend the 'del' function to store
//     * values from the cache when deleting
//     * a set
//     */
//    private fun Transaction.delSetCache(key: String) {
//        if (!listOfKeysToDelete.contains(key)) {
//            this.del(key)
//            listOfKeysToDelete.add(key)
//        }
//    }
//    /**
//     * Extend the 'del' function to store
//     * values from the cache when deleting
//     * a list
//     */
//    private fun Transaction.delListCache(key: String) {
//        if (!listOfKeysToDelete.contains(key)) {
//
//            /** Add all elements of the list to the cache object */
//            jQuery!!.smembersCache(key)?.let {
//                mapOfListsToRemove.getOrPut(key) { hashSetOf() }.addAll(
//                    it
//                )
//            }
//            this.del(key)
//            listOfKeysToDelete.add(key)
//        }
//    }
//
//    /**
//     * Extend the 'hset' function to store
//     * values from the cache
//     */
//    private fun Transaction.hsetCache(key: String, field: String, value: String) {
//        this.hset(key, field, value)
//        mapOfValuesToAdd.getOrPut(key) { hashMapOf() }[field] = value
//        listOfKeysToDelete.remove(key)
//    }
//
//    /**
//     * Extend the 'hset' function to store
//     * values from the cache
//     */
//    private fun Transaction.hsetCache(key: String, values: HashMap<String, String>) {
//        this.hset(key, values)
//        mapOfValuesToAdd.putIfAbsent(key, hashMapOf())
//        values.forEach { (field, value) ->
//            mapOfValuesToAdd[key]!![field] = value
//        }
//        listOfKeysToDelete.remove(key)
//    }
//
//    /**
//     * Extend the 'sadd' function to store
//     * values from the cache
//     */
//    private fun Transaction.saddCache(key: String, value: String) {
//        this.sadd(key, value)
//        if (mapOfListsToRemove.containsKey(key)) {
//            mapOfListsToRemove[key]!!.remove(value)
//            if (mapOfListsToRemove[key]!!.size == 0)
//                mapOfListsToRemove.remove(key)
//        }
//        mapOfListsToAdd.getOrPut(key) { hashSetOf() }.add(value)
//        listOfKeysToDelete.remove(key)
//    }
//
//    /**
//     * Extend the 'srem' function to store
//     * values from the cache
//     */
//    private fun Transaction.sremCache(key: String, value: String) {
//        this.srem(key, value)
//        if (mapOfListsToAdd.containsKey(key)) {
//            mapOfListsToAdd[key]!!.remove(value)
//            if (mapOfListsToAdd[key]!!.size == 0)
//                mapOfListsToAdd.remove(key)
//        }
//        mapOfListsToRemove.getOrPut(key) { hashSetOf() }.add(value)
//        listOfKeysToDelete.remove(key)
//    }
//
//    /**
//     * Return [key] is [key] was not renamed,
//     * the renamed key otherwise
//     */
//    private fun checkIfKeyWasRenamed(key: String) = if (listOfKeysRenamed.containsKey(key)) {
//        listOfKeysRenamed[key]
//    } else {
//        key
//    }
//}
