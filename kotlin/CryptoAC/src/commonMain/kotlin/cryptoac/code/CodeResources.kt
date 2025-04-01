package cryptoac.code

import cryptoac.OutcomeCode
import cryptoac.tuple.Resource

/** Wrapper binding a [code] with a set of [resources] */
data class CodeResources(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val resources: HashSet<Resource>? = null
)
