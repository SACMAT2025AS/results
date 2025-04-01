package cryptoac.rm.model

import cryptoac.tuple.Resource
import kotlinx.serialization.Serializable

/**
 * Wrapper for binding together an updated [resource],
 * the [username] making the request, and the [roleName]
 * assumed by [username]
 */
@Serializable
data class WriteResourceRBACRequest(
    val username: String,
    val roleName: String,
    val resource: Resource,
)
