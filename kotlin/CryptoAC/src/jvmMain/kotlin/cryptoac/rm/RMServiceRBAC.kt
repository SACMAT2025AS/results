package cryptoac.rm

import cryptoac.OutcomeCode
import cryptoac.tuple.RolePermission
import cryptoac.tuple.Resource

/**
 * Interface defining the methods for invoking the APIs of
 * an RM entity providing the functions of the RM for RB-CAC
 */
interface RMServiceRBAC : RMService {

    /**
     * Invoke the RM to validate the creation of
     * a new resource involving the given
     * [newResource] and [adminRolePermission]
     * and return the outcome code
     */
    fun checkAddResource(
        newResource: Resource,
        adminRolePermission: RolePermission
    ): OutcomeCode

    /**
     * Invoke the RM to validate the update of
     * a resource using the given [roleName] and
     * involving the given [newResource] and
     * [symKeyVersionNumber], and return the
     * outcome code
     */
    fun checkWriteResource(
        roleName: String,
        symKeyVersionNumber: Int,
        newResource: Resource
    ): OutcomeCode
}