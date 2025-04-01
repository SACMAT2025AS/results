package cryptoac.code

import cryptoac.OutcomeCode
import cryptoac.core.CoreParameters
import kotlinx.serialization.Serializable

/** Wrapper binding a [code] with some [coreParameters] */
@Serializable
data class CodeCoreParameters(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val coreParameters: CoreParameters? = null
)
