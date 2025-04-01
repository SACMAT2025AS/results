package cryptoac.code

import cryptoac.OutcomeCode
import cryptoac.tuple.Attribute

/** Wrapper binding a [code] with a set of [attributes] */
data class CodeAttributes(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val attributes: HashSet<Attribute>? = null
)