package cryptoac.ac.xacmlauthzforce

import cryptoac.OutcomeCode

/** Wrapper binding a [code] with a [xacmlPolicySetRoot] */
data class XACMLPolicySetRootCode(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val xacmlPolicySetRoot: XACMLPolicySetRoot? = null,
)
