package cryptoac.tuple

import io.ktor.utils.io.core.*
import kotlinx.serialization.Serializable

/**
 * A Attribute is an Element representing
 * an attribute in an access control policy
 */
@Serializable
class Attribute(
    override val name: String,
    override val status: TupleStatus = TupleStatus.OPERATIONAL,
    val versionNumber: Int = 1,
) : Element() {

    init {
        requirePositiveNumber(versionNumber)
    }
    override var token = generateRandomTokenForAdmin(name)

    override fun getBytesForSignature(): ByteArray = (
            "$name$status$versionNumber$token"
            ).toByteArray()

    override fun toArray(): Array<String> = arrayOf(
        name,
        status.toString(),
        versionNumber.toString(),
        token
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Attribute) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false
        if (status != other.status) return false
        if (versionNumber != other.versionNumber) return false
        if (token != other.token) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + versionNumber
        result = 31 * result + token.hashCode()
        return result
    }

    override fun toString(): String {
        return "Attribute name:$name, status:$status, " +
        "token:$token, versionNumber:$versionNumber"
    }
}
