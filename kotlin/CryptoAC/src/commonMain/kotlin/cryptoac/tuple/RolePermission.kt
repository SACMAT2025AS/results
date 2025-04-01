package cryptoac.tuple

import cryptoac.crypto.EncryptedSymKey
import io.ktor.utils.io.core.*
import kotlinx.serialization.Serializable

/**
 * A RolePermission is an assignment representing an assignment between
 * a role and a permission in an access control policy. Beside the inherited
 * fields, a RolePermission is defined by a [roleName], a [resourceName],
 * a [roleToken], a [resourceToken], an [operation] type, two positive version
 * numbers ([roleVersionNumber], [resourceVersionNumber]) and an encrypted
 * symmetric key ([encryptedSymKey])
 */
@Serializable
class RolePermission(
    val roleName: String,
    val resourceName: String,
    val roleToken: String,
    val resourceToken: String,
    val operation: Operation,
    val encryptedSymKey: EncryptedSymKey? = null,
    val roleVersionNumber: Int = 1,
    val resourceVersionNumber: Int = 1,
    override val status: TupleStatus = TupleStatus.OPERATIONAL,
) : Assignment() {

    init {
        requirePositiveNumber(roleVersionNumber)
        requirePositiveNumber(resourceVersionNumber)
    }

    override fun getBytesForSignature(): ByteArray =
        ("$roleName$resourceName$roleToken$resourceToken" +
         "$operation$roleVersionNumber$resourceVersionNumber$status" +
        encryptedSymKey?.key.contentToString()).toByteArray()

    override fun toArray(): Array<String> = arrayOf(
        roleName,
        resourceName,
        resourceVersionNumber.toString(),
        operation.toString()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as RolePermission

        if (roleName != other.roleName) return false
        if (resourceName != other.resourceName) return false
        if (roleToken != other.roleToken) return false
        if (resourceToken != other.resourceToken) return false
        if (operation != other.operation) return false
        if (encryptedSymKey != other.encryptedSymKey) return false
        if (roleVersionNumber != other.roleVersionNumber) return false
        if (resourceVersionNumber != other.resourceVersionNumber) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + roleName.hashCode()
        result = 31 * result + resourceName.hashCode()
        result = 31 * result + roleToken.hashCode()
        result = 31 * result + resourceToken.hashCode()
        result = 31 * result + operation.hashCode()
        result = 31 * result + (encryptedSymKey?.hashCode() ?: 0)
        result = 31 * result + roleVersionNumber
        result = 31 * result + resourceVersionNumber
        result = 31 * result + status.hashCode()
        return result
    }

    override fun toString(): String {
        return "RolePermission roleName:$roleName, " +
                "resourceName:$resourceName, " +
                "roleToken:$roleToken, resourceToken$resourceToken, " +
                "permission:$operation, roleVersionNumber:$roleVersionNumber, " +
                "resourceVersionNumber:$resourceVersionNumber"
    }
}
