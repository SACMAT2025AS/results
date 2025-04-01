package cryptoac.tuple

import cryptoac.crypto.EncryptedAsymKeys
import io.ktor.utils.io.core.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A UserRole is an assignment representing an assignment between
 * a user and a role in an access control policy. Beside the inherited
 * fields, a UserRole is defined by a [username], a [roleName],
 * a positive [userVersionNumber], a positive [roleVersionNumber] and 2
 * encrypted key pairs: [encryptedAsymEncKeys] and [encryptedAsymSigKeys]
 */
@Serializable
class UserRole(
    val username: String,
    val roleName: String,
    // val userVersionNumber: Int = 1,
    val roleVersionNumber: Int = 1,
    override val status: TupleStatus = TupleStatus.OPERATIONAL,
    @Transient val encryptedAsymEncKeys: EncryptedAsymKeys? = null,
    @Transient val encryptedAsymSigKeys: EncryptedAsymKeys? = null
) : Assignment() {

    init {
        // requirePositiveNumber(userVersionNumber)
        requirePositiveNumber(roleVersionNumber)
    }

    override fun getBytesForSignature(): ByteArray = (
            "$username$roleName$roleVersionNumber$status" +
                    "${encryptedAsymEncKeys.hashCode()}${encryptedAsymSigKeys.hashCode()}"
            ).toByteArray()

    override fun toArray(): Array<String> = arrayOf(
        username,
        roleName,
        status.toString(),
        roleVersionNumber.toString()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as UserRole

        if (username != other.username) return false
        if (roleName != other.roleName) return false
        if (status != other.status) return false
        if (roleVersionNumber != other.roleVersionNumber) return false
        if (encryptedAsymEncKeys != other.encryptedAsymEncKeys) return false
        if (encryptedAsymSigKeys != other.encryptedAsymSigKeys) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + roleName.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + roleVersionNumber
        result = 31 * result + (encryptedAsymEncKeys?.hashCode() ?: 0)
        result = 31 * result + (encryptedAsymSigKeys?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "UserRole username:$username, roleName:$roleName, " +
                "status:$status, " +
                "roleVersionNumber:$roleVersionNumber"
    }
}
