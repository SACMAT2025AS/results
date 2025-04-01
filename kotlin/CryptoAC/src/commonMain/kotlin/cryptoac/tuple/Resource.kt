package cryptoac.tuple

import cryptoac.crypto.EncryptedSymKey
import io.ktor.utils.io.core.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A Resource is an Element representing a resource in an access control policy.
 * Beside the [signature] and the [signer], and the (unique) [name], the
 * [status], the [versionNumber], and the [token], a Resource is defined by a
 * [reEncryptionThresholdNumber], an encrypted symmetric key [encryptedSymKey], and
 * an [enforcement] type
 */
@Serializable
class Resource(
    override val name: String,
    override val status: TupleStatus = TupleStatus.OPERATIONAL,
    val versionNumber: Int = 1,
    // val reEncryptionThresholdNumber: Int = 1,
    // val enforcement: Enforcement,
    @Transient val encryptedSymKey: EncryptedSymKey? = null,
) : Element() {

    init {
        requirePositiveNumber(versionNumber)
        // requirePositiveNumber(reEncryptionThresholdNumber)
    }

    override var token = generateRandomToken()

    override fun getBytesForSignature(): ByteArray = (
            "$name$status$versionNumber$token" +
                    //"$reEncryptionThresholdNumber$enforcement" +
                    encryptedSymKey?.key.contentToString()
            ).toByteArray()

    override fun toArray(): Array<String> = arrayOf(
        name,
        status.toString(),
        versionNumber.toString(),
       // reEncryptionThresholdNumber.toString(),
        token,
        // enforcement.toString(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as Resource

        if (name != other.name) return false
        if (status != other.status) return false
        if (versionNumber != other.versionNumber) return false
      //  if (reEncryptionThresholdNumber != other.reEncryptionThresholdNumber) return false
       //  if (enforcement != other.enforcement) return false
        if (encryptedSymKey != other.encryptedSymKey) return false
        if (token != other.token) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + versionNumber
    //    result = 31 * result + reEncryptionThresholdNumber
      //   result = 31 * result + enforcement.hashCode()
        result = 31 * result + (encryptedSymKey?.hashCode() ?: 0)
        result = 31 * result + token.hashCode()
        return result
    }

    override fun toString(): String {
        return "Resource name:$name, status:$status, " +
                "token:$token, versionNumber:$versionNumber"
                //"reEncryptionThresholdNumber:$reEncryptionThresholdNumber, " +
         //        "enforcement:$enforcement"
    }
}
