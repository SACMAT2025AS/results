package cryptoac.core.files

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

// TODO implementa la possibilità di dare WRITE permissions
//  only anche nel cloud? Però attenti che il CSP può colludere

///**
// * The CoreCACABACCryptoAC implements an attribute-based CAC scheme
// * with hybrid cryptography for the base CryptoAC scenario.
// * It requires an MM, an RM and a DM service, while AC is optional
// * It uses the given [crypto] object and the [coreParameters]
// */
//class CoreCACABACryptoAC(
//    override val crypto: CryptoABE,
//    override val coreParameters: CoreParametersABAC
//) : CoreCACABACTuples(crypto, coreParameters)
//
//// TODO implement; SOTTO, C'E' GIA' CODICE PER FUNZIONE:
//    - READRESOURCE, ANCHE SE E' DA SISTEMARE
//    - RE-ENCRYPT RESOURCE


///**
// * Download, decrypt and check the signature of
// * the content of the resource [resourceName]
// * and return it along with the outcome code (if an
// * error occurred, the content of the resource will
// * be null)
// */
//override fun readResource(
//    resourceName: String
//): CodeResource {
//    cryptoac.core.logger.info { "Reading resource $resourceName by user ${user.name}" }
//
//    /** Guard clauses */
//    if (resourceName.isBlank()) {
//        cryptoac.core.logger.warn { "Resource name is blank" }
//        return CodeResource(OutcomeCode.CODE_020_INVALID_PARAMETER)
//    }
//
//    /** Lock the status of the services */
//    val code = startOfMethod(acLock = false)
//    if (code != OutcomeCode.CODE_000_SUCCESS) {
//        return CodeResource(code)
//    }
//
//    val resource = mm.getResources(
//        resourceName = resourceName,
//        isAdmin = user.isAdmin,
//        offset = 0,
//        limit = 1,
//    ).firstOrNull()
//    if (resource == null) {
//        cryptoac.core.logger.warn {
//            "Resource not found. Either user ${user.name} does not have " +
//                    "access to resource $resourceName or resource does not exist"
//        }
//        return CodeResource(
//            code = endOfMethod(
//                code = OutcomeCode.CODE_006_RESOURCE_NOT_FOUND,
//                acLocked = false
//            ))
//    }
//
//    DOVREMMO LEGGERE TUTTI I PEZZI DI RISORSA CIFRATI CON CHIAVI DIVERSE E METTERLI ASSIEME
//            DOVREMMO LEGGERE TUTTI I PEZZI DI RISORSA CIFRATI CON CHIAVI DIVERSE E METTERLI ASSIEME
//            DOVREMMO LEGGERE TUTTI I PEZZI DI RISORSA CIFRATI CON CHIAVI DIVERSE E METTERLI ASSIEME
//            DOVREMMO LEGGERE TUTTI I PEZZI DI RISORSA CIFRATI CON CHIAVI DIVERSE E METTERLI ASSIEME
//            DOVREMMO LEGGERE TUTTI I PEZZI DI RISORSA CIFRATI CON CHIAVI DIVERSE E METTERLI ASSIEME
//            DOVREMMO LEGGERE TUTTI I PEZZI DI RISORSA CIFRATI CON CHIAVI DIVERSE E METTERLI ASSIEME
//            DOVREMMO LEGGERE TUTTI I PEZZI DI RISORSA CIFRATI CON CHIAVI DIVERSE E METTERLI ASSIEME
//    val resourceToReadResult = dm.readResource(resourceName)
//    if (resourceToReadResult.code != OutcomeCode.CODE_000_SUCCESS) {
//        return CodeResource(
//            code = endOfMethod(
//                code = resourceToReadResult.code,
//                acLocked = false
//            ))
//    }
//    var resourceStream = resourceToReadResult.stream!!
//
//    when (resource.enforcement) {
//        /** Do nothing */
//        Enforcement.TRADITIONAL -> { }
//        /** We need the symmetric key to decrypt the resource */
//        Enforcement.COMBINED -> {
//            val symKeyCode = getSymmetricKey(
//                resourceName = resource.name,
//                symKeyVersionNumber = RESOURCE_VERSION_NUMBER_OBTAINED_FROM_dm_readResource
//            )
//            if (symKeyCode.code != OutcomeCode.CODE_000_SUCCESS) {
//                return CodeResource(
//                    code = endOfMethod(
//                        code = symKeyCode.code,
//                        acLocked = false
//                    ))
//            }
//
//            resourceStream = cryptoSKE.decryptStream(
//                decryptingKey = symKeyCode.key!!,
//                stream = resourceStream
//            )
//        }
//    }
//
//    return CodeResource(
//        code = endOfMethod(
//            code = OutcomeCode.CODE_000_SUCCESS,
//            acLocked = false
//        ),
//        stream = resourceStream
//    )
//}







