package cryptoac.code

import cryptoac.OutcomeCode
import cryptoac.crypto.SymmetricKeyCryptoAC

/** Wrapper binding a [code] with a [symmetricKeyCryptoAC] */
data class CodeSymmetricKey(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val symmetricKeyCryptoAC: SymmetricKeyCryptoAC? = null,
)
