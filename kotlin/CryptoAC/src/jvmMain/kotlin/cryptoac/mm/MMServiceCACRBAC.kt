package cryptoac.mm

import cryptoac.Constants.DEFAULT_LIMIT
import cryptoac.Constants.DEFAULT_OFFSET
import cryptoac.OutcomeCode
import cryptoac.crypto.AsymKeysType
import cryptoac.tuple.*
import java.security.PublicKey

/**
 * Interface defining the methods for invoking the APIs of
 * an MM entity providing the functions of the MM for RB-CAC
 */
interface MMServiceCACRBAC : MMService {

    /**
     * Add the [newRole] in the metadata and
     * return the outcome code. Check that the
     * role does not already exist or was deleted
     */
    fun addRole(
        newRole: Role
    ): OutcomeCode

    /**
     * Add the [newResource] in the metadata and
     * return the outcome code. Check that the
     * resource does not already exist or was deleted
     */
    fun addResource(
        newResource: Resource
    ): OutcomeCode

    /**
     * Add the [newUsersRoles] in the metadata and
     * return the outcome code. Check that involved
     * users exist, are not incomplete or were not
     * deleted, and that involved roles exist and
     * were not deleted. Also check whether a
     * user-role assignment already exists
     */
    fun addUsersRoles(
        newUsersRoles: HashSet<UserRole>
    ): OutcomeCode

    /**
     * Add the [newRolesPermissions] in the metadata
     * and return the outcome code. Check that involved
     * roles exist and were not deleted and that involved
     * resources exist and were not deleted. Also check whether
     * a role-permission assignment already exists
     */
    fun addRolesPermissions(
        newRolesPermissions: HashSet<RolePermission>
    ): OutcomeCode

    /**
     * Retrieve the roles matching the specified
     * [roleName] and [status], if given, starting from
     * the [offset] limiting the number of roles to return
     * to the given [limit] and with the (possibly) relevant
     * information of whether the user invoking this function
     * [isAdmin]. If no roles are found, return an empty set.
     * This method should support invocations by non-admin users
     */
    fun getRoles(
        roleName: String? = null,
        status: TupleStatus? = null,
        isAdmin: Boolean = true,
        offset: Int = DEFAULT_OFFSET,
        limit: Int = DEFAULT_LIMIT,
    ): HashSet<Role>

    /**
     * Retrieve the resources matching the specified
     * [resourceName] and [status], if given, starting from
     * the [offset] limiting the number of resources to return
     * to the given [limit] and with the (possibly) relevant
     * information of whether the user invoking this function
     * [isAdmin]. If no resources are found, return an empty set.
     * This method should support invocations by non-admin users
     */
    fun getResources(
        resourceName: String? = null,
        status: TupleStatus? = null,
        isAdmin: Boolean = true,
        offset: Int = DEFAULT_OFFSET,
        limit: Int = DEFAULT_LIMIT,
    ): HashSet<Resource>

    /**
     * Retrieve the user-role assignments matching the
     * [username] and/or the [roleName], if given, starting
     * from the [offset] limiting the number of user-role
     * assignments to return to the given [limit] and with
     * the (possibly) relevant information of whether the
     * user invoking this function [isAdmin]. If no user-role
     * assignments are found, return an empty set. This method
     * should support invocations by non-admin users
     */
    fun getUsersRoles(
        username: String? = null,
        roleName: String? = null,
        isAdmin: Boolean = true,
        status: TupleStatus? = null,
        offset: Int = DEFAULT_OFFSET,
        limit: Int = DEFAULT_LIMIT,
    ): HashSet<UserRole>

    /**
     * Retrieve the role-permission assignments matching the [roleName] and/or
     * the [resourceName], starting from the [offset] limiting the number
     * of role-permission assignments to return to the given [limit] and with
     * the (possibly) relevant information of whether the user invoking this
     * function [isAdmin]. If no role-permission assignments are found, return
     * an empty set. This method should support invocations by non-admin users
     */
    // TODO test this method when giving neither the role name nor the resource name
    fun getRolesPermissions(
        roleName: String? = null,
        resourceName: String? = null,
        isAdmin: Boolean = true,
        status: TupleStatus? = null,
        offset: Int = DEFAULT_OFFSET,
        limit: Int = DEFAULT_LIMIT,
    ): HashSet<RolePermission>

    /**
     * Retrieve the public asymmetric key of the given
     * [asymKeyType] belonging to the element of the
     * specified [elementType] by matching the [name] or
     * the [token] (at least one required). Note that
     * only operational or deleted elements are considered,
     * and resources do not have public keys. If the key was
     * not found, return null. This method should support
     * invocations by non-admin users
     * // TODO add isAdmin parameter and update doc
     */
    fun getPublicKey(
        name: String? = null,
        token: String? = null,
        elementType: RBACElementType,
        asymKeyType: AsymKeysType,
    ): ByteArray?

