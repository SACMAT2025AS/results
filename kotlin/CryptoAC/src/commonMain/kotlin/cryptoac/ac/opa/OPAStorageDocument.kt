package cryptoac.ac.opa

import cryptoac.OutcomeCode

/** Wrapper binding a [code] with an [opaDocument] */
data class OPAStorageDocument(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val opaDocument: OPADocument? = null,
)
