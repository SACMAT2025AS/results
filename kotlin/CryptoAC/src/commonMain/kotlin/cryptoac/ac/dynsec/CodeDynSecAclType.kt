package cryptoac.ac.dynsec

import cryptoac.OutcomeCode

/** Wrap a list of [DynSecAclType] with an outcome code */
data class CodeDynSecAclType(
    val acls: List<DynSecAclType>? = null,
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
)