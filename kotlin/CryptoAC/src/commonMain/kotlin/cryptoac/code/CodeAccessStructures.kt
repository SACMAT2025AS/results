package cryptoac.code

import cryptoac.OutcomeCode

/** Wrapper binding a [code] with a set of [accessStructures] as strings */
data class CodeAccessStructures(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val accessStructures: HashSet<String>? = null
)