package cryptoac.tuple

import kotlinx.serialization.Serializable

/**
 * A resource may be protected with one among the following enforcements:
 * - [TRADITIONAL]: the resource is not encrypted and traditional access control is enforced;
 * - [CRYPTOGRAPHIC]: the resource is encrypted and traditional access control is not enforced;
 * - [COMBINED]: the resource is encrypted and traditional access control is enforced.
 */
@Serializable
enum class Enforcement {
    TRADITIONAL, CRYPTOGRAPHIC, COMBINED
}
