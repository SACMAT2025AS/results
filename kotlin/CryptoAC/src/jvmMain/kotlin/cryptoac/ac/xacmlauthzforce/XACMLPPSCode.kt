package cryptoac.ac.xacmlauthzforce

import cryptoac.OutcomeCode

/** Wrapper binding a [code] with a [xacmlPPS] */
data class XACMLPPSCode(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val xacmlPPS: XACMLPPS? = null,
)
