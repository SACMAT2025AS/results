package cryptoac.tuple

import cryptoac.crypto.AsymKeys
import io.ktor.utils.io.core.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


/**
 * A Role is an Element representing a role in an access control policy.
 * Beside the inherited fields, a Role is defined by a pair of [asymEncKeys]
 * and a pair of [asymSigKeys]
 */
@Serializable
class Role(
    override val name: String,
    val versionNumber: Int = 1,
    @Transient val asymEncKeys: AsymKeys? = null,
    @Transient val asymSigKeys: AsymKeys? = null,
    override val status: TupleStatus = TupleStatus.OPERATIONAL,
) : Element() {

    init {
        requirePositiveNumber(versionNumber)
    }

    override var token: String = generateRandomTokenForAdmin(name)

    override fun getBytesForSignature(): ByteArray = (
            "$name$status$versionNumber$token" +
                    "${asymEncKeys?.public}${asymEncKeys?.private}${asymEncKeys?.keysType?.name}" +
                    "${asymSigKeys?.public}${asymSigKeys?.private}${asymSigKeys?.keysType?.name}"
            ).toByteArray()

    override fun toArray(): Array<String> = arrayOf(
        name,
        status.toString(),
        versionNumber.toString(),
        token
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as Role

        if (name != other.name) return false
        if (status != other.status) return false
        if (versionNumber != other.versionNumber) return false
        if (asymEncKeys != other.asymEncKeys) return false
        if (asymSigKeys != other.asymSigKeys) return false
        if (token != other.token) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + versionNumber
        result = 31 * result + (asymEncKeys?.hashCode() ?: 0)
        result = 31 * result + (asymSigKeys?.hashCode() ?: 0)
        result = 31 * result + token.hashCode()
        return result
    }

    override fun toString(): String {
        return "Role name:$name, status:$status, versionNumber:" +
                "$versionNumber, token:$token"
    }
}
