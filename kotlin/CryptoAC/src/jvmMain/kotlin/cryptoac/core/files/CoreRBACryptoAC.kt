//package cryptoac.core.files
//
//import cryptoac.OutcomeCode
//import cryptoac.crypto.*
//import cryptoac.ac.*
//import cryptoac.code.*
//import cryptoac.core.CoreParametersRBAC
//import cryptoac.core.CoreCACRBACTuples
//import cryptoac.dm.DMServiceRBAC
//import cryptoac.mm.MMServiceCACRBAC
//import cryptoac.rm.RMServiceRBAC
//import cryptoac.tuple.Enforcement
//import cryptoac.tuple.Operation
//import cryptoac.tuple.User
//import mu.KotlinLogging
//import java.io.InputStream
//
//private val logger = KotlinLogging.logger {}
//
//// TODO implementa la possibilità di dare WRITE permissions
////  only anche nel cloud? Però attenti che il CSP può colludere
//
///**
// * The CoreCACRBACCryptoAC implements a role-based CAC scheme
// * with hybrid cryptography for the base CryptoAC scenario.
// * It requires an MM, an RM and a DM service, while AC is optional
// * It receives the [coreParameters] and uses the [cryptoPKE] and
// * [cryptoSKE] objects to perform cryptographic computations
// */
//class CoreCACRBACryptoAC(
//    override val cryptoPKE: CryptoPKE,
//    override val cryptoSKE: CryptoSKE,
//    override val coreParameters: CoreParametersRBAC
//) : CoreCACRBACTuples(cryptoPKE, cryptoSKE, coreParameters) {
//
//
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
//
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
//    override fun getUsers(): CodeUsers {
//        TODO("Not yet implemented")
//    }
//
//
//
//
//
//
//    //
////    /** The MM service */
////    override val mm: MMServiceCACRBAC = MMFactory.getMM(
////        mmParameters = coreParameters.mmServiceParameters
////    ) as MMServiceCACRBAC
////
////    /** The DM service */
////    override val dm: DMServiceCryptoAC = DMFactory.getDM(
////        dmParameters = coreParameters.dmServiceParameters
////    ) as DMServiceCryptoAC
////
////    /** The RM service */
////    override val rm: RMServiceRBAC = RMFactory.getRM(
////        rmParameters = coreParameters.rmServiceParameters!!
////    ) as RMServiceRBAC
////
////    /** The AC service */
////    override val ac: ACServiceRBAC? = coreParameters.acServiceParameters?.let {
////        ACFactory.getAC(acParameters = it)
////    } as ACServiceRBAC?
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
////
////
////    /**
////     * Add a new resource with the given name [resourceName],
////     * [resourceContent], [enforcement] type to the policy.
////     * Encrypt, sign and upload the new [resourceContent]
////     * for the resource [resourceName]. Also, assign all
////     * permissions to the admin over the resource. Finally,
////     * return the outcome code
////     *
////     * In this implementation, the user cannot upload
////     * the resource by herself, but she has instead to ask
////     * the RM
////     */
////    // TODO implementa per il CoreCACRBAC il versioning (i.e., senza RM)
////    override fun addResource(
////        resourceName: String,
////        resourceContent: InputStream,
////        enforcement: Enforcement
////    ): OutcomeCode {
////        logger.info { "Adding resource $resourceName with enforcement $enforcement" }
////
////        /** Guard clauses */
////        if (resourceName.isBlank()) {
////            logger.warn { "Resource name is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod(acLock = false)
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /**
////         * Based on the [enforcement], encrypt or
////         * not the [resourceContent] of the [resourceName]
////         */
////        val encryptedResourceContent: InputStream
////        val encryptedSymKey: EncryptedSymKey
////        when (enforcement) {
////            Enforcement.TRADITIONAL -> {
////                encryptedResourceContent = resourceContent
////                encryptedSymKey = EncryptedSymKey("null".toByteArray())
////            }
////            Enforcement.COMBINED -> {
////                val symKey = cryptoSKE.generateSymKey()
////
////                /** Encrypt the [resourceContent] of the [resourceName] */
////                encryptedResourceContent = cryptoSKE.encryptStream(
////                    encryptingKey = symKey,
////                    stream = resourceContent
////                )
////
////                /**
////                 * Encrypt the symmetric key with the
////                 * admin's asymmetric encrypting public key
////                 */
////                val adminAsymEncPublicKeyBytes = mm.getPublicKey(
////                    token = ADMIN,
////                    elementType = RBACElementType.USER,
////                    asymKeyType = AsymKeysType.ENC,
////                )
////                val adminAsymEncPublicKey = cryptoPKE.recreateAsymPublicKey(
////                    asymPublicKeyBytes = adminAsymEncPublicKeyBytes!!,
////                    type = AsymKeysType.ENC
////                )
////                encryptedSymKey = cryptoPKE.encryptSymKey(
////                    encryptingKey = adminAsymEncPublicKey,
////                    symKey = symKey
////                )
////            }
////        }
////        val newResource = Resource(
////            name = resourceName,
////            enforcement = enforcement
////        )
////
////        /** Give read and write permission to the admin */
////        val adminRolePermission = RolePermission(
////            roleName = ADMIN,
////            resourceName = resourceName,
////            roleToken = ADMIN,
////            resourceToken = newResource.token,
////            permission = Operation.READWRITE,
////            encryptingSymKey = encryptedSymKey,
////            decryptingSymKey = encryptedSymKey,
////        )
////        val rolePermissionSignature = cryptoPKE.createSignature(
////            bytes = adminRolePermission.getBytesForSignature(),
////            signingKey = asymSigKeyPair.private
////        )
////        adminRolePermission.updateSignature(
////            newSignature = rolePermissionSignature,
////            newSigner = user.token,
////        )
////
////        /**
////         * Below, do not add the resource as it is, but
////         * instead invoke the RM to validate the operation
////         */
////
////        /** Add the resource in the DM */
////        code = dm.addResource(
////            newResource = newResource,
////            content = encryptedResourceContent
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                acLocked = false
////            )
////        }
////
////        /** Ask the RM to add the resource */
////        code = rm.checkAddResource(
////            newResource = newResource,
////            adminRolePermission = adminRolePermission
////        )
////        return if (code != CODE_000_SUCCESS) {
////            /** TODO can a user invoke this function? The RM should do it */
////            val deleteTempResourceCode = dm.deleteTemporaryResource(newResource.name)
////            if (deleteTempResourceCode != CODE_000_SUCCESS) {
////                logger.error {
////                    "Added resource in the DM, the RM could not check it and " +
////                    "we were not able to delete the (temporary) resource from" +
////                    " the DM; contact the system administrator"
////                }
////                endOfMethod(
////                    code = CODE_058_INCONSISTENT_STATUS_DELETE_TEMPORARY_RESOURCE_IN_DM,
////                    acLocked = false
////                )
////            } else
////                endOfMethod(
////                    code = code,
////                    acLocked = false
////                )
////        } else {
////            endOfMethod(
////                code = code,
////                acLocked = false
////            )
////        }
////    }
////
////
////    /**
////     * Encrypt, sign and upload the new [resourceContent]
////     * for the resource [resourceName]. Finally, return
////     * the outcome code
////     *
////     * In this implementation, the user cannot overwrite
////     * the resource by herself, but she has instead to
////     * upload the resource in the DM and then ask the RM
////     * to validate the operation
////     */
////    override fun writeResource(
////        resourceName: String,
////        resourceContent: InputStream
////    ): OutcomeCode {
////        logger.info { "Writing resource $resourceName by user ${user.name}" }
////
////        /** Guard clauses */
////        if (resourceName.isBlank()) {
////            logger.warn { "Resource name is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod(acLock = false)
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        val resourceObject = mm.getResources(
////            resourceName = resourceName,
////            isAdmin = user.isAdmin,
////            offset = 0,
////            limit = 1
////        ).firstOrNull()
////        if (resourceObject == null) {
////            logger.warn {
////                "Resource not found. Either user ${user.name} does not have " +
////                "access to resource $resourceName or resource does not exist"
////            }
////            return endOfMethod(
////                code = CODE_006_RESOURCE_NOT_FOUND,
////                acLocked = false
////            )
////        }
////
////        val resourceStream: InputStream
////        val latestResourceVersionNumber = resourceObject.symEncKeyVersionNumber
////        val roleAssumed: String
////
////        when (resourceObject.enforcement) {
////            Enforcement.COMBINED -> {
////                val symKeyCode = getEncSymmetricKey(resourceObject)
////                if (symKeyCode.code != CODE_000_SUCCESS) {
////                    return endOfMethod(
////                        code = symKeyCode.code,
////                        acLocked = false
////                    )
////                }
////
////                roleAssumed = symKeyCode.role!!
////                resourceStream = cryptoSKE.encryptStream(
////                    encryptingKey = symKeyCode.key!!,
////                    stream = resourceContent
////                )
////            }
////            Enforcement.TRADITIONAL -> {
////                roleAssumed = TODO()
////                resourceStream = resourceContent
////            }
////        }
////
////        val newResource = Resource(
////            name = resourceName,
////            symDecKeyVersionNumber = latestResourceVersionNumber,
////            symEncKeyVersionNumber = latestResourceVersionNumber,
////            enforcement = resourceObject.enforcement
////        )
////        newResource.token = resourceObject.token
////
////        code = dm.addResource(
////            newResource = newResource,
////            content = resourceStream
////        )
////        return if (code != CODE_000_SUCCESS) {
////            endOfMethod(
////                code = code,
////                acLocked = false
////            )
////        } else {
////            code = rm.checkWriteResource(
////                roleName = roleAssumed,
////                symEncKeyVersionNumber = newResource.symEncKeyVersionNumber,
////                newResource = newResource,
////            )
////            if (code != CODE_000_SUCCESS) {
////                /** TODO can a user invoke this function? The RM should do it */
////                val deleteTempResourceCode = dm.deleteTemporaryResource(newResource.name)
////                if (deleteTempResourceCode != CODE_000_SUCCESS) {
////                    logger.error {
////                        "Added resource in the DM, the RM could not check it and " +
////                        "we were not able to delete the (temporary) resource from" +
////                        " the DM; contact the system administrator"
////                    }
////                    endOfMethod(
////                        code = CODE_058_INCONSISTENT_STATUS_DELETE_TEMPORARY_RESOURCE_IN_DM,
////                        acLocked = false
////                    )
////                } else {
////                    endOfMethod(
////                        code = code,
////                        acLocked = false
////                    )
////                }
////            } else {
////                endOfMethod(
////                    code = code,
////                    acLocked = false
////                )
////            }
////        }
////    }
//}
