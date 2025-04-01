package cryptoac.rm.model

import cryptoac.tuple.Resource
import cryptoac.tuple.RolePermission
import kotlinx.serialization.Serializable

/**
 * Wrapper for binding together a [resource] and a
 * [rolePermission] giving the admin access to
 * the resource
 */
@Serializable
data class AddResourceRBACRequest(
    val resource: Resource,
    val rolePermission: RolePermission,
)