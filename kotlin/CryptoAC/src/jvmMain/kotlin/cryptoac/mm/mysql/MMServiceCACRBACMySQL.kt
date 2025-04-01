//package cryptoac.mm.mysql
//
//import com.mysql.cj.jdbc.exceptions.CommunicationsException
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
//import cryptoac.code.CodeServiceParameters
//import cryptoac.code.*
//import cryptoac.mm.MMType
//import cryptoac.tuple.*
//import mu.KotlinLogging
//import java.io.BufferedReader
//import java.io.FileNotFoundException
//import java.io.InputStreamReader
//import java.security.PublicKey
//import java.sql.*
//import java.util.*
//import kotlin.collections.ArrayList
//import kotlin.collections.HashSet
//import kotlin.collections.LinkedHashMap
//
//private val logger = KotlinLogging.logger {}
//
//// TODO use OPA to filter queries to MySQL database?
//
//// TODO the databaseRBAC.sql code contains triggers to
////  manage the update of tokens, but they should be
////  removed (because they are useless), right?
//
///**
// * Class implementing the methods for invoking the APIs of a MySQL8+
// * database configured with the given [mmMySQLServiceParameters] for RB-CAC
// *
// * Note that the database is configured to avoid the disclosure of
// * the AC policy to users with tokens and views.
// * Note that the name of the user connecting to the database is the
// * same as the name in the AC policy.
// * Note that any value represented by a byte array is converted to
// * BASE64 before being stored
// */
//class MMServiceCACRBACMySQL(
//    private val mmMySQLServiceParameters: MMServiceMySQLParameters
//) : MMServiceCACRBAC, MMServiceMySQL {
//
//    companion object {
//        /** The name of the schema in the database */
//        private const val databaseName = "cryptoac"
//        const val usersTable = "$databaseName.users"
//        const val deletedUsersTable = "$databaseName.deletedUsers"
//        const val usersView = "$databaseName.user_specific_users"
//        const val usersStatusView = "$databaseName.user_specific_users_status"
//        const val rolesTable = "$databaseName.roles"
//        const val deletedRolesTable = "$databaseName.deletedRoles"
//        const val resourcesTable = "$databaseName.resources"
//        const val deletedResourcesTable = "$databaseName.deletedResources"
//        const val resourcesView = "$databaseName.user_specific_resources"
//        const val usersRolesTable = "$databaseName.usersRoles"
//        const val usersRolesView = "$databaseName.user_specific_usersRoles"
//        const val rolesPermissionsTable = "$databaseName.rolesPermissions"
//        const val rolesPermissionsView = "$databaseName.user_specific_rolesPermissions"
//        const val userTokenColumn = "userToken"
//        const val roleTokenColumn = "roleToken"
//        const val resourceTokenColumn = "resourceToken"
//        const val asymEncPublicKeyColumn = "asymEncPublicKey"
//        const val asymSigPublicKeyColumn = "asymSigPublicKey"
//        const val userVersionNumberColumn = "userVersionNumber"
//        const val roleVersionNumberColumn = "roleVersionNumber"
//        const val resourceVersionNumberColumn = "resourceVersionNumber"
//        const val reEncryptionThresholdNumberColumn = "reEncryptionThresholdNumber"
//        const val encryptedAsymEncPublicKeyColumn = "encryptedAsymEncPublicKey"
//        const val encryptedAsymEncPrivateKeyColumn = "encryptedAsymEncPrivateKey"
//        const val encryptedAsymSigPublicKeyColumn = "encryptedAsymSigPublicKey"
//        const val encryptedAsymSigPrivateKeyColumn = "encryptedAsymSigPrivateKey"
//        const val encryptedSymKeyColumn = "encryptedSymKey"
//        const val signatureColumn = "signature"
//        const val operationColumn = "operation"
//        const val signerTokenColumn = "signerToken"
//        const val usernameColumn = "username"
//        const val roleNameColumn = "roleName"
//        const val resourceNameColumn = "resourceName"
//        const val statusColumn = "status"
//        const val enforcementColumn = "enforcement"
//        const val isAdminColumn = "isAdmin"
//
//        /** The file containing SQL commands to initialize the database */
//        const val INIT_RBAC_SQL_CODE = "/cryptoac/MySQL/databaseRBAC.sql"
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
//     * views, and triggers in the database. Deny
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
//        val sqlFile = MMServiceCACRBACMySQL::class.java.getResourceAsStream(INIT_RBAC_SQL_CODE)
//        if (sqlFile == null) {
//            val message = "Initialization SQL file $INIT_RBAC_SQL_CODE not found"
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
//        logger.info { "No action required to initialize the MM RBAC MySQL service" }
//    }
//
//    override fun deinit() {
//        logger.info { "No action required to de-initialize the MM RBAC MySQL service" }
//    }
//
//    /**
//     * In this implementation, add the [newAdmin] in the
//     * metadata by setting the public keys and token of the
//     * [newAdmin] and return the outcome code
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
//            TupleStatus.OPERATIONAL,
//            newAdmin.isAdmin
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
//        /** Update the user's metadata */
//        logger.debug { "Updating the user's metadata" }
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
//                    table = if (mmMySQLServiceParameters.username != username) usersTable else usersStatusView,
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
//     * in the "initUser" function
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
//
//        if (getUsers(
//                username = username,
//                status = TupleStatus.DELETED
//            ).isNotEmpty()) {
//            logger.warn { "User $username was previously deleted" }
//            return CodeServiceParameters(CODE_013_USER_WAS_DELETED)
//        }
//
//        /** TODO check password generation */
//        /* Brunello: The java.util.Random class is not a cryptographically secure generator. Alternatively, you can
//         * use the java.security.SecureRandom class (which extends java.util.Random).
//         *
//         * See:
//         *  - https://docs.oracle.com/javase/8/docs/api/java/util/Random.html
//         *  - https://docs.oracle.com/javase/8/docs/api/java/security/SecureRandom.html
//         *  - https://github.com/KotlinCrypto/secure-random
//         */
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
//            TupleStatus.INCOMPLETE,
//            newUser.isAdmin
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
//                "GRANT SELECT ON $usersStatusView TO ?"
//            ).use {
//                logger.debug { "Granting permission on users status view" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            connection!!.prepareStatement(
//                "GRANT SELECT (" +
//                        "$roleTokenColumn, " +
//                        "$asymEncPublicKeyColumn, " +
//                        "$asymSigPublicKeyColumn, " +
//                        "$statusColumn, " +
//                        "$roleVersionNumberColumn) ON $rolesTable TO ?"
//            ).use {
//                logger.debug { "Granting permission on roles table" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            connection!!.prepareStatement(
//                "GRANT SELECT (" +
//                        "$roleTokenColumn, " +
//                        "$asymEncPublicKeyColumn, " +
//                        "$asymSigPublicKeyColumn, " +
//                        "$statusColumn, " +
//                        "$roleVersionNumberColumn) ON $deletedRolesTable TO ?"
//            ).use {
//                logger.debug { "Granting permission on deleted roles table" }
//                it.setString(1, username)
//                it.execute()
//            }
//
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
//                "GRANT SELECT ON $resourcesView TO ?"
//            ).use {
//                logger.debug { "Granting permission on resources view" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            connection!!.prepareStatement(
//                "GRANT SELECT ON $usersRolesView TO ?"
//            ).use {
//                logger.debug { "Granting permission on user-role assignments view" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            connection!!.prepareStatement(
//                "GRANT SELECT ON $rolesPermissionsView TO ?"
//            ).use {
//                logger.debug { "Granting permission on role-permission assignments view" }
//                it.setString(1, username)
//                it.execute()
//            }
//
//            CODE_000_SUCCESS
//        } catch (e: SQLException) {
//            logger.warn { "Could not create users $username in MySQL" }
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
//                mmType = MMType.RBAC_MYSQL
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
//                        type = RBACElementType.USER
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
//                    logger.warn { "Error while deleting user $username from the database" }
//                    CODE_056_DELETE_USER_MM
//                } else {
//                    throw e
//                }
//            }
//        }
//    }
//
//    override fun addRole(
//        newRole: Role
//    ): OutcomeCode {
//        val roleName = newRole.name
//
//        /** Guard clauses */
//        if (roleName.isBlank()) {
//            logger.warn { "Role name is blank" }
//            return CODE_020_INVALID_PARAMETER
//        }
//
//
//        logger.info { "Adding the role ${newRole.name} in the metadata" }
//
//        if (getRoles(
//                roleName = newRole.name,
//                status = TupleStatus.DELETED
//            ).isNotEmpty()) {
//            logger.warn { "Role ${newRole.name} was previously deleted" }
//            return CODE_014_ROLE_WAS_DELETED
//        }
//
//        /** Add the role in the metadata */
//        logger.debug { "Adding the role in the metadata" }
//        val roleValues = arrayListOf<Any?>(
//            newRole.name,
//            newRole.token,
//            newRole.asymEncKeys!!.public,
//            newRole.asymSigKeys!!.public,
//            newRole.versionNumber,
//            TupleStatus.OPERATIONAL
//        )
//        return createInsertStatement(
//            table = rolesTable,
//            values = arrayListOf(roleValues),
//            connection = connection!!,
//        ).use {
//            if (it.executeUpdate() == 1) {
//                CODE_000_SUCCESS
//            } else {
//                logger.warn { "Role ${newRole.name} already present in the metadata" }
//                CODE_002_ROLE_ALREADY_EXISTS
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
//        logger.info { "Adding the resource ${newResource.name} in the metadata" }
//
//        if (getResources(
//                resourceName = newResource.name,
//                status = TupleStatus.DELETED
//            ).isNotEmpty()) {
//            logger.warn { "Resource ${newResource.name} was previously deleted" }
//            return CODE_015_RESOURCE_WAS_DELETED
//        }
//
//        /** Add the resource in the metadata */
//        logger.debug { "Adding the resource in the metadata" }
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
//    override fun addUsersRoles(newUsersRoles: HashSet<UserRole>): OutcomeCode {
//        val size = newUsersRoles.size
//        if (size == 0) {
//            logger.warn { "No user-role assignments given" }
//            return CODE_000_SUCCESS
//        }
//
//        // TODO questo pezzo di codice non c'è in "addUsersAttributes" (che è
//        //  una funzione equivalente), è giusto qui oppure di là?
//        var code = CODE_000_SUCCESS
//        run error@{
//            newUsersRoles.forEach { newUserRole ->
//
//                val username = newUserRole.username
//                val roleName = newUserRole.roleName
//
//                code = when (
//                    getStatus(name = username, type = RBACElementType.USER)
//                ) {
//                    null -> {
//                        logger.warn { "User $username was not found" }
//                        CODE_004_USER_NOT_FOUND
//                    }
//                    TupleStatus.DELETED -> {
//                        logger.warn { "User $username was previously deleted" }
//                        CODE_013_USER_WAS_DELETED
//                    }
//                    TupleStatus.INCOMPLETE -> {
//                        logger.warn { "User $username is incomplete" }
//                        CODE_053_USER_IS_INCOMPLETE
//                    }
//                    else ->
//                        CODE_000_SUCCESS
//                }
//                if (code != CODE_000_SUCCESS) {
//                    return@error
//                }
//
//                code = when (
//                    getStatus(name = roleName, type = RBACElementType.ROLE)
//                ) {
//                    null -> {
//                        logger.warn { "Role $roleName was not found" }
//                        CODE_005_ROLE_NOT_FOUND
//                    }
//                    TupleStatus.DELETED -> {
//                        logger.warn { "Role $roleName was previously deleted" }
//                        CODE_014_ROLE_WAS_DELETED
//                    }
//                    else ->
//                        CODE_000_SUCCESS
//                }
//                if (code != CODE_000_SUCCESS) {
//                    return@error
//                }
//            }
//        }
//        if (code != CODE_000_SUCCESS) {
//            return code
//        }
//
//        /** Create the list of values to insert in the metadata */
//        logger.info { "Adding $size user-role assignments to the metadata (one per row below):" }
//        val usersRoles = ArrayList<ArrayList<Any?>>(size)
//        newUsersRoles.forEachIndexed { index, userRole ->
//            logger.info {
//                "${index + 1}: user ${userRole.username} " +
//                        "to role ${userRole.roleName}"
//            }
//            usersRoles.add(createArray(userRole))
//        }
//
//
//        /** Add the user-role assignments to the metadata */
//        return createInsertStatement(
//            table = usersRolesTable,
//            values = usersRoles,
//            connection = connection!!,
//        ).use {
//            val rowCount = it.executeUpdate()
//            if (rowCount != size) {
//                logger.warn { "One ore more user-role assignments were not added (expected $size, actual $rowCount)" }
//                run loop@{
//                    /**
//                     * Check whether the operation failed because the user-role
//                     * assignment already exists or the user or role are missing
//                     */
//                    newUsersRoles.forEach { userRole ->
//                        val username = userRole.username
//                        val roleName = userRole.roleName
//
//                        val codeUser = when (getStatus(
//                            name = username,
//                            type = RBACElementType.USER
//                        )) {
//                            TupleStatus.INCOMPLETE -> CODE_053_USER_IS_INCOMPLETE
//                            TupleStatus.OPERATIONAL -> CODE_010_USER_ROLE_ASSIGNMENT_ALREADY_EXISTS
//                            TupleStatus.DELETED -> CODE_013_USER_WAS_DELETED
//                            null -> CODE_004_USER_NOT_FOUND
//                        }
//                        if (codeUser != CODE_010_USER_ROLE_ASSIGNMENT_ALREADY_EXISTS) {
//                            return@loop codeUser
//                        }
//
//                        val codeRole = when (getStatus(
//                            name = roleName,
//                            type = RBACElementType.ROLE
//                        )) {
//                            TupleStatus.OPERATIONAL -> CODE_010_USER_ROLE_ASSIGNMENT_ALREADY_EXISTS
//                            TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                            TupleStatus.INCOMPLETE, null -> CODE_005_ROLE_NOT_FOUND
//                        }
//                        if (codeRole != CODE_010_USER_ROLE_ASSIGNMENT_ALREADY_EXISTS) {
//                            return@loop codeRole
//                        }
//                    }
//                    CODE_010_USER_ROLE_ASSIGNMENT_ALREADY_EXISTS
//                }
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    override fun addRolesPermissions(
//        newRolesPermissions: HashSet<RolePermission>
//    ): OutcomeCode {
//        val size = newRolesPermissions.size
//        if (size == 0) {
//            logger.warn { "No role-permission assignments given" }
//            return CODE_000_SUCCESS
//        }
//
//        /** Create the list of values to insert in the metadata */
//        logger.info { "Adding $size role-permission assignments to the metadata (one per row below):" }
//        val rolesPermissions = ArrayList<ArrayList<Any?>>(size)
//        newRolesPermissions.forEachIndexed { index, rolePermission ->
//            logger.info {
//                "${index + 1}: role ${rolePermission.roleName} to resource " +
//                "${rolePermission.resourceName} with operation ${rolePermission.operation}"
//            }
//            rolesPermissions.add(createArray(rolePermission))
//        }
//
//        /** Add the role-permission assignments to the metadata */
//        return createInsertStatement(
//            table = rolesPermissionsTable,
//            values = rolesPermissions,
//            connection = connection!!,
//        ).use {
//            val rowCount = it.executeUpdate()
//            if (rowCount != size) {
//                logger.warn {
//                    "One ore more role-permission assignments were " +
//                            "not added (expected $size, actual $rowCount)"
//                }
//                run loop@{
//                    /**
//                     * Check whether the operation failed because the role-permission
//                     * assignments already exists or the role or resource are missing
//                     */
//                    newRolesPermissions.forEach { rolePermission ->
//                        val resourceName = rolePermission.resourceName
//                        val roleName = rolePermission.roleName
//
//                        val codeRole = when (getStatus(
//                            name = roleName,
//                            type = RBACElementType.ROLE
//                        )) {
//                            TupleStatus.OPERATIONAL -> CODE_011_ROLE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS
//                            TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                            TupleStatus.INCOMPLETE, null -> CODE_005_ROLE_NOT_FOUND
//                        }
//
//                        if (codeRole != CODE_011_ROLE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS) {
//                            return@loop codeRole
//                        }
//
//                        val codeResource = when (getStatus(
//                            name = resourceName,
//                            type = RBACElementType.RESOURCE
//                        )) {
//                            TupleStatus.OPERATIONAL -> CODE_011_ROLE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS
//                            TupleStatus.DELETED -> CODE_015_RESOURCE_WAS_DELETED
//                            TupleStatus.INCOMPLETE, null -> CODE_006_RESOURCE_NOT_FOUND
//                        }
//
//                        if (codeResource != CODE_011_ROLE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS) {
//                            return@loop codeResource
//                        }
//                    }
//                    CODE_011_ROLE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS
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
//    override fun getRoles(
//        roleName: String?,
//        status: TupleStatus?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int,
//    ): HashSet<Role> {
//        logger.info { "Getting data of roles (offset $offset, limit $limit)" }
//
//        // TODO support invocations by non-admin users
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!roleName.isNullOrBlank()) {
//            logger.info { "Filtering by matching role name $roleName" }
//            whereParameters[roleNameColumn] = roleName
//        }
//        if (status != null && status != TupleStatus.DELETED) {
//            logger.info { "Filtering by matching status $status" }
//            whereParameters[statusColumn] = status
//        }
//
//        val roles = HashSet<Role>()
//
//        createSelectStatement(
//            table = rolesTable,
//            whereParameters = whereParameters,
//            limit = limit,
//            offset = offset,
//            connection = connection!!,
//        ).use { firstStatement ->
//            createSelectStatement(
//                table = deletedRolesTable,
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
//                        val role = Role(
//                            name = sanitizeForHTML(rs.getString(roleNameColumn)),
//                            status = status ?: TupleStatus.valueOf(
//                                value = sanitizeForHTML(rs.getString(statusColumn))
//                            ),
//                            versionNumber = rs.getInt(roleVersionNumberColumn),
//                        )
//                        role.token = sanitizeForHTML(rs.getString(roleTokenColumn))
//                        roles.add(role)
//                    }
//                }
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
//            table = if (isAdmin) resourcesTable else resourcesView,
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
//    override fun getUsersRoles(
//        username: String?,
//        roleName: String?,
//        isAdmin: Boolean,
//        offset: Int,
//        limit: Int
//    ): HashSet<UserRole> {
//        logger.info { "Getting data of user-role assignments (offset $offset, limit $limit)" }
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!username.isNullOrBlank()) {
//            logger.info { "Filtering by matching username $username" }
//            whereParameters[usernameColumn] = username
//        }
//        if (!roleName.isNullOrBlank()) {
//            logger.info { "Filtering by matching role name $roleName" }
//            whereParameters[roleNameColumn] = roleName
//        }
//        if (whereParameters.isEmpty()) {
//            logger.info { "Not filtering for user or role name" }
//        }
//
//        val usersRoles = HashSet<UserRole>()
//
//        createSelectStatement(
//            table = if (isAdmin) usersRolesTable else usersRolesView,
//            whereParameters = whereParameters,
//            limit = limit,
//            offset = offset,
//            connection = connection!!,
//        ).use {
//            val rs = it.executeQuery()
//            while (rs.next()) {
//                val userRole = UserRole(
//                    username = sanitizeForHTML(rs.getString(usernameColumn)),
//                    roleName = sanitizeForHTML(rs.getString(roleNameColumn)),
//                    userVersionNumber = rs.getInt(userVersionNumberColumn),
//                    roleVersionNumber = rs.getInt(roleVersionNumberColumn),
//                    encryptedAsymEncKeys = EncryptedAsymKeys(
//                        public = sanitizeForHTML(rs.getString(encryptedAsymEncPublicKeyColumn)).decodeBase64(),
//                        private = sanitizeForHTML(rs.getString(encryptedAsymEncPrivateKeyColumn)).decodeBase64(),
//                        keyType = AsymKeysType.ENC,
//                    ),
//                    encryptedAsymSigKeys = EncryptedAsymKeys(
//                        public = sanitizeForHTML(rs.getString(encryptedAsymSigPublicKeyColumn)).decodeBase64(),
//                        private = sanitizeForHTML(rs.getString(encryptedAsymSigPrivateKeyColumn)).decodeBase64(),
//                        keyType = AsymKeysType.SIG,
//                    ),
//                )
//                userRole.updateSignature(
//                    newSignature = sanitizeForHTML(rs.getString(signatureColumn)).decodeBase64(),
//                    newSigner = ADMIN,
//                )
//                usersRoles.add(userRole)
//            }
//        }
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
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!roleName.isNullOrBlank()) {
//            logger.info { "Filtering by matching role name $roleName" }
//            whereParameters[roleNameColumn] = roleName
//        }
//        if (!resourceName.isNullOrBlank()) {
//            logger.info { "Filtering by matching resource name $resourceName" }
//            whereParameters[resourceNameColumn] = resourceName
//        }
//        if (whereParameters.isEmpty()) {
//            logger.info { "Not filtering for role or resource name" }
//        }
//
//        val rolesPermissions = HashSet<RolePermission>()
//
//        createSelectStatement(
//            table = if (isAdmin) rolesPermissionsTable else rolesPermissionsView,
//            whereParameters = whereParameters,
//            limit = limit,
//            offset = offset,
//            connection = connection!!,
//        ).use {
//            val rs = it.executeQuery()
//            while (rs.next()) {
//                val rolePermission = RolePermission(
//                    roleName = sanitizeForHTML(rs.getString(roleNameColumn)),
//                    resourceName = sanitizeForHTML(rs.getString(resourceNameColumn)),
//                    roleToken = sanitizeForHTML(rs.getString(roleTokenColumn)),
//                    resourceToken = sanitizeForHTML(rs.getString(resourceTokenColumn)),
//                    roleVersionNumber = rs.getInt(roleVersionNumberColumn),
//                    resourceVersionNumber = rs.getInt(resourceVersionNumberColumn),
//                    operation = Operation.valueOf(sanitizeForHTML(rs.getString(operationColumn))),
//                    encryptedSymKey = EncryptedSymKey(
//                        sanitizeForHTML(rs.getString(encryptedSymKeyColumn)).decodeBase64()
//                    ),
//                )
//                rolePermission.updateSignature(
//                    newSignature = sanitizeForHTML(rs.getString(signatureColumn)).decodeBase64(),
//                    newSigner = sanitizeForHTML(rs.getString(signerTokenColumn)),
//                )
//                rolesPermissions.add(rolePermission)
//            }
//        }
//        logger.debug { "Found ${rolesPermissions.size} role-permission assignments" }
//        return rolesPermissions
//    }
//
//    override fun getPublicKey(
//        name: String?,
//        token: String?,
//        elementType: RBACElementType,
//        asymKeyType: AsymKeysType,
//    ): ByteArray? {
//        logger.info { "Getting public key of type $asymKeyType of an element of type $elementType" }
//
//        // TODO support invocations by non-admin users
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!name.isNullOrBlank()) {
//            logger.info { "Filtering by matching name $name" }
//            whereParameters[if (elementType == RBACElementType.USER)
//                usernameColumn
//            else
//                roleNameColumn
//            ] = name
//        }
//        if (!token.isNullOrBlank()) {
//            logger.info { "Filtering by matching token $token" }
//            whereParameters[if (elementType == RBACElementType.USER)
//                userTokenColumn
//            else
//                roleTokenColumn
//            ] = token
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
//         * Search the key in both the users and roles
//         * table and the deleted users and deleted roles table
//         */
//        createSelectStatement(
//            table = if (elementType == RBACElementType.USER)
//                usersTable
//            else
//                rolesTable,
//            whereParameters = whereParameters,
//            selectedColumns = selectedColumns,
//            whereNotParameters = whereNotParameters,
//            limit = 1,
//            offset = 0,
//            connection = connection!!,
//        ).use { firstStatement ->
//            createSelectStatement(
//                table = if (elementType == RBACElementType.USER)
//                    deletedUsersTable
//                else
//                    deletedRolesTable,
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
//        logger.debug { "Public key was${ if (asymPublicKeyBytes == null) "not" else ""} found" }
//        return asymPublicKeyBytes
//    }
//
//    override fun getVersionNumber(
//        name: String?,
//        token: String?,
//        elementType: RBACElementType,
//    ): Int? {
//        logger.info { "Getting version number of a $elementType" }
//
//        // TODO support invocations by non-admin users
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!name.isNullOrBlank()) {
//            logger.info { "Filtering by matching name $name" }
//            whereParameters[when (elementType) {
//                RBACElementType.USER -> usernameColumn
//                RBACElementType.ROLE -> roleNameColumn
//                RBACElementType.RESOURCE -> resourceNameColumn
//            }] = name
//        }
//        if (!token.isNullOrBlank()) {
//            logger.info { "Filtering by matching token $token" }
//            whereParameters[when (elementType) {
//                RBACElementType.USER -> userTokenColumn
//                RBACElementType.ROLE -> roleTokenColumn
//                RBACElementType.RESOURCE -> resourceTokenColumn
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
//            RBACElementType.USER -> userVersionNumberColumn
//            RBACElementType.ROLE -> roleVersionNumberColumn
//            RBACElementType.RESOURCE -> resourceVersionNumberColumn
//        }
//        val selectedColumns = arrayListOf(selectedColumn)
//
//        var versionNumber: Int? = null
//
//        createSelectStatement(
//            table = when (elementType) {
//                RBACElementType.USER -> usersTable
//                RBACElementType.ROLE -> rolesTable
//                RBACElementType.RESOURCE -> resourcesTable
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
//        type: RBACElementType
//    ): String? {
//        logger.info { "Getting token of a $type" }
//
//        // TODO support invocations by non-admin users
//        val whereParameters = LinkedHashMap<String, Any?>()
//        logger.info { "Filtering by matching name $name" }
//        whereParameters[
//                when (type) {
//                    RBACElementType.USER -> usernameColumn
//                    RBACElementType.ROLE -> roleNameColumn
//                    RBACElementType.RESOURCE -> resourceNameColumn
//                }] = name
//
//        val table1: String
//        val table2: String
//
//        val selectedColumn = when (type) {
//            RBACElementType.USER -> { table1 = usersTable; table2 = deletedUsersTable; userTokenColumn }
//            RBACElementType.ROLE -> { table1 = rolesTable; table2 = deletedRolesTable; roleTokenColumn }
//            RBACElementType.RESOURCE -> { table1 = resourcesTable; table2 = deletedResourcesTable; resourceTokenColumn }
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
//        type: RBACElementType
//    ): TupleStatus? {
//        logger.debug { "Getting the status of a $type" }
//
//        // TODO support invocations by non-admin users
//
//        val table1: String
//        val table2: String
//
//        when (type) {
//            RBACElementType.USER -> { table1 = usersTable; table2 = deletedUsersTable; }
//            RBACElementType.ROLE -> { table1 = rolesTable; table2 = deletedRolesTable; }
//            RBACElementType.RESOURCE -> { table1 = resourcesTable; table2 = deletedResourcesTable; }
//        }
//        val selectedColumns = arrayListOf(statusColumn)
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!name.isNullOrBlank()) {
//            logger.info { "Filtering by matching name $name" }
//            whereParameters[
//                    when (type) {
//                        RBACElementType.USER -> usernameColumn
//                        RBACElementType.ROLE -> roleNameColumn
//                        RBACElementType.RESOURCE -> resourceNameColumn
//                    }
//            ] = name
//        }
//        if (!token.isNullOrBlank()) {
//            logger.info { "Filtering by matching token $token" }
//            whereParameters[
//                    when (type) {
//                        RBACElementType.USER -> userTokenColumn
//                        RBACElementType.ROLE -> roleTokenColumn
//                        RBACElementType.RESOURCE -> resourceTokenColumn
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
//     * In this implementation, move the [roleName]
//     * in the deleted roles table
//     */
//    override fun deleteRole(
//        roleName: String
//    ): OutcomeCode {
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
//        logger.debug { "Moving the role from table $rolesTable to table $deletedRolesTable" }
//        val whereParameters = linkedMapOf<String, Any?>(
//            roleNameColumn to roleName
//        )
//        val selectedColumns = arrayListOf(
//            roleNameColumn,
//            roleTokenColumn,
//            asymEncPublicKeyColumn,
//            asymSigPublicKeyColumn,
//            roleVersionNumberColumn,
//            "'${TupleStatus.DELETED}'"
//        )
//        createSelectStatement(
//            table = rolesTable,
//            selectedColumns = selectedColumns,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use { selectStatement ->
//            connection!!.prepareStatement(
//                "INSERT INTO $deletedRolesTable (${selectStatement.asString()})"
//            ).use {
//                if (it.executeUpdate() != 1) {
//                    logger.warn { "Role $roleName not found in the metadata" }
//                    return when (getStatus(
//                        name = roleName,
//                        type = RBACElementType.ROLE
//                    )) {
//                        TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                        null -> CODE_005_ROLE_NOT_FOUND
//                        else -> {
//                            val message = "Role not found but role is in table"
//                            logger.error { message }
//                            throw IllegalArgumentException(message)
//                        }
//                    }
//                }
//            }
//        }
//
//        logger.debug { "Deleting the role from table $rolesTable" }
//        createDeleteStatement(
//            table = rolesTable,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            return if (it.executeUpdate() != 1) {
//                logger.warn { "Role $roleName not found in the metadata" }
//                CODE_005_ROLE_NOT_FOUND
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    /**
//     * In this implementation, move the [resourceName]
//     * in the deleted resources table
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
//        logger.debug { "Moving the resource from table $resourcesTable to table $deletedResourcesTable" }
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
//                        type = RBACElementType.RESOURCE
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
//    override fun deleteUsersRoles(roleName: String): OutcomeCode {
//        logger.info { "Deleting user-role assignments for role name $roleName" }
//
//        /** Guard Clauses */
//        if (roleName == ADMIN) {
//            logger.warn { "Cannot delete the $ADMIN user-role assignment" }
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }
//
//        logger.info { "Filtering by matching role name $roleName" }
//        val whereParameters = linkedMapOf<String, Any?>(
//            roleNameColumn to roleName
//        )
//
//        logger.debug { "Deleting the user-role assignments from table $usersRolesTable" }
//        createDeleteStatement(
//            table = usersRolesTable,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            val affectedRows = it.executeUpdate()
//            logger.debug { "$affectedRows user-role assignments were deleted" }
//            return if (affectedRows <= 0) {
//                logger.warn { "No user-role assignment was deleted" }
//                /**
//                 * Check whether the operation failed because there
//                 * are no user-role assignments or because the role
//                 * is missing or was deleted
//                 */
//                when (getStatus(
//                    name = roleName,
//                    type = RBACElementType.ROLE
//                )) {
//                    TupleStatus.OPERATIONAL -> CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND
//                    TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                    TupleStatus.INCOMPLETE, null -> CODE_005_ROLE_NOT_FOUND
//                }
//            } else {
//                CODE_000_SUCCESS
//            }
//        }
//    }
//
//    override fun deleteRolesPermissions(
//        roleName: String?,
//        resourceName: String?,
//    ): OutcomeCode {
//        logger.info { "Deleting role-permission assignments" }
//
//        /** Guard Clauses */
//        if (roleName == ADMIN) {
//            logger.warn { "Cannot delete the $ADMIN role-permission assignment" }
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }
//
//        val whereParameters = LinkedHashMap<String, Any?>()
//        if (!roleName.isNullOrBlank()) {
//            logger.info { "Filtering by matching role name $roleName" }
//            whereParameters[roleNameColumn] = roleName
//        }
//        if (!resourceName.isNullOrBlank()) {
//            logger.info { "Filtering by matching resource name $resourceName" }
//            whereParameters[resourceNameColumn] = resourceName
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
//        logger.debug { "Deleting the role-permission assignments from table $rolesPermissionsTable" }
//        createDeleteStatement(
//            table = rolesPermissionsTable,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            val affectedRows = it.executeUpdate()
//            logger.debug { "$affectedRows role-permission assignments were deleted" }
//            return if (affectedRows <= 0) {
//                logger.warn { "No role-permission assignment was deleted" }
//                /**
//                 * Check whether the operation failed because there
//                 * are no role-permission assignments or because the
//                 * role or the resource is missing or was deleted
//                 */
//                val roleExists = if (roleName != null) {
//                    when (getStatus(
//                        name = roleName,
//                        type = RBACElementType.ROLE
//                    )) {
//                        TupleStatus.OPERATIONAL -> CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//                        TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                        TupleStatus.INCOMPLETE, null -> CODE_005_ROLE_NOT_FOUND
//                    }
//                } else {
//                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//                }
//
//                if (roleExists == CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND) {
//                    if (resourceName != null) {
//                        when (getStatus(
//                            name = resourceName,
//                            type = RBACElementType.RESOURCE
//                        )) {
//                            TupleStatus.OPERATIONAL -> CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//                            TupleStatus.DELETED -> CODE_015_RESOURCE_WAS_DELETED
//                            TupleStatus.INCOMPLETE, null -> CODE_006_RESOURCE_NOT_FOUND
//                        }
//                    } else {
//                        CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//                    }
//                } else {
//                    roleExists
//                }
//            } else {
//                CODE_000_SUCCESS
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
//        val whereParameters = linkedMapOf<String, Any?>(
//            roleNameColumn to roleName
//        )
//        val values = linkedMapOf<String, Any>(
//            roleTokenColumn to newRoleToken,
//            asymEncPublicKeyColumn to newAsymEncPublicKey.encoded.encodeBase64(),
//            asymSigPublicKeyColumn to newAsymSigPublicKey.encoded.encodeBase64(),
//            roleVersionNumberColumn to (oldRoleVersionNumber + 1)
//        )
//
//        return createUpdateStatement(
//            table = rolesTable,
//            values = values,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            if (it.executeUpdate() != 1) {
//                logger.warn { "Role $roleName not found in the metadata" }
//                when (getStatus(
//                    name = roleName,
//                    type = RBACElementType.ROLE
//                )) {
//                    TupleStatus.DELETED -> CODE_014_ROLE_WAS_DELETED
//                    null -> CODE_005_ROLE_NOT_FOUND
//                    else -> {
//                        val message = "Role not found but role is in table"
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
//    override fun updateResourceVersionNumber(
//        updatedResource: Resource
//    ): OutcomeCode {
//        val resourceName = updatedResource.name
//        logger.info { "Updating the version numbers of $resourceName" }
//
//        val whereParameters = linkedMapOf<String, Any?>(
//            resourceNameColumn to resourceName
//        )
//        val values = linkedMapOf<String, Any>(
//            resourceVersionNumberColumn to updatedResource.versionNumber,
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
//                    type = RBACElementType.RESOURCE
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
//    override fun updateResourceTokenAndVersionNumber(
//        resourceName: String,
//        oldResourceToken: String,
//        newResourceToken: String,
//        newVersionNumber: Int,
//    ): OutcomeCode {
//        logger.info { "Updating the token and the version number of $resourceName" }
//
//        val whereParameters = linkedMapOf<String, Any?>(
//            resourceNameColumn to resourceName
//        )
//        val values = linkedMapOf<String, Any>(
//            resourceTokenColumn to newResourceToken,
//            resourceVersionNumberColumn to newVersionNumber,
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
//                    type = RBACElementType.RESOURCE
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
//    override fun updateRolePermission(
//        updatedRolePermission: RolePermission
//    ): OutcomeCode {
//        logger.info {
//            "Updating the role-permission assignment of role ${updatedRolePermission.roleName} " +
//                    "and resource ${updatedRolePermission.resourceName}"
//        }
//
//        val whereParameters = linkedMapOf<String, Any?>(
//            roleNameColumn to updatedRolePermission.roleName,
//            resourceNameColumn to updatedRolePermission.resourceName,
//            resourceVersionNumberColumn to updatedRolePermission.resourceVersionNumber
//        )
//        val values = linkedMapOf<String, Any>(
//            operationColumn to updatedRolePermission.operation,
//            signatureColumn to updatedRolePermission.signature!!.encodeBase64(),
//            resourceVersionNumberColumn to updatedRolePermission.resourceVersionNumber,
//            signerTokenColumn to updatedRolePermission.signer!!,
//            roleTokenColumn to updatedRolePermission.roleToken,
//            resourceTokenColumn to updatedRolePermission.resourceToken,
//            encryptedSymKeyColumn to updatedRolePermission.encryptedSymKey!!.key.encodeBase64(),
//            resourceVersionNumberColumn to updatedRolePermission.resourceVersionNumber,
//            roleVersionNumberColumn to updatedRolePermission.roleVersionNumber
//        )
//
//        createUpdateStatement(
//            table = rolesPermissionsTable,
//            values = values,
//            whereParameters = whereParameters,
//            connection = connection!!,
//        ).use {
//            return if (it.executeUpdate() != 1) {
//                logger.warn { "Role-permission assignment was not found" }
//                CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
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
//     * Create and return an array list from the given [userRole]
//     * to match the order of the columns of the user-role assignments
//     * table
//     */
//    private fun createArray(userRole: UserRole): ArrayList<Any?> {
//        return arrayListOf(
//            userRole.username,
//            userRole.roleName,
//            userRole.encryptedAsymEncKeys!!.public.encodeBase64(),
//            userRole.encryptedAsymEncKeys.private.encodeBase64(),
//            userRole.encryptedAsymSigKeys!!.public.encodeBase64(),
//            userRole.encryptedAsymSigKeys.private.encodeBase64(),
//            userRole.userVersionNumber,
//            userRole.roleVersionNumber,
//            userRole.signature!!.encodeBase64()
//        )
//    }
//
//    /**
//     * Create and return an array list from the given [rolePermission]
//     * to match the order of the columns of the role-permission assignments
//     * table
//     */
//    private fun createArray(rolePermission: RolePermission): ArrayList<Any?> {
//        return arrayListOf(
//            rolePermission.roleName,
//            rolePermission.resourceName,
//            rolePermission.roleToken,
//            rolePermission.resourceToken,
//            rolePermission.encryptedSymKey!!.key.encodeBase64(),
//            rolePermission.roleVersionNumber,
//            rolePermission.resourceVersionNumber,
//            rolePermission.operation,
//            rolePermission.signer,
//            rolePermission.signature!!.encodeBase64()
//        )
//    }
//}
