//package cryptoac.mm
//
//import cryptoac.Constants.DEFAULT_LIMIT
//import cryptoac.Constants.DEFAULT_OFFSET
//import cryptoac.OutcomeCode
//import cryptoac.crypto.AsymKeysType
//import cryptoac.tuple.*
//
///**
// * Interface defining the methods for invoking the APIs of
// * an MM entity providing the functions of the MM for AB-CAC
// */
//interface MMServiceCACABAC : MMService {
//
//    /**
//     * Set the ABE master public key
//     * ([mpk]) in the metadata
//     */
//    fun setMPK(mpk: String): OutcomeCode
//
//    /**
//     * Get the ABE MPK from the metadata.
//     * Return null if the ABE MPK was not
//     * found
//     */
//    fun getMPK(): String?
//
//    /**
//     * Add the [newAttribute] in the metadata and
//     * return the outcome code. Check that the
//     * attribute does not already exist or
//     * was deleted. If the attribute was deleted
//     * and [restoreIfDeleted] was set, restore
//     * the operational status of the attribute
//     */
//    fun addAttribute(
//        newAttribute: Attribute,
//        restoreIfDeleted: Boolean
//    ): OutcomeCode
//
//    /**
//     * Add the [newResource] in the metadata and
//     * return the outcome code. Check that the
//     * resource does not already exist or was deleted
//     */
//    fun addResource(
//        newResource: Resource
//    ): OutcomeCode
//
//    /**
//     * Add the [newUsersAttributes] in the metadata
//     * and return the outcome code. Check that involved
//     * users exist, are not incomplete, or were not
//     * deleted. Also check whether a user-attribute
//     * assignment already exists
//     */
//    fun addUsersAttributes(
//        newUsersAttributes: HashSet<UserAttribute>
//    ): OutcomeCode
//
//    /**
//     * Add the [newAccessStructuresPermissions] in the metadata
//     * and return the outcome code. Check that involved
//     * resources exist and were not deleted. Also check
//     * whether an access structure-permission assignment
//     * already exists
//     */
//    fun addAccessStructuresPermissions(
//        newAccessStructuresPermissions: HashSet<AccessStructurePermission>
//    ): OutcomeCode
//
//    /**
//     * Retrieve the attributes matching the specified
//     * [attributeName] and [status], if given, starting from
//     * the [offset] limiting the number of attributes to return
//     * to the given [limit] and with the (possibly) relevant
//     * information of whether the user invoking this function
//     * [isAdmin]. If no attributes are found, return an empty set.
//     * This method should support invocations by non-admin users
//     */
//    fun getAttributes(
//        attributeName: String? = null,
//        status: TupleStatus? = null,
//        isAdmin: Boolean = true,
//        offset: Int = DEFAULT_OFFSET,
//        limit: Int = DEFAULT_LIMIT,
//    ): HashSet<Attribute>
//
//    /**
//     * Retrieve the resources matching the specified
//     * [resourceName] and [status], if given, starting from
//     * the [offset] limiting the number of resources to return
//     * to the given [limit] and with the (possibly) relevant
//     * information of whether the user invoking this function
//     * [isAdmin]. If no resources are found, return an empty set.
//     * This method should support invocations by non-admin users
//     */
//    fun getResources(
//        resourceName: String? = null,
//        status: TupleStatus? = null,
//        isAdmin: Boolean = true,
//        offset: Int = DEFAULT_OFFSET,
//        limit: Int = DEFAULT_LIMIT,
//    ): HashSet<Resource>
//
//    /**
//     * Retrieve the user-attribute assignments matching
//     * the [username] and/or the [attributeName] (at least one
//     * required), starting from the [offset] limiting the number
//     * of user-attribute assignments to return to the given
//     * [limit] and with the (possibly) relevant information of
//     * whether the user invoking this function [isAdmin]. If no
//     * user-attribute assignments are found, return an empty set.
//     * This method should support invocations by non-admin users
//     */
//    fun getUsersAttributes(
//        username: String? = null,
//        attributeName: String? = null,
//        isAdmin: Boolean = true,
//        offset: Int = DEFAULT_OFFSET,
//        limit: Int = DEFAULT_LIMIT,
//    ): HashSet<UserAttribute>
//
//    /**
//     * Retrieve the access structure-permission assignments
//     * matching the [resourceName] and [resourceVersionNumber], if
//     * given, starting from the [offset] limiting the number of access
//     * structure-permission assignments to return to the given [limit]
//     * and with the (possibly) relevant information of whether the user
//     * invoking this function [isAdmin]. If no access structure-permission
//     * assignments are found, return an empty set. This method should
//     * support invocations by non-admin users
//     */
//    fun getAccessStructuresPermissions(
//        resourceName: String? = null,
//        resourceVersionNumber: Int? = null,
//        isAdmin: Boolean = true,
//        offset: Int = DEFAULT_OFFSET,
//        limit: Int = DEFAULT_LIMIT,
//    ): HashSet<AccessStructurePermission>
//
//    /**
//     * Retrieve the public asymmetric key of the given
//     * [asymKeyType] belonging to the [name] or the
//     * [token] (at least one required). Note that
//     * only operational or deleted users are considered.
//     * If the key was not found, return null. This method
//     * should support invocations by non-admin users
//     * // TODO add isAdmin parameter and update doc
//     */
//    fun getUserPublicKey(
//        name: String? = null,
//        token: String? = null,
//        asymKeyType: AsymKeysType,
//    ): ByteArray?
//
//    /**
//     * Retrieve the version number belonging to the element
//     * of the specified [elementType] by matching the [name]
//     * or [token] (at least one required). Note that only
//     * operational elements are considered. If the version
//     * number was not found, return null. This method should
//     * support invocations by non-admin users
//     */
//    fun getVersionNumber(
//        name: String? = null,
//        token: String? = null,
//        elementType: ABACElementType,
//    ): Int?
//
//    /**
//     * Retrieve the token of the element of
//     * the given [type] matching the [name].
//     * If the token was not found, return null.
//     * Note that only operational and deleted
//     * elements are considered.
//     * This method should support invocations
//     * by non-admin users
//     * // TODO add isAdmin parameter and update doc
//     */
//    fun getToken(
//        name: String,
//        type: ABACElementType
//    ): String?
//
//    /**
//     * Retrieve the status of the element of the
//     * given [type] by matching the [name]
//     * or [token] (at least one required).
//     * If the status was not found, return null.
//     * This method should support invocations by
//     * non-admin users
//     * // TODO add isAdmin parameter and update doc
//     */
//    fun getStatus(
//        name: String? = null,
//        token: String? = null,
//        type: ABACElementType
//    ): TupleStatus?
//
//    /**
//     * Delete the [attributeName] by moving the data into a
//     * proper data structure holding deleted elements.
//     * Indeed, even if deleted, we may need the data of
//     * an attribute (e.g., the version number, in case
//     * the attribute gets restored). Finally, return the
//     * outcome code. Check that the attribute exists and
//     * was not already deleted
//     */
//    fun deleteAttribute(
//        attributeName: String
//    ): OutcomeCode
//
//    /**
//     * Delete the [resourceName] by moving the data into a
//     * proper data structure holding deleted elements.
//     * Indeed, even if deleted, we may need the data of
//     * a resource. Finally, return the outcome code.
//     * Check that the resource exists and was not already
//     * deleted
//     */
//    fun deleteResource(
//        resourceName: String
//    ): OutcomeCode
//
//    /**
//     * Delete the user-attribute assignments matching the [username]
//     * and/or the [attributeName] (at least one required). Finally,
//     * return the outcome code. Check that at least one user-attribute
//     * assignment is deleted, and if not check whether the [username]
//     * or the [attributeName] exists and was not deleted
//     */
//    fun deleteUsersAttributes(
//        username: String? = null,
//        attributeName: String? = null,
//    ): OutcomeCode
//
//    /**
//     * Delete the access structure-permission assignments matching
//     * the [resourceName] and the [operation], if given. Finally,
//     * return the outcome code.
//     * TODO SISTEMARE QUESTA DOCUMENTAIZONE SOTTO, NON SONO SICURO
//     * Check that exactly one access structure-permission assignment
//     * is deleted, and if not check whether the [resourceName] exists
//     * and was not deleted, or that the access structure-permission
//     * assignment actually exists
//     */
//    fun deleteAccessStructuresPermissions(
//        resourceName: String,
//        operation: Operation? = null
//    ): OutcomeCode
//
//    /**
//     * Update the token of the [resourceName] with the
//     * [newResourceToken] and the version number with
//     * the [newResourceVersionNumber]. This method should
//     * also update the resource's token across all metadata.
//     * Check that the resource exists and was not deleted
//     */
////    TODO NO, QUESTO METODO NON DEVE "update the resource's token across all metadata"
////     PERCHE' LE TUPLE VANNO FIRMATE DIGITALMENTE UNA PER UNA
//    fun updateResourceTokenAndVersionNumber(
//        resourceName: String,
//        oldResourceToken: String,
//        newResourceToken: String,
//        newResourceVersionNumber: Int,
//    ): OutcomeCode
//
//    /**
//     * Update the token, access structure, encrypted symmetric
//     * key and version number of the access structure-permission
//     * assignment with the given [updatedAccessStructurePermission]
//     */
//    fun updateAccessStructurePermission(
//        updatedAccessStructurePermission: AccessStructurePermission
//    ): OutcomeCode
//
//    /**
//     * Update the token of the [attributeName] with the
//     * [newAttributeToken] and the version number with
//     * the [newVersionNumber]. This method should also
//     * update the attribute's token across all metadata.
//     * Check that the attribute exists and was not deleted
//     */
////    TODO NO, QUESTO METODO NON DEVE "update the attribute's token across all metadata"
////     PERCHE' LE TUPLE VANNO FIRMATE DIGITALMENTE UNA PER UNA
//    fun updateAttributeTokenAndVersionNumber(
//        attributeName: String,
//        oldAttributeToken: String,
//        newAttributeToken: String,
//        newVersionNumber: Int
//    ): OutcomeCode
//    /**
//     * Replace the ABE key for [username] with the given
//     * [newEncryptedUserABEKey]. Only operational users
//     * are considered
//     */
//    fun updateUserABEKey(
//        username: String,
//        newEncryptedUserABEKey: ByteArray?
//    ): OutcomeCode
//
//    /**
//     * Get the [username]'s ABE key. Only
//     * operational users are considered.
//     * If the key was not found or is null,
//     * return null
//     */
//    fun getUserABEKey(
//        username: String,
//    ): ByteArray?
//
//}