package cryptoac.code

import cryptoac.OutcomeCode
import cryptoac.tuple.Role

/** Wrapper binding a [code] with a set of [roles] */
data class CodeRoles(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val roles: HashSet<Role>? = null
)
