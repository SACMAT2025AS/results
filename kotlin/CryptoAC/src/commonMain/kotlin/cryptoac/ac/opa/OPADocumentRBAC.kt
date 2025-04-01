package cryptoac.ac.opa

import cryptoac.tuple.Operation
import kotlinx.serialization.Serializable

/**
 * Describe the RBAC document for OPA:
 * - [allow] is the evaluation of the "allow" policy;
 * - [randomID] is a random string acting as ID;
 * - [ur] is the list of UR assignments for
 *   each user. The key is the name of the user,
 *   the value is the list of roles assigned to
 *   the user;
 * - [pa] is the list of PA assignments for
 *   each role. The key is the name of the role,
 *   the value is the list of permissions assigned to
 *   the role.
 */
@Serializable
data class OPADocumentRBAC(
    var allow: Boolean? = null,
    val randomID: String? = null,
    val ur: HashMap<String, HashSet<String>>,
    val pa: HashMap<String, HashSet<OPAPermission>>,
) {

    /** Delete [username]'s assignments from PA */
    fun deleteUser(username: String) {
        ur.remove(username)
    }

    /** Delete [roleName]'s assignments from UR and PA */
    fun deleteRole(roleName: String) {
        ur.forEach {
            it.value.remove(roleName)
        }
        pa.remove(roleName)
    }

    /**
     * Delete [resourceName]'s assignments from PA
     */
    fun deleteResource(resourceName: String) {
        pa.forEach {
            it.value.removeAll { permission ->
                permission.resource == resourceName
            }
        }
    }

    /** Add ([username], [roleName]) to UR */
    fun assignUserToRole(
        username: String,
        roleName: String
    ) {
        ur.getOrPut(username) {
            hashSetOf()
        }.add(roleName)
    }

    /** Delete ([username], [roleName]) from UR */
    fun revokeUserFromRole(
        username: String,
        roleName: String
    ) {
        ur[username]?.remove(roleName)
    }

    /**
     * Add ([roleName], ([operation],
     * [resourceName])) to PA
     */
    fun assignPermissionToRole(
        roleName: String,
        resourceName: String,
        operation: Operation
    ) {
        pa.getOrPut(roleName) {
            hashSetOf()
        }.add(
            OPAPermission(
                resource = resourceName,
                operation = operation
            ))
    }

    /** Delete ([roleName], (-, [resourceName])) from PA */
    fun revokePermissionFromRole(
        roleName: String,
        resourceName: String
    ) {
        pa[roleName]?.removeAll {
            it.resource == resourceName
        }
    }

    /** Return the set of users assigned to [roleName] */
    fun getUsersAssignedToRole(roleName: String): HashSet<String> {
        val users = hashSetOf<String>()
        ur.forEach {
            if (it.value.contains(roleName)) {
                users.add(it.key)
            }
        }
        return users
    }

    /**
     * Return the permissions assigned to [roleName] in a
     * map with as key the resource name and as value the
     * list of operations
     */
    fun getPermissionsAssignedToRole(
        roleName: String
    ): HashMap<String, HashSet<Operation>> {
        val permissionsByResource = HashMap<String, HashSet<Operation>>()
        pa[roleName]?.forEach {
            permissionsByResource.getOrPut(
                it.resource
            ){
                hashSetOf()
            }.add(it.operation)
        }
        return permissionsByResource
    }
}