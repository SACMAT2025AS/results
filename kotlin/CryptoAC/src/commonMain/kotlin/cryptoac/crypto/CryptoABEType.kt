package cryptoac.crypto

import kotlinx.serialization.Serializable

/**
 * A cryptographic object supporting ABE can be implemented by:
 * - [OPENABE]: native-level implementation of attribute-based encryption
 *              (https://github.com/StefanoBerlato/kotlin-multiplatform-openabe).
 */
@Serializable
enum class CryptoABEType {
    OPENABE,
}
