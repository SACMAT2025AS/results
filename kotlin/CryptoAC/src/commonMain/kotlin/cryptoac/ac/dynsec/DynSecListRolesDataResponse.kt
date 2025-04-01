package cryptoac.ac.dynsec

import kotlinx.serialization.Serializable

/**
 * Describe the response received when sending
 * the 'listRoles' command to DynSec
 */
@Serializable
data class DynSecListRolesResponse(
    val responses: ArrayList<DynSecListRolesDataResponse>
)

@Serializable
data class DynSecListRolesDataResponse(
    val command: String,
    val data: DynSecRolesData
)

@Serializable
data class DynSecRolesData(
    val totalCount: Int,
    val roles: ArrayList<String>
)
