package cryptoac.code

import cryptoac.OutcomeCode

/** Wrapper binding a [code] with a [boolean] value */
data class CodeBoolean(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val boolean: Boolean = true
)
