package cryptoac.code

import cryptoac.OutcomeCode
import cryptoac.parameters.ServiceParameters
import kotlinx.serialization.Serializable

/** Wrapper binding a [code] with some [serviceParameters] */
@Serializable
data class CodeServiceParameters(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val serviceParameters: ServiceParameters? = null
)