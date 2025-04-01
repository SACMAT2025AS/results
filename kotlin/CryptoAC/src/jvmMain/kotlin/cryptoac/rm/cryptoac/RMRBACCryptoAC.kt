package cryptoac.rm.cryptoac

import cryptoac.server.API.RM
import cryptoac.Constants.ADMIN
import cryptoac.OutcomeCode
import cryptoac.OutcomeCode.*
import cryptoac.ResponseRoutes
import cryptoac.ResponseRoutes.Companion.internalError
import cryptoac.ResponseRoutes.Companion.ok
import cryptoac.ResponseRoutes.Companion.serviceUnavailable
import cryptoac.ResponseRoutes.Companion.unprocessableEntity
import cryptoac.ServiceType
import cryptoac.ac.ACFactory
import cryptoac.ac.opa.ACServiceRBACOPA
import cryptoac.core.CoreType
import cryptoac.crypto.*
import cryptoac.dm.DMFactory
import cryptoac.dm.cryptoac.DMServiceCryptoAC
import cryptoac.mm.MMFactory
import cryptoac.mm.MMServiceCACRBAC
import cryptoac.rm.model.AddResourceRBACRequest
import cryptoac.rm.model.WriteResourceRBACRequest
import cryptoac.server.API.RESOURCES
import cryptoac.server.SERVER
import cryptoac.tuple.Operation
import cryptoac.tuple.RBACElementType
import cryptoac.tuple.Resource
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/** The objects to interact with the MM based on the core */
private var mmMap: HashMap<CoreType, MMServiceCACRBAC> = hashMapOf()

/** The objects to interact with the DM based on the core */
private var dmMap: HashMap<CoreType, DMServiceCryptoAC> = hashMapOf()

/** The objects to interact with the AC based on the core */
private var acMap: HashMap<CoreType, ACServiceRBACOPA?> = hashMapOf()

/** The objects to interact with the RM based on the core */
private var rmMap: HashMap<CoreType, ACServiceRBACOPA?> = hashMapOf()
// TODO bisogna fillare rmMap così come già facciamo con le altre (dmMap, mmMap, acMap)

/** The PKE cryptographic objects based on the core */
private var cryptoPKEMap: HashMap<CoreType, CryptoPKE?> = hashMapOf()

/** A mutex to handle concurrent requests to the DM */
// TODO however, this means that the RM can serve
//  one user only at a time; can we improve this?
//  Is the mutex really necessary?
private val mutex = Mutex()

/**
 * CryptoAC test implementation of an RM (for RBAC).
 * Routes related to resources:
 *   - add (post);
 *   - write (patch).
 */
