package cryptoac.ac

import cryptoac.OutcomeCode
import cryptoac.tuple.Operation

/**
 * Interface defining the methods for invoking the APIs of
 * an AC entity providing the functions of a traditional
 * role-based access control enforcement mechanism
 */
interface ACServiceRBAC : ACService {

    /**
     * The status of a RBAC policy is composed of the following sets:
     * U: set of users;
     * R: set of roles;
     * P: set of resources;
     * UR: set of user-role assignments;
     * PA: set of role-permission assignments.
     */

    /** Add [roleName] to R */
    fun addRole(
        roleName: String
    ): OutcomeCode

    /**
     * Delete [roleName] from R and
     * [roleName]'s assignments from
     * UR and PA
     */
    fun deleteRole(
        roleName: String
    ): OutcomeCode

    /** Add [resourceName] to P */
    fun addResource(
        resourceName: String
    ): OutcomeCode

    /**
     * Delete [resourceName] from P and
     * [resourceName]'s assignments from PA
     */
    fun deleteResource(
        resourceName: String
    ): OutcomeCode

    /** Add ([username], [roleName]) to UR */
    fun assignUserToRole(
        username: String,
        roleName: String
    ): OutcomeCode

    /** Delete ([username], [roleName]) from UR */
    fun revokeUserFromRole(
        username: String,
        roleName: String
    ): OutcomeCode

    /**
     * Add ([roleName], ([operation],
     * [resourceName])) to PA
     */
    fun assignPermissionToRole(
        roleName: String,
        operation: Operation,
        resourceName: String
    ): OutcomeCode

    /** Delete ([roleName], (-, [resourceName])) from PA */
    fun revokePermissionFromRole(
        roleName: String,
        resourceName: String,
    ): OutcomeCode

    /**
     * Replace ([roleName], (-, [resourceName])) with
     * ([roleName], ([newOperation], [resourceName]))
     * in PA
     */
    fun updatePermissionToRole(
        roleName: String,
        newOperation: Operation,
        resourceName: String
    ): OutcomeCode

    /**
     * Check whether the [username] has the permission
     * to perform the [operation] over the [resourceName]
     * by assuming the [roleName], if give, and return
     * either CODE_000_SUCCESS (if the user is authorized)
     * or CODE_037_FORBIDDEN (if the user is not authorized).
     * The [operation] can be either READ or WRITE
     */
    fun canDo(
        username: String,
        roleName: String? = null,
        operation: Operation,
        resourceName: String,
    ): OutcomeCode
}
