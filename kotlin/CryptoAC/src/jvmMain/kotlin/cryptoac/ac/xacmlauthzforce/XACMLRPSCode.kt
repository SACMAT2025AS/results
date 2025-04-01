package cryptoac.ac.xacmlauthzforce

import cryptoac.OutcomeCode

/** Wrapper binding a [code] with a [xacmlRPS] */
data class XACMLRPSCode(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val xacmlRPS: XACMLRPS? = null,
)