///**
// * Re-encrypt data in the [newResource] with the new
// * [newEncryptionSymmetricKey] for each access structure tuple in
// * [accessStructuresPermissionsToUpdateByPermission]
// */
//override fun reEncryptResource(
//    newResource: Resource,
//    accessStructuresPermissionsToUpdateByPermission: Map<Operation, List<AccessStructurePermission>>,
//    newEncryptionSymmetricKey: SymmetricKeyCryptoAC
//): OutcomeCode {
//    var code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS
//    val resourceName = newResource.name
//    accessStructuresPermissionsToUpdateByPermission.values.first().forEach { accessStructurePermission ->
//        val currentSymKeyVersionNumber = accessStructurePermission.symKeyVersionNumber
//        cryptoac.core.logger.info {
//            "Re-encrypting content of resource with " +
//                    "version number $currentSymKeyVersionNumber"
//        }
//
//        val readCode = dm.readResource(
//            resourceName = resourceName,
//            resourceVersionNumber = currentSymKeyVersionNumber
//        )
//        if (readCode.code != OutcomeCode.CODE_000_SUCCESS) {
//            code = readCode.code
//            return@forEach
//        }
//
//        val encryptedResourceContent = readCode.stream
//        if (encryptedResourceContent != null) {
//            val currentSymmetricKey = cryptoABE.decryptABE(
//                keyID = user.name, // TODO is this ID fine?
//                ciphertext = accessStructurePermission.symKey!!.key.encodeBase64()
//            ).decodeBase64()
//
//            val decryptedResourceContent = cryptoSKE.decryptStream(
//                decryptingKey = SymmetricKeyCryptoAC(
//                    secretKey = SecretKeyOpenABE(
//                        secretKey = currentSymmetricKey
//                    )
//                    /**
//                     * TODO This above should be something like:
//                     *  secretKey = when (coreParameters.cryptoType) {
//                     *      CryptoType.JAVA -> SecretKeyJava(
//                     *          secretKey = symKey!!
//                     *      )
//                     *      CryptoType.SODIUM -> SecretKeySodium(
//                     *          secretKey = symKey!!
//                     *      )
//                     *      CryptoType.OPENABE -> SecretKeyOpenABE(
//                     *          secretKey = symKey!!
//                     *      )
//                     *  }
//                     */
//                    /**
//                     * TODO This above should be something like:
//                     *  secretKey = when (coreParameters.cryptoType) {
//                     *      CryptoType.JAVA -> SecretKeyJava(
//                     *          secretKey = symKey!!
//                     *      )
//                     *      CryptoType.SODIUM -> SecretKeySodium(
//                     *          secretKey = symKey!!
//                     *      )
//                     *      CryptoType.OPENABE -> SecretKeyOpenABE(
//                     *          secretKey = symKey!!
//                     *      )
//                     *  }
//                     */
//                ),
//                stream = encryptedResourceContent
//            )
//
//            val newEncryptedResourceContent = cryptoSKE.encryptStream(
//                encryptingKey = newEncryptionSymmetricKey!!,
//                stream = decryptedResourceContent
//            )
//
//            code = dm.writeResource(
//                updatedResource = newResource,
//                content = newEncryptedResourceContent
//            )
//            if (code != OutcomeCode.CODE_000_SUCCESS) {
//                return@forEach
//            }
//
//            code = dm.deleteResource(
//                resourceName = resourceName,
//                resourceVersionNumber = currentSymKeyVersionNumber
//            )
//            if (code != OutcomeCode.CODE_000_SUCCESS) {
//                return@forEach
//            }
//        } else {
//            cryptoac.core.logger.info { "No content for current version number" }
//        }
//    }
//    return code
//}