fun Route.resourcesRouting() {

    // TODO Authenticate modules (proxy, rm, dm) to
    //  each other (and the user) (PKI, JWT)

    // TODO To counter malicious users who keep adding useless files
    //  to exhaust resources (e.g., storage space), we can implement
    //  a mechanism to limit the number (or size) of files each user
    //  can upload. Similarly, to counter malicious users and DoS
    //  attacks, we can implement a mechanism to limit the number of
    //  files a user can upload or send a warning to the admin.

    /** Wrap all routes related to the RM */
    route(RM) {

        route(RESOURCES) {

            /** Check an add resource operation */
            post {
                val coreParam = call.parameters[SERVER.CORE]
                    ?: return@post unprocessableEntity(
                        call,
                        "Missing ${SERVER.CORE} parameter",
                        CODE_019_MISSING_PARAMETERS
                    )
                val core: CoreType = CoreType.valueOf(coreParam)

                /** If the RM was not configured yet */
                if (
                    !acMap.contains(core) ||
                    !dmMap.contains(core) ||
                    !mmMap.contains(core) ||
                    !cryptoPKEMap.contains(core)
                ) {
                    return@post serviceUnavailable(
                        call = call,
                        message = "This RM was not configured",
                        code = CODE_021_RM_CONFIGURATION
                    )
                }

                logger.debug { "Retrieving the add resource request" }
                val addResourceRBACRequest = call.receive<AddResourceRBACRequest>()
                val resource = addResourceRBACRequest.resource
                val resourceName = resource.name
                val resourceToken = resource.token
                // val reEncryptionThresholdNumber = resource.reEncryptionThresholdNumber
                val rolePermission = addResourceRBACRequest.rolePermission
                val rolePermissionSignature = rolePermission.signature

                logger.info {
                    "User is asking to add a resource with " +
                    "name $resourceName for core $core"
                } // TODO add username (first you need to authenticate users)

                /**
                 * The RM has to check that:
                 * 1. role-permission assignment signature is correct;
                 * 2. the resource version number of the resource is equal to 1;
                 * 3. the role-permission assignment gives permission over the resource to the admin;
                 * 4. the resource and the role-permission assignment are consistent;
                 */

                logger.debug { "Checking signature was given" }
                if (rolePermissionSignature == null) {
                    return@post unprocessableEntity(
                        call = call,
                        message = "Missing role-permission assignment digital signature",
                        code = CODE_019_MISSING_PARAMETERS
                    )
                }

                logger.debug { "Checking resource version number is 1" }
                if (resource.versionNumber != 1 || rolePermission.resourceVersionNumber != 1) {
                    return@post unprocessableEntity(
                        call = call,
                        message = "Version numbers are not 1 (" +
                                "resource version number is ${resource.versionNumber} and " +
                                "role-permission assignment version number ${rolePermission.resourceVersionNumber}",
                        code = CODE_017_INVALID_VERSION_NUMBER
                    )
                }

                logger.debug { "Checking role version number is 1" }
                if (rolePermission.roleVersionNumber != 1) {
                    return@post unprocessableEntity(
                        call = call,
                        message = "Admin role version number is not 1 " +
                                "(it is ${rolePermission.roleVersionNumber})",
                        code = CODE_017_INVALID_VERSION_NUMBER
                    )
                }

                logger.debug { "Checking resource name across resource and role-permission assignment is the same" }
                if (resourceName != rolePermission.resourceName) {
                    return@post unprocessableEntity(
                        call = call,
                        message = "Resource name is not the same across resource and " +
                                "role-permission assignment (resource is $resourceName " +
                                "and role-permission assignment is ${rolePermission.resourceName})",
                        code = CODE_026_TUPLE_FORMAT
                    )
                }

                logger.debug { "Checking resource token across resource and role-permission assignment is the same" }
                if (resourceToken != rolePermission.resourceToken) {
                    return@post unprocessableEntity(
                        call = call,
                        message = "Resource token is not the same across resource and " +
                                "role-permission assignment (resource is $resourceToken " +
                                "and role-permission assignment is ${rolePermission.resourceToken})",
                        code = CODE_026_TUPLE_FORMAT
                    )
                }

                logger.debug { "Checking permission is ${Operation.READWRITE}" }
                if (rolePermission.operation != Operation.READWRITE) {
                    return@post unprocessableEntity(
                        call = call,
                        message = "Permission is not ${Operation.READWRITE} " +
                                "(it is ${rolePermission.operation})",
                        code = CODE_016_INVALID_PERMISSION
                    )
                }

                logger.debug { "Checking the role assigned to the resource is the admin $ADMIN" }
                if (rolePermission.roleName != ADMIN || rolePermission.roleToken != ADMIN) {
                    return@post unprocessableEntity(
                        call = call,
                        message = "Role name or token is not $ADMIN in role-permission assignment " +
                                "(name is ${rolePermission.roleName} and token is " +
                                "${rolePermission.roleToken})",
                        code = CODE_026_TUPLE_FORMAT
                    )
                }

                /** Synchronize the RM */
                val returnCode = mutex.withLock {

                    /** Lock the status of the services */
                    var code = startOfMethod(core)
                    if (code != CODE_000_SUCCESS) {
                        logger.warn { "Error while locking services ($code)" }
                        return@withLock code
                    }

                    logger.debug {
                        "Retrieving the signer's public key" +
                        " to check digital signature"
                    }
                    val rolePermissionSigner = rolePermission.signer
                    val asymSigPubKeyBytes = mmMap[core]!!.getPublicKey(
                        token = rolePermissionSigner,
                        elementType = RBACElementType.USER,
                        asymKeyType = AsymKeysType.SIG,
                    )
                    if (asymSigPubKeyBytes == null) {
                        logger.warn { "Signer user not found (token is $rolePermissionSigner)" }
                        return@withLock endOfMethod(CODE_004_USER_NOT_FOUND, core)
                    }

                    val asymSigPubKey = cryptoPKEMap[core]!!.recreateAsymPublicKey(
                        asymPublicKeyBytes = asymSigPubKeyBytes,
                        type = AsymKeysType.SIG,
                    )

                    logger.debug { "Checking digital signatures" }
                    cryptoPKEMap[core]!!.verifySignature(
                        signature = rolePermissionSignature,
                        bytes = rolePermission.getBytesForSignature(),
                        verifyingKey = asymSigPubKey
                    )

                    logger.debug { "Adding resource in the MM" }
                    code = mmMap[core]!!.addResource(resource)
                    if (code != CODE_000_SUCCESS) {
                        logger.warn { "Error while adding the resource in the MM ($code)" }
                        return@withLock endOfMethod(code, core)
                    }

                    code = acMap[core]?.addResource(
                        resourceName = rolePermission.resourceName,
                    ) ?: CODE_000_SUCCESS
                    if (code != CODE_000_SUCCESS) {
                        logger.warn { "Error while adding the resource in the AC ($code)" }
                        return@withLock endOfMethod(code, core)
                    }

                    code = acMap[core]?.assignPermissionToRole(
                        roleName = rolePermission.roleName,
                        resourceName = rolePermission.resourceName,
                        operation = rolePermission.operation
                    ) ?: CODE_000_SUCCESS
                    if (code != CODE_000_SUCCESS) {
                        logger.warn { "Error while adding PA assignments in the AC ($code)" }
                        return@withLock endOfMethod(code, core)
                    }

                    code = mmMap[core]!!.addRolesPermissions(hashSetOf(rolePermission))
                    if (code != CODE_000_SUCCESS) {
                        logger.warn { "Error while adding the role-permission assignment in the MM ($code)" }
                        return@withLock endOfMethod(code, core)
                    }

                    logger.debug { "Ask the DM to move the resource in the download folder" }
                    code = dmMap[core]!!.writeResource(
                        updatedResource = Resource(
                            name = resourceName,
                            // enforcement = resource.enforcement
                        ),
                        resourceContent = byteArrayOf().inputStream()
                    )
                    if (code == CODE_000_SUCCESS) {
                        logger.info {
                            "Add request for resource with name " +
                            "$resourceName for core $core was completed"
                        }
                    } else {
                        logger.warn { "DM returned error code ($code)" }
                    }
                    return@withLock endOfMethod(code, core)
                }

                when (returnCode) {
                    CODE_000_SUCCESS -> ok(call)
                    CODE_004_USER_NOT_FOUND -> return@post ResponseRoutes.notFound(
                        call = call,
                        message = "Signer user not found",
                        code = returnCode
                    )
                    else -> return@post internalError(
                        call = call,
                        message = "Error during add resource operation ($returnCode)",
                        code = returnCode
                    )
                }
            }

            // TODO perhaps the resource name should be a path parameter?
            /** Check a write resource operation */
            patch {
                val coreParam = call.parameters[SERVER.CORE]
                    ?: return@patch unprocessableEntity(
                        call,
                        "Missing ${SERVER.CORE} parameter",
                        CODE_019_MISSING_PARAMETERS
                    )
                val core: CoreType = CoreType.valueOf(coreParam)

                /** If the RM was not configured yet */
                if (
                    !acMap.contains(core) ||
                    !dmMap.contains(core) ||
                    !mmMap.contains(core) ||
                    !cryptoPKEMap.contains(core)
                ) {
                    return@patch serviceUnavailable(
                        call = call,
                        message = "This RM was not configured",
                        code = CODE_021_RM_CONFIGURATION
                    )
                }

                logger.debug { "Retrieving the write resource request" }
                val writeResourceRBACRequest = call.receive<WriteResourceRBACRequest>()
                val roleName = writeResourceRBACRequest.roleName
                val resource = writeResourceRBACRequest.resource
                val resourceName = resource.name
                // val reEncryptionThresholdNumber = resource.reEncryptionThresholdNumber

                /**
                 * TODO we take the username from the write resource request
                 *  just because we need to know the name of the user to ask OPA
                 *  if the user can write the resource. In the future, this should
                 *  be replaced/integrated by the login/authentication method used by the RM
                 */
                val username = writeResourceRBACRequest.username

                logger.info {
                    "The user $username is asking to write resource " +
                    " $resourceName as role $roleName for core $core"
                }


                /** Synchronize the RM */
                val returnCode = mutex.withLock {

                    /** Lock the status of the services */
                    var code = startOfMethod(core)
                    if (code != CODE_000_SUCCESS) {
                        logger.warn { "Error while locking services ($code)" }
                        return@withLock code
                    }

                    /**
                     * If OPA was configured, check that the user
                     * is authorized to write over the given file
                     */
                    if (acMap[core] != null) {
                        code = acMap[core]!!.canDo(
                            username = username,
                            roleName = roleName,
                            operation = Operation.WRITE,
                            resourceName = resourceName
                        )
                        when (code) {
                            CODE_037_FORBIDDEN -> {
                                /** Return a 404 to avoid information leakage */
                                logger.warn {
                                    "Unauthorized user $username tried " +
                                    "to write over resource $resourceName"
                                }
                                return@withLock endOfMethod(CODE_006_RESOURCE_NOT_FOUND, core)
                            }
                            CODE_000_SUCCESS -> {
                                logger.debug { "User is authorized" }
                            }
                            else -> {
                                logger.warn { "Error during request to OPA" }
                                return@withLock endOfMethod(code, core)
                            }
                        }
                    }

                    /** Get the resource to overwrite */
                    val oldResource = mmMap[core]!!.getResources(
                        resourceName = resourceName,
                        offset = 0, limit = 1,
                    ).firstOrNull()
                    if (oldResource == null) {
                        logger.warn { "Resource $resourceName was not found" }
                        return@withLock endOfMethod(CODE_006_RESOURCE_NOT_FOUND, core)
                    }

                    // TODO shouldn't we do what we do below only if the enforcement includes cryptography?
                    /**
                     * The RM has to check that:
                     * 1. role-permission assignment's signature is correct;
                     * 2. the resource version number is the latest one;
                     * 3. the role used to write the resource has '*WRITE' permission over the resource;
                     * 4. the specified enforcement is the same as the old one.
                     */

                    logger.debug { "Checking enforcement is the same as old one" }
//                    if (resource.enforcement != oldResource.enforcement) {
//                        logger.warn {
//                            "Specified enforcement (${resource.enforcement}) does " +
//                            "not correspond to the old one (${oldResource.enforcement})"
//                        }
//                        return@withLock endOfMethod(CODE_027_AC_ENFORCEMENT_INCONSISTENT, core)
//                    }

                    logger.debug {
                        "Checking that the resource version number is the latest" +
                        " (i.e., the resource was encrypted with the latest key)"
                    }
                    val newSymKeyVersionNumber = resource.versionNumber
                    val oldSymKeyVersionNumber = oldResource.versionNumber
                    if (newSymKeyVersionNumber != oldSymKeyVersionNumber) {
                        logger.warn {
                            "Specified symmetric key version number ($newSymKeyVersionNumber) " +
                            "does not correspond to the latest one ($oldSymKeyVersionNumber)"
                        }
                        return@withLock endOfMethod(CODE_017_INVALID_VERSION_NUMBER, core)
                    }

                    logger.debug { "Checking encryption and decryption resource version numbers are the same" }
                    if (newSymKeyVersionNumber != newSymKeyVersionNumber) {
                        logger.warn {
                            "Encryption version number ($newSymKeyVersionNumber) is not " +
                            "the same as decryption version number ($newSymKeyVersionNumber)"
                        }
                        return@withLock endOfMethod(CODE_017_INVALID_VERSION_NUMBER, core)
                    }

                    logger.debug { "Checking that the role has WRITE permission over the resource" }
                    val rolePermission = mmMap[core]!!.getRolesPermissions(
                        resourceName = resourceName,
                        roleName = roleName
                    ).firstOrNull {
                        it.operation == Operation.READWRITE
                    }

                    /** Return a 404 instead of a 403 to avoid information leakage */
                    if (rolePermission == null) {
                        logger.warn { "The role $roleName does not have WRITE permission over  $resourceName" }
                        return@withLock endOfMethod(CODE_006_RESOURCE_NOT_FOUND, core)
                    }

                    if (rolePermission.roleName != roleName) {
                        logger.warn { "Given role token does not match across role-permission assignments" }
                        return@withLock endOfMethod(CODE_026_TUPLE_FORMAT, core)
                    }

                    logger.debug {
                        "Retrieving the role-permission assignment signer's " +
                        "public key to check the digital signature"
                    }
                    val adminAsymSigPubKeyBytes = mmMap[core]!!.getPublicKey(
                        token = rolePermission.signer,
                        elementType = RBACElementType.USER,
                        asymKeyType = AsymKeysType.SIG,
                    )
                    if (adminAsymSigPubKeyBytes == null) {
                        logger.warn {
                            "Signer user for role-permission assignment " +
                            "not found (token is $rolePermission.signer)"
                        }
                        return@withLock endOfMethod(CODE_004_USER_NOT_FOUND, core)
                    }


                    val adminAsymSigPubKey = cryptoPKEMap[core]!!.recreateAsymPublicKey(
                        asymPublicKeyBytes = adminAsymSigPubKeyBytes,
                        type = AsymKeysType.SIG,
                    )

                    logger.debug { "Checking digital signature of the role-permission assignment" }
                    cryptoPKEMap[core]!!.verifySignature(
                        signature = rolePermission.signature!!,
                        bytes = rolePermission.getBytesForSignature(),
                        verifyingKey = adminAsymSigPubKey
                    )

                    logger.debug { "Replace the resource in the MM" }
                    code = mmMap[core]!!.updateResourceVersionNumber(resource)
                    if (code != CODE_000_SUCCESS) {
                        logger.warn { "Error while updating the resource version numbers ($code)" }
                        return@withLock endOfMethod(code, core)
                    }

                    logger.debug { "Ask the DM to overwrite the resource in the download folder" }
                    code = dmMap[core]!!.writeResource(
                        updatedResource = Resource(
                            name = resourceName,
                            // enforcement = resource.enforcement
                        ),
                        resourceContent = byteArrayOf().inputStream()
                    )

                    if (code == CODE_000_SUCCESS) {
                        logger.info {
                            "Write request for resource with name " +
                            "$resourceName for core $core was completed"
                        }
                    } else {
                        logger.warn { "DM returned error code ($code)" }
                    }
                    return@withLock endOfMethod(code, core)
                }

                when (returnCode) {
                    CODE_000_SUCCESS -> ok(call)
                    CODE_004_USER_NOT_FOUND -> return@patch ResponseRoutes.notFound(
                        call = call,
                        message = "Signer user not found",
                        code = returnCode
                    )
                    CODE_006_RESOURCE_NOT_FOUND -> return@patch ResponseRoutes.notFound(
                        call = call,
                        message = "Resource user not found",
                        code = returnCode
                    )
                    CODE_017_INVALID_VERSION_NUMBER -> return@patch unprocessableEntity(
                        call = call,
                        message = "Version numbers do not correspond to the previous ones",
                        code = returnCode
                    )
                    CODE_026_TUPLE_FORMAT -> return@patch unprocessableEntity(
                        call = call,
                        message = "Given role token does not match across role-permission assignments",
                        code = returnCode
                    )
                    CODE_027_AC_ENFORCEMENT_INCONSISTENT -> return@patch unprocessableEntity(
                        call = call,
                        message = "Specified enforcement does not correspond to the previous one",
                        code = returnCode
                    )
                    else -> return@patch internalError(
                        call = call,
                        message = "Error during write resource operation ($returnCode)",
                        code = returnCode
                    )
                }
            }
        }
    }
}

