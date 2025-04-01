//package cryptoac.core
//
//import cryptoac.OutcomeCode
//import cryptoac.ac.ACServiceABAC
//import cryptoac.crypto.*
//import cryptoac.dm.DMServiceABAC
//import cryptoac.mm.MMServiceCACABAC
//import cryptoac.code.*
//import cryptoac.tuple.Enforcement
//import cryptoac.rm.RMServiceABAC
//import cryptoac.tuple.Operation
//// import cryptoac.tuple.AccessStructurePermission
//import java.io.InputStream
//
///**
// * A CoreCACABAC extends the [Core] class as a cryptographic
// * enforcement mechanism for an attribute-based access control
// * model/scheme. The CoreCACABAC uses the [cryptoABE] to perform
// * ABE cryptographic computations
// */
//abstract class CoreCACABAC(
//    override val cryptoPKE: CryptoPKE,
//    override val cryptoSKE: CryptoSKE,
//    open val cryptoABE: CryptoABE,
//    override val coreParameters: CoreParameters,
//) : CoreCAC(
//    cryptoPKE,
//    cryptoSKE,
//    coreParameters
//) {
//
//    abstract override val mm: MMServiceCACABAC?
//
//    abstract override val rm: RMServiceABAC?
//
//    abstract override val dm: DMServiceABAC?
//
//    abstract override val ac: ACServiceABAC?
//
//
//
//    /**
//     * Add a new attribute with the given [attributeName] and assign the admin
//     * to the new attribute. If the attribute is a new one, set the version
//     * number to 1. If the attribute was previously deleted (and it is thus
//     * being restored), set the version number to the old version number plus 1.
//     * Finally, return the outcome code
//     */
//    abstract fun addAttribute(
//        attributeName: String
//    ): OutcomeCode
//
//    /**
//     * Delete the [attribute] from the metadata and from all ABE keys and
//     * access structures, refreshing ABE and symmetric keys. If all the
//     * attributes of the access structure of a resource are deleted (i.e.,
//     * if the access structure would be empty), return an error code.
//     * Finally, return the outcome code
//     */
//    abstract fun deleteAttributes(
//        attribute: String
//    ): OutcomeCode
//
//    /**
//     * Delete the [attributes] from the metadata and from
//     * all ABE keys and access structures, refreshing ABE
//     * and symmetric keys. If all the attributes of the
//     * access structure of a resource are deleted (i.e.,
//     * if the access structure would be empty), return
//     * an error code. Finally, return the outcome code
//     */
//    abstract fun deleteAttributes(
//        attributes: HashSet<String>
//    ): OutcomeCode
//
//    /**
//     * Add a new resource with the given name [resourceName],
//     * [resourceContent] and [enforcement] type to the policy. Also, assign
//     * an [accessStructure] giving [operation] over the resource. The
//     * [accessStructure] is assumed not to embed version numbers yet. Encrypt
//     * and sign, and upload the new [resourceContent] for the resource
//     * [resourceName]. Finally, return the outcome code
//     */
//    abstract fun addResource(
//        resourceName: String,
//        resourceContent: InputStream,
//        accessStructure: String,
//        operation: Operation,
//        enforcement: Enforcement
//    ): OutcomeCode
//
//    /**
//     * Delete the resource with the matching [resourceName] from
//     * the policy, and delete all the related access structures
//     * Finally, return the outcome code
//     */
//    abstract fun deleteResource(
//        resourceName: String
//    ): OutcomeCode
//
//    /**
//     * Assign the [attributeName] to the [username]
//     * in the policy with the optional [attributeValue]
//     * and refresh the [username]'s ABE key. Finally,
//     * return the outcome code
//     */
//    abstract fun assignUserToAttributes(
//        username: String,
//        attributeName: String,
//        attributeValue: String? = null
//    ): OutcomeCode
//
//    /**
//     * Assign the [attributes] (key-value pairs)
//     * to the [username] in the policy and refresh
//     * the [username]'s ABE key. Finally, return
//     * the outcome code
//     */
//    abstract fun assignUserToAttributes(
//        username: String,
//        attributes: HashMap<String, String?>
//    ): OutcomeCode
//
//    /**
//     * Revoke the [attributeName] from the [username] in
//     * the policy and refresh the [username]'s ABE key.
//     * Refresh also symmetric keys to which the user
//     * does not have access anymore, updating the
//     * [attributeName]' version number. Finally,
//     * return the outcome code
//     */
//    abstract fun revokeAttributesFromUser(
//        username: String,
//        attributeName: String
//    ): OutcomeCode
//
//    /**
//     * Revoke the [attributes] from the [username] in
//     * the policy and refresh the [username]'s ABE key.
//     * Refresh also symmetric keys to which the user
//     * does not have access anymore, updating each of
//     * the [attributes]' version numbers. Finally,
//     * return the outcome code
//     */
//    abstract fun revokeAttributesFromUser(
//        username: String,
//        attributes: HashSet<String>
//    ): OutcomeCode
//
//    /**
//     * Add access structure-permission assignments with the given [accessStructure]
//     * and granting the given [operation] over [resourceName]. This may result in
//     * adding several structure-permission assignments, from 1 to the threshold number
//     * of [resourceName] (depending on how many outdated structure-permission assignments
//     * there are). The current behaviour is to assign the same symmetric key to all
//     * structure-permission assignments of a given resource for a specific version
//     * number (although it is possible to modify this behaviour so to assign different
//     * keys or, e.g., tokens, to different access structures). Finally, return the
//     * outcome code
//     */
//    abstract fun assignAccessStructure(
//        resourceName: String,
//        accessStructure: String,
//        operation: Operation,
//    ): OutcomeCode
//
//    /**
//     * Update the token and increase by 1 the version number of the
//     * [resourceName]. Then, generate a new symmetric key for the
//     * [resourceName] and create a new structure-permission assignment
//     * for each existing permission. |THRESHOLD| Then, if the number of
//     * structure-permission assignments (per permission) of the resource
//     * is NOT greater or equal to the re-encryption threshold number of
//     * the resource, update the access structure-permission assignments
//     * in [accessStructuresPermissionsToUpdate] with the new resource
//     * token and refresh the encryption of the symmetric key. Otherwise,
//     * delete all old structure-permission assignments of the resource,
//     * re-encrypting old data in the [resourceName] with the new symmetric
//     * key. The access structures of the structure-permission assignments
//     * in [accessStructuresPermissionsToUpdate] can either already embed
//     * the version number of attributes or not, depending on flag set by
//     * [newAccessStructuresAlreadyEmbedVersionNumbers]. Finally, return
//     * the outcome code. Note: [accessStructuresPermissionsToUpdate] should
//     * contain ALL structure-permission assignments of [resourceName]
//     */
//    abstract fun updateResourceAndAccessStructures(
//        resourceName: String,
//        accessStructuresPermissionsToUpdate: HashSet<AccessStructurePermission>,
//        newAccessStructuresAlreadyEmbedVersionNumbers: Boolean,
//    ): OutcomeCode
//
//    /**
//     * Revoke the access structure giving [operation]
//     * over the [resourceName] in the policy, refreshing
//     * also the symmetric key of the [resourceName] in
//     * remaining access structures. At least one access
//     * structure should remain to avoid losing the symmetric
//     * keys. If an invocation to this method would delete
//     * the last access structure, return an error. Finally,
//     * return the outcome code
//     */
//    abstract fun revokeAccessStructure(
//        resourceName: String,
//        operation: Operation,
//    ): OutcomeCode
//
//    /**
//     * Refresh the ABE key of [username] according
//     * to the assigned attributes. Finally, return
//     * the outcome code
//     */
//    abstract fun generateUserABEKey(
//        username: String,
//    ): OutcomeCode
//
//
//
//    /**
//     * Download, decrypt and check the signature of
//     * the content of the resource [resourceName]
//     * and return it along with the outcome code (if an
//     * error occurred, the content of the resource will
//     * be null)
//     */
//    abstract fun readResource(
//        resourceName: String
//    ): CodeResource
//
//    /**
//     * Encrypt, sign and upload the new [resourceContent]
//     * for the resource [resourceName]. Finally, return
//     * the outcome code
//     */
//    abstract fun writeResource(
//        resourceName: String,
//        resourceContent: InputStream
//    ): OutcomeCode
//
//
//
//    /**
//     * Return the set of attributes, along with the
//     * outcome code (if an error occurred, the
//     * set of attributes will be null)
//     */
//    abstract fun getAttributes(): CodeAttributes
//
//    /**
//     * Return the set of resources, along with the
//     * outcome code (if an error occurred, the
//     * set of resources will be null)
//     */
//    abstract fun getResources(): CodeResources
//
//    /**
//     * Return the user-attribute assignments filtering
//     * by the [username] and [attributeName], if given,
//     * along with the outcome code (if an error
//     * occurred, the set of attribute assignments will be
//     * null)
//     */
//    abstract fun getAttributeAssignments(
//        username: String? = null,
//        attributeName: String? = null
//    ): CodeUsersAttributes
//
//    /**
//     * Return the resource-access structure assignments
//     * filtering by the [resourceName], if given, along
//     * with the outcome code (if an error occurred, the
//     * set of access structures will be null)
//     */
//    abstract fun getAccessStructures(
//        resourceName: String? = null
//    ): CodeAccessStructuresPermissions
//}
