package cryptoac.core

import cryptoac.Constants.ADMIN
import cryptoac.OutcomeCode
import cryptoac.OutcomeCode.*
import cryptoac.code.*
import cryptoac.crypto.*
import cryptoac.decodeBase64
import cryptoac.dm.DMServiceRBAC
import cryptoac.dm.local.DMLocal
import cryptoac.dm.local.DMServiceLocalParameters
import cryptoac.encodeBase64
import cryptoac.inputStream
import cryptoac.mm.MMServiceCACRBAC
import cryptoac.mm.MMType
import cryptoac.mm.local.MMServiceCACRBACLocal
import cryptoac.mm.local.MMServiceRBACLocalParameters
import cryptoac.rm.RMServiceRBAC
import cryptoac.tuple.*
import mu.KotlinLogging
import java.io.InputStream
import kotlin.jvm.optionals.getOrNull

private val logger = KotlinLogging.logger {}

// TODO is there any information on what role a
//  user assumes to read/write on a file? We
//  should add this information somewhere

/**
 * A CoreCACRBACTuples extends the [CoreCACRBAC] class as a cryptographic
 * enforcement mechanism for a role-based access control model/scheme
 * The implementation of the CoreCACRBACTuples is based on the
 * paper "<TODO quello di cryptoac e predicati a cui lavoriamo con simone>"
 */
