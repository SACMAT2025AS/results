package cryptoac.ac.xacmlauthzforce

import cryptoac.OutcomeCode

/** Wrapper binding a [code] with a [xacmlREPS] */
data class XACMLREPSCode(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val xacmlREPS: XACMLREPS? = null,
)