/**
 * Routes related to configuration:
 *   - configure (post).
 */
fun Route.configureRouting() {

    /** Configure the RM */
    route(RM) {

        post("{${SERVER.CORE}}/") {

            // TODO authenticate user (login as in the CryptoacMap[core]?)

            // TODO logging

            // TODO check that received parameters are ok, e.g., OPA server is
            //  running at the specified url and port. etc. if not, return an error code:
            //  OutcomeCode.CODE_018_INTERFACE_CONFIGURATION_PARAMETERS

            val coreParam = call.parameters[SERVER.CORE]
                ?: return@post unprocessableEntity(
                    call,
                    "Missing ${SERVER.CORE} parameter",
                    CODE_019_MISSING_PARAMETERS
                )
            val core: CoreType = CoreType.valueOf(coreParam)

            /** Get the storage parameters from the request */
            val parameters: RMRBACCryptoACParameters = call.receive()

            mmMap[core] = MMFactory.getMM(parameters.mmServiceParameters) as MMServiceCACRBAC
            dmMap[core] = DMFactory.getDM(parameters.dmServiceCryptoACParameters) as DMServiceCryptoAC
            acMap[core] = parameters.acServiceParameters?.let { it1 -> ACFactory.getAC(it1) } as ACServiceRBACOPA?
            cryptoPKEMap[core] = CryptoPKEFactory.getCrypto(parameters.crypto)

            var code = startOfMethod(core)
            if (code != CODE_000_SUCCESS) {
                return@post internalError(
                    call = call,
                    message = "Error during locking of interfaces ($code)",
                    code = code
                )
            }
            mmMap[core]!!.init()
            dmMap[core]!!.init()
            acMap[core]?.init()
            cryptoPKEMap[core]!!.init()

            code = endOfMethod(code, core)
            if (code != CODE_000_SUCCESS) {
                return@post internalError(
                    call = call,
                    message = "Error during unlocking of interfaces ($code)",
                    code = code
                )
            }

            ok(call)
        }
    }
}

