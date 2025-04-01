package cryptoac.rm.cryptoac

import cryptoac.ac.ACServiceParameters
import cryptoac.crypto.CryptoType
import cryptoac.dm.cryptoac.DMServiceCryptoACParameters
import cryptoac.mm.MMServiceParameters
import kotlinx.serialization.Serializable

/**
 * Wrapper for binding together a [crypto] type, [mmServiceParameters],
 * [dmServiceCryptoACParameters] and [ACServiceParameters], if given
 */
@Serializable
data class RMRBACCryptoACParameters(
    val crypto: CryptoType,
    val mmServiceParameters: MMServiceParameters,
    val acServiceParameters: ACServiceParameters? = null,
    val dmServiceCryptoACParameters: DMServiceCryptoACParameters,
)