    /**
     * Retrieve the version number belonging to the element
     * of the specified [elementType] by matching the [name]
     * or [token] (at least one required). Note that only
     * operational elements are considered. If the version
     * number was not found, return null. This method should
     * support invocations by non-admin users
     * // TODO add isAdmin parameter and update doc
     */
    fun getVersionNumber(
        name: String? = null,
        token: String? = null,
        elementType: RBACElementType
    ): Int?

    /**
     * Retrieve the token of the element of
     * the given [type] matching the [name].
     * If the token was not found, return null.
     * Note that only operational and deleted
     * elements are considered.
     * This method should support invocations
     * by non-admin users
     * // TODO add isAdmin parameter and update doc
     */
    fun getToken(
        name: String,
        type: RBACElementType
    ): String?

    /**
     * Retrieve the status of the element of the
     * given [type] by matching the [name]
     * or [token] (at least one required).
     * If the status was not found, return null.
     * This method should support invocations by
     * non-admin users
     * // TODO add isAdmin parameter and update doc
     */
    fun getStatus(
        name: String? = null,
        token: String? = null,
        type: RBACElementType
    ): TupleStatus?

    /**
     * Delete the [roleName] by moving the data into a
     * proper data structure holding deleted elements.
     * Indeed, even if deleted, we may need the data of
     * a role (e.g., the public key to verify digital
     * signatures). Finally, return the outcome code.
     * Check that the role exists and was not already
     * deleted. Check that [roleName] is not the admin
     */
    fun deleteRole(roleName: String): OutcomeCode

    /**
     * Delete the [resourceName] by moving the data into a
     * proper data structure holding deleted elements.
     * Indeed, even if deleted, we may need the data of
     * a resource. Finally, return the outcome code.
     * Check that the resource exists and was not already
     * deleted
     */
    fun deleteResource(resourceName: String): OutcomeCode

    /**
     * Delete the user-role assignments matching
     * the given [roleName] and return the outcome
     * code. Check that [roleName] is not the admin.
     * Also check that at least one user-role assignment
     * is deleted, and if not check whether the
     * [roleName] exists and was not deleted
     */
    fun deleteUsersRoles(username: String? = null, roleName: String, status: TupleStatus? = TupleStatus.OPERATIONAL): OutcomeCode

    /**
     * Delete the role-permission assignments matching
     * the [roleName] and/or the [resourceName] (at
     * least one required). Finally, return the outcome
     * code. Check that [roleName] is not the admin.
     * Also check that at least one role-permission
     * assignment is deleted, and if not check whether
     * the [roleName] and the [resourceName] exist and
     * were not deleted
     */
    fun deleteRolesPermissions(
        roleName: String? = null,
        resourceName: String? = null,
    ): OutcomeCode

    /**
     * Update the token of [roleName] with the [newRoleToken],
     * the asymmetric encryption and signing public keys with
     * the [newAsymEncPublicKey] and [newAsymSigPublicKey]
     * and increase by 1 the [oldRoleVersionNumber]. Check that
     * the role exists and was not deleted
     */
    fun updateRoleTokenAndVersionNumberAndAsymKeys(
        roleName: String,
        oldRoleVersionNumber: Int,
        oldRoleToken: String,
        newRoleToken: String,
        newAsymEncPublicKey: PublicKey,
        newAsymSigPublicKey: PublicKey
    ): OutcomeCode

    /**
     * Update the version number of the [updatedResource]
     * and return the outcome code. Check that the resource exists
     * TODO also check that the resource was not deleted (perhaps this check is already implemented in overriding method)
     */
    fun updateResourceVersionNumber(
        updatedResource: Resource
    ): OutcomeCode

    /**
     * Update the token of the [resourceName] with the
     * [newResourceToken] and the version number with
     * the [newVersionNumber]. Check that the resource exists
     * TODO also check that the resource was not deleted (perhaps this check is already implemented in overriding method)
     */
    fun updateResourceTokenAndVersionNumber(
        resourceName: String,
        oldResourceToken: String,
        newResourceToken: String,
        newVersionNumber: Int,
    ): OutcomeCode

    /**
     * Update all fields (except role and resource name) of the
     * given [updatedRolePermission] and return the outcome code.
     * Check that the role-permission assignment exists
     */
    fun updateRolePermission(
        updatedRolePermission: RolePermission
    ): OutcomeCode
}
