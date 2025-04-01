package cryptoac.code

import cryptoac.OutcomeCode
import cryptoac.tuple.UserRole

/** Wrapper binding a [code] with a set of [usersRoles] */
data class CodeUsersRoles(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val usersRoles: HashSet<UserRole>? = null
)
