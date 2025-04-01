package cryptoac.code

import cryptoac.OutcomeCode
import cryptoac.tuple.User

/** Wrapper binding a [code] with a set of [users] */
data class CodeUsers(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val users: HashSet<User>? = null
)
