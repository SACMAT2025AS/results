package cryptoac.code

import cryptoac.OutcomeCode

/** Wrapper binding a [code] with a [string] */
data class CodeString(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val string: String? = null
)
