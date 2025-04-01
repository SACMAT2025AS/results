package cryptoac.ac.dynsec

import kotlinx.serialization.Serializable

/**
 * Describe the response received when sending
 * the 'listUsers' command to DynSec
 */
@Serializable
data class DynSecListUsersResponse(
    val responses: ArrayList<DynSecListUsersDataResponse>
)

@Serializable
data class DynSecListUsersDataResponse(
    val command: String,
    val data: DynSecUsersData
)

@Serializable
data class DynSecUsersData(
    val totalCount: Int,
    val clients: ArrayList<String>
)