open class CoreCACRBACTuples(
    override val cryptoPKE: CryptoPKE,
    override val cryptoSKE: CryptoSKE,
    override val coreParameters: CoreParametersRBAC,
//    private val adminEncKeyPair: KeyPairCryptoAC,
//    private val adminSigKeyPair: KeyPairCryptoAC,
//    override val user: User
) : CoreCACRBAC(cryptoPKE, cryptoSKE, coreParameters) {

    override val mm: MMServiceCACRBAC = MMServiceCACRBACLocal()

    override val rm: RMServiceRBAC? = null /* RMServiceRBACCryptoAC(
        RMServiceRBACCryptoACParameters(
            password = "password",
            username = ADMIN,
            port = 8443,
            url = "10.1.0.4"
        )
    )*/

    override public var dm: DMServiceRBAC? = DMLocal(DMServiceLocalParameters())

    override val ac = null // TODO this should always be null

    override val user: User = coreParameters.user

    /** Asymmetric encryption key pair */
    val adminEncKeyPair: KeyPairCryptoAC = cryptoPKE.recreateAsymKeyPair(
        asymPublicKeyBytes = user.asymEncKeys!!.public.decodeBase64(),
        asymPrivateKeyBytes = user.asymEncKeys!!.private.decodeBase64(),
        type = AsymKeysType.ENC
    )

    /** Asymmetric signature key pair */
    val adminSigKeyPair: KeyPairCryptoAC = cryptoPKE.recreateAsymKeyPair(
        asymPublicKeyBytes = user.asymSigKeys!!.public.decodeBase64(),
        asymPrivateKeyBytes = user.asymSigKeys!!.private.decodeBase64(),
        type = AsymKeysType.SIG
    )

//    /** Asymmetric encryption key pair */
//    val adminEncKeyPair: KeyPairCryptoAC =
//
//    /** Asymmetric signature key pair */
//    val adminSigKeyPair: KeyPairCryptoAC = cryptoPKE.generateAsymSigKeys()

    /* --- STATE-CHANGE RULES --- */

    /**
     * In this implementation, add the admin user, the
     * admin role, and the admin-admin UR assignment (as
     * well as the admin's encrypting and verifying public
     * keys) in the cryptographic policy state and return
     * the outcome code
     */
    override fun configureServices(): OutcomeCode {

        dm = DMLocal(DMServiceLocalParameters())
        mm.init()

        logger.info { "CoreCACRBACTuples: configureServices for admin ${user.name}" }

        /** Lock the status of the services */
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }

        logger.info { "Configuring the services for admin ${user.name}" }

        code = mm.configure()
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }
        code = rm?.configure(coreParameters)
            ?: CODE_000_SUCCESS
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }
        code = dm?.configure(coreParameters)
            ?: CODE_000_SUCCESS
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        logger.info { adminSigKeyPair.public.encoded.encodeBase64() }
        logger.info { user.asymSigKeys!!.public }

        logger.info { "Adding the admin ${user.name} in the cryptographic policy state" }

        // generate (asymmetric) encryption key pair
        val encryptedAdminEncKeys = cryptoPKE.encryptAsymKeys(
            encryptingKey = adminEncKeyPair.public,
            asymKeys = adminEncKeyPair,
            type = AsymKeysType.ENC
        )

        // generate (asymmetric) signing key pair
        val encryptedAdminSigKeys = cryptoPKE.encryptAsymKeys(
            encryptingKey = adminEncKeyPair.public,
            asymKeys = adminSigKeyPair,
            type = AsymKeysType.SIG
        )

        // no need to create the user admin because it is a parameter of this scheme
        // only signature is necessary
        val adminUserSignature = cryptoPKE.createSignature(
            signingKey = adminSigKeyPair.private,
            bytes = user.getBytesForSignature()
        )
        user.updateSignature(
            newSignature = adminUserSignature,
            newSigner = user.name
        )

        // create admin role
        val adminRole = Role(
            name = ADMIN,
            status = TupleStatus.OPERATIONAL,
            versionNumber = 1,
            asymEncKeys = AsymKeys(
                public = user.asymEncKeys!!.public,
                private = "",
                keysType = AsymKeysType.ENC
            ),
            asymSigKeys = AsymKeys(
                public = user.asymSigKeys!!.public,
                private = "",
                keysType = AsymKeysType.SIG
            ),
        )
        val adminRoleSignature = cryptoPKE.createSignature(
            bytes = adminRole.getBytesForSignature(),
            signingKey = adminSigKeyPair.private
        )
        adminRole.updateSignature(
            newSignature = adminRoleSignature,
            newSigner = user.name,
        )

        // create admin-admin user-role
        val adminUserRole = UserRole(
            username = ADMIN,
            roleName = ADMIN,
            status = TupleStatus.OPERATIONAL,
            encryptedAsymEncKeys = encryptedAdminEncKeys,
            encryptedAsymSigKeys = encryptedAdminSigKeys,
            roleVersionNumber = 1
        )
        val signature = cryptoPKE.createSignature(
            bytes = adminUserRole.getBytesForSignature(),
            signingKey = adminSigKeyPair.private
        )
        adminUserRole.updateSignature(
            newSignature = signature,
            newSigner = user.name,
        )

        // add tuples to MM

        code = mm.addAdmin(newAdmin = user)
        initUser()
        if (code != CODE_000_SUCCESS) {
            return if (code == CODE_035_ADMIN_ALREADY_INITIALIZED) {
                logger.warn { "Code was $code, replacing with $CODE_077_SERVICE_ALREADY_CONFIGURED" }
                endOfMethod(CODE_077_SERVICE_ALREADY_CONFIGURED)
            } else {
                endOfMethod(code)
            }
        }

        code = mm.addRole(newRole = adminRole)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        code = mm.addUsersRoles(hashSetOf(adminUserRole))
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        // add tuples to DM

        logger.info { "Adding the admin ${user.name} in the DM" }
        code = dm?.addAdmin(newAdmin = user)
            ?: CODE_000_SUCCESS
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        code = rm?.addAdmin(
            newAdmin = user
        ) ?: CODE_000_SUCCESS
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }


        // finish

        logger.info { "Adding the admin ${user.name} in the RM" }
        return endOfMethod(CODE_000_SUCCESS)
    }

    override fun initUser(): OutcomeCode {
        /** Lock the status of the services */
        val lockCode = startOfMethod()
        if (lockCode != CODE_000_SUCCESS) {
            return lockCode
        }

        val mmUser = mm.getUsers(
            username = user.name,
            offset = 0,
            limit = 1
        )
        if(mmUser.isEmpty()) {
            return CODE_004_USER_NOT_FOUND
        }
        if(user.name != ADMIN) {
            when (mmUser.first().status) {
                TupleStatus.INCOMPLETE -> {}
                TupleStatus.OPERATIONAL -> return endOfMethod(CODE_052_USER_ALREADY_INITIALIZED)
                TupleStatus.CACHED, TupleStatus.DELETED -> return endOfMethod(CODE_013_USER_WAS_DELETED)
            }
        }

        // update signature
        val userSignature = cryptoPKE.createSignature(
            signingKey = adminSigKeyPair.private,
            bytes = user.getBytesForSignature()
        )
        user.updateSignature(
            newSignature = userSignature,
            newSigner = user.name,
        )

        val code = mm.initUser(user)

//        val initUserResult = mm.initUser(user)
//        if (initUserResult != CODE_000_SUCCESS) {
//            return endOfMethod(initUserResult)
//        }

        // finish
        return endOfMethod(code)
    }

    override fun deleteUser(username: String): OutcomeCode {
        logger.info { "Deleting user $username" }

        /** Guard clauses */
        if (username.isBlank()) {
            logger.warn { "Username is blank" }
            return CODE_020_INVALID_PARAMETER
        }
        if (username == ADMIN) {
            logger.warn { "Cannot delete the $ADMIN user" }
            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
        }

        /** Lock the status of the services */
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }

        /** Check that the user is not assigned to a role */
        val userRoles = mm.getUsersRoles(
            username = username,
            status = TupleStatus.OPERATIONAL
        )
        if (userRoles.isNotEmpty()) {
            logger.warn { "Cannot delete user $username because it is assigned to one or more roles" }
            return CODE_023_USER_CANNOT_BE_MODIFIED
        }

        // retrieve old user
        val oldUserOpt = mm.getUsers(username = username)
        if (oldUserOpt.count() != 1) {
            return CODE_004_USER_NOT_FOUND
        }
        val oldUser = oldUserOpt.first()

        // create cached user
        val cachedUser = User(
            name = oldUser.name,
            asymEncKeys = oldUser.asymEncKeys,
            asymSigKeys = oldUser.asymSigKeys,
            status = TupleStatus.CACHED,
        )
        val cachedUserSignature = cryptoPKE.createSignature(
            signingKey = adminSigKeyPair.private,
            bytes = cachedUser.getBytesForSignature()
        )
        cachedUser.updateSignature(
            newSignature = cachedUserSignature,
            newSigner = user.name,
        )

        // update tuples in MM

        /** Delete the user from the MM */
        code = mm.deleteUser(username)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(
                code = code
            )
        }

        val userOut = mm.addUser(cachedUser)
        if(userOut.code != CODE_000_SUCCESS) {
            return endOfMethod(userOut.code)
        }

        /** Delete the user from the RM */
        code = rm?.deleteUser(username)
            ?: CODE_000_SUCCESS
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(
                code = code
            )
        }

        /** Delete the user from the DM */
        code = dm!!.deleteUser(username)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(
                code = code
            )
        }

        return endOfMethod(
            code = CODE_000_SUCCESS
        )

    }

    override fun addUser(
        username: String
    ): CodeCoreParameters {
        logger.info { "Adding user $username" }

        /** Guard clauses */
        if (username.isBlank()) {
            logger.warn { "Username is blank" }
            return CodeCoreParameters(CODE_020_INVALID_PARAMETER)
        }

        /** Lock the status of the services */
        val lockCode = startOfMethod()
        if (lockCode != CODE_000_SUCCESS) {
            return CodeCoreParameters(lockCode)
        }

        // get old user
        val oldUser = mm.getUsers(username=username)
        if(oldUser.isNotEmpty()) {
            val old = oldUser.first()
            return when(old.status) {
                TupleStatus.INCOMPLETE, TupleStatus.OPERATIONAL -> CodeCoreParameters(endOfMethod(CODE_001_USER_ALREADY_EXISTS))
                TupleStatus.CACHED, TupleStatus.DELETED -> CodeCoreParameters(endOfMethod(CODE_013_USER_WAS_DELETED))
            }
        }

        // create user tuple
        val newUser = User(
            name = username,
            status = TupleStatus.INCOMPLETE
        )
        val newUserSignature = cryptoPKE.createSignature(
            signingKey = adminSigKeyPair.private,
            bytes = newUser.getBytesForSignature()
        )
        newUser.updateSignature(
            newSignature = newUserSignature,
            newSigner = user.name,
        )

        // add tuples to MM

        /** Add the user in the MM */
        val addMMResult = mm.addUser(
            newUser = newUser
        )
        if (addMMResult.code != CODE_000_SUCCESS) {
            return CodeCoreParameters(
                endOfMethod(
                    code = addMMResult.code
                )
            )
        }

        // add tuples to RM

        /** Add the user in the RM */
        val addRMResult = rm?.addUser(
            newUser = newUser
        )
        if (addRMResult != null) {
            if (addRMResult.code != CODE_000_SUCCESS) {
                return CodeCoreParameters(
                    endOfMethod(
                        code = addRMResult.code
                    )
                )
            }
        }

        // add tuples to DM

        /** Add the user in the DM */
        val addDMResult = dm?.addUser(
            newUser = newUser
        )
        if (addDMResult != null) {
            if (addDMResult.code != CODE_000_SUCCESS) {
                return CodeCoreParameters(
                    endOfMethod(
                        code = addDMResult.code
                    )
                )
            }
        }

        return CodeCoreParameters(
            code = endOfMethod(
                code = CODE_000_SUCCESS
            ),
            coreParameters = CoreParametersRBAC(
                user = newUser,
                coreType = coreParameters.coreType,
                cryptoType = coreParameters.cryptoType,
                mmServiceParameters = MMServiceRBACLocalParameters(MMType.RBAC_LOCAL),
                rmServiceParameters = null,
                dmServiceParameters = DMServiceLocalParameters(),
                acServiceParameters = null,
//                mmServiceParameters = addMMResult.serviceParameters as MMServiceParameters,
//                rmServiceParameters = addRMResult?.serviceParameters as RMServiceParameters?,
//                dmServiceParameters = addDMResult?.serviceParameters as DMServiceParameters?,
//                acServiceParameters = null // addACResult?.serviceParameters as ACServiceParameters?,
            )
        )
    }

    override fun addRole(roleName: String): OutcomeCode {
        logger.info { "Adding role $roleName" }

        /** Guard clauses */
        if (roleName.isBlank()) {
            logger.warn { "Role name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        /** Lock the status of the services */
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }

        if (
            mm.getRoles(
                roleName = roleName,
                status = TupleStatus.OPERATIONAL
            ).isNotEmpty()
        ) {
            return endOfMethod(CODE_002_ROLE_ALREADY_EXISTS)
        }

        // generate encryption & signing keys
        val asymEncKeys = cryptoPKE.generateAsymEncKeys()
        val asymSigKeys = cryptoPKE.generateAsymSigKeys()

        // create role tuples
        val newRole = Role(
            name = roleName,
            asymEncKeys = AsymKeys(
                public = asymEncKeys.public.encoded.encodeBase64(),
                private = asymEncKeys.private.encoded.encodeBase64(),
                keysType = AsymKeysType.ENC
            ),
            asymSigKeys = AsymKeys(
                public = asymSigKeys.public.encoded.encodeBase64(),
                private = asymSigKeys.private.encoded.encodeBase64(),
                keysType = AsymKeysType.SIG
            ),
            status = TupleStatus.OPERATIONAL,
            versionNumber = 1,
        )
        val roleSigning = cryptoPKE.createSignature(
            signingKey = adminSigKeyPair.private,
            bytes = newRole.getBytesForSignature()
        )
        newRole.updateSignature(
            newSignature = roleSigning,
            newSigner = user.name,
        )

        // create user-role tuples (admin role)
        val encryptedAsymEncKeys = cryptoPKE.encryptAsymKeys(
            encryptingKey = adminEncKeyPair.public,
            asymKeys = asymEncKeys,
            type = AsymKeysType.ENC
        )
        val encryptedAsymSigKeys = cryptoPKE.encryptAsymKeys(
            encryptingKey = adminEncKeyPair.public,
            asymKeys = asymSigKeys,
            type = AsymKeysType.SIG
        )

        val adminUserRole = UserRole(
            username = ADMIN,
            roleName = roleName,
            encryptedAsymEncKeys = encryptedAsymEncKeys,
            encryptedAsymSigKeys = encryptedAsymSigKeys
        )

        val signature = cryptoPKE.createSignature(
            bytes = adminUserRole.getBytesForSignature(),
            signingKey = adminSigKeyPair.private
        )
        adminUserRole.updateSignature(
            newSignature = signature,
            newSigner = user.name,
        )

        // add tuples to MM
        code = mm.addRole(newRole)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(
                code = code,
                dmLocked = false
            )
        }
        code = mm.addUsersRoles(hashSetOf(adminUserRole))
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(
                code = code,
                dmLocked = false
            )
        }

        // finish
        return endOfMethod(
            code = code,
            dmLocked = false
        )
    }

    override fun deleteRole(roleName: String): OutcomeCode {
        logger.info { "Deleting role $roleName" }

        /** Guard clauses */
        if (roleName.isBlank()) {
            logger.warn { "Role name is blank" }
            return CODE_020_INVALID_PARAMETER
        }
        if (roleName == ADMIN) {
            logger.warn { "Cannot delete the $ADMIN role" }
            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
        }

        /** Lock the status of the services */
        var code = startOfMethod(dmLock = false)
        if (code != CODE_000_SUCCESS) {
            return code
        }

        // retrieve old role
        val oldRoleOpt = mm.getRoles(
            roleName = roleName,
            status = TupleStatus.OPERATIONAL
        )
        if (oldRoleOpt.count() != 1) {
            return endOfMethod(
                code = CODE_005_ROLE_NOT_FOUND,
                dmLocked = false
            )
        }
        val oldRole = oldRoleOpt.first()

        // check that no user-role tuples exist
//        val userRoles = mm.getUsersRoles(
//            roleName = roleName,
//            status = TupleStatus.OPERATIONAL
//        )
//        if (userRoles.count() != 1) {
//            TODO("Only admin")
//        }
//        if (userRoles.first().username != ADMIN) {
//            TODO("Only admin")
//        }

        // check that no role-permissions tuples exist
        val rolePerms = mm.getRolesPermissions(
            roleName = roleName,
            status = TupleStatus.OPERATIONAL
        )
        if(rolePerms.size != 0) {
            for(rp in rolePerms) {
                print(rp.roleName)
                print(rp.resourceName)
                print("---")
            }
            TODO("error")
        }

        // delete admin-role
        code = mm.deleteUsersRoles(
            roleName = roleName,
            status = TupleStatus.OPERATIONAL
        )
        if(code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        // create cached role
        val cachedRole = Role(
            name = oldRole.name,
            asymEncKeys = oldRole.asymEncKeys,
            asymSigKeys = oldRole.asymSigKeys,
            versionNumber = oldRole.versionNumber,
            status = TupleStatus.CACHED
        )
        val cachedRoleSignature = cryptoPKE.createSignature(
            signingKey = adminSigKeyPair.private,
            bytes = cachedRole.getBytesForSignature()
        )
        cachedRole.updateSignature(
            newSignature = cachedRoleSignature,
            newSigner = user.name,
        )

        // update tuples in MM
        code = mm.deleteRole(roleName = roleName)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(
                code = code,
                dmLocked = false
            )
        }
        code = mm.addRole(cachedRole)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(
                code = code,
                dmLocked = false
            )
        }

        // finish
        return endOfMethod(
            code = code,
            dmLocked = false
        )
    }

    override fun addResource(
        resourceName: String,
        resourceContent: InputStream,
        enforcement: Enforcement
    ): OutcomeCode {
        logger.info { "Adding resource $resourceName with enforcement $enforcement" }

        /** Guard clauses */
        if (resourceName.isBlank()) {
            logger.warn { "Resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        /** Lock the status of the services */
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }

        // generate symmetric key
        val symKey = cryptoSKE.generateSymKey()

        if(
            mm.getResources(resourceName = resourceName, status = TupleStatus.OPERATIONAL).isNotEmpty()
        ) {
            return endOfMethod(CODE_003_RESOURCE_ALREADY_EXISTS)
        }

        // generate resource tuple
        val resource = Resource(
            name = resourceName,
            encryptedSymKey = null,
            status = TupleStatus.OPERATIONAL,
            versionNumber = 1
        )
        val resourceSignature = cryptoPKE.createSignature(
            signingKey = adminSigKeyPair.private,
            bytes = resource.getBytesForSignature()
        )
        resource.updateSignature(
            newSignature = resourceSignature,
            newSigner = user.name,
        )

        // generate admin-resource tuple
        val encryptedSymKey = cryptoPKE.asymEncryptSymKey(
            encryptingKey = adminEncKeyPair.public,
            symKey = symKey
        )
        val permission = RolePermission(
            roleName = ADMIN,
            resourceName = resourceName,
            encryptedSymKey = encryptedSymKey,
            resourceVersionNumber = 1,
            roleVersionNumber = 1,
            operation = Operation.READWRITE,
            roleToken = "1",
            resourceToken = "1",
            status = TupleStatus.OPERATIONAL
        )
        val permissionSignature = cryptoPKE.createSignature(
            bytes = permission.getBytesForSignature(),
            signingKey = adminSigKeyPair.private,
        )
        permission.updateSignature(
            newSignature = permissionSignature,
            newSigner = user.name,
        )

        // add tuples to MM

        /** Add the resource in the MM */
        code = mm.addResource(resource)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        /** Add the role-permission assignments in the MM */
        code = mm.addRolesPermissions(hashSetOf(permission))
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        code = dm?.addResource(resource, "".inputStream()) ?: CODE_000_SUCCESS
        if(code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }


        /** Add the resource in the DM */
        return endOfMethod(CODE_000_SUCCESS)
    }

    override fun deleteResource(resourceName: String): OutcomeCode {
        logger.info { "Deleting resource $resourceName" }

        /** Guard clauses */
        if (resourceName.isBlank()) {
            logger.warn { "Resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        /** Lock the status of the services */
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }

        // verify that no permission is associated with the resource
//        val permissions = mm.getRolesPermissions(
//            resourceName = resourceName,
//            status = TupleStatus.OPERATIONAL
//        )
//        if (permissions.count() != 1) {
//            TODO("only admin")
//        }
//        if (permissions.first().roleName != ADMIN) {
//            TODO("only admin")
//        }

        code = mm.deleteRolesPermissions(
            roleName = ADMIN,
            resourceName = resourceName
        )
        if(code != CODE_000_SUCCESS && code != CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND) {
            return endOfMethod(code)
        }

        val resourcesOpt = mm.getResources(
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL
        )
        // update cahced tuples in MM
        code = mm.deleteResource(resourceName)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }
        for (oldResource in resourcesOpt) {
            // generate cached tuples
            val cachedResource = Resource(
                name = oldResource.name,
                encryptedSymKey = oldResource.encryptedSymKey,
                versionNumber = oldResource.versionNumber,
                status = TupleStatus.CACHED
            )
            val cachedResourceSignature = cryptoPKE.createSignature(
                signingKey = adminSigKeyPair.private,
                bytes = cachedResource.getBytesForSignature()
            )
            cachedResource.updateSignature(
                newSignature = cachedResourceSignature,
                newSigner = user.name,
            )

            code = mm.addResource(cachedResource)
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }
        }

        // delete resource from DM
        code = dm?.deleteResource(
            resourceName = resourceName,
            // resourceVersionNumber = null
        ) ?: CODE_000_SUCCESS
        if(code == CODE_006_RESOURCE_NOT_FOUND) {
            // the resource can be created but never written
            code = CODE_000_SUCCESS
        }

        /** Delete the resource from the DM */
        return endOfMethod(code)
    }

    override fun assignUserToRole(username: String, roleName: String): OutcomeCode {
        logger.info { "Assigning user $username to role $roleName" }

        /** Guard clauses */
        if (username.isBlank() || roleName.isBlank()) {
            logger.warn { "Username or role name is blank" }
            return CODE_020_INVALID_PARAMETER
        }
//        if (roleName == ADMIN) {
//            logger.warn { "Cannot assign users to the $ADMIN role" }
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }

        /** Lock the status of the services */
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }

        val usr = mm.getUsers(username = username)
            .filter { it.status == TupleStatus.OPERATIONAL || it.status == TupleStatus.INCOMPLETE }
        if(usr.isEmpty()) {
            return endOfMethod(CODE_004_USER_NOT_FOUND)
        }
        if(usr.first().status == TupleStatus.INCOMPLETE) {
            return endOfMethod(CODE_053_USER_IS_INCOMPLETE)
        }

        if(
            mm.getRoles(
                roleName = roleName,
                status = TupleStatus.OPERATIONAL
            ).isEmpty()
        ) {
            return endOfMethod(CODE_005_ROLE_NOT_FOUND)
        }

        if(
            mm.getUsersRoles(
                username = username,
                roleName = roleName,
                status = TupleStatus.OPERATIONAL,
                limit = 1,
                offset = 0
            ).isNotEmpty()
        ) {
            return endOfMethod(CODE_010_USER_ROLE_ASSIGNMENT_ALREADY_EXISTS)
        }

        // retrieve user tuple
        val userToAssign = mm.getUsers(
            username = username,
            status = TupleStatus.OPERATIONAL,
            offset = 0,
            limit = 1
        ).firstOrNull() ?: return endOfMethod(CODE_004_USER_NOT_FOUND)
        code = verifyTupleSignature(userToAssign)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        // retrieve user-role of admin
        val adminUserRole = mm.getUsersRoles(
            username = ADMIN,
            roleName = roleName,
            offset = 0,
            limit = 1,
            status = TupleStatus.OPERATIONAL
        ).firstOrNull()
        if (adminUserRole == null) {
            logger.warn {
                "Admin user-role assignment for role $roleName not" +
                        " found. Probably the role does not exist"
            }
            return endOfMethod(
                code = CODE_005_ROLE_NOT_FOUND,
                dmLocked = false
            )
        }
        code = verifyTupleSignature(adminUserRole, true)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        // decrypt role's enc keys with admin's private key
        val asymEncKeys = cryptoPKE.decryptAsymEncKeys(
            encryptingKey = adminEncKeyPair.public,
            decryptingKey = adminEncKeyPair.private,
            encryptedAsymEncKeys = EncryptedAsymKeys(
                private = adminUserRole.encryptedAsymEncKeys!!.private,
                public = adminUserRole.encryptedAsymEncKeys.public,
                keyType = AsymKeysType.ENC,
            )
        )

        // decrypt role's sig keys with admin's private key
        val asymSigKeys = cryptoPKE.decryptAsymSigKeys(
            encryptingKey = adminEncKeyPair.public,
            decryptingKey = adminEncKeyPair.private,
            encryptedAsymSigKeys = EncryptedAsymKeys(
                private = adminUserRole.encryptedAsymSigKeys!!.private,
                public = adminUserRole.encryptedAsymSigKeys.public,
                keyType = AsymKeysType.SIG,
            )
        )

        // obtain user's encryption key
        val userEncKey = cryptoPKE.recreateAsymPublicKey(
            asymPublicKeyBytes = userToAssign.asymEncKeys!!.public.decodeBase64(),
            type = AsymKeysType.SIG
        )

        // encrypt key pairs of role with user's public key
        val encryptedAsymEncKeys = cryptoPKE.encryptAsymKeys(
            encryptingKey = userEncKey,
            asymKeys = asymEncKeys,
            type = AsymKeysType.ENC
        )
        val encryptedAsymSigKeys = cryptoPKE.encryptAsymKeys(
            encryptingKey = userEncKey,
            asymKeys = asymSigKeys,
            type = AsymKeysType.SIG
        )

        // create new user role tuple
        val newUserRole = UserRole(
            username = username,
            roleName = roleName,
            roleVersionNumber = adminUserRole.roleVersionNumber,
            encryptedAsymEncKeys = encryptedAsymEncKeys,
            encryptedAsymSigKeys = encryptedAsymSigKeys,
            status = TupleStatus.OPERATIONAL
        )
        val newUserRoleSignature = cryptoPKE.createSignature(
            bytes = newUserRole.getBytesForSignature(),
            signingKey = adminSigKeyPair.private
        )
        newUserRole.updateSignature(
            newSignature = newUserRoleSignature,
            newSigner = user.name,
        )

        // add tuple to MM
        code = mm.addUsersRoles(hashSetOf(newUserRole))
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(
                code = code,
                dmLocked = false
            )
        }

        return endOfMethod(CODE_000_SUCCESS)

        // alread exists
    }

    override fun revokeUserFromRole(username: String, roleName: String): OutcomeCode {
        logger.info { "Revoking user $username from role $roleName" }

        /** Guard clauses */
        if (username.isBlank() || roleName.isBlank()) {
            logger.warn { "Username or role name is blank" }
            return CODE_020_INVALID_PARAMETER
        }
//        if (username == ADMIN || roleName == ADMIN) {
//            logger.warn { "Cannot revoke the $ADMIN user or role" }
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }

        /** Lock the status of the services */
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }

        if(mm.getRoles(
            roleName = roleName,
            status = TupleStatus.OPERATIONAL
        ).isEmpty()) {
            return endOfMethod(CODE_005_ROLE_NOT_FOUND)
        }

        // retrieve old tuple
        val userRole = mm.getUsersRoles(
            username = username,
            roleName = roleName,
            status = TupleStatus.OPERATIONAL,
            offset = 0,
            limit = 1
        ).firstOrNull() ?: return endOfMethod(CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND)
        code = verifyTupleSignature(userRole, true)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        // create cached tuple
        val cachedUserRole = UserRole(
            username = userRole.username,
            roleName = userRole.roleName,
            encryptedAsymEncKeys = userRole.encryptedAsymEncKeys,
            encryptedAsymSigKeys = userRole.encryptedAsymSigKeys,
            roleVersionNumber = userRole.roleVersionNumber,
            status = TupleStatus.CACHED
        )
        val cachedUserRoleSignature = cryptoPKE.createSignature(
            bytes = cachedUserRole.getBytesForSignature(),
            signingKey = adminSigKeyPair.private
        )
        cachedUserRole.updateSignature(
            newSignature = cachedUserRoleSignature,
            newSigner = user.name,
        )

        code = mm.deleteUsersRoles(
            username = username,
            roleName = roleName,
            status = TupleStatus.OPERATIONAL
        )
        if(code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        code = mm.addUsersRoles(hashSetOf(cachedUserRole))
        return endOfMethod(code)
    }

    override fun assignPermissionToRole(roleName: String, resourceName: String, operation: Operation): OutcomeCode {
        logger.info {
            "Assigning operation $operation to " +
                    "role $roleName over resource $resourceName"
        }

        /** Guard clauses */
        if (roleName.isBlank() || resourceName.isBlank()) {
            logger.warn { "Role or resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }
//        if (operation != Operation.READ && operation != Operation.READWRITE) {
//            logger.warn {
//                "Invalid permission, must be either " +
//                        "${Operation.READ} or ${Operation.READWRITE}"
//            }
//            return CODE_016_INVALID_PERMISSION
//        }

        /** Lock the status of the services */
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }

        // get role-permission assignment
        val rolePermOpt = mm.getRolesPermissions(
            roleName = roleName,
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL,
            offset = 0,
            limit = 1
        )

        if(rolePermOpt.isNotEmpty()) {
            val oldRolePerm = rolePermOpt.first()
            code = verifyTupleSignature(oldRolePerm, true)
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            val totalOp = oldRolePerm.operation.merge(operation)

            // already have ops
            if (totalOp == oldRolePerm.operation) {
                return endOfMethod(CODE_000_SUCCESS)
            }

            // create new
            val rolePerm = RolePermission(
                roleName = oldRolePerm.roleName,
                resourceName = oldRolePerm.resourceName,
                encryptedSymKey = oldRolePerm.encryptedSymKey,
                roleVersionNumber = oldRolePerm.roleVersionNumber,
                resourceVersionNumber = oldRolePerm.resourceVersionNumber,
                status = TupleStatus.OPERATIONAL,
                operation = totalOp,
                roleToken = "1",
                resourceToken = "1",
            )
            val permissionSignature = cryptoPKE.createSignature(
                bytes = rolePerm.getBytesForSignature(),
                signingKey = adminSigKeyPair.private,
            )
            rolePerm.updateSignature(
                newSignature = permissionSignature,
                newSigner = user.name,
            )

            // replace tuple in MM
            code = mm.deleteRolesPermissions(
                roleName = roleName,
                resourceName = resourceName,
            )
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }
            code = mm.addRolesPermissions(hashSetOf(rolePerm))
            return endOfMethod(code)
        } else {
            // role permission does not exist

            // retrieve role tuple
            val role = mm.getRoles(
                roleName = roleName,
                status = TupleStatus.OPERATIONAL,
                offset = 0,
                limit = 1
            ).firstOrNull() ?: return endOfMethod(CODE_005_ROLE_NOT_FOUND)
            code = verifyTupleSignature(role, true)
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            // retrieve admin-resource assignment
            val adminPerm = mm.getRolesPermissions(
                roleName = ADMIN,
                resourceName = resourceName,
                status = TupleStatus.OPERATIONAL,
                offset = 0,
                limit = 1
            ).firstOrNull() ?: return endOfMethod(CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND)
            code = verifyTupleSignature(adminPerm)
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            // decrypt file encryption key
            val resourceKey = cryptoPKE.asymDecryptSymKey(
                encryptingKey = adminEncKeyPair.public,
                decryptingKey = adminEncKeyPair.private,
                encryptedSymKey = adminPerm.encryptedSymKey!!
            )

            // reconstruct role key
            val roleEncKey = cryptoPKE.recreateAsymPublicKey(
                asymPublicKeyBytes = role.asymEncKeys!!.public.decodeBase64(),
                type = AsymKeysType.ENC
            )

            // encrypt file key with the role's key
            val encResourceKey = cryptoPKE.asymEncryptSymKey(
                encryptingKey = roleEncKey,
                symKey = resourceKey
            )

            // create role-perm tuple
            val rolePerm = RolePermission(
                roleName = roleName,
                resourceName = resourceName,
                roleToken = "1",
                resourceToken = "1",
                encryptedSymKey = encResourceKey,
                roleVersionNumber = role.versionNumber,
                resourceVersionNumber = adminPerm.resourceVersionNumber,
                operation = operation,
                status = TupleStatus.OPERATIONAL,
            )
            val permissionSignature = cryptoPKE.createSignature(
                bytes = rolePerm.getBytesForSignature(),
                signingKey = adminSigKeyPair.private,
            )
            rolePerm.updateSignature(
                newSignature = permissionSignature,
                newSigner = user.name,
            )
            code = verifyTupleSignature(rolePerm)

            // add tuple to MM
            code = mm.addRolesPermissions(hashSetOf(rolePerm))
            return endOfMethod(code)
        }
    }

    override fun revokePermissionFromRole(roleName: String, resourceName: String, operation: Operation): OutcomeCode {
        logger.info {
            "Revoking operation $operation from role " +
                    "$roleName over resource $resourceName"
        }

        /** Guard clauses */
        if (roleName.isBlank() || resourceName.isBlank()) {
            logger.warn { "Role or resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }
//        if (roleName == ADMIN) {
//            logger.warn { "Cannot revoke the $ADMIN role" }
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }

        /** Lock the status of the services */
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }

        // get resource
        val resource = mm.getResources(
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL,
            offset = 0,
            limit = 1
        )
        if(resource.isEmpty()) {
            return CODE_006_RESOURCE_NOT_FOUND
        }

        // retrieve old tuple
        val oldRolePerm = mm.getRolesPermissions(
            roleName = roleName,
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL,
            offset = 0,
            limit = 1
        ).firstOrNull() ?: return endOfMethod(CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND)
        code = verifyTupleSignature(oldRolePerm, true)
        if(code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        val newOps = oldRolePerm.operation.subtract(operation)

        // remove old tuple from MM
        code = mm.deleteRolesPermissions(
            roleName = roleName,
            resourceName = resourceName,
        )

        if(newOps == null) {
            // move the tuple to cache
            val rolePerm = RolePermission(
                roleName = oldRolePerm.roleName,
                resourceName = oldRolePerm.resourceName,
                roleToken = "1",
                resourceToken = "1",
                operation = oldRolePerm.operation,
                encryptedSymKey = oldRolePerm.encryptedSymKey!!,
                roleVersionNumber = oldRolePerm.roleVersionNumber,
                resourceVersionNumber = oldRolePerm.resourceVersionNumber,
                status = TupleStatus.CACHED
            )
            val rolePermSignature = cryptoPKE.createSignature(
                bytes = rolePerm.getBytesForSignature(),
                signingKey = adminSigKeyPair.private
            )
            rolePerm.updateSignature(
                newSignature = rolePermSignature,
                newSigner = user.name,
            )

            // add tuple to MM
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            code = mm.addRolesPermissions(hashSetOf(rolePerm))
            return endOfMethod(code)
        } else {
            // create a tuple with removed permission in cache
            val rolePerm = RolePermission(
                roleName = oldRolePerm.roleName,
                resourceName = oldRolePerm.resourceName,
                roleToken = "1",
                resourceToken = "1",
                encryptedSymKey = oldRolePerm.encryptedSymKey,
                roleVersionNumber = oldRolePerm.roleVersionNumber,
                resourceVersionNumber = oldRolePerm.resourceVersionNumber,
                operation = operation,
                status = TupleStatus.CACHED
            )
            val rolePermSignature = cryptoPKE.createSignature(
                bytes = rolePerm.getBytesForSignature(),
                signingKey = adminSigKeyPair.private
            )
            rolePerm.updateSignature(
                newSignature = rolePermSignature,
                newSigner = user.name,
            )
            // add tuple to MM
            code = mm.addRolesPermissions(hashSetOf(rolePerm))
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            // create operative tuples without removed permissions
            val newRolePerm = RolePermission(
                roleName = oldRolePerm.roleName,
                resourceName = oldRolePerm.resourceName,
                roleToken = "1",
                resourceToken = "1",
                encryptedSymKey = oldRolePerm.encryptedSymKey,
                roleVersionNumber = oldRolePerm.roleVersionNumber,
                resourceVersionNumber = oldRolePerm.resourceVersionNumber,
                operation = newOps,
                status = TupleStatus.OPERATIONAL,
            )
            val newRolePermSignature = cryptoPKE.createSignature(
                bytes = newRolePerm.getBytesForSignature(),
                signingKey = adminSigKeyPair.private
            )
            newRolePerm.updateSignature(
                newSignature = newRolePermSignature,
                newSigner = user.name,
            )
            // add tuple to MM
            code = mm.addRolesPermissions(hashSetOf(newRolePerm))
            return endOfMethod(code)
        }
    }

    override fun readResource(resourceName: String): CodeResource {
        logger.info { "Reading resource $resourceName by user ${user.name}" }

        /** Guard clauses */
        if (resourceName.isBlank()) {
            logger.warn { "Resource name is blank" }
            return CodeResource(CODE_020_INVALID_PARAMETER)
        }

        /** Lock the status of the services */
        var code = startOfMethod(acLock = false)
        if (code != CODE_000_SUCCESS) {
            return CodeResource(code)
        }

        // search usable role
        val rolePerms = mm.getRolesPermissions(
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL
        )
        var rp: RolePermission? = null;
        var ur: UserRole? = null;

        for (rolePerm in rolePerms) {
            // verify operation
            if(rolePerm.operation == Operation.WRITE) {
                // cannot be used to read the resource
                continue
            }

            // verify tuple
            code = verifyTupleSignature(rolePerm)
            if(code != CODE_000_SUCCESS) {
                return CodeResource(endOfMethod(code))
            }

            // get associated tuple
            val urOpt = mm.getUsersRoles(
                username = user.name,
                roleName = rolePerm.roleName,
                offset = 0,
                limit = 1
            )
            if(urOpt.isNotEmpty()) {
                code = verifyTupleSignature(urOpt.first(), true)
                if (code != CODE_000_SUCCESS) {
                    return CodeResource(endOfMethod(code))
                }
                rp = rolePerm;
                ur = urOpt.first()
                break
            }
        }
        if(rp == null) {
            return CodeResource(endOfMethod(CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND))
        }
        if(ur == null) {
            return CodeResource(endOfMethod(CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND))
        }

        // retrieve role
        val role = mm.getRoles(
            roleName = ur.roleName,
            offset = 0,
            limit = 1
        ).firstOrNull() ?: return CodeResource(endOfMethod(CODE_005_ROLE_NOT_FOUND))

        // retrieve resource
        val resources = mm.getResources(
            resourceName = resourceName
        )
        if(resources.isEmpty()){
            return CodeResource(endOfMethod(CODE_006_RESOURCE_NOT_FOUND))
        }
        for(resource in resources) {
            code = verifyTupleSignature(resource)
            if (code != CODE_000_SUCCESS) {
                return CodeResource(endOfMethod(code))
            }
        }

        // find last file version
        val firstResource = resources.stream()
            .sorted { o1, o2 -> o2.versionNumber.compareTo(o1.versionNumber) }
            .findFirst().get()


        // decrypt roles key
        val roleEncKeyPair = cryptoPKE.decryptAsymEncKeys(
            encryptedAsymEncKeys = ur.encryptedAsymEncKeys!!,
            encryptingKey = adminEncKeyPair.public,
            decryptingKey = adminEncKeyPair.private
        )

        // decrypt last file key
        val lastFileKey = cryptoPKE.asymDecryptSymKey(
            encryptedSymKey = rp.encryptedSymKey!!,
            encryptingKey = roleEncKeyPair.public,
            decryptingKey = roleEncKeyPair.private
        )

        // decrypt eventually first file key
        val fileKey = if (firstResource.versionNumber < rp.resourceVersionNumber) {
            cryptoSKE.symDecryptSymKey(
                encryptedKey = firstResource.encryptedSymKey!!,
                decryptingKey = lastFileKey
            )
        } else {
            lastFileKey
        }

        // decrypt resource

        val resourceToReadResult = dm?.readResource(resourceName) ?:
                return CodeResource(endOfMethod(CODE_999_GENERIC_ERROR))

        if (resourceToReadResult.code != CODE_000_SUCCESS) {
            return CodeResource(
                code = endOfMethod(
                    code = resourceToReadResult.code,
                    acLocked = false
                ))
        }
        var resourceStream = resourceToReadResult.stream!!
        resourceStream = cryptoSKE.decryptStream(
            decryptingKey = fileKey,
            stream = resourceStream
        )

        return CodeResource(
            code = endOfMethod(
                code = CODE_000_SUCCESS,
                acLocked = false
            ),
            stream = resourceStream
        )
    }

    override fun writeResource(resourceName: String, resourceContent: InputStream): OutcomeCode {
        logger.info { "Reading resource $resourceName by user ${user.name}" }

        /** Guard clauses */
        if (resourceName.isBlank()) {
            logger.warn { "Resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        /** Lock the status of the services */
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }

        if(
            mm.getResources(
                resourceName = resourceName,
                status = TupleStatus.OPERATIONAL
            ).isEmpty()
        ) {
            return endOfMethod(CODE_006_RESOURCE_NOT_FOUND)
        }

        // search usable role
        val rolePerms = mm.getRolesPermissions(
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL
        )
        var rp: RolePermission? = null;
        var ur: UserRole? = null;

        for (rolePerm in rolePerms) {
            // verify operation
            if(rolePerm.operation == Operation.WRITE) {
                // cannot be used to read the resource
                continue
            }

            // verify tuple
            code = verifyTupleSignature(rolePerm)
            if(code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            // get associated tuple
            val urOpt = mm.getUsersRoles(
                username = user.name,
                roleName = rolePerm.roleName,
                status = TupleStatus.OPERATIONAL,
                offset = 0,
                limit = 1
            )
            if(urOpt.isNotEmpty()) {
                code = verifyTupleSignature(urOpt.first(), true)
                if (code != CODE_000_SUCCESS) {
                    return endOfMethod(code)
                }
                rp = rolePerm;
                ur = urOpt.first()
                break
            }
        }
        if(rp == null) {
            return endOfMethod(CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND)
        }
        if(ur == null) {
            return endOfMethod(CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND)
        }

        val oldResourceTuples = mm.getResources(
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL
        )
        var lastRes: Resource? = null
        for (res in oldResourceTuples) {
            // do not delete the last tuple
            if(res.versionNumber == rp.resourceVersionNumber) {
                lastRes = res
                continue
            }

            // verify signature
            code = verifyTupleSignature(res)
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            // cached tuple
            val cachedRes = Resource(
                name = res.name,
                encryptedSymKey = res.encryptedSymKey,
                versionNumber = res.versionNumber,
                status = TupleStatus.DELETED
            )
            val cachedResSignature = cryptoPKE.createSignature(
                bytes = cachedRes.getBytesForSignature(),
                signingKey = adminSigKeyPair.private
            )
            cachedRes.updateSignature(
                newSignature = cachedResSignature,
                newSigner = user.name
            )
        }
        // delete all (except last)
        mm.deleteResource(resourceName)
        mm.addResource(lastRes!!)

        // call cleanup
        cleanup()

        // decrypt resource key
        // decrypt roles key
        val roleEncKeyPair = cryptoPKE.decryptAsymEncKeys(
            encryptedAsymEncKeys = ur.encryptedAsymEncKeys!!,
            encryptingKey = adminEncKeyPair.public,
            decryptingKey = adminEncKeyPair.private
        )

        // decrypt last file key
        val lastFileKey = cryptoPKE.asymDecryptSymKey(
            encryptedSymKey = rp.encryptedSymKey!!,
            encryptingKey = roleEncKeyPair.public,
            decryptingKey = roleEncKeyPair.private
        )

        // encrypt resource
        val encrypted = cryptoSKE.encryptStream(
            encryptingKey = lastFileKey,
            stream = resourceContent
        )

        // update DM
        code = dm!!.writeResource(
            resourceContent = encrypted,
            updatedResource = lastRes
        )
        return endOfMethod(code)
    }

    fun cleanup() {
        dm = DMLocal(DMServiceLocalParameters())
    }

    override fun getUsers(
        statuses: Array<TupleStatus>
    ): CodeUsers {
        return CodeUsers(
            code = CODE_000_SUCCESS,
            users = HashSet(
                mm.getUsers()
                    .filter { statuses.contains(it.status) }
            )
        )
    }

    override fun getRoles(status: Array<TupleStatus>?): CodeRoles {
        var st = mm.getRoles().stream()
        if(status != null) {
            st = st.filter { status.contains(it.status) }
        }
        return CodeRoles(
            CODE_000_SUCCESS,
            HashSet(st.toList())
        )
    }

    override fun getResources(status: Array<TupleStatus>?): CodeResources {
        var st = mm.getResources().stream()
        if(status != null) {
            st = st.filter { status.contains(it.status) }
        }
        return CodeResources(
            CODE_000_SUCCESS,
            HashSet(st.toList())
        )
    }

    override fun getUsersRoles(username: String?, roleName: String?, status: Array<TupleStatus>?): CodeUsersRoles {
        var st = mm.getUsersRoles(
            username = username,
            roleName = roleName
        ).stream()
        if (status != null) {
            st = st.filter { status.contains(it.status) }
        }
        return CodeUsersRoles(
            CODE_000_SUCCESS,
            HashSet(st.toList())
        )
    }

    override fun getRolesPermissions(
        username: String?,
        roleName: String?,
        resourceName: String?,
        status: Array<TupleStatus>?
    ): CodeRolesPermissions {
        if(username != null) {
            val roles = mm.getUsersRoles(username, status = TupleStatus.OPERATIONAL)
            val result = HashSet<RolePermission>()

            for (role in roles) {
                var st = mm.getRolesPermissions(
                    roleName = role.roleName,
                    resourceName = resourceName,
                ).stream()
                if (status != null) {
                    st = st.filter { status.contains(it.status) }
                }
                result.addAll(st.toList())
            }

            return CodeRolesPermissions(
                CODE_000_SUCCESS,
                result
            )

        } else {

            var st = mm.getRolesPermissions(
                roleName = roleName,
                resourceName = resourceName,
            ).stream()
            if (status != null) {
                st = st.filter { status.contains(it.status) }
            }
            return CodeRolesPermissions(
                CODE_000_SUCCESS,
                HashSet(st.toList())
            )
        }
    }

    override fun rotateResourceKey(resourceName: String) : OutcomeCode{
        /** Lock the status of the services */
        var code = startOfMethod(acLock = false)
        if (code != CODE_000_SUCCESS) {
            return code
        }

        // find admin PA to the resource
        val adminPerm = mm.getRolesPermissions(
            roleName = ADMIN,
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL,
            offset = 0,
            limit = 1
        ).firstOrNull() ?: return endOfMethod(CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND)

        // get alla resources
        val resources = mm.getResources(
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL
        )
        val lastRes = resources.stream().filter { it.versionNumber == adminPerm.resourceVersionNumber }.findFirst().get()
        code = verifyTupleSignature(lastRes)
        if(code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }
        val oldRes = resources.stream().filter { it.versionNumber != adminPerm.resourceVersionNumber }.findFirst().getOrNull()
        if(oldRes != null) {
            code = verifyTupleSignature(oldRes)
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }
        }

        code = mm.deleteResource(resourceName)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        // get last file key
        val lastKey = cryptoPKE.asymDecryptSymKey(
            decryptingKey = adminEncKeyPair.private,
            encryptingKey = adminEncKeyPair.public,
            encryptedSymKey = adminPerm.encryptedSymKey!!
        )

        // generate new file key
        val newFileKey = cryptoSKE.generateSymKey()

        if(oldRes != null) {
            val oldOldKey = cryptoSKE.symDecryptSymKey(
                encryptedKey = oldRes.encryptedSymKey!!,
                decryptingKey = lastKey
            )

            val newOldKey = cryptoSKE.symEncryptSymKey(
                encryptingKey = newFileKey,
                key = oldOldKey
            )

            // recreate tuple
            val newOldResource = Resource(
                name = oldRes.name,
                encryptedSymKey = newOldKey,
                versionNumber = oldRes.versionNumber
            )
            val newOldResourceSignature = cryptoPKE.createSignature(
                signingKey = adminSigKeyPair.private,
                bytes = newOldResource.getBytesForSignature()
            )
            newOldResource.updateSignature(
                newSigner = user.name,
                newSignature = newOldResourceSignature
            )

            mm.addResource(newOldResource)
        }

        // create new tuple
        val newResource = Resource(
            name = lastRes.name,
            encryptedSymKey = null,
            versionNumber = lastRes.versionNumber + 1,
        )
        val newResourceSignature = cryptoPKE.createSignature(
            signingKey = adminSigKeyPair.private,
            bytes = newResource.getBytesForSignature()
        )
        newResource.updateSignature(
            newSigner = user.name,
            newSignature = newResourceSignature
        )
        code = mm.addResource(newResource)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        val rolePerms = mm.getRolesPermissions(
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL
        )
        code = mm.deleteRolesPermissions(
            resourceName = resourceName,
        )
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }
        for (rp in rolePerms) {
            // verify tuple
            code = verifyTupleSignature(rp)
            if(code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            // find role
            val role = mm.getRoles(
                roleName = rp.roleName,
                status = TupleStatus.OPERATIONAL,
                offset = 0,
                limit = 1
            ).firstOrNull() ?: return endOfMethod(CODE_005_ROLE_NOT_FOUND)

            // encrypt sym key with role pub key
            val encryptedNewKey = cryptoPKE.asymEncryptSymKey(
                symKey = newFileKey,
                encryptingKey = cryptoPKE.recreateAsymPublicKey(
                    asymPublicKeyBytes = role.asymEncKeys!!.public.decodeBase64(),
                    type = AsymKeysType.ENC
                )
            )

            // recreate old tuple in cache
            val oldPerm = RolePermission(
                roleName = rp.roleName,
                resourceName = rp.resourceName,
                resourceToken = "1",
                roleToken = "1",
                encryptedSymKey = encryptedNewKey,
                status = TupleStatus.CACHED,
                operation = rp.operation,
                roleVersionNumber = rp.roleVersionNumber,
                resourceVersionNumber = rp.resourceVersionNumber
            )
            val oldPermSignature = cryptoPKE.createSignature(
                signingKey = adminSigKeyPair.private,
                bytes = oldPerm.getBytesForSignature()
            )
            oldPerm.updateSignature(
                newSignature = oldPermSignature,
                newSigner = user.name
            )

            // create new tuple
            val newPerm = RolePermission(
                roleName = rp.roleName,
                resourceName = rp.resourceName,
                resourceToken = "1",
                roleToken = "1",
                encryptedSymKey = encryptedNewKey,
                status = TupleStatus.OPERATIONAL,
                operation = rp.operation,
                roleVersionNumber = rp.roleVersionNumber,
                resourceVersionNumber = lastRes.versionNumber + 1
            )
            val newPermSignature = cryptoPKE.createSignature(
                signingKey = adminSigKeyPair.private,
                bytes = newPerm.getBytesForSignature()
            )
            newPerm.updateSignature(
                newSignature = newPermSignature,
                newSigner = user.name
            )

            // add tuples to MM
            code = mm.addRolesPermissions(hashSetOf(oldPerm, newPerm ))
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }
        }

        return endOfMethod(CODE_000_SUCCESS)
    }

    override fun eagerReencryption(resourceName: String): OutcomeCode {
        val result = readResource(resourceName)
        if(result.code != CODE_000_SUCCESS) {
            return result.code
        }

        val code = writeResource(resourceName, result.stream!!)
        return code
    }

    override fun rotateRoleKeyUserRoles(roleName: String): OutcomeCode {
        /** Lock the status of the services */
        var code = startOfMethod(acLock = false)
        if (code != CODE_000_SUCCESS) {
            return code
        }

        // retrieve role tuple
        val role = mm.getRoles(
            roleName = roleName,
            status = TupleStatus.OPERATIONAL,
            offset = 0,
            limit = 1
        ).firstOrNull() ?: return endOfMethod(CODE_005_ROLE_NOT_FOUND)

        // generate new key pairs
        val newEncKeyPair = cryptoPKE.generateAsymEncKeys()
        val newSigKeyPair = cryptoPKE.generateAsymSigKeys()

        // create cached tuple
        val cachedRole = Role(
            name = role.name,
            asymEncKeys = role.asymEncKeys,
            asymSigKeys = role.asymSigKeys,
            versionNumber = role.versionNumber,
            status = TupleStatus.CACHED
        )
        val cachedRoleSignature = cryptoPKE.createSignature(
            signingKey = adminSigKeyPair.private,
            bytes = cachedRole.getBytesForSignature()
        )
        cachedRole.updateSignature(
            newSignature = cachedRoleSignature,
            newSigner = user.name
        )

        // update MM
        code = mm.deleteRole(roleName = roleName)
        if(code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }
        code = mm.addRole(cachedRole)
        if(code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        // create new role
        val newRole = Role(
            name = role.name,
            asymEncKeys = AsymKeys(
                keysType = AsymKeysType.ENC,
                public = newEncKeyPair.public.encoded.encodeBase64(),
                private = ""
            ),
            asymSigKeys = AsymKeys(
                keysType = AsymKeysType.SIG,
                public = newSigKeyPair.public.encoded.encodeBase64(),
                private = ""
            ),
            versionNumber = role.versionNumber + 1,
            status = TupleStatus.OPERATIONAL
        )
        val newRoleSignature = cryptoPKE.createSignature(
            signingKey = adminSigKeyPair.private,
            bytes = newRole.getBytesForSignature()
        )
        newRole.updateSignature(
            newSignature = newRoleSignature,
            newSigner = user.name
        )

        code = mm.addRole(newRole)
        if(code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        // replace UR tuples
        val userRoles = mm.getUsersRoles(
            roleName = roleName,
            status = TupleStatus.OPERATIONAL
        )
        code = mm.deleteUsersRoles(roleName = roleName, status = TupleStatus.OPERATIONAL)
        if(code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        for(ur in userRoles) {
            // verify signature
            code = verifyTupleSignature(ur, true)
            if(code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            // retrieve user tuple
            val oldUser = mm.getUsers(
                username = ur.username,
                status = TupleStatus.OPERATIONAL
            ).firstOrNull() ?: return endOfMethod(CODE_004_USER_NOT_FOUND)
            code = verifyTupleSignature(user)
            if(code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            // create new user role tuple
            val userPubKey = cryptoPKE.recreateAsymPublicKey(
                asymPublicKeyBytes = oldUser.asymEncKeys!!.public.decodeBase64(),
                type = AsymKeysType.ENC
            )
            val encryptedRoleEncKey = cryptoPKE.encryptAsymKeys(
                encryptingKey = userPubKey,
                asymKeys = newEncKeyPair,
                type = AsymKeysType.ENC
            )
            val encryptedRoleSigKey = cryptoPKE.encryptAsymKeys(
                encryptingKey = userPubKey,
                asymKeys = newSigKeyPair,
                type = AsymKeysType.SIG
            )

            val newUserRole = UserRole(
                username = ur.username,
                roleName = ur.roleName,
                encryptedAsymEncKeys = encryptedRoleEncKey,
                encryptedAsymSigKeys = encryptedRoleSigKey,
                roleVersionNumber = newRole.versionNumber,
                status = TupleStatus.OPERATIONAL
            )
            val newUserRoleSignature = cryptoPKE.createSignature(
                signingKey = adminSigKeyPair.private,
                bytes = newUserRole.getBytesForSignature()
            )
            newUserRole.updateSignature(
                newSigner = user.name,
                newSignature = newUserRoleSignature
            )

            code = mm.addUsersRoles(hashSetOf(newUserRole))
            if(code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }
        }

        return endOfMethod(CODE_000_SUCCESS)
    }

    override fun rotateRoleKeyPermissions(roleName: String): OutcomeCode {
        /** Lock the status of the services */
        var code = startOfMethod(acLock = false)
        if (code != CODE_000_SUCCESS) {
            return code
        }

        // retrieve role tuple
        val role = mm.getRoles(
            roleName = roleName,
            status = TupleStatus.OPERATIONAL,
            offset = 0,
            limit = 1
        ).firstOrNull() ?: return endOfMethod(CODE_005_ROLE_NOT_FOUND)
        code = verifyTupleSignature(role)
        if(code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        // recreate permission tuples
        val rolePerms = mm.getRolesPermissions(
            roleName = roleName,
            status = TupleStatus.OPERATIONAL,
        )
        code = mm.deleteRolesPermissions(
            roleName = roleName
        )
        if(code != CODE_000_SUCCESS && code != CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND) {
            return endOfMethod(code)
        }
        code = CODE_000_SUCCESS
        for(rp in rolePerms) {
            // verify tuple
            code = verifyTupleSignature(rp)
            if(code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            // get admin-file tuple
            val adminRes = mm.getRolesPermissions(
                roleName = ADMIN,
                resourceName = rp.resourceName,
                status = TupleStatus.OPERATIONAL,
                offset = 0,
                limit = 1
            ).firstOrNull() ?: continue
            // Non è il modo più bello ma per ora non posso debuggarlo
//            ).firstOrNull() ?: return endOfMethod(CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND)
            code = verifyTupleSignature(adminRes)
            if(code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            // decrypt file key
            val fileKey = cryptoPKE.asymDecryptSymKey(
                encryptedSymKey = adminRes.encryptedSymKey!!,
                encryptingKey = adminEncKeyPair.public,
                decryptingKey = adminEncKeyPair.private
            )

            // encrypt with role key
            val fileKeyNewRole = cryptoPKE.asymEncryptSymKey(
                symKey = fileKey,
                encryptingKey = cryptoPKE.recreateAsymPublicKey(
                    type = AsymKeysType.ENC,
                    asymPublicKeyBytes = role.asymEncKeys!!.public.decodeBase64()
                )
            )

            // create new rolePerm tuple
            val newRolePerm = RolePermission(
                roleName = rp.roleName,
                resourceName = rp.resourceName,
                roleToken = "1",
                resourceToken = "1",
                encryptedSymKey = fileKeyNewRole,
                roleVersionNumber = role.versionNumber,
                resourceVersionNumber = rp.resourceVersionNumber,
                operation = rp.operation,
                status = TupleStatus.OPERATIONAL,
            )
            val newRolePermSignature = cryptoPKE.createSignature(
                signingKey = adminSigKeyPair.private,
                bytes = newRolePerm.getBytesForSignature()
            )
            newRolePerm.updateSignature(
                newSignature = newRolePermSignature,
                newSigner = user.name
            )

            code = mm.addRolesPermissions(hashSetOf(newRolePerm))
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }
        }

        return endOfMethod(CODE_000_SUCCESS)
    }

    /**
     * Verify the signature of the given [tuple]. If the
     * signature is invalid, a SignatureException will be thrown
     */
    private fun verifyTupleSignature(
        tuple: Tuple,
        verifyAdminSigner: Boolean = false,
    ) : OutcomeCode {

        val signature = tuple.signature ?: return CODE_101_NOT_SIGNED

        // verify signer
        if (verifyAdminSigner && tuple.signer != ADMIN) {
            return CODE_100_SIGNATURE_NOT_VALID
        }

        val verKey = if(user.name == ADMIN) {
            adminSigKeyPair.public
        } else {
            val signer = mm.getUsers(
                username = tuple.signer,
                status = TupleStatus.OPERATIONAL
            ).firstOrNull() ?: return CODE_100_SIGNATURE_NOT_VALID
            cryptoPKE.recreateAsymPublicKey(
                signer.asymSigKeys!!.public.decodeBase64(),
                AsymKeysType.ENC
            )
        }

        // admin signed the tuple
        if (tuple.signer == ADMIN) {
            try {
                cryptoPKE.verifySignature(
                    signature = signature,
                    bytes = tuple.getBytesForSignature(),
                    verifyingKey = verKey
                )
            } catch (e: Exception) {
                return CODE_100_SIGNATURE_NOT_VALID
            }
            return CODE_000_SUCCESS
        } else if (tuple is User && tuple.signer == tuple.name) {
            // how to verify it?
            return CODE_000_SUCCESS
        } else {
            // get user tuple
            val user = mm.getUsers(
                username = tuple.signer,
                status = TupleStatus.OPERATIONAL,
                offset = 0,
                limit = 1
            ).firstOrNull() ?: return CODE_100_SIGNATURE_NOT_VALID

            val code = verifyTupleSignature(
                tuple = user,
                verifyAdminSigner = true
            )
            if (code != CODE_000_SUCCESS) {
                return endOfMethod(code)
            }

            val asymSignKey = cryptoPKE.recreateAsymPublicKey(
                asymPublicKeyBytes = user.asymSigKeys!!.public.decodeBase64(),
                type = AsymKeysType.SIG
            )

            try {
                cryptoPKE.verifySignature(
                    signature = signature,
                    bytes = tuple.getBytesForSignature(),
                    verifyingKey = asymSignKey
                )
            } catch (e: Exception) {
                return CODE_100_SIGNATURE_NOT_VALID
            }

            return CODE_000_SUCCESS
        }

        // never reached
        return CODE_100_SIGNATURE_NOT_VALID
    }

    override fun isProtectedWithCAC(resourceName: String): Boolean {
        return mm.getResources(
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL
        ).isNotEmpty()
    }

    override fun canUserDo(username: String, resourceName: String, operation: Operation): Boolean {
        val userRoles = mm.getUsersRoles(
            username = username,
            status = TupleStatus.OPERATIONAL
        )
        for (ur in userRoles) {
            val rp = mm.getRolesPermissions(
                roleName = ur.roleName,
                resourceName = resourceName
            ).firstOrNull() ?: continue

            if(rp.operation.includes(operation)) {
                return true
            }
        }
        return false
    }

    override fun canUserDoViaRoleCache(
        username: String,
        roleName: String,
        resourceName: String,
        operation: Operation
    ): Boolean {
        val userRoles = mm.getUsersRoles(
            username = username,
            roleName = roleName
        ).filter { it.status == TupleStatus.OPERATIONAL || it.status == TupleStatus.CACHED }

        for (ur in userRoles) {
            val rp = mm.getRolesPermissions(
                roleName = ur.roleName,
                resourceName = resourceName,
            ).filter { it.status == TupleStatus.OPERATIONAL || it.status == TupleStatus.CACHED }
                .firstOrNull { it.roleVersionNumber == ur.roleVersionNumber }

            if (rp != null && rp.operation.includes(operation)) {
                return true
            }
        }
        return false
    }

    override fun canUserDoViaRoleCacheLast(
        username: String,
        roleName: String,
        resourceName: String,
        operation: Operation
    ): Boolean {
        val lastVersion = mm.getResources(
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL
        ).sortedWith { o1, o2 -> o1.versionNumber.compareTo(o2.versionNumber) }
            .lastOrNull()?.versionNumber ?: return false

        val userRoles = mm.getUsersRoles(
            username = username,
            roleName = roleName
        ).filter { it.status == TupleStatus.OPERATIONAL || it.status == TupleStatus.CACHED }

        for (ur in userRoles) {
            val rp = mm.getRolesPermissions(
                roleName = ur.roleName,
                resourceName = resourceName,
            ).filter { it.status == TupleStatus.OPERATIONAL || it.status == TupleStatus.CACHED }
                .filter { it.resourceVersionNumber == lastVersion }
                .firstOrNull { it.roleVersionNumber == ur.roleVersionNumber }

            if (rp != null && rp.operation.includes(operation)) {
                return true
            }
        }
        return false
    }

    override fun canRoleDo(roleName: String, operation: Operation, resourceName: String): Boolean {
        val rp = mm.getRolesPermissions(
            roleName = roleName,
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL
        ).firstOrNull() ?: return false

        return rp.operation.includes(operation)
    }

    override fun canRoleDoCache(roleName: String, operation: Operation, resourceName: String): Boolean {
        val rps = mm.getRolesPermissions(
            roleName = roleName,
            resourceName = resourceName,
        ).filter { it.status == TupleStatus.OPERATIONAL || it.status == TupleStatus.CACHED }

        for (rp in rps) {
            if(rp.operation.includes(operation)) {
                return true
            }
        }
        return false
    }

    override fun canRoleDoCacheLast(roleName: String, operation: Operation, resourceName: String): Boolean {
        val lastVersion = mm.getResources(
            resourceName = resourceName,
            status = TupleStatus.OPERATIONAL
        ).sortedWith { o1, o2 -> o1.versionNumber.compareTo(o2.versionNumber) }
            .lastOrNull()?.versionNumber ?: return false

        val rps = mm.getRolesPermissions(
            roleName = roleName,
            resourceName = resourceName,
        ).filter { it.status == TupleStatus.OPERATIONAL || it.status == TupleStatus.CACHED }
            .filter { it.resourceVersionNumber == lastVersion }

        for (rp in rps) {
            if(rp.operation.includes(operation)) {
                return true
            }
        }
        return false
    }

    override fun canUserBe(username: String, roleName: String): Boolean {
        return mm.getUsersRoles(
            username = username,
            roleName = roleName,
            status = TupleStatus.OPERATIONAL
        ).isNotEmpty()
    }

    override fun canUserBeCache(username: String, roleName: String): Boolean {
        return mm.getUsersRoles(
            username = username,
            roleName = roleName
        ).any { it.status == TupleStatus.OPERATIONAL || it.status == TupleStatus.CACHED }
    }

}
