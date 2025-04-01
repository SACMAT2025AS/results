package cryptoac.code

import cryptoac.OutcomeCode
import cryptoac.tuple.Predicate
import cryptoac.tuple.UserRole

/** Wrapper binding a [code] with a list of pairs of strings */
data class CodeElementsPredicates(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val elementsPredicates: List<Pair<String, Predicate>>? = null
)