/** Register the routes for the RM */
fun Application.registerRMRoutes() {
    logger.info { "Registering RM routes" }
    routing {
        resourcesRouting()
        configureRouting()
    }
}



/** Lock the service and return the outcome code */
private fun startOfMethod(core: CoreType): OutcomeCode {
    val mmLockCode = mmMap[core]!!.lock()
    return if (mmLockCode == CODE_000_SUCCESS) {
        val opaLockCode = acMap[core]?.lock()
            ?: CODE_000_SUCCESS
        if (opaLockCode == CODE_000_SUCCESS) {
            val dmLockCode = dmMap[core]!!.lock()
            if (dmLockCode == CODE_000_SUCCESS) {
                CODE_000_SUCCESS
            } else {
                logger.warn { "DM lock failed, code is $dmLockCode" }
                unlockOrRollbackService(ServiceType.AC, core)
                unlockOrRollbackService(ServiceType.MM, core)
                dmLockCode
            }
        } else {
            logger.warn { "AC lock failed, code is $opaLockCode" }
            unlockOrRollbackService(ServiceType.MM, core)
            opaLockCode
        }
    } else {
        logger.warn { "MM lock failed, code is $mmLockCode" }
        mmLockCode
    }
}

/**
 * If the [code] is a success, unlock the service
 * (i.e., commit the changes). Otherwise, rollback
 * to the previous status. In both cases, return
 * the outcome code
 */
