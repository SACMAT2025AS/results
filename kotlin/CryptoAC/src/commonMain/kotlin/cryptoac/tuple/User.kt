package cryptoac.tuple

import cryptoac.crypto.AsymKeys
import io.ktor.utils.io.core.*
import kotlinx.serialization.Serializable

/**
 * A User is an Element representing a user in an access control policy.
 * Beside the inherited fields, a User is defined by a boolean [isAdmin] flag
 */
@Serializable
class User(
    override val name: String,
    // override val versionNumber: Int = 1,
    override val status: TupleStatus = TupleStatus.OPERATIONAL,
    val asymEncKeys: AsymKeys? = null, // TODO should this be @Transient?
    val asymSigKeys: AsymKeys? = null, // TODO should this be @Transient?
    val isAdmin: Boolean = false,
) : Element() {

    init {
        // requirePositiveNumber(versionNumber)
    }

    override var token: String = generateRandomTokenForAdmin(name)

    override fun getBytesForSignature(): ByteArray = (
            "$name$status$isAdmin$token" +
                    "${asymEncKeys?.public}${asymEncKeys?.private}${asymEncKeys?.keysType?.name}" +
                    "${asymSigKeys?.public}${asymSigKeys?.private}${asymSigKeys?.keysType?.name}"
            ).toByteArray()

    override fun toArray(): Array<String> = arrayOf(
        name,
        status.toString(),
        // versionNumber.toString(),
        isAdmin.toString(),
        token
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as User

        if (name != other.name) return false
        // if (versionNumber != other.versionNumber) return false
        if (asymEncKeys != other.asymEncKeys) return false
        if (asymSigKeys != other.asymSigKeys) return false
        if (isAdmin != other.isAdmin) return false
        if (token != other.token) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        // result = 31 * result + versionNumber
        result = 31 * result + (asymEncKeys?.hashCode() ?: 0)
        result = 31 * result + (asymSigKeys?.hashCode() ?: 0)
        result = 31 * result + isAdmin.hashCode()
        result = 31 * result + token.hashCode()
        return result
    }

    override fun toString(): String {
        return "User name:$name, status:$status, versionNumber:" +
                "token:$token, isAdmin:$isAdmin"
    }
}
