//package cryptoac.mm.mysql
//
//import com.mysql.cj.jdbc.exceptions.CommunicationsException
//import cryptoac.Constants.ADMIN
//import cryptoac.OutcomeCode
//import cryptoac.OutcomeCode.*
//import cryptoac.core.CoreParameters
//import cryptoac.crypto.AsymKeysType
//import cryptoac.crypto.EncryptedSymKey
//import cryptoac.decodeBase64
//import cryptoac.encodeBase64
//import cryptoac.mm.MMServiceCACABAC
//import cryptoac.code.CodeServiceParameters
//import cryptoac.code.CodeBoolean
//import cryptoac.mm.MMType
//import cryptoac.tuple.*
//import io.ktor.util.*
//import mu.KotlinLogging
//import java.io.BufferedReader
//import java.io.FileNotFoundException
//import java.io.InputStreamReader
//import java.sql.*
//import java.util.*
//import kotlin.collections.ArrayList
//import kotlin.collections.HashSet
//import kotlin.collections.LinkedHashMap
//
//private val logger = KotlinLogging.logger {}
//
//// TODO as of now, there is no way to hide
////  resources and access structures from
////  users through views, as there is no
////  ABAC syntax in MySQL. Therefore, for now,
////  users can see all resources (a solution
////  could be to use OPA to filter SQL queries)
//
//
///**
// * Class implementing the methods for invoking the APIs of a MySQL8+
// * database configured with the given [mmMySQLServiceParameters] for AB-CAC
// *
// * Note that the database is configured to avoid the disclosure of
// * the AC policy to users with tokens and views.
// * Note that the name of the user connecting to the database is the
// * same as the name in the AC policy.
// * Note that any value represented by a byte array is converted to
// * BASE64 before being stored
// */
//class MMServiceCACABACMySQL(
//    private val mmMySQLServiceParameters: MMServiceMySQLParameters
//) : MMServiceCACABAC, MMServiceMySQL {
//
//    companion object {
//        /** The name of the schema in the database */
//        private const val databaseName = "cryptoac"
//        const val masterPublicKeyTable = "$databaseName.masterPublicKey"
//        const val usersTable = "$databaseName.users"
//        const val deletedUsersTable = "$databaseName.deletedUsers"
//        const val usersView = "$databaseName.user_specific_users"
//        const val usersInfoView = "$databaseName.user_specific_users_info"
//        const val attributesTable = "$databaseName.attributes"
//        const val deletedAttributesTable = "$databaseName.deletedAttributes"
//        const val resourcesTable = "$databaseName.resources"
//        const val deletedResourcesTable = "$databaseName.deletedResources"
//        const val usersAttributesTable = "$databaseName.usersAttributes"
//        const val usersAttributesView = "$databaseName.user_specific_usersAttributes"
//        const val accessStructuresPermissionsTable = "$databaseName.accessStructuresPermissions"
//        const val userTokenColumn = "userToken"
//        const val attributeTokenColumn = "attributeToken"
//        const val resourceTokenColumn = "resourceToken"
//        const val asymEncPublicKeyColumn = "asymEncPublicKey"
//        const val asymSigPublicKeyColumn = "asymSigPublicKey"
//        const val abeSecretKeyColumn = "abeSecretKey"
//        const val userVersionNumberColumn = "userVersionNumber"
//        const val attributeVersionNumberColumn = "attributeVersionNumber"
//        const val resourceVersionNumberColumn = "resourceVersionNumber"
//        const val reEncryptionThresholdNumberColumn = "reEncryptionThresholdNumber"
//        const val encryptedSymKeyColumn = "encryptedSymKey"
//        const val signatureColumn = "signature"
//        const val operationColumn = "operation"
//        const val usernameColumn = "username"
//        const val attributeNameColumn = "attributeName"
//        const val attributeValueColumn = "attributeValue"
//        const val resourceNameColumn = "resourceName"
//        const val statusColumn = "status"
//        const val enforcementColumn = "enforcement"
//        const val isAdminColumn = "isAdmin"
//        const val accessStructureColumn = "accessStructure"
//        const val signerTokenColumn = "signerToken"
//        const val mpkColumn = "mpk"
//
//        /** The file containing SQL commands to initialize the database */
//        const val INIT_ABAC_SQL_CODE = "/cryptoac/MySQL/databaseABAC.sql"
//    }
//
//    override var locks = 0
//
//    /** The connection object to the database */
//    var connection: Connection? = null
//
//    /** Properties related to the connection object */
//    private val connectionProperties: Properties = Properties()
//
//    /** The jDBUrl of the database */
//    private val jDBUrl: String = "jdbc:mysql://${mmMySQLServiceParameters.url}:${mmMySQLServiceParameters.port}"
//
//    init {
//        connectionProperties.setProperty("user", mmMySQLServiceParameters.username)
//        connectionProperties.setProperty("password", mmMySQLServiceParameters.password)
//        connectionProperties.setProperty("useSSL", "true")
//    }
//
//
//
//    /**
//     * In this implementation, check if the
//     * admin user is present in the database
//     */
//    override fun alreadyConfigured(): CodeBoolean {
//        return try {
//            if (getUsers(username = ADMIN).isNotEmpty()) {
//                logger.info { "Database already initialized" }
//                CodeBoolean()
//            } else {
//                CodeBoolean(
//                    boolean = false
//                )
//            }
//        } catch (e: SQLSyntaxErrorException) {
//            if (e.message!!.contains("Unknown database")) {
//                CodeBoolean(
//                    boolean = false
//                )
//            } else {
//                throw e
//            }
//        }
//    }
//
//    /**
//     * In this implementation, create the tables,
//     * views and triggers in the database. Deny
//     * subsequent invocations
//     */
//    override fun configure(parameters: CoreParameters?): OutcomeCode {
//
//        logger.info { "Initializing the database" }
//
//        val serviceStatus = alreadyConfigured()
//        if (serviceStatus.code != CODE_000_SUCCESS) {
//            return serviceStatus.code
//        } else if (serviceStatus.boolean) {
//            logger.warn { "The database was already initialized" }
//            return CODE_077_SERVICE_ALREADY_CONFIGURED
//        }
//
//        val sqlFile = MMServiceCACABACMySQL::class.java.getResourceAsStream(INIT_ABAC_SQL_CODE)
//        if (sqlFile == null) {
//            val message = "Initialization SQL file $INIT_ABAC_SQL_CODE not found"
//            logger.error { message }
//            throw FileNotFoundException(message)
//        }
//
//        /** Read the SQL commands and initialize the database */
//        connection!!.createStatement().use { statement ->
//            BufferedReader(InputStreamReader(sqlFile)).use { reader ->
//                val builder = StringBuilder()
//                val defaultDelimiter = ";"
//                var delimiter = defaultDelimiter
//
//                reader.forEachLine { line ->
//                    builder.append(line)
//
//                    /** A new delimiter char is being set (probably for stored procedure) */
//                    if (line.startsWith("DELIMITER")) {
//                        delimiter = line.split(" ")[1]
//                        builder.clear()
//                    } else if (line.endsWith(delimiter)) {
//                        builder.replace(
//                            builder.length - delimiter.length,
//                            builder.length,
//                            defaultDelimiter
//                        )
//                        val commandWitComments = builder.toString().trimStart { it == ' ' }
//                        val command = commandWitComments.replace("/\\*.*?\\*/".toRegex(), "")
//                        logger.debug { "Locks number is $locks" }
//                        logger.debug { "Sending command $command" }
//                        statement.executeUpdate(command)
//                        builder.clear()
//                        delimiter = defaultDelimiter
//                    }
//                    builder.append(" ")
//                }
//            }
//        }
//        return CODE_000_SUCCESS
//    }
//
//    override fun init() {
//        logger.info { "No action required to initialize the MM ABAC MySQL service" }
//    }
//
//    override fun deinit() {
//        logger.info { "No action required to de-initialize the MM ABAC MySQL service" }
//    }
//
//    /**
//     * In this implementation, add the [newAdmin] in the
//     * metadata by setting the public keys and token of
//     * the [newAdmin] and return the outcome code
//     */
//    override fun addAdmin(
//        newAdmin: User
//    ): OutcomeCode {
//        logger.info { "Adding the admin in the MM" }
//
//        /** Guard clauses */
//        if (newAdmin.name != ADMIN) {
//            logger.warn { "Admin user has name ${newAdmin.name}, but admin name should be $ADMIN" }
//            return CODE_036_ADMIN_NAME
//        }
//        try {
//            if (getUsers(username = ADMIN).isNotEmpty()) {
//                logger.warn { "Admin $ADMIN already initialized" }
//                return CODE_035_ADMIN_ALREADY_INITIALIZED
//            }
//        } catch (e: SQLSyntaxErrorException) {
//            if (e.message.isNullOrBlank() || !e.message!!.contains("Unknown database")) {
//                throw e
//            }
//        }
//
//        /** Add the admin as user in the metadata */
//        logger.debug { "Adding the admin as user" }
//        val adminUserValues = arrayListOf<Any?>(
//            ADMIN,
//            ADMIN,
//            1,
//            newAdmin.asymEncKeys!!.public,
//            newAdmin.asymSigKeys!!.public,
//            null,
//            TupleStatus.OPERATIONAL,
//            newAdmin.isAdmin,
//        )
//        return createInsertStatement(
//            table = usersTable,
//            values = arrayListOf(adminUserValues),
//            connection = connection!!,
//        ).use {
//            if (it.executeUpdate() == 1) {
//                CODE_000_SUCCESS
//            } else {
//                val message = "Admin was not present but update failed"
//                logger.error { message }
//                throw IllegalArgumentException(message)
//            }
//        }
//    }
//
//    /**
//     * In this implementation, initialize the user by
//     * adding in the metadata the public keys and token
//     * of the [user], updating also the status flag,
//     * and return the outcome code
//     */
//    override fun initUser(
//        user: User
//    ): OutcomeCode {
//        val username = user.name
//        logger.info { "Initializing user $username in the metadata" }
//
//        /** Update the user' metadata */
//        logger.debug { "Updating the user metadata" }
//        val userValues = linkedMapOf<String, Any>(
//            asymEncPublicKeyColumn to user.asymEncKeys!!.public,
//            asymSigPublicKeyColumn to user.asymSigKeys!!.public,
//            userTokenColumn to user.token,
//            statusColumn to TupleStatus.OPERATIONAL,
//        )
//        val whereParameters = linkedMapOf<String, Any?>(
//            usernameColumn to username,
//            statusColumn to TupleStatus.INCOMPLETE,
//        )
//        return createUpdateStatement(
//            table = if (mmMySQLServiceParameters.username != username) usersTable else usersView,
//            values = userValues,
//            whereParameters = if (mmMySQLServiceParameters.username != username) whereParameters else null,
//            connection = connection!!,
//        ).use {
//            if (it.executeUpdate() != 1) {
//                logger.warn { "Error while initializing user $username" }
//
//                /** Check the reason of the error */
//                val status = createSelectStatement(
//                    table = if (mmMySQLServiceParameters.username != username) usersTable else usersInfoView,
//                    whereParameters = if (mmMySQLServiceParameters.username != username) linkedMapOf(usernameColumn to username) else null,
//                    connection = connection!!,
//                ).use { selectStatement ->
//                    val rs = selectStatement.executeQuery()
//                    if (rs.isBeforeFirst) {
//                        rs.next()
//                        TupleStatus.valueOf(rs.getString(statusColumn))
//                    } else {
//                        null
//                    }
//                }
//
//                when (status) {
//                    null -> {
//                        logger.warn { "User ${user.name} does not exist in the metadata" }
//                        CODE_004_USER_NOT_FOUND
//                    }
//                    TupleStatus.DELETED -> {
//                        logger.warn { "User $username was previously deleted" }
//                        CODE_013_USER_WAS_DELETED
//                    }
//                    TupleStatus.OPERATIONAL -> {
//                        logger.warn { "User $username already initialized" }
//                        CODE_052_USER_ALREADY_INITIALIZED
//                    }
//                    TupleStatus.INCOMPLETE -> {
//                        val message = "Error in initializing user $username but status is incomplete"
//                        logger.error { message }
//                        throw java.lang.IllegalStateException(message)
//                    }
//                }
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    /**
//     * In this implementation, the user's asymmetric
//     * encryption and signing public keys and token
//     * will be set by the user him/herself later on
//     * in the "initUser" function. Afterward, the
//     * admin will set the user's asymmetric ABE
//     * decryption key
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
//        logger.info { "Adding the user $username in the metadata and in the database" }
//        if (getUsers(
//                username = username,
//                status = TupleStatus.DELETED
//            ).isNotEmpty()
//        ) {
//            logger.warn { "User $username was previously deleted" }
//            return CodeServiceParameters(CODE_013_USER_WAS_DELETED)
//        }
//
//        /** TODO check password generation */
//        val passwordBytes = ByteArray(20)
//        Random().nextBytes(passwordBytes)
//        val newPassword = passwordBytes.encodeBase64()
//
//        /**
//         * Add the user in the metadata, keys and
//         * token will be set by the user
//         */
//        logger.debug { "Adding the user in the metadata" }
//        val adminUserValues = arrayListOf<Any?>(
//            username,
//            userToken,
//            1,
//            "mock",
//            "mock",
//            null,
//            TupleStatus.INCOMPLETE,
//            newUser.isAdmin,
//        )
//        createInsertStatement(
//            table = usersTable,
//            values = arrayListOf(adminUserValues),
//            connection = connection!!,
//        ).use {
//            if (it.executeUpdate() != 1) {
//                logger.warn { "User $username already present in the metadata" }
//                return CodeServiceParameters(CODE_001_USER_ALREADY_EXISTS)
//            }
//        }
//
//        val code = try {
//            /** Create the user at database level and then grant privileges on tables and views */
//            connection!!.prepareStatement(
//                "CREATE USER ? IDENTIFIED BY ?"
//            ).use {
//                logger.debug { "Creating the database user" }
//                it.setString(1, username)
//                it.setString(2, newPassword)
//                it.execute()
//            }
//
//            connection!!.prepareStatement(
//                "GRANT SELECT (" +
//                        "$userTokenColumn, " +
//                        "$asymEncPublicKeyColumn, " +
//                        "$asymSigPublicKeyColumn, " +
//                        "$statusColumn) ON $usersTable TO ?"
//            ).use {
//                logger.debug { "Granting permission on users table" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            connection!!.prepareStatement(
//                "GRANT SELECT (" +
//                        "$userTokenColumn, " +
//                        "$asymEncPublicKeyColumn, " +
//                        "$asymSigPublicKeyColumn, " +
//                        "$statusColumn) ON $deletedUsersTable TO ?"
//            ).use {
//                logger.debug { "Granting permission on deleted users table" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            connection!!.prepareStatement(
//                "GRANT UPDATE ON $usersView TO ?"
//            ).use {
//                logger.debug { "Granting permission on users view" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            connection!!.prepareStatement(
//                "GRANT SELECT ON $usersInfoView TO ?"
//            ).use {
//                logger.debug { "Granting permission on users status view" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            // TODO al momento, diamo permessi sui nomi degli attributi, visto
//            //  che dobbiamo ancora fare in modo che gli utenti usino solo i
//            //  token degli attributi
//            connection!!.prepareStatement(
//                "GRANT SELECT (" +
//                        "$attributeNameColumn, " +
//                        "$attributeTokenColumn, " +
//                        "$attributeVersionNumberColumn, " +
//                        "$statusColumn) ON $attributesTable TO ?"
//            ).use {
//                logger.debug { "Granting permission on attributes table" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            // TODO al momento, diamo permessi sui nomi degli attributi, visto
//            //  che dobbiamo ancora fare in modo che gli utenti usino solo i
//            //  tokend degli attributi
//            connection!!.prepareStatement(
//                "GRANT SELECT (" +
//                        "$attributeNameColumn, " +
//                        "$attributeTokenColumn, " +
//                        "$attributeVersionNumberColumn, " +
//                        "$statusColumn) ON $deletedAttributesTable TO ?"
//            ).use {
//                logger.debug { "Granting permission on deleted attributes table" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            // TODO al momento, diamo permessi sui nomi delle risorse
//            connection!!.prepareStatement(
//                "GRANT SELECT (" +
//                        "$resourceTokenColumn, " +
//                        "$resourceVersionNumberColumn, " +
//                        "$reEncryptionThresholdNumberColumn, " +
//                        "$enforcementColumn, " +
//                        "$encryptedSymKeyColumn, " +
//                        "$statusColumn) ON $resourcesTable TO ?"
//            ).use {
//                logger.debug { "Granting permission on resources table" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            connection!!.prepareStatement(
//                "GRANT SELECT ON $usersAttributesView TO ?"
//            ).use {
//                logger.debug { "Granting permission on user-attribute assignments view" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            // TODO al momento, diamo permessi sulla tabella delle access structure
//            //  tuples, dato che non sappiamo come restringere i permessi alle
//            //  access structure alle quali l'utente ha accesso
//            connection!!.prepareStatement(
//                "GRANT SELECT ON $accessStructuresPermissionsTable TO ?"
//            ).use {
//                logger.debug { "Granting permission on access structure-permission assignments view" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            // TODO dare accesso ad una view delle risorse, come facciamo adesso per RBAC
//
//            CODE_000_SUCCESS
//        } catch (e: SQLException) {
//            logger.warn { "Could not create user $username in MySQL" }
//            CODE_054_CREATE_USER_MM
//        }
//
//        return CodeServiceParameters(
//            code = code,
//            serviceParameters = MMServiceMySQLParameters(
//                username = username,
//                password = newPassword,
//                port = mmMySQLServiceParameters.port,
//                url = mmMySQLServiceParameters.url,
//                mmType = MMType.ABAC_MYSQL
//            )
//        )
//    }
//
//    /**
//     * In this implementation, move the [username] in the
//     * deleted users' table and delete the user at database
//     * level
//     */
//    override fun deleteUser(
//        username: String
//    ): OutcomeCode {
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
//        logger.debug { "Moving the user from table $usersTable to table $deletedUsersTable" }
//        val whereParameters = linkedMapOf<String, Any?>(
//            usernameColumn to username
//        )
//        val selectedColumns = arrayListOf(
//            usernameColumn,
//            userTokenColumn,
//            userVersionNumberColumn,
//            asymEncPublicKeyColumn,
//            asymSigPublicKeyColumn,
//            abeSecretKeyColumn, // TODO instead of copying the abe key, delete it, i.e., set it to null or something similar
//            "'${TupleStatus.DELETED}'",
//            isAdminColumn
//        )
//        createSelectStatement(
//            table = usersTable,
//            selectedColumns = selectedColumns,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use { selectStatement ->
//            connection!!.prepareStatement(
//                "INSERT INTO $deletedUsersTable (${selectStatement.asString()})"
//            ).use {
//                if (it.executeUpdate() != 1) {
//                    logger.warn { "User $username not found in the metadata" }
//                    return when (getStatus(
//                        name = username,
//                        type = ABACElementType.USER
//                    )) {
//                        TupleStatus.DELETED -> CODE_013_USER_WAS_DELETED
//                        null -> CODE_004_USER_NOT_FOUND
//                        else -> {
//                            val message = "User not found but user is in table"
//                            logger.error { message }
//                            throw IllegalArgumentException(message)
//                        }
//                    }
//                }
//            }
//        }
//
//        logger.debug { "Deleting the user from table $usersTable" }
//        createDeleteStatement(
//            table = usersTable,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            if (it.executeUpdate() != 1) {
//                logger.warn { "User $username not found in the metadata" }
//                return CODE_004_USER_NOT_FOUND
//            }
//        }
//
//        connection!!.prepareStatement("DROP USER ?").use {
//            logger.debug { "Deleting the user from the database" }
//            it.setString(1, username)
//            return try {
//                it.execute()
//                CODE_000_SUCCESS
//            } catch (e: SQLException) {
//                if (e.message?.contains("Operation DROP USER failed") == true) {
//                    logger.warn { "Error while deleting user $username from the database " }
//                    CODE_056_DELETE_USER_MM
//                } else {
//                    throw e
//                }
//            }
//        }
//    }
//
//
//
//    override fun setMPK(mpk: String): OutcomeCode {
//        logger.info { "Adding ABE MPK in the metadata" }
//
//        val mpkValues = arrayListOf<Any?>(
//            "1",
//            mpk
//        )
//        createInsertStatement(
//            table = masterPublicKeyTable,
//            values = arrayListOf(mpkValues),
//            connection = connection!!,
//        ).use {
//            return if (it.executeUpdate() != 1) {
//                logger.warn { "Insertion of ABE MPK failed (perhaps, the MPK is already present in the metadata)" }
//                CODE_072_MPK_ALREADY_INITIALIZED
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    override fun getMPK(): String? {
//        logger.info { "Getting the ABE MPK" }
//
//        createSelectStatement(
//            table = masterPublicKeyTable,
//            connection = connection!!,
//        ).use {
//            val rs = it.executeQuery()
//            return if (rs.isBeforeFirst) {
//                rs.next()
//                rs.getString(mpkColumn)
//            } else {
//                null
//            }
//        }
//    }
//
//    override fun addAttribute(
//        newAttribute: Attribute,
//        restoreIfDeleted: Boolean
//    ): OutcomeCode {
//        val attributeName = newAttribute.name
//
//        /** Guard clauses */
//        if (attributeName.isBlank()) {
//            logger.warn { "Attribute name is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//
//
//        logger.info { "Adding the attribute $attributeName in the metadata" }
//
//        /** Check whether the attribute is to be restored or not */
//        val attributeToRestore = if (getAttributes(
//                attributeName = attributeName,
//                status = TupleStatus.DELETED
//            ).isNotEmpty()) {
//            if (restoreIfDeleted) {
//                logger.warn { "Attribute $attributeName was previously deleted, restoring it" }
//                true
//            } else {
//                logger.warn { "Attribute $attributeName was previously deleted" }
//                return CODE_067_ATTRIBUTE_WAS_DELETED
//            }
//        } else {
//            false
//        }
//
//        /** Remove the attribute's entry from the deleted attributes table */
//        if (attributeToRestore) {
//            logger.debug { "Deleting the attribute from table $deletedAttributesTable" }
//            val whereParameters = linkedMapOf<String, Any?>(
//                attributeNameColumn to attributeName
//            )
//            createDeleteStatement(
//                table = deletedAttributesTable,
//                whereParameters = whereParameters,
//                connection = connection!!,
//            ).use {
//                if (it.executeUpdate() != 1) {
//                    val message = "Attribute was deleted but it is not present in $deletedAttributesTable"
//                    logger.error { message }
//                    throw IllegalArgumentException(message)
//                }
//            }
//        }
//
//        /** Add the attribute in the metadata */
//        logger.debug { "Adding the attribute in the metadata" }
//        val attributeValues = arrayListOf<Any?>(
//            attributeName,
//            newAttribute.token,
//            newAttribute.versionNumber,
//            TupleStatus.OPERATIONAL
//        )
//
//        return createInsertStatement(
//            table = attributesTable,
//            values = arrayListOf(attributeValues),
//            connection = connection!!,
//        ).use {
//            if (it.executeUpdate() == 1) {
//                CODE_000_SUCCESS
//            } else {
//                logger.warn { "Attribute $attributeName already present in the metadata" }
//                CODE_065_ATTRIBUTE_ALREADY_EXISTS
//            }
//        }
//    }
//
//    override fun addResource(newResource: Resource): OutcomeCode {
//        val resourceName = newResource.name
//
//        /** Guard clauses */
//        if (resourceName.isBlank()) {
//            logger.warn { "Resource name is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//
//
//        logger.info { "Adding the resource ${newResource.name} to the metadata" }
//
//        if (getResources(
//                resourceName = newResource.name,
//                status = TupleStatus.DELETED
//            ).isNotEmpty()) {
//            logger.warn { "Resource ${newResource.name} was previously deleted" }
//            return CODE_015_RESOURCE_WAS_DELETED
//        }
//
//        /** Add the resource to the metadata */
//        logger.debug { "Adding the resource to the metadata" }
//        val resourceValues = arrayListOf<Any?>(
//            newResource.name,
//            newResource.token,
//            newResource.versionNumber,
//            newResource.reEncryptionThresholdNumber,
//            TupleStatus.OPERATIONAL,
//            newResource.enforcement,
//            newResource.encryptedSymKey?.key?.encodeBase64()
//        )
//        return createInsertStatement(
//            table = resourcesTable,
//            values = arrayListOf(resourceValues),
//            connection = connection!!,
//        ).use {
//            if (it.executeUpdate() == 1) {
//                CODE_000_SUCCESS
//            } else {
//                logger.warn { "Resource ${newResource.name} already present in the metadata" }
//                CODE_003_RESOURCE_ALREADY_EXISTS
//            }
//        }
//    }
//
//    override fun addUsersAttributes(
//        newUsersAttributes: HashSet<UserAttribute>
//    ): OutcomeCode {
//        val size = newUsersAttributes.size
//        if (size == 0) {
//            logger.warn { "No user-attribute assignments given" }
//            return CODE_000_SUCCESS
//        }
//
//        /** Create the list of values to insert in the metadata */
//        logger.info { "Adding $size user-attribute assignments to the metadata (one per row below):" }
//        val usersAttributes = ArrayList<ArrayList<Any?>>(size)
//        newUsersAttributes.forEachIndexed { index, userAttribute ->
//            logger.info {
//                "${index + 1}: user ${userAttribute.username} " +
//                "to attribute ${userAttribute.attributeName}"
//            }
//            usersAttributes.add(createArray(userAttribute))
//        }
//
//        /** Add the user-attribute assignments to the metadata */
//        return createInsertStatement(
//            table = usersAttributesTable,
//            values = usersAttributes,
//            connection = connection!!,
//        ).use {
//            val rowCount = it.executeUpdate()
//            if (rowCount != size) {
//                logger.warn {
//                    "One ore more user-attribute assignments " +
//                            "were not added (expected $size, actual $rowCount)"
//                }
//                run loop@{
//                    /**
//                     * Check whether the operation failed because the
//                     * user-attribute assignments already exists or
//                     * the user or attribute are missing
//                     */
//                    newUsersAttributes.forEach { userAttribute ->
//                        val username = userAttribute.username
//                        val attributeName = userAttribute.attributeName
//
//                        val codeUser = when (getStatus(
//                            name = username,
//                            type = ABACElementType.USER
//                        )) {
//                            TupleStatus.INCOMPLETE -> CODE_053_USER_IS_INCOMPLETE
//                            TupleStatus.OPERATIONAL -> CODE_068_USER_ATTRIBUTE_ASSIGNMENT_ALREADY_EXISTS
//                            TupleStatus.DELETED -> CODE_013_USER_WAS_DELETED
//                            null -> CODE_004_USER_NOT_FOUND
//                        }
//                        if (codeUser != CODE_068_USER_ATTRIBUTE_ASSIGNMENT_ALREADY_EXISTS) {
//                            return@loop codeUser
//                        }
//
//                        val codeAttribute = when (getStatus(
//                            name = attributeName,
//                            type = ABACElementType.ATTRIBUTE
//                        )) {
//                            TupleStatus.OPERATIONAL -> CODE_068_USER_ATTRIBUTE_ASSIGNMENT_ALREADY_EXISTS
//                            TupleStatus.DELETED -> CODE_067_ATTRIBUTE_WAS_DELETED
//                            TupleStatus.INCOMPLETE, null -> CODE_066_ATTRIBUTE_NOT_FOUND
//                        }
//                        if (codeAttribute != CODE_068_USER_ATTRIBUTE_ASSIGNMENT_ALREADY_EXISTS) {
//                            return@loop codeAttribute
//                        }
//                    }
//                    CODE_068_USER_ATTRIBUTE_ASSIGNMENT_ALREADY_EXISTS
//                }
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    override fun addAccessStructuresPermissions(
//        newAccessStructuresPermissions: HashSet<AccessStructurePermission>
//    ): OutcomeCode {
//        val size = newAccessStructuresPermissions.size
//        if (size == 0) {
//            logger.warn { "No access structure-permission assignments given" }
//            return CODE_000_SUCCESS
//        }
//
//        /** Create the list of values to insert in the metadata */
//        logger.info {
//            "Adding $size access structure-permission " +
//                    "assignments to the metadata (one per row below):"
//        }
//        val accessStructuresPermissions = ArrayList<ArrayList<Any?>>(size)
//        newAccessStructuresPermissions.forEachIndexed { index, accessStructurePermission ->
//            logger.info {
//                "${index + 1}: resource ${accessStructurePermission.resourceName} " +
//                "with operation ${accessStructurePermission.operation} " +
//                "and access structure ${accessStructurePermission.accessStructure}"
//            }
//            accessStructuresPermissions.add(createArray(accessStructurePermission))
//        }
//
//        /** Add the access structure-permission assignments to the metadata */
//        return createInsertStatement(
//            table = accessStructuresPermissionsTable,
//            values = accessStructuresPermissions,
//            connection = connection!!,
//        ).use {
//            val rowCount = it.executeUpdate()
//            if (rowCount != size) {
//                logger.warn {
//                    "One ore more access structure-permission assignments " +
//                            "were not added (expected $size, actual $rowCount)"
//                }
//                run loop@{
//                    /**
//                     * Check whether the operation failed because the
//                     * access structure-permission assignment already
//                     * exists or the resource is missing
//                     */
//                    newAccessStructuresPermissions.forEach { accessStructurePermission ->
//                        val resourceName = accessStructurePermission.resourceName
//
//                        val codeResource = when (getStatus(
//                            name = resourceName,
//                            type = ABACElementType.RESOURCE
//                        )) {
//                            TupleStatus.OPERATIONAL -> CODE_069_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS
//                            TupleStatus.DELETED -> CODE_015_RESOURCE_WAS_DELETED
//                            TupleStatus.INCOMPLETE, null -> CODE_006_RESOURCE_NOT_FOUND
//                        }
//                        if (codeResource != CODE_069_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS) {
//                            return@loop codeResource
//                        }
//                    }
//                    CODE_069_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS
//                }
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    override fun getUsers(
//        username: String?,
//        status: TupleStatus?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int,
//    ): HashSet<User> {
//        logger.info { "Getting data of users (offset $offset, limit $limit)" }
//
//        // TODO support invocations by non-admin users
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!username.isNullOrBlank()) {
//            logger.info { "Filtering by matching username $username" }
//            whereParameters[usernameColumn] = username
//        }
//        if (status != null) {
//            logger.info { "Filtering by matching status $status" }
//            whereParameters[statusColumn] = status
//        }
//
//        val users = HashSet<User>()
//
//        createSelectStatement(
//            table = usersTable,
//            whereParameters = whereParameters,
//            limit = limit,
//            offset = offset,
//            connection = connection!!,
//        ).use { firstStatement ->
//            createSelectStatement(
//                table = deletedUsersTable,
//                whereParameters = whereParameters,
//                limit = limit,
//                offset = offset,
//                connection = connection!!,
//            ).use { secondStatement ->
//                val finalStatement = when (status) {
//                    null -> {
//                        "(${firstStatement.asString()}) UNION (${secondStatement.asString()})"
//                    }
//                    TupleStatus.DELETED -> {
//                        secondStatement.asString()
//                    }
//                    else -> {
//                        firstStatement.asString()
//                    }
//                }
//                connection!!.prepareStatement(finalStatement).use {
//                    val rs = it.executeQuery()
//                    while (rs.next()) {
//                        val user = User(
//                            name = sanitizeForHTML(rs.getString(usernameColumn)),
//                            status = status ?: TupleStatus.valueOf(
//                                value = sanitizeForHTML(rs.getString(statusColumn))
//                            ),
//                            versionNumber = rs.getInt(userVersionNumberColumn),
//                            isAdmin = rs.getBoolean(isAdminColumn)
//                        )
//                        user.token = sanitizeForHTML(rs.getString(userTokenColumn))
//                        users.add(user)
//                    }
//                }
//            }
//        }
//
//        logger.debug { "Found ${users.size} users" }
//        return users
//    }
//
//    override fun getAttributes(
//        attributeName: String?,
//        status: TupleStatus?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int,
//    ): HashSet<Attribute> {
//        logger.info { "Getting data of attributes (offset $offset, limit $limit)" }
//
//        // TODO support invocations by non-admin users
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!attributeName.isNullOrBlank()) {
//            logger.info { "Filtering by matching attribute name $attributeName" }
//            whereParameters[attributeNameColumn] = attributeName
//        }
//        if (status != null && status != TupleStatus.DELETED) {
//            logger.info { "Filtering by matching status $status" }
//            whereParameters[statusColumn] = status
//        }
//
//        val attributes = HashSet<Attribute>()
//
//        createSelectStatement(
//            table = attributesTable,
//            whereParameters = whereParameters,
//            limit = limit,
//            offset = offset,
//            connection = connection!!,
//        ).use { firstStatement ->
//            createSelectStatement(
//                table = deletedAttributesTable,
//                whereParameters = whereParameters,
//                limit = limit,
//                offset = offset,
//                connection = connection!!,
//            ).use { secondStatement ->
//                val finalStatement = when (status) {
//                    null -> {
//                        "(${firstStatement.asString()}) UNION (${secondStatement.asString()})"
//                    }
//                    TupleStatus.DELETED -> {
//                        secondStatement.asString()
//                    }
//                    else -> {
//                        firstStatement.asString()
//                    }
//                }
//                connection!!.prepareStatement(finalStatement).use {
//                    val rs = it.executeQuery()
//                    while (rs.next()) {
//                        val attribute = Attribute(
//                            name = sanitizeForHTML(rs.getString(attributeNameColumn)),
//                            status = status ?: TupleStatus.valueOf(
//                                value = sanitizeForHTML(rs.getString(statusColumn))
//                            ),
//                            versionNumber = rs.getInt(attributeVersionNumberColumn),
//                        )
//                        attribute.token = sanitizeForHTML(rs.getString(attributeTokenColumn))
//                        attributes.add(attribute)
//                    }
//                }
//            }
//        }
//
//        logger.debug { "Found ${attributes.size} attributes" }
//        return attributes
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
//        // TODO support invocations by non-admin users
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!resourceName.isNullOrBlank()) {
//            logger.info { "Filtering by matching resources name $resourceName" }
//            whereParameters[resourceNameColumn] = resourceName
//        }
//        if (status != null && status != TupleStatus.DELETED) {
//            logger.info { "Filtering by matching status $status" }
//            whereParameters[statusColumn] = status
//        }
//
//        val resources = HashSet<Resource>()
//
//        createSelectStatement(
//            table = resourcesTable, // TODO should be "if (isAdmin) resourcesTable else resourcesView,"
//            whereParameters = whereParameters,
//            limit = limit,
//            offset = offset,
//            connection = connection!!,
//        ).use { firstStatement ->
//            createSelectStatement(
//                table = deletedResourcesTable,
//                whereParameters = whereParameters,
//                limit = limit,
//                offset = offset,
//                connection = connection!!,
//            ).use { secondStatement ->
//                val finalStatement = if (!isAdmin) {
//                    firstStatement.asString()
//                } else {
//                    when (status) {
//                        null -> {
//                            "(${firstStatement.asString()}) UNION (${secondStatement.asString()})"
//                        }
//                        TupleStatus.DELETED -> {
//                            secondStatement.asString()
//                        }
//                        else -> {
//                            firstStatement.asString()
//                        }
//                    }
//                }
//                connection!!.prepareStatement(finalStatement).use {
//                    val rs = it.executeQuery()
//                    while (rs.next()) {
//                        val resource = Resource(
//                            name = sanitizeForHTML(rs.getString(resourceNameColumn)),
//                            versionNumber = rs.getInt(resourceVersionNumberColumn),
//                            reEncryptionThresholdNumber = rs.getInt(reEncryptionThresholdNumberColumn),
//                            status = status ?: TupleStatus.valueOf(
//                                value = sanitizeForHTML(rs.getString(statusColumn))
//                            ),
//                            enforcement = Enforcement.valueOf(
//                                value = sanitizeForHTML(
//                                    rs.getString(
//                                        enforcementColumn
//                                    )
//                                )
//                            ),
//                            encryptedSymKey = rs.getString(encryptedSymKeyColumn).let { encryptedSymKey ->
//                                if (rs.wasNull() || encryptedSymKey == "") {
//                                    null
//                                } else {
//                                    EncryptedSymKey(
//                                        key = sanitizeForHTML(encryptedSymKey).decodeBase64(),
//                                    )
//
//                                }
//                            },
//                        )
//                        resource.token = sanitizeForHTML(rs.getString(resourceTokenColumn))
//                        resources.add(resource)
//                    }
//                }
//            }
//        }
//
//        logger.debug { "Found ${resources.size} resources" }
//        return resources
//    }
//
//    override fun getUsersAttributes(
//        username: String?,
//        attributeName: String?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int,
//    ): HashSet<UserAttribute> {
//        logger.info { "Getting data of user-attribute assignments (offset $offset, limit $limit)" }
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!username.isNullOrBlank()) {
//            logger.info { "Filtering by matching username $username" }
//            whereParameters[usernameColumn] = username
//        }
//        if (!attributeName.isNullOrBlank()) {
//            logger.info { "Filtering by matching attribute name $attributeName" }
//            whereParameters[attributeNameColumn] = attributeName
//        }
//        if (whereParameters.isEmpty()) {
//            val message = "A user or attribute name has to be specified"
//            logger.error { message }
//            throw IllegalArgumentException(message)
//        }
//
//        val usersAttributes = HashSet<UserAttribute>()
//
//        createSelectStatement(
//            table = if (isAdmin) usersAttributesTable else usersAttributesView,
//            whereParameters = whereParameters,
//            limit = limit,
//            offset = offset,
//            connection = connection!!,
//        ).use {
//            val rs = it.executeQuery()
//            while (rs.next()) {
//                val attributeValue: String?
//                rs.getString(attributeValueColumn).apply {
//                    attributeValue = if (rs.wasNull() || this == "") {
//                        null
//                    } else {
//                        sanitizeForHTML(this)
//                    }
//                }
//                val userAttribute = UserAttribute(
//                    username = sanitizeForHTML(rs.getString(usernameColumn)),
//                    attributeName = sanitizeForHTML(rs.getString(attributeNameColumn)),
//                    attributeValue = attributeValue,
//                )
//                userAttribute.updateSignature(
//                    newSignature = sanitizeForHTML(rs.getString(signatureColumn)).decodeBase64(),
//                    newSigner = ADMIN,
//                )
//                usersAttributes.add(userAttribute)
//            }
//        }
//        logger.debug { "Found ${usersAttributes.size} user-attribute assignments" }
//        return usersAttributes
//    }
//
//    override fun getAccessStructuresPermissions(
//        resourceName: String?,
//        resourceVersionNumber: Int?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int,
//    ): HashSet<AccessStructurePermission> {
//        logger.info { "Getting data of access structure-permission assignments (offset $offset, limit $limit)" }
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!resourceName.isNullOrBlank()) {
//            logger.info { "Filtering by matching resource name $resourceName" }
//            whereParameters[resourceNameColumn] = resourceName
//        }
//        if (resourceVersionNumber != null) {
//            logger.info { "Filtering by matching resource version number $resourceVersionNumber" }
//            whereParameters[resourceVersionNumberColumn] = resourceVersionNumber
//        }
//
//        val accessStructuresPermissions = HashSet<AccessStructurePermission>()
//
//        createSelectStatement(
//            table = accessStructuresPermissionsTable, // TODO should be "if (isAdmin) tuplesAccessStructurePermissionTable else tuplesAccessStructurePermissionView,"
//            whereParameters = whereParameters,
//            limit = limit,
//            offset = offset,
//            connection = connection!!,
//        ).use {
//            val rs = it.executeQuery()
//            while (rs.next()) {
//                val accessStructurePermission = AccessStructurePermission(
//                    resourceName = sanitizeForHTML(rs.getString(resourceNameColumn)),
//                    resourceToken = sanitizeForHTML(rs.getString(resourceTokenColumn)),
//                    accessStructure = sanitizeForHTML(rs.getString(accessStructureColumn)).decodeBase64String(),
//                    resourceVersionNumber = rs.getInt(resourceVersionNumberColumn),
//                    operation = Operation.valueOf(sanitizeForHTML(rs.getString(operationColumn))),
//                    encryptedSymKey = EncryptedSymKey(
//                        sanitizeForHTML(rs.getString(encryptedSymKeyColumn)).decodeBase64()
//                    ),
//                )
//                accessStructurePermission.updateSignature(
//                    newSignature = sanitizeForHTML(rs.getString(signatureColumn)).decodeBase64(),
//                    newSigner = sanitizeForHTML(rs.getString(signerTokenColumn)),
//                )
//                accessStructuresPermissions.add(accessStructurePermission)
//            }
//        }
//        logger.debug { "Found ${accessStructuresPermissions.size} access structure-permission assignments" }
//        return accessStructuresPermissions
//    }
//
//    override fun getUserPublicKey(
//        name: String?,
//        token: String?,
//        asymKeyType: AsymKeysType,
//    ): ByteArray? {
//        logger.info { "Getting public key of type $asymKeyType of user" }
//
//        // TODO support invocations by non-admin users
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!name.isNullOrBlank()) {
//            logger.info { "Filtering by matching name $name" }
//            whereParameters[usernameColumn] = name
//        }
//        if (!token.isNullOrBlank()) {
//            logger.info { "Filtering by matching token $token" }
//            whereParameters[userTokenColumn] = token
//        }
//        if (whereParameters.isEmpty()) {
//            val message = "A name or a token has to be specified"
//            logger.error { message }
//            throw IllegalArgumentException(message)
//        }
//
//        val whereNotParameters = LinkedHashMap<String, Any?>()
//        whereNotParameters[statusColumn] = TupleStatus.INCOMPLETE
//
//        val selectedColumns = arrayListOf(
//            if (asymKeyType == AsymKeysType.ENC)
//                asymEncPublicKeyColumn
//            else
//                asymSigPublicKeyColumn
//        )
//        var asymPublicKeyBytes: ByteArray? = null
//
//        /**
//         * Search the key in both the users
//         * table and the deleted users table
//         */
//        createSelectStatement(
//            table = usersTable,
//            whereParameters = whereParameters,
//            selectedColumns = selectedColumns,
//            whereNotParameters = whereNotParameters,
//            limit = 1,
//            offset = 0,
//            connection = connection!!,
//        ).use { firstStatement ->
//            createSelectStatement(
//                table = deletedUsersTable,
//                whereParameters = whereParameters,
//                selectedColumns = selectedColumns,
//                limit = 1,
//                offset = 0,
//                connection = connection!!,
//            ).use { secondStatement ->
//                val finalStatement = "(${firstStatement.asString()}) UNION (${secondStatement.asString()})"
//                connection!!.prepareStatement(finalStatement).use {
//                    val rs = it.executeQuery()
//                    if (rs.next()) {
//                        asymPublicKeyBytes = sanitizeForHTML(
//                            rs.getString(
//                                if (asymKeyType == AsymKeysType.ENC)
//                                    asymEncPublicKeyColumn
//                                else
//                                    asymSigPublicKeyColumn
//                            )
//                        ).decodeBase64()
//                    }
//                }
//            }
//        }
//
//        logger.debug { "Public key was${ if (asymPublicKeyBytes == null) " not" else ""} found" }
//        return asymPublicKeyBytes
//    }
//
//    override fun getVersionNumber(
//        name: String?,
//        token: String?,
//        elementType: ABACElementType,
//    ): Int? {
//        logger.info { "Getting version number of a $elementType" }
//
//        // TODO support invocations by non-admin users
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!name.isNullOrBlank()) {
//            logger.info { "Filtering by matching name $name" }
//            whereParameters[when (elementType) {
//                ABACElementType.USER -> usernameColumn
//                ABACElementType.ATTRIBUTE -> attributeNameColumn
//                ABACElementType.RESOURCE -> resourceNameColumn
//            }] = name
//        }
//        if (!token.isNullOrBlank()) {
//            logger.info { "Filtering by matching token $token" }
//            whereParameters[when (elementType) {
//                ABACElementType.USER -> userTokenColumn
//                ABACElementType.ATTRIBUTE -> attributeTokenColumn
//                ABACElementType.RESOURCE -> resourceTokenColumn
//            }] = token
//        }
//        if (whereParameters.isEmpty()) {
//            val message = "A name or a token has to be specified"
//            logger.error { message }
//            throw IllegalArgumentException(message)
//        }
//        whereParameters[statusColumn] = TupleStatus.OPERATIONAL
//
//        val selectedColumn = when (elementType) {
//            ABACElementType.USER -> userVersionNumberColumn
//            ABACElementType.ATTRIBUTE -> attributeVersionNumberColumn
//            ABACElementType.RESOURCE -> resourceVersionNumberColumn
//        }
//        val selectedColumns = arrayListOf(selectedColumn)
//
//        var versionNumber: Int? = null
//
//        createSelectStatement(
//            table = when (elementType) {
//                ABACElementType.USER -> usersTable
//                ABACElementType.ATTRIBUTE -> attributesTable
//                ABACElementType.RESOURCE -> resourcesTable
//            },
//            whereParameters = whereParameters,
//            selectedColumns = selectedColumns,
//            limit = 1,
//            offset = 0,
//            connection = connection!!,
//        ).use {
//            val rs = it.executeQuery()
//            if (rs.next()) {
//                versionNumber = rs.getInt(selectedColumn)
//            }
//        }
//        logger.debug { "Version number was ${ if (versionNumber == null) "not " else ""} found" }
//        return versionNumber
//    }
//
//    override fun getToken(
//        name: String,
//        type: ABACElementType
//    ): String? {
//        logger.info { "Getting token of a $type" }
//
//        // TODO support invocations by non-admin users
//        val whereParameters = LinkedHashMap<String, Any?>()
//        logger.info { "Filtering by matching name $name" }
//        whereParameters[
//                when (type) {
//                    ABACElementType.USER -> usernameColumn
//                    ABACElementType.ATTRIBUTE -> attributeNameColumn
//                    ABACElementType.RESOURCE -> resourceNameColumn
//                }
//        ] = name
//
//        val table1: String
//        val table2: String
//
//        val selectedColumn = when (type) {
//            ABACElementType.USER -> { table1 = usersTable; table2 = deletedUsersTable; userTokenColumn }
//            ABACElementType.ATTRIBUTE -> { table1 = attributesTable; table2 = deletedAttributesTable; attributeTokenColumn }
//            ABACElementType.RESOURCE -> { table1 = resourcesTable; table2 = deletedResourcesTable; resourceTokenColumn }
//        }
//        val selectedColumns = arrayListOf(selectedColumn)
//
//        var token: String? = null
//
//        createSelectStatement(
//            table = table1,
//            whereParameters = whereParameters,
//            selectedColumns = selectedColumns,
//            limit = 1,
//            offset = 0,
//            connection = connection!!,
//        ).use { firstStatement ->
//            createSelectStatement(
//                table = table2,
//                whereParameters = whereParameters,
//                selectedColumns = selectedColumns,
//                limit = 1,
//                offset = 0,
//                connection = connection!!,
//            ).use { secondStatement ->
//                connection!!.prepareStatement(
//                    "(${firstStatement.asString()}) UNION (${secondStatement.asString()})"
//                ).use {
//                    val rs = it.executeQuery()
//                    if (rs.next()) {
//                        token = rs.getString(selectedColumn)
//                    }
//                }
//            }
//        }
//        logger.debug { "Token was ${ if (token == null) "not" else ""} found" }
//        return token
//    }
//
//    override fun getStatus(
//        name: String?,
//        token: String?,
//        type: ABACElementType
//    ): TupleStatus? {
//        logger.debug { "Getting the status of a $type" }
//
//        // TODO support invocations by non-admin users
//
//        val table1: String
//        val table2: String
//
//        when (type) {
//            ABACElementType.USER -> { table1 = usersTable; table2 = deletedUsersTable; }
//            ABACElementType.ATTRIBUTE -> { table1 = attributesTable; table2 = deletedAttributesTable; }
//            ABACElementType.RESOURCE -> { table1 = resourcesTable; table2 = deletedResourcesTable; }
//        }
//        val selectedColumns = arrayListOf(statusColumn)
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!name.isNullOrBlank()) {
//            logger.info { "Filtering by matching name $name" }
//            whereParameters[
//                    when (type) {
//                        ABACElementType.USER -> usernameColumn
//                        ABACElementType.ATTRIBUTE -> attributeNameColumn
//                        ABACElementType.RESOURCE -> resourceNameColumn
//                    }
//            ] = name
//        }
//        if (!token.isNullOrBlank()) {
//            logger.info { "Filtering by matching token $token" }
//            whereParameters[
//                    when (type) {
//                        ABACElementType.USER -> userTokenColumn
//                        ABACElementType.ATTRIBUTE -> attributeTokenColumn
//                        ABACElementType.RESOURCE -> resourceTokenColumn
//                    }
//            ] = token
//        }
//        if (whereParameters.isEmpty()) {
//            val message = "A name or a token has to be specified"
//            logger.error { message }
//            throw IllegalArgumentException(message)
//        }
//
//        createSelectStatement(
//            table = table1,
//            whereParameters = whereParameters,
//            selectedColumns = selectedColumns,
//            connection = connection!!,
//        ).use { firstStatement ->
//            createSelectStatement(
//                table = table2,
//                whereParameters = whereParameters,
//                selectedColumns = selectedColumns,
//                connection = connection!!,
//            ).use { secondStatement ->
//                connection!!.prepareStatement(
//                    "(${firstStatement.asString()}) UNION (${secondStatement.asString()})"
//                ).use {
//                    val rs = it.executeQuery()
//                    return if (rs.isBeforeFirst) {
//                        rs.next()
//                        TupleStatus.valueOf(rs.getString(statusColumn))
//                    } else {
//                        null
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * In this implementation, move the [attributeName]
//     * to the deleted attributes table
//     */
//    override fun deleteAttribute(
//        attributeName: String
//    ): OutcomeCode {
//
//        if (attributeName == ADMIN) {
//            logger.warn { "Cannot delete the $ADMIN attribute" }
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }
//        if (attributeName.isBlank()) {
//            logger.warn { "Attribute name is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//
//
//        logger.info { "Deleting attribute $attributeName" }
//
//        logger.debug { "Moving the attribute from table $attributesTable to table $deletedAttributesTable" }
//        val whereParameters = linkedMapOf<String, Any?>(
//            attributeNameColumn to attributeName
//        )
//        val selectedColumns = arrayListOf(
//            attributeNameColumn,
//            attributeTokenColumn,
//            attributeVersionNumberColumn,
//            "'${TupleStatus.DELETED}'"
//        )
//        createSelectStatement(
//            table = attributesTable,
//            selectedColumns = selectedColumns,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use { selectStatement ->
//            connection!!.prepareStatement(
//                "INSERT INTO $deletedAttributesTable (${selectStatement.asString()})"
//            ).use {
//                if (it.executeUpdate() != 1) {
//                    logger.warn { "Attribute $attributeName not found in the metadata" }
//                    return when (getStatus(
//                        name = attributeName,
//                        type = ABACElementType.ATTRIBUTE
//                    )) {
//                        TupleStatus.DELETED -> CODE_067_ATTRIBUTE_WAS_DELETED
//                        null -> CODE_066_ATTRIBUTE_NOT_FOUND
//                        else -> {
//                            val message = "Attribute not found but attribute is in table"
//                            logger.error { message }
//                            throw IllegalArgumentException(message)
//                        }
//                    }
//                }
//            }
//        }
//
//        logger.debug { "Deleting the attribute from table $attributesTable" }
//        createDeleteStatement(
//            table = attributesTable,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            return if (it.executeUpdate() != 1) {
//                logger.warn { "Attribute $attributeName not found in the metadata" }
//                CODE_066_ATTRIBUTE_NOT_FOUND
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    /**
//     * In this implementation, move the [resourceName]
//     * to the deleted resources table
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
//        logger.debug { "Moving the resources from table $resourcesTable to table $deletedResourcesTable" }
//        val whereParameters = linkedMapOf<String, Any?>(
//            resourceNameColumn to resourceName
//        )
//        val selectedColumns = arrayListOf(
//            resourceNameColumn,
//            resourceTokenColumn,
//            resourceVersionNumberColumn,
//            reEncryptionThresholdNumberColumn,
//            "'${TupleStatus.DELETED}'",
//            enforcementColumn,
//            encryptedSymKeyColumn
//        )
//        createSelectStatement(
//            table = resourcesTable,
//            selectedColumns = selectedColumns,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use { selectStatement ->
//            connection!!.prepareStatement(
//                "INSERT INTO $deletedResourcesTable (${selectStatement.asString()})"
//            ).use {
//                if (it.executeUpdate() != 1) {
//                    logger.warn { "Resource $resourceName not found in the metadata" }
//                    return when (getStatus(
//                        name = resourceName,
//                        type = ABACElementType.RESOURCE
//                    )) {
//                        TupleStatus.DELETED -> CODE_015_RESOURCE_WAS_DELETED
//                        null -> CODE_006_RESOURCE_NOT_FOUND
//                        else -> {
//                            val message = "Resource not found but resource is in table"
//                            logger.error { message }
//                            throw IllegalArgumentException(message)
//                        }
//                    }
//                }
//            }
//        }
//
//        logger.debug { "Deleting the resource from table $resourcesTable" }
//        createDeleteStatement(
//            table = resourcesTable,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            return if (it.executeUpdate() != 1) {
//                logger.warn { "Resource $resourceName not found in the metadata" }
//                CODE_006_RESOURCE_NOT_FOUND
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    override fun deleteUsersAttributes(
//        username: String?,
//        attributeName: String?,
//    ): OutcomeCode {
//        logger.info { "Deleting user-attribute assignments" }
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!username.isNullOrBlank()) {
//            logger.info { "Filtering by matching username $username" }
//            whereParameters[usernameColumn] = username
//        }
//        if (!attributeName.isNullOrBlank()) {
//            logger.info { "Filtering by matching attribute name $attributeName" }
//            whereParameters[attributeNameColumn] = attributeName
//        }
//        /**
//         * At least one parameter has to be specified, otherwise
//         * the delete operation would delete all rows in the table
//         */
//        if (whereParameters.isEmpty()) {
//            val message = "No arguments were provided for delete operation"
//            logger.error { message }
//            throw IllegalArgumentException(message)
//        }
//
//        logger.debug { "Deleting the user-attribute assignments from table $usersAttributesTable" }
//        createDeleteStatement(
//            table = usersAttributesTable,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            val affectedRows = it.executeUpdate()
//            logger.debug { "$affectedRows user-attribute assignments were deleted" }
//            return if (affectedRows <= 0) {
//                logger.warn { "No user-attribute assignment was deleted" }
//                /**
//                 * Check whether the operation failed because there
//                 * are no user-attribute assignments or because the
//                 * user or the attribute is missing or was deleted
//                 */
//                val userExists = if (username != null) {
//                    when (getStatus(
//                        name = username,
//                        type = ABACElementType.USER
//                    )) {
//                        TupleStatus.OPERATIONAL -> CODE_070_USER_ATTRIBUTE_ASSIGNMENT_NOT_FOUND
//                        TupleStatus.DELETED -> CODE_013_USER_WAS_DELETED
//                        TupleStatus.INCOMPLETE, null -> CODE_004_USER_NOT_FOUND
//                    }
//                } else {
//                    CODE_070_USER_ATTRIBUTE_ASSIGNMENT_NOT_FOUND
//                }
//
//                if (userExists == CODE_070_USER_ATTRIBUTE_ASSIGNMENT_NOT_FOUND) {
//                    if (attributeName != null) {
//                        when (getStatus(
//                            name = attributeName,
//                            type = ABACElementType.ATTRIBUTE
//                        )) {
//                            TupleStatus.OPERATIONAL -> CODE_070_USER_ATTRIBUTE_ASSIGNMENT_NOT_FOUND
//                            TupleStatus.DELETED -> CODE_067_ATTRIBUTE_WAS_DELETED
//                            TupleStatus.INCOMPLETE, null -> CODE_066_ATTRIBUTE_NOT_FOUND
//                        }
//                    } else {
//                        CODE_070_USER_ATTRIBUTE_ASSIGNMENT_NOT_FOUND
//                    }
//                } else {
//                    userExists
//                }
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    override fun deleteAccessStructuresPermissions(
//        resourceName: String,
//        operation: Operation?
//    ): OutcomeCode {
//        logger.info {
//            "Deleting access structure-permission assignments for " +
//            "resource name $resourceName and operation $operation"
//        }
//
//        logger.info { "Filtering by matching resource name $resourceName " }
//        val whereParameters = linkedMapOf<String, Any?>(
//            resourceNameColumn to resourceName
//        )
//        if (operation != null) {
//            logger.info { "Filtering by matching operation $operation" }
//            whereParameters[operationColumn] = operation
//        }
//
//        logger.debug {
//            "Deleting the access structure-permission " +
//                    "assignments from table $accessStructuresPermissionsTable"
//        }
//        createDeleteStatement(
//            table = accessStructuresPermissionsTable,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            val affectedRows = it.executeUpdate()
//            logger.debug { "$affectedRows access structure-permission assignments were deleted" }
//            return if (affectedRows <= 0) {
//                logger.warn { "No access structure-permission assignment was deleted" }
//                /**
//                 * Check whether the operation failed because there
//                 * are no access structure-permission assignments or
//                 * because the resource is missing or was deleted
//                 */
//                when (getStatus(
//                    name = resourceName,
//                    type = ABACElementType.RESOURCE
//                )) {
//                    TupleStatus.OPERATIONAL -> CODE_071_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_NOT_FOUND
//                    TupleStatus.DELETED -> CODE_015_RESOURCE_WAS_DELETED
//                    TupleStatus.INCOMPLETE, null -> CODE_006_RESOURCE_NOT_FOUND
//                }
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    /**
//     * In this implementation, the update of the resource's
//     * token is done with "ON UPDATE CASCADE"
//     */
//    override fun updateResourceTokenAndVersionNumber(
//        resourceName: String,
//        oldResourceToken: String,
//        newResourceToken: String,
//        newResourceVersionNumber: Int,
//    ): OutcomeCode {
//        logger.info { "Updating the token and the version number of $resourceName" }
//
//        val whereParameters = linkedMapOf<String, Any?>(
//            resourceNameColumn to resourceName
//        )
//        val values = linkedMapOf<String, Any>(
//            resourceTokenColumn to newResourceToken,
//            resourceVersionNumberColumn to newResourceVersionNumber,
//        )
//
//        return createUpdateStatement(
//            table = resourcesTable,
//            values = values,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            if (it.executeUpdate() != 1) {
//                logger.warn { "Resource $resourceName not found in the metadata" }
//                when (getStatus(
//                    name = resourceName,
//                    type = ABACElementType.RESOURCE
//                )) {
//                    TupleStatus.DELETED -> CODE_015_RESOURCE_WAS_DELETED
//                    null -> CODE_006_RESOURCE_NOT_FOUND
//                    else -> {
//                        val message = "Resource not found but resource is in table"
//                        logger.error { message }
//                        throw IllegalArgumentException(message)
//                    }
//                }
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    override fun updateAccessStructurePermission(
//        updatedAccessStructurePermission: AccessStructurePermission
//    ): OutcomeCode {
//        logger.info {
//            "Updating the access structure-permission assignment of resource " +
//            "${updatedAccessStructurePermission.resourceName} "
//        }
//
//        val whereParameters = linkedMapOf<String, Any?>(
//            resourceNameColumn to updatedAccessStructurePermission.resourceName,
//            operationColumn to updatedAccessStructurePermission.operation.toString()
//        )
//        val values = linkedMapOf<String, Any>(
//            resourceTokenColumn to updatedAccessStructurePermission.resourceToken,
//            accessStructureColumn to updatedAccessStructurePermission.accessStructure.encodeBase64(),
//            encryptedSymKeyColumn to updatedAccessStructurePermission.encryptedSymKey!!.key.encodeBase64(),
//            resourceVersionNumberColumn to updatedAccessStructurePermission.resourceVersionNumber,
//            signerTokenColumn to updatedAccessStructurePermission.signer!!,
//            signatureColumn to updatedAccessStructurePermission.signature!!.encodeBase64()
//        )
//
//        createUpdateStatement(
//            table = accessStructuresPermissionsTable,
//            values = values,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            return if (it.executeUpdate() != 1) {
//                logger.warn { "Access structure-permission assignment was not found" }
//                CODE_071_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_NOT_FOUND
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    override fun updateUserABEKey(
//        username: String,
//        newEncryptedUserABEKey: ByteArray?
//    ): OutcomeCode {
//        logger.info { "Updating the ABE key of user $username" }
//
//        val whereParameters = linkedMapOf<String, Any?>(
//            usernameColumn to username,
//            statusColumn to TupleStatus.OPERATIONAL
//        )
//        val values = linkedMapOf<String, Any>(
//            abeSecretKeyColumn to (newEncryptedUserABEKey?.encodeBase64() ?: ""),
//        )
//
//        return createUpdateStatement(
//            table = usersTable,
//            values = values,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            if (it.executeUpdate() != 1) {
//                logger.warn { "User $username not found in the metadata" }
//                when (getStatus(
//                    name = username,
//                    type = ABACElementType.USER
//                )) {
//                    TupleStatus.INCOMPLETE -> CODE_053_USER_IS_INCOMPLETE
//                    TupleStatus.DELETED -> CODE_013_USER_WAS_DELETED
//                    null -> CODE_004_USER_NOT_FOUND
//                    else -> {
//                        val message = "User not found but user is in table"
//                        logger.error { message }
//                        throw IllegalArgumentException(message)
//                    }
//                }
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    override fun getUserABEKey(
//        username: String,
//    ): ByteArray? {
//        // TODO add the isAdmin parameter and toggle below the table to look in for the ABE key
//        logger.info { "Getting the ABE key of user $username" }
//        val msk: ByteArray?
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        whereParameters[usernameColumn] = username
//
//        createSelectStatement(
//            table = usersInfoView,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            val rs = it.executeQuery()
//            msk = if (rs.next()) {
//                val abeKey: ByteArray?
//                rs.getString(abeSecretKeyColumn).apply {
//                    abeKey = if (rs.wasNull() || this == "") {
//                        null
//                    } else {
//                        sanitizeForHTML(this).decodeBase64()
//                    }
//                }
//                abeKey
//            } else {
//                null
//            }
//        }
//
//        logger.debug { "Key was${if (msk != null) {""} else {" not"}} found" }
//        return msk
//    }
//
//    override fun updateAttributeTokenAndVersionNumber(
//        attributeName: String,
//        oldAttributeToken: String,
//        newAttributeToken: String,
//        newVersionNumber: Int
//    ): OutcomeCode {
//        logger.info { "Updating the token and the version number of $attributeName" }
//
//        val whereParameters = linkedMapOf<String, Any?>(
//            attributeNameColumn to attributeName
//        )
//        val values = linkedMapOf<String, Any>(
//            attributeTokenColumn to newAttributeToken,
//            attributeVersionNumberColumn to newVersionNumber,
//        )
//
//        return createUpdateStatement(
//            table = attributesTable,
//            values = values,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            if (it.executeUpdate() != 1) {
//                logger.warn { "Attribute $attributeName not found in the metadata" }
//                when (getStatus(
//                    name = attributeName,
//                    type = ABACElementType.ATTRIBUTE
//                )) {
//                    TupleStatus.DELETED -> CODE_067_ATTRIBUTE_WAS_DELETED
//                    null -> CODE_066_ATTRIBUTE_NOT_FOUND
//                    else -> {
//                        val message = "Attribute not found but attribute is in table"
//                        logger.error { message }
//                        throw IllegalArgumentException(message)
//                    }
//                }
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//
//
//    /** In this implementation, set the "autocommit" MySQL flag to OFF */
//    override fun lock(): OutcomeCode {
//        return if (locks == 0) {
//            logger.info { "Locking the status of the MM" }
//            try {
//                if (connection == null || connection!!.isClosed) {
//                    connection = DriverManager.getConnection(jDBUrl, connectionProperties)
//                    connection!!.autoCommit = false
//                    locks++
//                    CODE_000_SUCCESS
//                } else {
//                    /** A lock has been set but not released */
//                    logger.warn { "A lock has been set but not released" }
//                    connection!!.rollback()
//                    connection!!.close()
//                    locks = 0
//                    CODE_031_LOCK_CALLED_IN_INCONSISTENT_STATUS
//                }
//            } catch (e: CommunicationsException) {
//                if ((e.message ?: "").contains("Communications link failure")) {
//                    logger.warn { "MM MySQL - connection timeout" }
//                    CODE_045_MM_CONNECTION_TIMEOUT
//                } else {
//                    throw e
//                }
//            } catch (e: SQLException) {
//                if ((e.message ?: "").contains("Access denied for user")) {
//                    logger.warn { "MM MySQL - access denied for user" }
//                    CODE_055_ACCESS_DENIED_TO_MM
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
//    /** In this implementation, rollback the transaction */
//    override fun rollback(): OutcomeCode {
//        return if (locks == 1) {
//            logger.info { "Rollback the status of the MM" }
//            locks--
//            if (!connection!!.isClosed) {
//                connection!!.rollback()
//                connection!!.close()
//                CODE_000_SUCCESS
//            } else {
//                /** The lock has already been released */
//                logger.warn { "The lock was released but the connection was not closed" }
//                CODE_033_ROLLBACK_CALLED_IN_INCONSISTENT_STATUS
//            }
//        } else if (locks > 1) {
//            locks--
//            logger.debug { "Decrement lock number to $locks" }
//            CODE_000_SUCCESS
//        } else {
//            logger.warn { "MM rollback number is zero or negative ($locks)" }
//            CODE_033_ROLLBACK_CALLED_IN_INCONSISTENT_STATUS
//        }
//    }
//
//    /** In this implementation, commit the transaction */
//    override fun unlock(): OutcomeCode {
//        return if (locks == 1) {
//            logger.info { "Unlocking the status of the MM" }
//            locks--
//            if (!connection!!.isClosed) {
//                try {
//                    connection!!.commit()
//                } catch (e: SQLException) {
//                    logger.warn { "Commit of MySQL database failed" }
//                    CODE_034_UNLOCK_FAILED
//                }
//                connection!!.close()
//                CODE_000_SUCCESS
//            } else {
//                /** The lock has already been released */
//                logger.warn { "The lock was released but the connection was not closed" }
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
//     * Create and return an array list from the given [userAttribute]
//     * to match the order of the columns of the user-attribute
//     * assignments table
//     */
//    private fun createArray(userAttribute: UserAttribute): ArrayList<Any?> {
//        return arrayListOf(
//            userAttribute.username,
//            userAttribute.attributeName,
//            userAttribute.attributeValue ?: "",
//            userAttribute.signature!!.encodeBase64()
//        )
//    }
//
//    /**
//     * Create and return an array list from the given [accessStructurePermission]
//     * to match the order of the columns of the access structure-permission
//     * assignment table. We encode in Base64 the access structure as it may contain
//     * HTML chars (i.e., <, >) TODO is there a better way?
//     */
//    private fun createArray(accessStructurePermission: AccessStructurePermission): ArrayList<Any?> {
//        return arrayListOf(
//            accessStructurePermission.resourceName,
//            accessStructurePermission.resourceToken,
//            accessStructurePermission.accessStructure.encodeBase64(),
//            accessStructurePermission.operation,
//            accessStructurePermission.encryptedSymKey!!.key.encodeBase64(),
//            accessStructurePermission.resourceVersionNumber,
//            accessStructurePermission.signer,
//            accessStructurePermission.signature!!.encodeBase64()
//        )
//    }
//}