private fun endOfMethod(code: OutcomeCode, core: CoreType): OutcomeCode {
    if (code == CODE_000_SUCCESS) {
        unlockOrRollbackService(ServiceType.MM, core)
        unlockOrRollbackService(ServiceType.AC, core)
        unlockOrRollbackService(ServiceType.DM, core)
    } else {
        unlockOrRollbackService(ServiceType.MM, core, true)
        unlockOrRollbackService(ServiceType.AC, core, true)
        unlockOrRollbackService(ServiceType.DM, core, true)
    }
    return code
}

/**
 * Unlock or rollback the specified [serviceType],
 * depending on the [rollback] flag
 */
private fun unlockOrRollbackService(serviceType: ServiceType, core: CoreType, rollback: Boolean = false) {
    val code = when (serviceType) {
        ServiceType.MM -> if (rollback) mmMap[core]!!.rollback() else mmMap[core]!!.unlock()
        ServiceType.DM -> if (rollback) dmMap[core]!!.rollback() else dmMap[core]!!.unlock()
        ServiceType.AC -> if (rollback) acMap[core]?.rollback()
                ?: CODE_000_SUCCESS
            else acMap[core]?.unlock()
                ?: CODE_000_SUCCESS
        ServiceType.RM -> if (rollback) rmMap[core]?.rollback()
            ?: CODE_000_SUCCESS
        else rmMap[core]?.unlock()
            ?: CODE_000_SUCCESS

    }
    if (code != CODE_000_SUCCESS) {
        val message = ("$serviceType lock was fine but "
            + (if (rollback) "rollback" else "unlock")
            + " failed, code is $code")
        logger.error { message }
        throw IllegalStateException(message)
    }
}
