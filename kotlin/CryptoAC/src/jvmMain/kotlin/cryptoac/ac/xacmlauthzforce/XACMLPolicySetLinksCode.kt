package cryptoac.ac.xacmlauthzforce

import cryptoac.OutcomeCode

/** Wrapper binding a [code] with a set of [policySetsLinks] */
data class XACMLPolicySetLinksCode(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val policySetsLinks: List<XACMLPolicySetLink>? = null,
)
