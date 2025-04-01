package cryptoac.code

import cryptoac.OutcomeCode

/** Wrapper binding a [code] with a set of [strings] */
data class CodeStrings(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val strings: HashSet<String>? = null
)
