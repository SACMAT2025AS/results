package cryptoac.crypto

import kotlinx.serialization.Serializable

/**
 * A pair of asymmetric cryptographic keys can be:
 * - [ENC]: encryption and decryption of data;
 * - [SIG]: signatures and verifications of data.
 */
@Serializable
enum class AsymKeysType {
    ENC, SIG
}