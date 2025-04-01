//package cryptoac.tuple
//
//import cryptoac.crypto.EncryptedSymKey
//import io.ktor.utils.io.core.*
//import kotlinx.serialization.Serializable
//
///**
// * A AccessStructurePermission is an assignment representing an
// * assignment between an access structure and a permission in an access
// * control policy. Beside the inherited fields, a
// * AccessStructurePermission is defined by a [resourceName], a
// * [resourceToken], an [accessStructure], an [operation], an [encryptedSymKey]
// * and a [resourceVersionNumber]
// */
//@Serializable
//class AccessStructurePermission(
//    val resourceName: String,
//    val resourceToken: String,
//    var accessStructure: String,
//    val operation: Operation,
//    var encryptedSymKey: EncryptedSymKey? = null,
//    val resourceVersionNumber: Int = 1,
//) : Assignment() {
//
//    override fun getBytesForSignature(): ByteArray =
//        ("$resourceName$resourceToken$accessStructure" +
//         "$operation$resourceVersionNumber" +
//         encryptedSymKey?.key.contentToString()).toByteArray()
//
//    override fun toArray(): Array<String> = arrayOf(
//        resourceName,
//        accessStructure,
//        operation.toString(),
//        resourceVersionNumber.toString()
//    )
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other == null || this::class != other::class) return false
//        if (!super.equals(other)) return false
//
//        other as AccessStructurePermission
//
//        if (resourceName != other.resourceName) return false
//        if (resourceToken != other.resourceToken) return false
//        if (accessStructure != other.accessStructure) return false
//        if (operation != other.operation) return false
//        if (encryptedSymKey != other.encryptedSymKey) return false
//        if (resourceVersionNumber != other.resourceVersionNumber) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = super.hashCode()
//        result = 31 * result + resourceName.hashCode()
//        result = 31 * result + resourceToken.hashCode()
//        result = 31 * result + accessStructure.hashCode()
//        result = 31 * result + operation.hashCode()
//        result = 31 * result + (encryptedSymKey?.hashCode() ?: 0)
//        result = 31 * result + resourceVersionNumber
//        return result
//    }
//
//    override fun toString(): String {
//        return "AccessStructurePermission resourceName:$resourceName, " +
//                "resourceToken:$resourceToken, accessStructure:$accessStructure, " +
//                "permission:$operation, resourceVersionNumber:$resourceVersionNumber"
//    }
//}
