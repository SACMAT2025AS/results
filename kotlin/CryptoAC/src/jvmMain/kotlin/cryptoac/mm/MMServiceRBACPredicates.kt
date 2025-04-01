package cryptoac.mm

import cryptoac.Constants.DEFAULT_LIMIT
import cryptoac.Constants.DEFAULT_OFFSET
import cryptoac.OutcomeCode
import cryptoac.tuple.Predicate

/**
 * Interface defining the methods for invoking the APIs of
 * an MM entity providing the functions of the MM for an
 * extended role-based access control model/scheme
 */
interface MMServiceRBACPredicates : MMService {

    // TODO create tests for this class

    /**
     * Add the [newRole] in the policy state and
     * return the outcome code. Check that the
     * role does not already exist or was deleted
     */
    fun addRole(
        newRole: String
    ): OutcomeCode

    /**
     * Add the [newResource] in the policy state and
     * return the outcome code. Check that the
     * resource does not already exist or was deleted
     */
    fun addResource(
        newResource: String
    ): OutcomeCode

    /**
     * Add the [newUsersRoles] in the policy state and
     * return the outcome code. Check that involved
     * users exist, are not incomplete or were not
     * deleted, and that involved roles exist and
     * were not deleted. Also check whether a
     * user-role assignment already exists
     */
    fun addUsersRoles(
        newUsersRoles: List<Pair<String, String>>
    ): OutcomeCode

    /**
     * Add the [newRolesPermissions] in the policy state
     * and return the outcome code. Check that involved
     * roles exist and were not deleted and that involved
     * resources exist and were not deleted. Also check whether
     * a role-permission assignment already exists
     */
    fun addRolesPermissions(
        newRolesPermissions: List<Triple<String, String, String>>
    ): OutcomeCode

    /**
     * Add the [newUsersPredicates] in the policy state and
     * return the outcome code. Check that involved users
     * exist, are not incomplete or were not deleted. Also
     * check whether a user-predicate assignment already exists
     */
    fun addUserPredicates(
        newUsersPredicates: List<Pair<String, Predicate>>
    ): OutcomeCode

    /**
     * Add the [newRolesPredicates] in the policy state and
     * return the outcome code. Check that involved roles
     * exist and were not deleted. Also check whether a
     * role-predicate assignment already exists
     */
    fun addRolePredicates(
        newRolesPredicates: List<Pair<String, Predicate>>
    ): OutcomeCode

    /**
     * Add the [newResourcesPredicates] in the policy state and
     * return the outcome code. Check that involved resources
     * exist and were not deleted. Also check whether a
     * resource-predicate assignment already exists
     */
    fun addResourcesPredicates(
        newResourcesPredicates: List<Pair<String, Predicate>>
    ): OutcomeCode

    /**
     * Retrieve the roles matching the specified
     * [roleName], if given, starting from the [offset]
     * limiting the number of roles to return to the
     * given [limit] and with the (possibly) relevant
     * information of whether the user invoking this function
     * [isAdmin]. If no roles are found, return an empty set.
     * This method should support invocations by non-admin users
     */
    fun getRoles(
        roleName: String? = null,
        isAdmin: Boolean = true,
        offset: Int = DEFAULT_OFFSET,
        limit: Int = DEFAULT_LIMIT,
    ): HashSet<String>

    /**
     * Retrieve the resources matching the specified
     * [resourceName], if given, starting from the [offset]
     * limiting the number of resources to return to the
     * given [limit] and with the (possibly) relevant
     * information of whether the user invoking this function
     * [isAdmin]. If no resources are found, return an empty set.
     * This method should support invocations by non-admin users
     */
    fun getResources(
        resourceName: String? = null,
        isAdmin: Boolean = true,
        offset: Int = DEFAULT_OFFSET,
        limit: Int = DEFAULT_LIMIT,
    ): HashSet<String>

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
        offset: Int = DEFAULT_OFFSET,
        limit: Int = DEFAULT_LIMIT,
    ): List<Pair<String, String>>

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
        offset: Int = DEFAULT_OFFSET,
        limit: Int = DEFAULT_LIMIT,
    ): List<Triple<String, String, String>>


    /**
     * Delete the [roleName] element by moving it to a
     * proper data structure holding deleted elements.
     * Finally, return the outcome code. Check that the
     * role exists and was not already deleted
     */
    fun deleteRole(
        roleName: String
    ): OutcomeCode

    /**
     * Delete the [resourceName] element by moving it to a
     * proper data structure holding deleted elements.
     * Finally, return the outcome code. Check that the
     * resource exists and was not already deleted
     */
    fun deleteResource(
        resourceName: String
    ): OutcomeCode

    /**
     * Delete the user-role assignments matching
     * the given [roleName] and return the outcome
     * code. Check that [roleName] is not the admin.
     * Also check that at least one user-role assignment
     * is deleted, and if not check whether the
     * [roleName] exists and was not deleted
     */
    fun deleteUsersRoles(
        roleName: String
    ): OutcomeCode

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
     * Delete the user-predicate assignments matching
     * the given [username] and/or the [predicate] (at
     * least one required). Finally, return the outcome
     * code. Check that at least one user-predicate
     * assignment is deleted, and if not check whether
     * the [username] exists and was not deleted
     */
    fun deleteUserPredicates(
        username: String? = null,
        predicate: Predicate? = null,
    ): OutcomeCode

    /**
     * Delete the role-predicate assignments matching
     * the given [roleName] and/or the [predicate] (at
     * least one required). Finally, return the outcome
     * code. Check that at least one role-predicate
     * assignment is deleted, and if not check whether
     * the [roleName] exists and was not deleted
     */
    fun deleteRolePredicates(
        roleName: String? = null,
        predicate: Predicate? = null,
    ): OutcomeCode

    /**
     * Delete the resource-predicate assignments matching
     * the given [resourceName] and/or the [predicate] (at
     * least one required). Finally, return the outcome
     * code. Check that at least one resource-predicate
     * assignment is deleted, and if not check whether
     * the [resourceName] exists and was not deleted
     */
    fun deleteResourcePredicates(
        resourceName: String? = null,
        predicate: Predicate? = null,
    ): OutcomeCode

    /**
     * Update the operation of the given [updatedRolePermission]
     * and return the outcome code. Check that the role-permission
     * assignment exists
     */
    fun updateRolePermission(
        updatedRolePermission: Triple<String, String, String>
    ): OutcomeCode
}
