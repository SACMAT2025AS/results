package cryptoac.core

import cryptoac.OutcomeCode
import cryptoac.ac.ACServiceRBAC
import cryptoac.crypto.*
import cryptoac.dm.DMServiceRBAC
import cryptoac.mm.MMServiceCACRBAC
import cryptoac.code.*
import cryptoac.tuple.Enforcement
import cryptoac.rm.RMServiceRBAC
import cryptoac.tuple.Operation
import cryptoac.tuple.TupleStatus
import java.io.InputStream

/**
 * A CoreCACRBAC extends the [Core] class as a cryptographic
 * enforcement mechanism for a role-based access control
 * model/scheme
 */
abstract class CoreCACRBAC(
    override val cryptoPKE: CryptoPKE,
    override val cryptoSKE: CryptoSKE,
    override val coreParameters: CoreParameters
) : CoreCAC(cryptoPKE, cryptoSKE, coreParameters) {

    abstract override val mm: MMServiceCACRBAC?

    abstract override val rm: RMServiceRBAC?

    abstract override val dm: DMServiceRBAC?

    abstract override val ac: ACServiceRBAC?



    /**
     * Add a new role with the given [roleName]
     * to the policy and assign the admin to
     * the new role. Finally, return the outcome
     * code
     */
    abstract fun addRole(
        roleName: String
    ): OutcomeCode

    /**
     * Delete the role with the matching [roleName] from
     * the policy and revoke all users and permissions
     * from the role. Finally, return the outcome code
     */
    abstract fun deleteRole(
        roleName: String
    ): OutcomeCode

    /**
     * Add a new resource with the given name [resourceName],
     * [resourceContent], [enforcement] type to the policy.
     * Encrypt, sign and upload the new [resourceContent]
     * for the resource [resourceName]. Also, assign all
     * permissions to the admin over the resource. Finally,
     * return the outcome code
     */
    abstract fun addResource(
        resourceName: String,
        resourceContent: InputStream,
        enforcement: Enforcement
    ): OutcomeCode

    /**
     * Delete the resource with the matching [resourceName] from
     * the policy, and delete all the related permissions.Finally,
     * return the outcome code
     */
    abstract fun deleteResource(
        resourceName: String
    ): OutcomeCode

    /**
     * Assign the user [username] to the role [roleName]
     * in the policy. Finally, return the outcome code
     */
    abstract fun assignUserToRole(
        username: String,
        roleName: String
    ): OutcomeCode

    /**
     * Revoke the user [username] from the role [roleName]
     * from the policy. Finally, return the outcome code
     */
    abstract fun revokeUserFromRole(
        username: String,
        roleName: String
    ): OutcomeCode

    /**
     * Assigns the [operation] to the role [roleName] over
     * the resource [resourceName] in the policy. Finally,
     * return the outcome code
     */
    abstract fun assignPermissionToRole(
        roleName: String,
        resourceName: String,
        operation: Operation
    ): OutcomeCode

    /**
     * Revoke the [operation] from the role [roleName] over
     * the resource [resourceName] in the policy. Finally,
     * return the outcome code
     */
    abstract fun revokePermissionFromRole(
        roleName: String,
        resourceName: String,
        operation: Operation
    ): OutcomeCode



    /**
     * Download, decrypt and check the signature of
     * the content of the resource [resourceName]
     * and return it along with the outcome code (if an
     * error occurred, the content of the resource will
     * be null)
     */
    abstract fun readResource(
        resourceName: String
    ): CodeResource

    /**
     * Encrypt, sign and upload the new [resourceContent]
     * for the resource [resourceName]. Finally, return
     * the outcome code
     */
    abstract fun writeResource(
        resourceName: String,
        resourceContent: InputStream
    ): OutcomeCode

    abstract fun rotateResourceKey(
        resourceName: String,
    ): OutcomeCode

    abstract fun eagerReencryption(
        resourceName: String,
    ): OutcomeCode

    abstract fun rotateRoleKeyUserRoles(
        roleName: String,
    ): OutcomeCode

    abstract fun rotateRoleKeyPermissions(
        roleName: String,
    ): OutcomeCode

    abstract fun isProtectedWithCAC(resourceName: String): Boolean

    abstract fun canUserDo(
        username: String,
        resourceName: String,
        operation: Operation,
    ): Boolean

    abstract fun canUserDoViaRoleCache(
        username: String,
        roleName: String,
        resourceName: String,
        operation: Operation,
    ): Boolean

    abstract fun canUserDoViaRoleCacheLast(
        username: String,
        roleName: String,
        resourceName: String,
        operation: Operation,
    ): Boolean

    abstract fun canRoleDo(
        roleName: String,
        operation: Operation,
        resourceName: String,
    ): Boolean

    abstract fun canRoleDoCache(
        roleName: String,
        operation: Operation,
        resourceName: String,
    ): Boolean

    abstract fun canRoleDoCacheLast(
        roleName: String,
        operation: Operation,
        resourceName: String,
    ): Boolean

    abstract fun canUserBe(
        username: String,
        roleName: String,
    ): Boolean

    abstract fun canUserBeCache(
        username: String,
        roleName: String,
    ): Boolean

    /**
     * Return the set of roles, along with the
     * outcome code (if an error occurred, the
     * set of roles will be null)
     */
    abstract fun getRoles(
        status: Array<TupleStatus>? = null
    ): CodeRoles

    /**
     * Return the set of resources, along with the
     * outcome code (if an error occurred, the
     * set of resources will be null)
     */
    abstract fun getResources(
        status: Array<TupleStatus>? = null
    ): CodeResources

    /**
     * Return the user-role assignments filtering
     * by the [username] and [roleName], if given,
     * along with the outcome code (if an error
     * occurred, the set of user-role assignments will be
     * null)
     */
    abstract fun getUsersRoles(
        username: String? = null,
        roleName: String? = null,
        status: Array<TupleStatus>? = null
    ): CodeUsersRoles

    /**
     * Return the role-resource permissions filtering
     * by the [username], [roleName] and [resourceName],
     * if given, along with the outcome code (if an error
     * occurred, the set of role-permission assignments
     * will be null)
     */
    abstract fun getRolesPermissions(
        username: String? = null,
        roleName: String? = null,
        resourceName: String? = null,
        status: Array<TupleStatus>? = null
    ): CodeRolesPermissions
}
