package cryptoac.tuple

import kotlinx.serialization.Serializable

/**
 * A [Tuple] is an element of any set of an access control policy
 * state such as the set of users, attributes, roles, resources,
 * user-role assignments, role-permission assignments,
 * user-attribute assignments, and access structure-permission
 * assignments.
 * A [Tuple] may have a digital [signature] for integrity check
 * and the identifier of the [signer] who created the signature
 */
@Serializable
abstract class Tuple {
    var signature: ByteArray? = null
    var signer: String? = null
    abstract val status: TupleStatus

    companion object {
        /**
         * Ensure that the [number] is strictly positive,
         * otherwise throw an IllegalArgumentException
         */
        fun requirePositiveNumber(number: Int) {
            if (number <= 0) {
                throw IllegalArgumentException("Given zero or negative version number $number")
            }
        }
    }

    /**
     * Return the concatenation of all identifying
     * fields of this tuple for computing the [signature]
     */
    abstract fun getBytesForSignature(): ByteArray

    /**
     * Update the [signature] and the [signer] with
     * the [newSignature] and the user [newSigner]
     */
    open fun updateSignature(
        newSignature: ByteArray,
        newSigner: String,
    ) {
        signature = newSignature
        signer = newSigner
    }

    /**
     * Return a String array of the significant
     * fields of this CryptoAC object
     */
    abstract fun toArray(): Array<String>

    /**
     * In this implementation, force subclasses
     * to implement the toString method
     */
    abstract override fun toString(): String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Tuple

        if (signature != null) {
            if (other.signature == null) return false
            if (!signature.contentEquals(other.signature)) return false
        } else if (other.signature != null) return false
        if (signer != other.signer) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = signature?.contentHashCode() ?: 0
        result = 31 * result + (signer?.hashCode() ?: 0)
        result = 31 * result + status.hashCode()
        return result
    }
}
