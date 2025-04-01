package cryptoac.tuple

import cryptoac.Constants.ADMIN
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * An Element represents a single element (e.g., user,
 * role, attribute, resource) in an access control policy.
 * An Element has a (unique) [name], a [status], a
 * [versionNumber], and a [token] acting as an identifier
 * for anonymization. The [token] should be either random or
 * computed from secret values
 */
@Serializable
abstract class Element : Tuple() {
    abstract val name: String
    // abstract val versionNumber: Int
    abstract var token: String

    companion object {
        /**
         * Generate a random token (string) with the given
         * strictly positive [length] or the default value.
         * The value of the token cannot be [ADMIN]
         */
        fun generateRandomToken(length: Int = 20): String {
            requirePositiveNumber(length)
            val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            var token: String;
            do {
                token = (1..length)
                    .map { Random.nextInt(0, charPool.size) }
                    .map(charPool::get)
                    .joinToString("")
            } while (token == ADMIN)
            return token
        }

        /**
         * Generate a random token (string) with the given
         * strictly positive [length] or the default value.
         * If the [name] is [ADMIN], return [ADMIN] as token
         */
        fun generateRandomTokenForAdmin(name: String, length: Int = 20): String {
            return if (name == ADMIN) {
                ADMIN
            } else {
                generateRandomToken(length)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as Element

        if (name != other.name) return false
        // if (versionNumber != other.versionNumber) return false
        if (token != other.token) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        // result = 31 * result + versionNumber
        result = 31 * result + token.hashCode()
        return result
    }

}
