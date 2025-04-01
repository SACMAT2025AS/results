//package cryptoac.core
//
//import cryptoac.crypto.*
////import it.stefanoberlato.oabe.OpenABECryptoContextDecrypt
//import mu.KotlinLogging
//
//private val logger = KotlinLogging.logger {}
//
//// TODO DOC here and also somewhere else (latex tesi Colombo),
////  you have to explain how to append VNs to attributes somewhere
//
//// TODO metti gli pseudonimi nelle access structures e nelle chiavi degli utenti
////  (=> modifica anche lo pseudocodice)
//
///**
// * The CoreCACABACTuples extends the [CoreCACABAC] class by implementing
// * the basic functions for an attribute-based CAC scheme based on tuples.
// * It receives the [coreParameters] and uses the [cryptoPKE] and
// * [cryptoSKE] objects to perform cryptographic computations,
// * and the [cryptoABE] for ABE cryptographic computations
// */
//abstract class CoreCACABACTuples(
//    override val cryptoPKE: CryptoPKE,
//    override val cryptoSKE: CryptoSKE,
//    override val cryptoABE: CryptoABE,
//    override val coreParameters: CoreParametersABAC,
//) : CoreCACABAC(
//    cryptoPKE,
//    cryptoSKE,
//    cryptoABE,
//    coreParameters
//) {
////
////    abstract override val mm: MMServiceCACABAC
////
////    /** Asymmetric encryption key pair */
////    protected abstract val asymEncKeyPair: KeyPairCryptoAC
////
////    /** Asymmetric signature key pair */
////    protected abstract val asymSigKeyPair: KeyPairCryptoAC
////
////    /** In this implementation, also deinit the [cryptoABE] */
////    override fun deinitCore() {
////        super.deinitCore()
////        cryptoABE.deinit()
////    }
////
////    /**
////     * In this implementation, add the ABE master public
////     * key and the admin's (encrypting and verifying) public
////     * keys in the metadata. Also, create an attribute [ADMIN]
////     * that gives access to any ABE-encrypted ciphertext. This
////     * is especially useful when the admin has to perform
////     * administrative operations (such as, e.g., updating access
////     * control tuples) and needs to decrypt old symmetric keys.
////     * Then, add the admin user in the AC and assign the admin
////     * to the admin attribute in the MM and AC. Finally, generate
////     * the admin's ABE secret key and return the outcome code
////     */
////    override fun configureServices(): OutcomeCode {
////
////        logger.info { "Initializing admin's keys for admin ${user.name}" }
////
////        /** Lock the status of the services */
////        var code = startOfMethod()
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /** Configure services (e.g., create tables in database) */
////        code = mm.configure()
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////        code = rm?.configure(coreParameters)
////            ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////        code = dm.configure(coreParameters)
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////        code = ac?.configure()
////            ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Set the MPK in the metadata */
////        val masterPublicKey = cryptoABE.exportABEPublicParams()
////        code = mm.setMPK(mpk = masterPublicKey)
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Add the admin in the ac */
////        code = ac?.addAdmin(newUserAdmin = user)
////            ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(if (code == CODE_035_ADMIN_ALREADY_INITIALIZED) {
////                logger.warn { "Code was $code, replacing with $CODE_077_SERVICE_ALREADY_CONFIGURED" }
////                CODE_077_SERVICE_ALREADY_CONFIGURED
////            } else {
////                code
////            })
////        }
////
////        /** Add the admin attribute in the AC */
////        code = ac?.addAttribute(attributeName = user.name)
////            ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Add the admin attribute assignment in the AC */
////        code = ac?.assignUserToAttributes(
////            username = user.name,
////            attributes = hashSetOf(user.name)
////        ) ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Add the admin in the MM */
////        code = mm.addAdmin(newUserAdmin = user)
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(if (code == CODE_035_ADMIN_ALREADY_INITIALIZED) {
////                logger.warn { "Code was $code, replacing with $CODE_077_SERVICE_ALREADY_CONFIGURED" }
////                CODE_077_SERVICE_ALREADY_CONFIGURED
////            } else {
////                code
////            })
////        }
////
////        /** Add the admin attribute in the MM */
////        code = mm.addAttribute(
////            newAttribute = Attribute(
////                name = user.name
////            ),
////            restoreIfDeleted = false
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Add the admin attribute assignment in the MM */
////        val adminUserAttribute = UserAttribute(
////            username = user.name,
////            attributeName = user.name,
////        )
////        val signature = cryptoPKE.createSignature(
////            bytes = adminUserAttribute.getBytesForSignature(),
////            signingKey = asymSigKeyPair.private
////        )
////        adminUserAttribute.updateSignature(
////            newSignature = signature,
////            newSigner = ADMIN,
////        )
////        code = mm.addUsersAttributes(
////            newUsersAttributes = hashSetOf(
////                adminUserAttribute
////            )
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Add the admin in the DM */
////        code = dm.addAdmin(newUserAdmin = user)
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Add the admin in the RM */
////        code = rm?.addAdmin(newUserAdmin = user)
////            ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Now generate the admin's ABE secret key */
////        val attributesAssignedToAdmin = hashSetOf(concatenateAttributeAndValue(
////            attribute = concatenateAttributeNameAndVersionNumber(
////                name = user.name,
////                versionNumber = 1
////            ),
////            value = null
////        ))
////
////        val newABEAdminKey = cryptoABE.generateABEPrivateKey(
////            attributes = concatenateAttributes(attributesAssignedToAdmin),
////            keyID = user.name
////        )
////
////        val encryptedNewABEAdminKey = cryptoPKE.asymEncrypt(
////            encryptingKey = cryptoPKE.recreateAsymPublicKey(
////                asymPublicKeyBytes = user.asymEncKeys!!.public.decodeBase64(),
////                type = AsymKeysType.ENC
////            ),
////            bytes = newABEAdminKey.encoded
////        )
////
////        return endOfMethod(
////            code = mm.updateUserABEKey(
////                username = user.name,
////                newEncryptedUserABEKey = encryptedNewABEAdminKey
////            ),
////        )
////    }
////
////    override fun addUser(
////        username: String
////    ): CodeCoreParameters {
////        logger.info { "Adding user $username" }
////
////        /** Guard clauses */
////        if (username.isBlank()) {
////            logger.warn { "Username is blank" }
////            return CodeCoreParameters(CODE_020_INVALID_PARAMETER)
////        }
////
////        /** Lock the status of the services */
////        val lockCode = startOfMethod()
////        if (lockCode != CODE_000_SUCCESS) {
////            return CodeCoreParameters(lockCode)
////        }
////
////        /** Add the user in the MM */
////        val userToAdd = User(username)
////        val addMMResult = mm.addUser(
////            newUser = userToAdd
////        )
////        if (addMMResult.code != CODE_000_SUCCESS) {
////            return CodeCoreParameters(endOfMethod(
////                code = addMMResult.code
////            ))
////        }
////
////        /** Add the user in the RM */
////        val addRMResult = rm?.addUser(
////            newUser = userToAdd
////        )
////        if (addRMResult != null) {
////            if (addRMResult.code != CODE_000_SUCCESS) {
////                return CodeCoreParameters(endOfMethod(
////                    code = addRMResult.code
////                ))
////            }
////        }
////
////        /** Add the user in the DM */
////        val addDMResult = dm.addUser(
////            newUser = userToAdd
////        )
////        if (addDMResult.code != CODE_000_SUCCESS) {
////            return CodeCoreParameters(endOfMethod(
////                code = addDMResult.code
////            ))
////        }
////
////        /** Add the user in the AC */
////        val addACResult = ac?.addUser(
////            newUser = userToAdd
////        )
////        if (addACResult != null) {
////            if (addACResult.code != CODE_000_SUCCESS) {
////                return CodeCoreParameters(endOfMethod(
////                    code = addACResult.code
////                ))
////            }
////        }
////
////        return CodeCoreParameters(
////            code = endOfMethod(
////                code = CODE_000_SUCCESS
////            ),
////            coreParameters = CoreParametersABAC(
////                user = User(username),
////                coreType = coreParameters.coreType,
////                cryptoType = coreParameters.cryptoType,
////                cryptoABEType = coreParameters.cryptoABEType,
////                abePublicParameters = cryptoABE.exportABEPublicParams(),
////                mmServiceParameters = addMMResult.serviceParameters as MMServiceParameters,
////                rmServiceParameters = addRMResult?.serviceParameters as RMServiceParameters?,
////                dmServiceParameters = addDMResult.serviceParameters as DMServiceParameters,
////                acServiceParameters = addACResult?.serviceParameters as ACServiceParameters?,
////            )
////        )
////    }
////
////    override fun deleteUser(
////        username: String
////    ): OutcomeCode {
////        logger.info { "Deleting user $username" }
////
////        /** Guard clauses */
////        if (username.isBlank()) {
////            logger.warn { "Username is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////        if (username == ADMIN) {
////            logger.warn { "Cannot delete the $ADMIN user" }
////            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod()
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /** Get all attributes assigned to the user */
////        val usersAttributes = mm.getUsersAttributes(
////            username = username,
////            isAdmin = user.isAdmin
////        )
////        if (usersAttributes.isNotEmpty()) {
////            val attributes = usersAttributes.map {
////                verifyTupleSignature(it)
////                it.attributeName
////            }.toHashSet()
////
////            /** Revoke all attributes from the user */
////            code = revokeAttributesFromUser(
////                username = username,
////                attributes = attributes
////            )
////            if (code != CODE_000_SUCCESS) {
////                return endOfMethod(
////                    code = code
////                )
////            }
////        }
////
////        /** Delete the user from the MM */
////        code = mm.deleteUser(username)
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code
////            )
////        }
////
////        /** Delete the user from the RM */
////        code = rm?.deleteUser(username)
////            ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code
////            )
////        }
////
////        /** Delete the user from the DM */
////        code = dm.deleteUser(username)
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code
////            )
////        }
////
////        /** Delete the user from the AC */
////        code = ac?.deleteUser(username)
////            ?: CODE_000_SUCCESS
////        return endOfMethod(
////            code = code
////        )
////    }
////
////
////
////    override fun addAttribute(
////        attributeName: String,
////    ): OutcomeCode{
////        logger.info { "Adding attribute $attributeName" }
////
////        /** Guard clauses */
////        if (attributeName.isBlank()) {
////            logger.warn { "Attribute name is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod(dmLock = false)
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /**
////         * Check whether the attribute was
////         * previously deleted or is a new one
////         */
////        val deletedAttribute = mm.getAttributes(
////            attributeName = attributeName
////        ).firstOrNull()
////        val isAttributeANewOne = (deletedAttribute == null)
////        val attributeVersionNumber: Int = if (isAttributeANewOne) {
////            logger.debug { "The attribute $attributeName is a new one" }
////            1
////        } else {
////            when (deletedAttribute!!.status) {
////                ElementStatus.DELETED -> {
////                    logger.warn { "The attribute $attributeName was deleted, restoring it" }
////                    deletedAttribute.versionNumber + 1
////                }
////                ElementStatus.OPERATIONAL -> {
////                    logger.info { "The attribute $attributeName already exists and it is operational" }
////                    return endOfMethod(
////                        code = CODE_065_ATTRIBUTE_ALREADY_EXISTS,
////                        dmLocked = false
////                    )
////                }
////                ElementStatus.INCOMPLETE -> {
////                    val message = "Attributes cannot be incomplete"
////                    logger.error { message }
////                    throw IllegalStateException(message)
////                }
////            }
////        }
////
////        /** Create the new Attribute object for the [attributeName] */
////        val newTupleAttribute = Attribute(
////            name = attributeName,
////            versionNumber = attributeVersionNumber
////        )
////
////        /** Add (or restore) the attribute in the MM */
////        code = mm.addAttribute(
////            newAttribute = newTupleAttribute,
////            restoreIfDeleted = !isAttributeANewOne
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        /** Add the attribute to the AC */
////        code = ac?.addAttribute(attributeName)
////            ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        /** Assign the attribute to the admin */
////        return endOfMethod(
////            code = assignUserToAttributes(
////                username = user.name,
////                attributeName = attributeName,
////            ),
////            dmLocked = false
////        )
////    }
////
////    override fun deleteAttributes(
////        attribute: String
////    ): OutcomeCode {
////        return deleteAttributes(hashSetOf(attribute))
////    }
////
////    override fun deleteAttributes(
////        attributes: HashSet<String>
////    ): OutcomeCode {
////        logger.info { "Deleting the following attributes (one per row below):" }
////        attributes.forEachIndexed { index, attribute ->
////            logger.info { "${index + 1}: attribute $attribute" }
////        }
////
////        /** Guard clauses */
////        if (attributes.isEmpty()) {
////            logger.warn { "Set of attributes to delete is empty" }
////            return CODE_020_INVALID_PARAMETER
////        }
////        attributes.forEach {
////            if (it.isBlank()) {
////                logger.warn { "Attribute name is blank" }
////                return CODE_020_INVALID_PARAMETER
////            }
////            if (it == ADMIN) { // TODO test that this behaviour works properly
////                logger.warn { "Cannot delete the $ADMIN attribute" }
////                return CODE_022_ADMIN_CANNOT_BE_MODIFIED
////            }
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod(dmLock = false)
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /**
////         * For each attribute, get the attribute
////         * version number (we will need the version
////         * number when checking for the presence of
////         * an attribute in an access structure).
////         * Then, get all users that are assigned to
////         * that attribute (these users are those for
////         * which we will need to update the ABE secret
////         * key), then delete all related users' attribute
////         * tuples
////         */
////        val attributesWithVersionNumberCode = getAttributeVersionNumbers(
////            attributes = attributes,
////        )
////        code = attributesWithVersionNumberCode.code
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        val usersToRenewKey = hashSetOf<String>()
////        val attributesWithVersionNumberString = hashSetOf<String>()
////        attributesWithVersionNumberCode.attributes!!.forEach { attribute ->
////            val attributeName = attribute.name
////            attributesWithVersionNumberString.add(concatenateAttributeNameAndVersionNumber(
////                name = attributeName,
////                versionNumber = attribute.versionNumber
////            ))
////
////            logger.debug { "Getting users assigned to attribute $attributeName" }
////            mm.getUsersAttributes(
////                attributeName = attributeName
////            ).forEach { tupleUserAttribute ->
////                verifyTupleSignature(tupleUserAttribute)
////                usersToRenewKey.add(tupleUserAttribute.username)
////            }
////
////            logger.debug { "Delete tuples of attribute $attributeName" }
////            code = mm.deleteUsersAttributes(
////                attributeName = attributeName
////            )
////            if (code != CODE_000_SUCCESS) {
////                return@forEach
////            }
////
////            logger.debug { "Delete attribute $attributeName" }
////            code = mm.deleteAttribute(
////                attributeName = attributeName
////            )
////            if (code != CODE_000_SUCCESS) {
////                return@forEach
////            }
////
////            /** Delete the attribute from the AC */
////            code = ac?.deleteAttribute(
////                attributeName = attributeName
////            ) ?: CODE_000_SUCCESS
////            if (code != CODE_000_SUCCESS) {
////                return@forEach
////            }
////        }
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        logger.info { "Renewing ABE key of ${usersToRenewKey.size} users" }
////        usersToRenewKey.forEach { username ->
////            logger.debug { "Renewing ABE key of user $username" }
////            code = generateUserABEKey(username)
////            if (code != CODE_000_SUCCESS) {
////                return@forEach
////            }
////        }
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////
////        /**
////         * Get all access structures tuples from which
////         * we will remove the deleted attributes. If
////         * an access structure gets modified, simply
////         * invoke the updateAccessStructure function.
////         * We group access structure tuples that are
////         * related to the same resource since, if even
////         * a single access structure tuple of a resource
////         * is updated, then we need to update all access
////         * structure tuples of that resource (e.g., to
////         * update the resource token across all tuples)
////         */
////        val accessStructuresPermissions = mm.getAccessStructuresPermissions()
////        val tuplesAccessStructurePermissionByResource = hashMapOf<String, HashSet<AccessStructurePermission>>()
////        accessStructuresPermissions.forEach { accessStructurePermission ->
////            verifyTupleSignature(accessStructurePermission)
////            val resourceName = accessStructurePermission.resourceName
////            tuplesAccessStructurePermissionByResource.getOrPut(resourceName) { hashSetOf() }.add(accessStructurePermission)
////        }
////
////        tuplesAccessStructurePermissionByResource.forEach { (resourceName, accessStructuresPermissions) ->
////            var needToUpdateResource = false
////            accessStructuresPermissions.forEach { accessStructurePermission ->
////                val currentAccessStructure = AccessStructure.parseAccessStructure(
////                    policyTree = accessStructurePermission.accessStructure
////                )
////                if (currentAccessStructure.containsAtLeastOne(attributesWithVersionNumberString)) {
////                    val oldCurrentAccessStructureString = currentAccessStructure.toString()
////                    currentAccessStructure.remove(attributesWithVersionNumberString)
////                    accessStructurePermission.accessStructure = currentAccessStructure.toString()
////
////                    if (currentAccessStructure.isEmpty()) {
////                        logger.warn {
////                            "Access structure ($oldCurrentAccessStructureString) " +
////                            "for resource ${accessStructurePermission.resourceName} " +
////                            "would be empty after attributes deletion. The admin " +
////                            "needs to delete the corresponding resource first"
////                        }
////                        return endOfMethod(
////                            code = CODE_064_DELETE_ATTRIBUTES_CAUSES_EMPTY_ACCESS_STRUCTURE,
////                            dmLocked = false
////                        )
////                    } else {
////                        needToUpdateResource = true
////                    }
////                }
////            }
////
////            if (needToUpdateResource) {
////                code = updateResourceAndAccessStructures(
////                    resourceName = resourceName,
////                    accessStructuresPermissionsToUpdate = accessStructuresPermissions,
////                    newAccessStructuresAlreadyEmbedVersionNumbers = true,
////                )
////                if (code != CODE_000_SUCCESS) {
////                    return@forEach
////                }
////            }
////        }
////
////        return endOfMethod(
////            code = code,
////            dmLocked = false
////        )
////    }
////
////    override fun addResource(
////        resourceName: String,
////        resourceContent: InputStream,
////        accessStructure: String,
////        operation: Operation,
////        enforcement: Enforcement
////    ): OutcomeCode {
////        logger.info {
////            "Adding resource $resourceName with enforcement $enforcement"
////        }
////
////        /** Guard clauses */
////        if (resourceName.isBlank()) {
////            logger.warn { "Resource name is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////        if (accessStructure.isBlank()) {
////            logger.warn { "Access structure is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod()
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /**
////         * Embed the admin attribute and the version numbers
////         * of all the attributes in the [accessStructure]
////         */
////        val accessStructuresAndCode = embedAdminAttributeAndVersionNumbers(
////            accessStructures = hashSetOf(accessStructure)
////        )
////        if (accessStructuresAndCode.code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = accessStructuresAndCode.code
////            )
////        }
////        val accessStructureWithVersionNumbers = accessStructuresAndCode.accessStructures!!.first()
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
////                 * Encrypt the symmetric key
////                 * under the access structure
////                 * embedding version numbers
////                 */
////                encryptedSymKey = EncryptedSymKey(
////                    key = cryptoABE.encryptABE(
////                        accessStructure = accessStructureWithVersionNumbers,
////                        plaintext = symKey.secretKey.encoded.encodeBase64()
////                    ).decodeBase64()
////                )
////            }
////        }
////
////        /** Add the resource in the MM */
////        val newResource = Resource(
////            name = resourceName,
////            enforcement = enforcement
////        )
////        code = mm.addResource(
////            newResource = newResource
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Add the access structure tuple in the MM */
////        val accessStructurePermission = AccessStructurePermission(
////            resourceName = resourceName,
////            resourceToken = newResource.token,
////            accessStructure = accessStructureWithVersionNumbers,
////            operation = operation,
////            encryptedSymKey = encryptedSymKey,
////            resourceVersionNumber = 1
////        )
////        val accessStructureSignature = cryptoPKE.createSignature(
////            bytes = accessStructurePermission.getBytesForSignature(),
////            signingKey = asymSigKeyPair.private
////        )
////        accessStructurePermission.updateSignature(
////            newSignature = accessStructureSignature,
////            newSigner = user.token,
////        )
////        code = mm.addAccessStructuresPermissions(
////            newAccessStructuresPermissions = hashSetOf(accessStructurePermission)
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Add the resource in the AC */
////        code = ac?.addResource(
////            resourceName = resourceName
////        ) ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /**
////         * Add the resource-access structure-permission
////         * assignment in the AC
////         */
////        code = ac?.assignPermissionToAccessStructure(
////            resourceName = resourceName,
////            accessStructure = accessStructureWithVersionNumbers,
////            operation = operation,
////        ) ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Add the resource in the DM */
////        return endOfMethod(
////            dm.addResource(
////                newResource = newResource,
////                resourceContent = encryptedResourceContent
////            )
////        )
////    }
////
////    override fun deleteResource(
////        resourceName: String
////    ): OutcomeCode {
////        logger.info { "Deleting resource $resourceName" }
////
////        /** Guard clauses */
////        if (resourceName.isBlank()) {
////            logger.warn { "Resource name is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod()
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /**
////         * Delete the access structure tuples
////         * matching the [resourceName] from the MM
////         */
////        code = mm.deleteAccessStructuresPermissions(
////            resourceName = resourceName
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /**
////         * Delete the resource matching
////         * the [resourceName] from the MM
////         */
////        code = mm.deleteResource(
////            resourceName = resourceName
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Delete the resource from the AC */
////        code = ac?.deleteResource(
////            resourceName = resourceName
////        ) ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(code)
////        }
////
////        /** Delete the resource from the DM */
////        return endOfMethod(dm.deleteResource(
////            resourceName = resourceName,
////            resourceVersionNumber = null
////        ))
////    }
////
////    override fun assignUserToAttributes(
////        username: String,
////        attributeName: String,
////        attributeValue: String?
////    ): OutcomeCode {
////        return assignUserToAttributes(
////            username = username,
////            attributes = hashMapOf(attributeName to attributeValue)
////        )
////    }
////
////    override fun assignUserToAttributes(
////        username: String,
////        attributes: HashMap<String, String?>
////    ): OutcomeCode {
////        logger.info { "Assigning to user $username the following attributes (one per row below):" }
////        var index = 0
////        attributes.forEach { (key, value) ->
////            logger.info { "${index + 1}: attribute $key (value is $value)" }
////            index =+ 1
////        }
////
////        /** Guard clauses */
////        if (username.isBlank()) {
////            logger.warn { "Username is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////        if (attributes.isEmpty()) {
////            logger.warn { "Map of attributes to assign is empty" }
////            return CODE_020_INVALID_PARAMETER
////        }
////        attributes.forEach {
////            if (it.key.isBlank()) {
////                logger.warn { "Attribute name is blank" }
////                return CODE_020_INVALID_PARAMETER
////            }
////            if (it.key == ADMIN) { // TODO test that this behaviour works properly
////                logger.warn { "Cannot assign users to the $ADMIN attribute" }
////                return CODE_022_ADMIN_CANNOT_BE_MODIFIED
////            }
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod(dmLock = false)
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /**
////         * Get the version number of all [attributes]
////         * (which must be operational)
////         */
////        val attributesWithVersionNumberCode = getAttributeVersionNumbers(
////            attributes = attributes.keys.toHashSet(),
////        )
////        code = attributesWithVersionNumberCode.code
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        val setOfUsersAttributesToAdd = hashSetOf<UserAttribute>()
////        attributesWithVersionNumberCode.attributes!!.forEach { attribute ->
////            val currentUserAttribute = UserAttribute(
////                username = username,
////                attributeName = attribute.name,
////                attributeValue = attributes[attribute.name]
////            )
////            val accessStructureSignature = cryptoPKE.createSignature(
////                bytes = currentUserAttribute.getBytesForSignature(),
////                signingKey = asymSigKeyPair.private
////            )
////            currentUserAttribute.updateSignature(
////                newSignature = accessStructureSignature,
////                newSigner = user.token,
////            )
////            setOfUsersAttributesToAdd.add(currentUserAttribute)
////        }
////
////        code = mm.addUsersAttributes(
////            newUsersAttributes = setOfUsersAttributesToAdd
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        /** Update the user-attributes assignments in the AC */
////        code = ac?.assignUserToAttributes(
////            username = username,
////            attributes = attributes
////        ) ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        /** Update the user ABE key */
////        return endOfMethod(
////            code = generateUserABEKey(
////                username = username
////            ),
////            dmLocked = false
////        )
////    }
////
////    override fun revokeAttributesFromUser(
////        username: String,
////        attributeName: String
////    ): OutcomeCode {
////        return revokeAttributesFromUser(
////            username = username,
////            attributes = hashSetOf(attributeName)
////        )
////    }
////
////    override fun revokeAttributesFromUser(
////        username: String,
////        attributes: HashSet<String>
////    ): OutcomeCode {
////        logger.info {
////            "Revoking from user $username the " +
////            "following attributes (one per row below):"
////        }
////        attributes.forEachIndexed { index, attribute ->
////            logger.info { "${index + 1}: attribute $attribute" }
////        }
////
////        /** Guard clauses */
////        if (username.isBlank()) {
////            logger.warn { "Username is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////        if (attributes.isEmpty()) {
////            logger.warn { "Set of attributes to revoke is empty" }
////            return CODE_020_INVALID_PARAMETER
////        }
////        attributes.forEach {
////            if (it.isBlank()) {
////                logger.warn { "Attribute name is blank" }
////                return CODE_020_INVALID_PARAMETER
////            }
////            if (it == ADMIN) { // TODO test that this behaviour works properly
////                logger.warn { "Cannot revoke users from the $ADMIN attribute" }
////                return CODE_022_ADMIN_CANNOT_BE_MODIFIED
////            }
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod(dmLock = false)
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /** Revoke the attributes from the user from AC */
////        code = ac?.revokeUserFromAttributes(
////            username = username,
////            attributes = attributes
////        ) ?: CODE_000_SUCCESS
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        /**
////         * For each attribute, get the attribute
////         * version number. Then, get all users
////         * that are assigned to that attribute
////         * (these users are those for which we
////         * will need to update the ABE secret key),
////         * then delete the [username]' attribute
////         * tuples
////         */
////        logger.debug { "Getting the version numbers of the attributes to revoke" }
////        val attributesWithVersionNumberCode = getAttributeVersionNumbers(
////            attributes = attributes,
////        )
////        code = attributesWithVersionNumberCode.code
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        val usersToRenewKey = hashSetOf<String>()
////        val attributesWithVersionNumberString = hashSetOf<String>()
////        attributesWithVersionNumberCode.attributes!!.forEach { attribute ->
////            val attributeName = attribute.name
////            attributesWithVersionNumberString.add(concatenateAttributeNameAndVersionNumber(
////                name = attributeName,
////                versionNumber = attribute.versionNumber
////            ))
////
////            logger.debug { "Getting users assigned to attribute $attributeName" }
////            var isUserAssignedToCurrentAttribute = false
////            mm.getUsersAttributes(
////                attributeName = attributeName
////            ).forEach { tupleUserAttribute ->
////                verifyTupleSignature(tupleUserAttribute)
////                usersToRenewKey.add(tupleUserAttribute.username)
////                if (tupleUserAttribute.username == username) {
////                    isUserAssignedToCurrentAttribute = true
////                }
////            }
////            /**
////             * We need to ensure that the [username] is
////             * indeed assigned to the current attribute
////             */
////            if (!isUserAssignedToCurrentAttribute) {
////                logger.warn { "User $username is not assigned to attribute $attributeName" }
////                return endOfMethod(
////                    code = CODE_070_USER_ATTRIBUTE_ASSIGNMENT_NOT_FOUND,
////                    dmLocked = false
////                )
////            }
////
////            logger.debug {
////                "Updating the token and the version " +
////                "number of the attribute $attributeName"
////            }
////            val newAttributeToken = Element.generateRandomToken()
////            mm.updateAttributeTokenAndVersionNumber(
////                attributeName = attributeName,
////                oldAttributeToken = attribute.token,
////                newAttributeToken = newAttributeToken,
////                newVersionNumber = attribute.versionNumber + 1
////            )
////
////            logger.debug {
////                "Delete tuples of user $username " +
////                "and attribute $attributeName"
////            }
////            code = mm.deleteUsersAttributes(
////                username = username,
////                attributeName = attributeName
////            )
////            if (code != CODE_000_SUCCESS) {
////                return@forEach
////            }
////        }
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        require(usersToRenewKey.contains(username))
////        logger.info { "Renewing ABE key of ${usersToRenewKey.size} users" }
////        usersToRenewKey.forEach { userToRenewKey ->
////            logger.debug { "Renewing ABE key of user $userToRenewKey" }
////            code = generateUserABEKey(userToRenewKey)
////            if (code != CODE_000_SUCCESS) {
////                return@forEach
////            }
////        }
////
////        /***
////         * Get all access structures tuples in which we
////         * will update the version number of the updated
////         * attributes. If an access structure gets modified,
////         * simply invoke the updateAccessStructure function.
////         * We group access structure tuples that are
////         * related to the same resource since, if even
////         * a single access structure tuple of a resource
////         * is updated, then we need to update all access
////         * structure tuples of that resource (e.g., to
////         * update the resource token across all tuples)
////         */
////        val accessStructuresPermissions = mm.getAccessStructuresPermissions()
////        val tuplesAccessStructurePermissionByResource = hashMapOf<String, HashSet<AccessStructurePermission>>()
////        accessStructuresPermissions.forEach { accessStructurePermission ->
////            verifyTupleSignature(accessStructurePermission)
////            val resourceName = accessStructurePermission.resourceName
////            tuplesAccessStructurePermissionByResource.getOrPut(resourceName) { hashSetOf() }.add(accessStructurePermission)
////        }
////
////        tuplesAccessStructurePermissionByResource.forEach { (resourceName, accessStructuresPermissions) ->
////            var needToUpdateResource = false
////            accessStructuresPermissions.forEach { accessStructurePermission ->
////                val currentAccessStructure = AccessStructure.parseAccessStructure(
////                    policyTree = accessStructurePermission.accessStructure
////                )
////                if (currentAccessStructure.containsAtLeastOne(attributesWithVersionNumberString)) {
////                    attributesWithVersionNumberString.forEach { attributeToUpdate ->
////                        currentAccessStructure.replaceAttribute(
////                            oldAttribute = attributeToUpdate,
////                            newAttribute = incrementByOneVersionNumberInAttributeNameAndVersionNumber(
////                                nameWithVersionNumber = attributeToUpdate
////                            )
////                        )
////                    }
////                    accessStructurePermission.accessStructure = currentAccessStructure.toString()
////                    needToUpdateResource = true
////                }
////            }
////
////            if (needToUpdateResource) {
////                code = updateResourceAndAccessStructures(
////                    resourceName = resourceName,
////                    accessStructuresPermissionsToUpdate = accessStructuresPermissions,
////                    newAccessStructuresAlreadyEmbedVersionNumbers = true,
////                )
////                if (code != CODE_000_SUCCESS) {
////                    return@forEach
////                }
////            }
////        }
////
////        return endOfMethod(
////            code = code,
////            dmLocked = false
////        )
////    }
////
////    override fun assignAccessStructure(
////        resourceName: String,
////        accessStructure: String,
////        operation: Operation,
////    ): OutcomeCode {
////        logger.info {
////            "Adding access structure giving operation $operation" +
////            "over resource $resourceName, access structure is " +
////            accessStructure
////        }
////
////        /** Guard clauses */
////        if (resourceName.isBlank()) {
////            logger.warn { "Resource name is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////        if (accessStructure.isBlank()) {
////            logger.warn { "Access structure is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod(dmLock = false)
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////
////
////        /**
////         * Embed the admin attribute and the version numbers
////         * of all the attributes in the [accessStructure]
////         */
////        val accessStructureAndCode = embedAdminAttributeAndVersionNumbers(
////            accessStructures = hashSetOf(accessStructure)
////        )
////        if (accessStructureAndCode.code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = accessStructureAndCode.code,
////                dmLocked = false
////            )
////        }
////        val accessStructureWithVersionNumbers = accessStructureAndCode.accessStructures!!.first()
////
////
////        /** Find the resource in the metadata */
////        val resource = mm.getResources(
////            resourceName = resourceName
////        ).firstOrNull() ?: return endOfMethod(
////            code = CODE_006_RESOURCE_NOT_FOUND,
////            dmLocked = false
////        )
////        if (resource.status == ElementStatus.DELETED) {
////            return endOfMethod(
////                code = CODE_015_RESOURCE_WAS_DELETED,
////                dmLocked = false
////            )
////        }
////
////        // TODO you have the "abeSecretKey" class variable to fill and use
////        // TODO we are retrieving the key from the MM, do we have to do it every time?
////        code = getABEKey()
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        /** Get access structure tuples of a specific permission from the MM */
////        val allResourceAccessStructuresPermissions = mm.getAccessStructuresPermissions(
////            resourceName = resourceName
////        )
////        val firstPermission = allResourceAccessStructuresPermissions.first().operation
////        val resourceAccessStructuresPermissions = allResourceAccessStructuresPermissions.filter {
////            it.operation == firstPermission
////        }
////        val tupleAccessStructurePermissionToAdd = hashSetOf<AccessStructurePermission>()
////        resourceAccessStructuresPermissions.forEach { resourceAccessStructurePermission ->
////
////            verifyTupleSignature(resourceAccessStructurePermission)
////
////            val symKey = when (resource.enforcement) {
////                Enforcement.TRADITIONAL -> "null".toByteArray()
////                Enforcement.COMBINED -> cryptoABE.encryptABE(
////                    accessStructure = accessStructureWithVersionNumbers,
////                    plaintext = cryptoABE.decryptABE(
////                        keyID = user.name, // TODO is this id fine?
////                        ciphertext = resourceAccessStructurePermission.encryptedSymKey!!.key.encodeBase64()
////                    )
////                ).decodeBase64()
////            }
////
////            /** Create a new access structure tuple */
////            val newAccessStructurePermission = AccessStructurePermission(
////                resourceName = resourceName,
////                resourceToken = resource.token,
////                accessStructure = accessStructureWithVersionNumbers,
////                operation = operation,
////                encryptedSymKey = EncryptedSymKey(key = symKey),
////                resourceVersionNumber = resourceAccessStructurePermission.resourceVersionNumber
////            )
////            val accessStructureSignature = cryptoPKE.createSignature(
////                bytes = newAccessStructurePermission.getBytesForSignature(),
////                signingKey = asymSigKeyPair.private
////            )
////            newAccessStructurePermission.updateSignature(
////                newSignature = accessStructureSignature,
////                newSigner = user.token,
////            )
////            tupleAccessStructurePermissionToAdd.add(newAccessStructurePermission)
////        }
////
////        code = mm.addAccessStructuresPermissions(
////            newAccessStructuresPermissions = tupleAccessStructurePermissionToAdd
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        /**
////         * Update the access structure
////         * of the resource in the AC
////         */
////        return endOfMethod(
////            code = (ac?.assignPermissionToAccessStructure(
////                resourceName = resourceName,
////                accessStructure = accessStructureWithVersionNumbers,
////                operation = operation,
////            ) ?: CODE_000_SUCCESS),
////            dmLocked = false
////        )
////    }
////
////    override fun updateResourceAndAccessStructures(
////        resourceName: String,
////        accessStructuresPermissionsToUpdate: HashSet<AccessStructurePermission>,
////        newAccessStructuresAlreadyEmbedVersionNumbers: Boolean
////    ): OutcomeCode {
////        logger.info {
////            "Updating the resource $resourceName and the related " +
////            "access structure tuples (one per row below):"
////        }
////
////        var index = 0
////        accessStructuresPermissionsToUpdate.forEach { tupleAccessStructurePermissionToUpdate ->
////            logger.info {
////                "${index + 1}: $tupleAccessStructurePermissionToUpdate "
////            }
////            index += 1
////        }
////
////        /** Guard clauses */
////        if (resourceName.isBlank()) {
////            logger.warn { "Resource name is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////        accessStructuresPermissionsToUpdate.forEach { tupleAccessStructurePermissionToUpdate ->
////            if (tupleAccessStructurePermissionToUpdate.accessStructure.isBlank()) {
////                logger.warn { "Access structure is blank ($tupleAccessStructurePermissionToUpdate)" }
////                return CODE_020_INVALID_PARAMETER
////            }
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod(dmLock = false)
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////
////        /**
////         * Get the resource; we need the enforcement
////         * and the latest version number
////         */
////        val resource = mm.getResources(
////            resourceName = resourceName
////        ).firstOrNull() ?: return endOfMethod(
////            code = CODE_006_RESOURCE_NOT_FOUND,
////            dmLocked = false
////        )
////        val newResourceToken = Element.generateRandomToken()
////        val newLatestSymKeyVersionNumber = resource.versionNumber + 1
////        val newEncryptionSymmetricKey = when (resource.enforcement) {
////            Enforcement.TRADITIONAL -> {
////                null
////            }
////            Enforcement.COMBINED -> {
////                // TODO you have the "abeSecretKey" class variable to fill and use
////                // TODO we are retrieving the key from the MM, do we have to do it every time?
////                code = getABEKey()
////                if (code != CODE_000_SUCCESS) {
////                    return endOfMethod(
////                        code = code,
////                        dmLocked = false
////                    )
////                }
////                cryptoSKE.generateSymKey()
////            }
////        }
////
////        /**
////         * Update the token and increase the
////         * version number of the resource in the MM
////         */
////        code = mm.updateResourceTokenAndVersionNumber(
////            resourceName = resourceName,
////            oldResourceToken = resource.token,
////            newResourceToken = newResourceToken,
////            newResourceVersionNumber = newLatestSymKeyVersionNumber
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        /**
////         * Delete the old access structure tuples
////         * (new tuples will be created below)
////         */
////        code = mm.deleteAccessStructuresPermissions(
////            resourceName = resourceName,
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        /**
////         * Embed the version number of the
////         * attributes in the new access
////         * structures depending on the flag
////         */
////        val tuplesAccessStructurePermissionToUpdateWithVersionNumbers = if (newAccessStructuresAlreadyEmbedVersionNumbers) {
////            accessStructuresPermissionsToUpdate
////        } else {
////            val tuplesAccessStructurePermissionAndCode = embedAdminAttributeAndVersionNumbers(
////                accessStructuresPermissions = accessStructuresPermissionsToUpdate
////            )
////            if (tuplesAccessStructurePermissionAndCode.code != CODE_000_SUCCESS) {
////                return endOfMethod(
////                    code = tuplesAccessStructurePermissionAndCode.code,
////                    dmLocked = false
////                )
////            }
////            tuplesAccessStructurePermissionAndCode.accessStructuresPermissions!!
////        }
////
////        /**
////         * Now check whether the threshold was passed.
////         * If so, we need to delete old access structure
////         * tuples and re-encrypt existing data in the resource.
////         * Otherwise, update existing access structure tuples
////         */
////        val accessStructuresPermissionsToUpdateByPermission =
////            tuplesAccessStructurePermissionToUpdateWithVersionNumbers.groupBy {
////                it.operation
////            }
////        val sizeOfFirst = accessStructuresPermissionsToUpdateByPermission.values.first().size
////        if (!accessStructuresPermissionsToUpdateByPermission.values.all {
////            it.size == sizeOfFirst
////        }) {
////            val message = "Not all permissions have the same number of access structure tuples"
////            logger.error { message }
////            throw IllegalStateException(message)
////        }
////        val thresholdWasPassed = (sizeOfFirst >= resource.reEncryptionThresholdNumber)
////
////        /** CASE "THRESHOLD WAS PASSED */
////        if (thresholdWasPassed) {
////            logger.warn { "Threshold was passed, re-encrypting resources with the new key" }
////            val newResource = Resource(
////                name = resourceName,
////                versionNumber = newLatestSymKeyVersionNumber,
////                reEncryptionThresholdNumber = resource.reEncryptionThresholdNumber,
////                enforcement = resource.enforcement
////            )
////            newResource.token = newResourceToken
////
////            code = reEncryptResource(
////                newResource = newResource,
////                accessStructuresPermissionsToUpdateByPermission = accessStructuresPermissionsToUpdateByPermission,
////                newEncryptionSymmetricKey = newEncryptionSymmetricKey!!
////            )
////            if (code != CODE_000_SUCCESS) {
////                return endOfMethod(
////                    code = code,
////                    dmLocked = false
////                )
////            }
////        }
////        /** CASE "THRESHOLD WAS NOT PASSED */
////        else {
////            logger.info {
////                "Threshold (${resource.reEncryptionThresholdNumber}) was not passed " +
////                "(currently we have $sizeOfFirst access structure tuples per permission)"
////            }
////
////            /**
////             * For each access structure tuple, update the token
////             * and re-encrypt the key under the new access structure
////             */
////            val updatedAccessStructuresPermissions = hashSetOf<AccessStructurePermission>()
////            tuplesAccessStructurePermissionToUpdateWithVersionNumbers.forEach { tupleAccessStructurePermissionToUpdate ->
////
////                /**
////                 * Encrypt the symmetric key
////                 * under the access structure
////                 * embedding version numbers
////                 */
////                val symKeyEncryptedUnderUpdatedAccessStructure = EncryptedSymKey(
////                    key = when (resource.enforcement) {
////                        Enforcement.TRADITIONAL -> {
////                            "null".toByteArray()
////                        }
////                        Enforcement.COMBINED -> {
////                            cryptoABE.encryptABE(
////                                accessStructure = tupleAccessStructurePermissionToUpdate.accessStructure,
////                                plaintext = cryptoABE.decryptABE(
////                                    keyID = user.name, // TODO is this ID fine?
////                                    ciphertext = tupleAccessStructurePermissionToUpdate.encryptedSymKey!!.key.encodeBase64()
////                                )
////                            ).decodeBase64()
////                        }
////                    }
////                )
////
////                val updatedAccessStructurePermission = AccessStructurePermission(
////                    resourceName = tupleAccessStructurePermissionToUpdate.resourceName,
////                    resourceToken = newResourceToken,
////                    accessStructure = tupleAccessStructurePermissionToUpdate.accessStructure,
////                    operation = tupleAccessStructurePermissionToUpdate.operation,
////                    encryptedSymKey = symKeyEncryptedUnderUpdatedAccessStructure,
////                    resourceVersionNumber = tupleAccessStructurePermissionToUpdate.resourceVersionNumber
////                )
////                val accessStructureSignature = cryptoPKE.createSignature(
////                    bytes = updatedAccessStructurePermission.getBytesForSignature(),
////                    signingKey = asymSigKeyPair.private
////                )
////                updatedAccessStructurePermission.updateSignature(
////                    newSignature = accessStructureSignature,
////                    newSigner = user.token,
////                )
////                updatedAccessStructuresPermissions.add(updatedAccessStructurePermission)
////            }
////
////            code = mm.addAccessStructuresPermissions(
////                newAccessStructuresPermissions = updatedAccessStructuresPermissions
////            )
////            if (code != CODE_000_SUCCESS) {
////                return endOfMethod(
////                    code = code,
////                    dmLocked = false
////                )
////            }
////        }
////
////        val newAccessStructuresPermissionsByPermission = hashSetOf<AccessStructurePermission>()
////        accessStructuresPermissionsToUpdateByPermission.forEach { (_, oldAccessStructuresPermissions) ->
////            val oldAccessStructurePermission = oldAccessStructuresPermissions.first()
////
////            val newAccessStructurePermission = AccessStructurePermission(
////                resourceName = resourceName,
////                resourceToken = newResourceToken,
////                accessStructure = oldAccessStructurePermission.accessStructure,
////                operation = oldAccessStructurePermission.operation,
////                encryptedSymKey = EncryptedSymKey(
////                    key = when (resource.enforcement) {
////                        Enforcement.TRADITIONAL -> {
////                            "null".toByteArray()
////                        }
////                        Enforcement.COMBINED -> {
////                            cryptoABE.encryptABE(
////                                accessStructure = oldAccessStructurePermission.accessStructure,
////                                plaintext = newEncryptionSymmetricKey!!.secretKey.encoded.encodeBase64()
////                            ).decodeBase64()
////                        }
////                    }
////                ),
////                resourceVersionNumber = newLatestSymKeyVersionNumber
////            )
////            val accessStructureSignature = cryptoPKE.createSignature(
////                bytes = newAccessStructurePermission.getBytesForSignature(),
////                signingKey = asymSigKeyPair.private
////            )
////            newAccessStructurePermission.updateSignature(
////                newSignature = accessStructureSignature,
////                newSigner = user.token,
////            )
////            newAccessStructuresPermissionsByPermission.add(newAccessStructurePermission)
////
////            /**
////             * Update the access structure
////             * of the resource in the AC
////             */
////            code = (ac?.updateAccessStructureToPermission(
////                resourceName = resourceName,
////                newAccessStructure = oldAccessStructurePermission.accessStructure,
////            ) ?: CODE_000_SUCCESS)
////            if (code != CODE_000_SUCCESS) {
////                return@forEach
////            }
////        }
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        return endOfMethod(
////            code = mm.addAccessStructuresPermissions(
////                newAccessStructuresPermissions = newAccessStructuresPermissionsByPermission
////            ),
////            dmLocked = false
////        )
////    }
////
////    override fun revokeAccessStructure(
////        resourceName: String,
////        operation: Operation,
////    ): OutcomeCode {
////        logger.info {
////            "Revoking access structure giving permission " +
////            "$operation over resource $resourceName"
////        }
////
////        /** Guard clauses */
////        if (resourceName.isBlank()) {
////            logger.warn { "Resource name is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////
////        /** Lock the status of the services */
////        var code = startOfMethod(dmLock = false)
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /**
////         * Get all access structures tuples of the
////         * [resourceName]. Delete the ones with the
////         * given [operation], generate a new symmetric
////         * key and update the other tuples (as well
////         * as the version number and pseudonym of the resource)
////         */
////        val accessStructuresPermissions = mm.getAccessStructuresPermissions(
////            resourceName = resourceName
////        )
////        accessStructuresPermissions.forEach { accessStructurePermission ->
////            verifyTupleSignature(accessStructurePermission)
////        }
////
////        if (accessStructuresPermissions.size == 0) {
////            return endOfMethod(
////                code = CODE_006_RESOURCE_NOT_FOUND,
////                dmLocked = false
////            )
////        }
////        if (!accessStructuresPermissions.any { it.operation != operation }) {
////            return endOfMethod(
////                code = CODE_074_CANNOT_REVOKE_LAST_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT,
////                dmLocked = false
////            )
////        }
////        if (!accessStructuresPermissions.any { it.operation == operation }) {
////            return endOfMethod(
////                code = CODE_071_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_NOT_FOUND,
////                dmLocked = false
////            )
////        }
////
////        /** Delete the access structure tuple for the given permission */
////        code = mm.deleteAccessStructuresPermissions(
////            resourceName = resourceName,
////            operation = operation
////        )
////        if (code != CODE_000_SUCCESS) {
////            return endOfMethod(
////                code = code,
////                dmLocked = false
////            )
////        }
////
////        code = updateResourceAndAccessStructures(
////            resourceName = resourceName,
////            accessStructuresPermissionsToUpdate = accessStructuresPermissions.filter {
////                it.operation != operation
////            }.toHashSet(),
////            newAccessStructuresAlreadyEmbedVersionNumbers = true,
////        )
////        return endOfMethod(
////            code = code,
////            dmLocked = false
////        )
////    }
////
////    override fun generateUserABEKey(
////        username: String,
////    ): OutcomeCode {
////        logger.info { "Generating secret ABE key for user $username" }
////
////        /** Guard clauses */
////        if (username.isBlank()) {
////            logger.warn { "Username is blank" }
////            return CODE_020_INVALID_PARAMETER
////        }
////
////        /** Lock the status of the services */
////        val code = startOfMethod(
////            dmLock = false,
////            acLock = false,
////        )
////        if (code != CODE_000_SUCCESS) {
////            return code
////        }
////
////        /**
////         * Get all attributes of the user,
////         * generate the new secret ABE key
////         * and update the user in the metadata
////         */
////        val attributesAssignedToUser = hashSetOf<String>()
////        mm.getUsersAttributes(
////            username = username
////        ).forEach { tupleUserAttribute ->
////            verifyTupleSignature(tupleUserAttribute)
////            val attributeName = tupleUserAttribute.attributeName
////            val attributeVersionNumber = mm.getAttributes(
////                attributeName = attributeName,
////                status = ElementStatus.OPERATIONAL,
////                isAdmin = user.isAdmin
////            ).first().versionNumber
////
////            attributesAssignedToUser.add(concatenateAttributeAndValue(
////                attribute = concatenateAttributeNameAndVersionNumber(
////                    name = attributeName,
////                    versionNumber = attributeVersionNumber
////                ),
////                value = tupleUserAttribute.attributeValue
////            ))
////        }
////
////        /** Generate the secret ABE key */
////        val newABEUserKey = if (attributesAssignedToUser.isEmpty()) {
////            null
////        } else {
////            cryptoABE.generateABEPrivateKey(
////                attributes = concatenateAttributes(attributesAssignedToUser),
////                keyID = username
////            )
////        }
////
////        val encryptedNewABETupleUserKey = if (newABEUserKey != null) {
////
////            val userAsymEncKeyBytes = mm.getUserPublicKey(
////                name = username,
////                asymKeyType = AsymKeysType.ENC
////            )
////            /**
////             * If we did not find the user's key, it means
////             * that the user does not exist
////             */
////            if (userAsymEncKeyBytes == null) {
////                logger.warn { "User's key not found. Checking the user's status" }
////                val status = mm.getStatus(
////                    name = username,
////                    type = ABACElementType.USER
////                )
////                return if (status != null) {
////                    logger.warn { "User's status is $status" }
////                    when (status) {
////                        ElementStatus.INCOMPLETE -> endOfMethod(
////                            code = CODE_053_USER_IS_INCOMPLETE,
////                            dmLocked = false,
////                            acLocked = false
////                        )
////                        ElementStatus.OPERATIONAL -> {
////                            val message = "User's $username key not found but user is operational"
////                            logger.error { message }
////                            throw IllegalStateException(message)
////                        }
////                        ElementStatus.DELETED -> endOfMethod(
////                            code = CODE_013_USER_WAS_DELETED,
////                            dmLocked = false,
////                            acLocked = false
////                        )
////                    }
////                } else {
////                    endOfMethod(
////                        code = CODE_004_USER_NOT_FOUND,
////                        dmLocked = false,
////                        acLocked = false
////                    )
////                }
////            }
////
////            val userAsymEncPublicKey = cryptoPKE.recreateAsymPublicKey(
////                asymPublicKeyBytes = userAsymEncKeyBytes,
////                type = AsymKeysType.ENC
////            )
////            cryptoPKE.asymEncrypt(
////                encryptingKey = userAsymEncPublicKey,
////                bytes = newABEUserKey.encoded
////            )
////        } else {
////            null
////        }
////
////        return endOfMethod(
////            code = mm.updateUserABEKey(
////                username = username,
////                newEncryptedUserABEKey = encryptedNewABETupleUserKey
////            ),
////            dmLocked = false,
////            acLocked = false
////        )
////    }
////
////    /** The "readResource" and "writeResource" functions are not implemented here */
////
////
////    override fun getUsers(): CodeUsers {
////        logger.info { "User ${user.name} (is admin = ${user.isAdmin}) is getting users" }
////
////        /** Lock the status of the services */
////        val code = startOfMethod(
////            dmLock = false,
////            acLock = false
////        )
////        return if (code != CODE_000_SUCCESS) {
////            CodeUsers(code)
////        } else {
////            val users = mm.getUsers()
////            CodeUsers(
////                code = endOfMethod(
////                    code = CODE_000_SUCCESS,
////                    dmLocked = false,
////                    acLocked = false
////                ),
////                users = users
////            )
////        }
////    }
////
////    override fun getAttributes(): CodeAttributes {
////        logger.info { "User ${user.name} (is admin = ${user.isAdmin}) is getting attributes" }
////
////        /** Lock the status of the services */
////        val code = startOfMethod(
////            dmLock = false,
////            acLock = false
////        )
////        return if (code != CODE_000_SUCCESS) {
////            CodeAttributes(code)
////        } else {
////            val attributes = mm.getAttributes()
////            CodeAttributes(
////                code = endOfMethod(
////                    code = CODE_000_SUCCESS,
////                    dmLocked = false,
////                    acLocked = false
////                ),
////                attributes = attributes
////            )
////        }
////    }
////
////    override fun getResources(): CodeResources {
////        logger.info {
////            "User ${user.name} (is admin = ${user.isAdmin}) is getting resources"
////        }
////
////        /** Lock the status of the services */
////        val code = startOfMethod(
////            dmLock = false,
////            acLock = false
////        )
////        return if (code != CODE_000_SUCCESS) {
////            CodeResources(code)
////        } else {
////            val resources = mm.getResources()
////            CodeResources(
////                code = endOfMethod(
////                    code = CODE_000_SUCCESS,
////                    dmLocked = false,
////                    acLocked = false
////                ),
////                resources = resources
////            )
////        }
////    }
////
////    override fun getAttributeAssignments(
////        username: String?,
////        attributeName: String?
////    ): CodeUsersAttributes {
////        logger.info { "User ${user.name} (is admin = ${user.isAdmin}) is getting attribute tuples" }
////
////        /** Lock the status of the services */
////        val code = startOfMethod(
////            dmLock = false,
////            acLock = false
////        )
////        return if (code != CODE_000_SUCCESS) {
////            CodeUsersAttributes(code)
////        } else {
////            val usersAttributes = mm.getUsersAttributes(
////                username = if (
////                    !user.isAdmin ||
////                    (username.isNullOrBlank() && attributeName.isNullOrBlank())
////                ) {
////                    user.name
////                } else {
////                    username
////                },
////                attributeName = attributeName,
////                isAdmin = user.isAdmin,
////                offset = 0, limit = NO_LIMIT,
////            ).onEach {
////                verifyTupleSignature(it)
////            }
////
////            CodeUsersAttributes(
////                code = endOfMethod(
////                    code = CODE_000_SUCCESS,
////                    dmLocked = false,
////                    acLocked = false
////                ),
////                usersAttributes = usersAttributes
////            )
////        }
////    }
////
////    override fun getAccessStructures(
////        resourceName: String?
////    ): CodeAccessStructuresPermissions {
////        logger.info { "User ${user.name} (is admin = ${user.isAdmin}) is getting access structure tuples" }
////
////        /** Lock the status of the services */
////        val code = startOfMethod(
////            acLock = false,
////            dmLock = false
////        )
////        return if (code != CODE_000_SUCCESS) {
////            CodeAccessStructuresPermissions(code)
////        } else {
////            val accessStructuresPermissions = mm.getAccessStructuresPermissions(
////                resourceName = resourceName,
////                isAdmin = user.isAdmin,
////                offset = 0,
////                limit = NO_LIMIT
////            ).onEach {
////                verifyTupleSignature(it)
////            }
////
////            CodeAccessStructuresPermissions(
////                code = endOfMethod(
////                    code = CODE_000_SUCCESS,
////                    acLocked = false,
////                    dmLocked = false
////                ),
////                accessStructuresPermissions = accessStructuresPermissions
////            )
////        }
////    }
////
////
////
////    /**
////     * Re-encrypt data in the [newResource] with the new
////     * [newEncryptionSymmetricKey] for each access structure tuple in
////     * [accessStructuresPermissionsToUpdateByPermission]
////     */
////    abstract fun reEncryptResource(
////        newResource: Resource,
////        accessStructuresPermissionsToUpdateByPermission: Map<Operation, List<AccessStructurePermission>>,
////        newEncryptionSymmetricKey: SymmetricKeyCryptoAC
////    ): OutcomeCode
////
////    /** Get the ABE key of this user */
////    private fun getABEKey(): OutcomeCode {
////        val encryptedABEKey = mm.getUserABEKey(
////            username = user.name
////        ) ?: return CODE_073_ABE_KEY_NOT_FOUND
////
////        val decryptedABEKey = PrivateKeyOpenABE(
////            private = cryptoPKE.asymDecrypt(
////                encryptingKey = asymEncKeyPair.public,
////                decryptingKey = asymEncKeyPair.private,
////                encBytes = encryptedABEKey
////            ).decodeToString(),
////            keyID = user.name // TODO is this ID ok?
////        )
////
////        // TODO we are importing the key, but we do not need to do this everytime,
////        //  but only the first time or when the ABE key changes. But how to do it?
////        decryptedABEKey.keyID = user.name // TODO moreover, what ID should we assign to the key?
////        cryptoABE.importABEUserKey(
////            userKey = decryptedABEKey
////        )
////        return CODE_000_SUCCESS
////    }
////
////    /**
////     * Get the symmetric key for the given [resourceName] and
////     * [symKeyVersionNumber]. Finally, return the key along
////     * with an outcome code
////     */
////    protected fun getSymmetricKey(
////        resourceName: String,
////        symKeyVersionNumber: Int,
////    ): CodeSymmetricKey {
////        logger.info {
////            "Getting the symmetric key for resource $resourceName " +
////            "and version number $symKeyVersionNumber"
////        }
////
////        // TODO we are retrieving the key from the MM, do we have to do it every time?
////        val codeABE = getABEKey()
////        if (codeABE != CODE_000_SUCCESS) {
////            return CodeSymmetricKey(
////                code = codeABE
////            )
////        }
////
////        val accessStructuresPermissions = mm.getAccessStructuresPermissions(
////            resourceName = resourceName,
////            resourceVersionNumber = symKeyVersionNumber,
////            isAdmin = user.isAdmin,
////            offset = 0,
////            limit = 1,
////        )
////        if (accessStructuresPermissions.isEmpty()) {
////            return CodeSymmetricKey(
////                code = CODE_006_RESOURCE_NOT_FOUND
////            )
////        }
////
////        /**
////         * Loop over the access structure tuples, trying to
////         * find the one satisfied by the ABE key of the user
////         */
////        var symKey: ByteArray? = null
////        accessStructuresPermissions.forEach { accessStructurePermission ->
////            verifyTupleSignature(accessStructurePermission)
////            try {
////                symKey = cryptoABE.decryptABE(
////                    keyID = user.name, // TODO is this ID fine?
////                    ciphertext = accessStructurePermission.encryptedSymKey!!.key.encodeBase64()
////                ).decodeBase64()
////            } catch (e: OpenABECryptoContextDecrypt) {
////                logger.debug {
////                    "User ABE key does not satisfy access structure" +
////                    accessStructurePermission.accessStructure
////                }
////            }
////        }
////
////        return if (symKey == null) {
////            CodeSymmetricKey(
////                code = CODE_006_RESOURCE_NOT_FOUND
////            )
////        } else {
////            CodeSymmetricKey(
////                symmetricKeyCryptoAC = SymmetricKeyCryptoAC(
////                    secretKey =  SecretKeyOpenABE(
////                        secretKey = symKey!!
////                    )
////                    /**
////                     * TODO This above should be something like:
////                     *  secretKey = when (coreParameters.cryptoType) {
////                     *      CryptoType.JAVA -> SecretKeyJava(
////                     *          secretKey = symKey!!
////                     *      )
////                     *      CryptoType.SODIUM -> SecretKeySodium(
////                     *          secretKey = symKey!!
////                     *      )
////                     *      CryptoType.OPENABE -> SecretKeyOpenABE(
////                     *          secretKey = symKey!!
////                     *      )
////                     *  }
////                     */
////                )
////            )
////        }
////    }
////
////    /**
////     * Verify the signature of the given [tupleAssignment]. If the
////     * signature is invalid, a SignatureException will be thrown
////     */
////    private fun verifyTupleSignature(
////        tupleAssignment: Assignment
////    ) {
////        // TODO is it the name or the token? should this be "user.token"? And then, are we sure
////        //  that the token in "user.token" corresponds to the user's token (or it was just generated
////        //  anew when creating the class instance)?
////        if (tupleAssignment.signer == user.name) {
////            cryptoPKE.verifySignature(
////                signature = tupleAssignment.signature!!,
////                bytes = tupleAssignment.getBytesForSignature(),
////                verifyingKey = asymSigKeyPair.public
////            )
////        } else {
////            val asymSigPublicKeyBytes = mm.getUserPublicKey(
////                token = tupleAssignment.signer,
////                asymKeyType = AsymKeysType.SIG
////            )
////            val asymSigPublicKey = cryptoPKE.recreateAsymPublicKey(
////                asymPublicKeyBytes = asymSigPublicKeyBytes!!,
////                type = AsymKeysType.SIG
////            )
////            cryptoPKE.verifySignature(
////                signature = tupleAssignment.signature!!,
////                bytes = tupleAssignment.getBytesForSignature(),
////                verifyingKey = asymSigPublicKey
////            )
////        }
////    }
////
////
////
////    /**
////     * Concatenate the given [name] and [versionNumber]
////     * using the dedicated delimiter
////     */
////    private fun concatenateAttributeNameAndVersionNumber(
////        name: String, versionNumber: Int
////    ): String {
////        return "$name$nameAndVersionNumberDelimiter$versionNumber"
////    }
////
////    /**
////     * Obtain the name from the given [nameWithVersionNumber]
////     * splitting by the dedicated delimiter
////     */
////    private fun getAttributeNameFromNameAndVersionNumber(
////        nameWithVersionNumber: String
////    ): String {
////        return nameWithVersionNumber.substringBeforeLast(nameAndVersionNumberDelimiter)
////    }
////
////    /**
////     * Obtain the version number from the given [nameWithVersionNumber]
////     * splitting by the dedicated delimiter
////     */
////    private fun getVersionNumberFromNameAndVersionNumber(
////        nameWithVersionNumber: String
////    ): String {
////        return nameWithVersionNumber.substringAfterLast(nameAndVersionNumberDelimiter)
////    }
////
////    /**
////     * Increment by one the version number in the
////     * given [nameWithVersionNumber] splitting and
////     * then re-joining the name and the number with
////     * the dedicated delimiter
////     */
////    private fun incrementByOneVersionNumberInAttributeNameAndVersionNumber(
////        nameWithVersionNumber: String
////    ): String {
////        val attributeName = getAttributeNameFromNameAndVersionNumber(nameWithVersionNumber)
////        val versionNumber = getVersionNumberFromNameAndVersionNumber(nameWithVersionNumber).toInt() + 1
////        return concatenateAttributeNameAndVersionNumber(
////            name = attributeName,
////            versionNumber = versionNumber
////        )
////    }
////
////    /**
////     * Concatenate the [attribute] with its [value],
////     * if given, using the dedicated delimiter
////     */
////    private fun concatenateAttributeAndValue(
////        attribute: String,
////        value: String?
////    ): String {
////        return if (value.isNullOrBlank()) {
////            attribute
////        } else {
////            "$attribute$attributeAndValueDelimiter$value"
////        }
////    }
////
////    /**
////     * Concatenate the [attributes] using
////     * the dedicated delimiter
////     */
////    private fun concatenateAttributes(
////        attributes: HashSet<String>
////    ): String {
////        return "$attributeSequenceDelimiter${attributes.joinToString(attributeSequenceDelimiter)}$attributeSequenceDelimiter"
////    }
////
////    /**
////     * Ensure that the [accessStructure] starts
////     * with the ADMIN attribute in an OR gate
////     */
////    private fun concatenateAccessStructureWithAdminAttribute( // TODO test that this behaviour works properly
////        accessStructure: String
////    ): String {
////        return if (accessStructure.startsWith("$ADMIN or (")) {
////            accessStructure
////        } else if (accessStructure.isBlank()) {
////            ADMIN
////        } else {
////            "$ADMIN or ($accessStructure)"
////        }
////    }
////
////    /**
////     * Embed version numbers in all access structures
////     * of the given set of [accessStructures]
////     */
////    private fun embedAdminAttributeAndVersionNumbers(
////        accessStructures: HashSet<String>
////    ): CodeAccessStructures {
////        val tuplesAccessStructurePermissionAndCode = embedAdminAttributeAndVersionNumbers(
////            accessStructuresPermissions = accessStructures.map {
////                AccessStructurePermission(
////                    resourceName = "",
////                    resourceToken = "",
////                    accessStructure = it,
////                    operation = Operation.READWRITE
////                )
////            }.toHashSet()
////        )
////        return if (tuplesAccessStructurePermissionAndCode.code != CODE_000_SUCCESS) {
////            CodeAccessStructures(code = tuplesAccessStructurePermissionAndCode.code)
////        } else {
////            CodeAccessStructures(
////                accessStructures = tuplesAccessStructurePermissionAndCode.accessStructuresPermissions!!.map {
////                    it.accessStructure
////                }.toHashSet()
////            )
////        }
////    }
////
////    /**
////     * Embed version numbers in all access structures tuples
////     * of the given set of [accessStructuresPermissions]
////     */
////    // TODO this should embed attribute's pseudonyms, not names; if you do this, remember that then
////    //  also ABE keys should embed pseudonyms
////    private fun embedAdminAttributeAndVersionNumbers(
////        accessStructuresPermissions: HashSet<AccessStructurePermission>
////    ): CodeAccessStructuresPermissions {
////
////        val attributesVersionNumbers = hashMapOf<String, Int>()
////        val cacheAttributesVersionNumbers = hashSetOf<String>()
////
////        accessStructuresPermissions.forEach { accessStructurePermission ->
////            val accessStructureWithAdmin = concatenateAccessStructureWithAdminAttribute(
////                accessStructure = accessStructurePermission.accessStructure
////            )
////            val accessStructureObject = parseAccessStructure(accessStructureWithAdmin)
////            val attributesInTheAccessStructure = accessStructureObject.getAttributes()
////            val attributesWithVersionNumberCode = getAttributeVersionNumbers(
////                attributes = (attributesInTheAccessStructure - cacheAttributesVersionNumbers).toHashSet()
////            )
////            if (attributesWithVersionNumberCode.code != CODE_000_SUCCESS) {
////                return CodeAccessStructuresPermissions(
////                    code = attributesWithVersionNumberCode.code,
////                )
////            }
////
////            cacheAttributesVersionNumbers.addAll(attributesInTheAccessStructure)
////            attributesWithVersionNumberCode.attributes!!.forEach {
////                if (attributesVersionNumbers.keys.contains(it.name)) {
////                    throw java.lang.IllegalStateException("SHOULD NOT HAPPEN") // TODO
////                } else {
////                    attributesVersionNumbers[it.name] = it.versionNumber
////                }
////            }
////
////            attributesInTheAccessStructure.forEach {
////                val attributeWasReplaced = accessStructureObject.replaceAttribute(
////                    oldAttribute = it,
////                    newAttribute = concatenateAttributeNameAndVersionNumber(
////                        name = it,
////                        versionNumber = attributesVersionNumbers[it]!!
////                    ),
////                )
////                if (!attributeWasReplaced) {
////                    val message = "Attribute $it is in access structure but was not replaced"
////                    logger.error { message }
////                    throw IllegalStateException(message)
////                }
////            }
////
////            accessStructurePermission.accessStructure = accessStructureObject.toString()
////        }
////
////        return CodeAccessStructuresPermissions(
////            accessStructuresPermissions = accessStructuresPermissions
////        )
////    }
////
////    /**
////     * For each attribute in the set of [attributes]:
////     * - if the attribute already exists, fetch the version number
////     * - if the attribute does not exist, return error
////     * Finally, return the set of attributes together with their
////     * version numbers
////     */
////    private fun getAttributeVersionNumbers(
////        attributes: HashSet<String>,
////    ): CodeAttributes {
////        var code = CODE_000_SUCCESS
////        val attributesWithVersionNumber: HashSet<Attribute> = hashSetOf()
////        attributes.forEach { attributeName ->
////            logger.debug {
////                "Getting status and version " +
////                "number of attribute $attributeName"
////            }
////
////            val attribute = mm.getAttributes(
////                attributeName = attributeName
////            ).firstOrNull()
////
////            if (attribute != null) {
////                when (attribute.status) {
////                    ElementStatus.OPERATIONAL -> {
////                        attributesWithVersionNumber.add(
////                            Attribute(
////                            name = attributeName,
////                            versionNumber = attribute.versionNumber
////                        ).apply {
////                            token = attribute.token
////                        })
////                    }
////                    ElementStatus.DELETED -> {
////                        logger.warn { "Attribute $attributeName was deleted" }
////                        code = CODE_067_ATTRIBUTE_WAS_DELETED
////                        return@forEach
////                    }
////                    ElementStatus.INCOMPLETE -> {
////                        val message = "Attributes cannot be incomplete"
////                        logger.error { message }
////                        throw IllegalStateException(message)
////                    }
////                }
////            } else {
////                logger.warn { "Attribute $attributeName was not found" }
////                code = CODE_066_ATTRIBUTE_NOT_FOUND
////                return@forEach
////            }
////        }
////        return CodeAttributes(
////            code = code,
////            attributes = attributesWithVersionNumber
////        )
////    }
//}
