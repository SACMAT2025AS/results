package cryptoac.code

import cryptoac.OutcomeCode
import java.io.InputStream

/** Wrapper binding a [code] with a [stream] */
data class CodeResource(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val stream: InputStream? = null
)
