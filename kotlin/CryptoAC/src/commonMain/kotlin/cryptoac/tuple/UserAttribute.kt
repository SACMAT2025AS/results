//package cryptoac.tuple
//
//import io.ktor.utils.io.core.*
//
///**
// * A UserAttribute is an assignment representing an assignment between
// * a user and an attribute in an access control policy. Beside the inherited
// * fields, a UserAttribute is defined by a [username] and an
// * [attributeName] with an optional [attributeValue]
// */
////@Serializable TODO decomment (this caused a weird error at compile-time, check whether the error is still there or was resolved (e.g., with an update)
//class UserAttribute(
//    val username: String,
//    val attributeName: String,
//    val attributeValue: String? = null,
//) : Assignment() {
//
//    override fun getBytesForSignature(): ByteArray = (
//            "$username$attributeName${attributeValue ?: ""}"
//            ).toByteArray()
//
//    override fun toArray(): Array<String> = arrayOf(
//        username,
//        attributeName,
//        attributeValue ?: ""
//    )
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other !is UserAttribute) return false
//        if (!super.equals(other)) return false
//
//        if (username != other.username) return false
//        if (attributeName != other.attributeName) return false
//        if (attributeValue != other.attributeValue) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = super.hashCode()
//        result = 31 * result + username.hashCode()
//        result = 31 * result + attributeName.hashCode()
//        result = 31 * result + (attributeValue?.hashCode() ?: 0)
//        return result
//    }
//
//    override fun toString(): String {
//        return "UserAttribute username:$username, " +
//                "attributeName:$attributeName, " +
//                "attributeValue:$attributeValue"
//    }
//}
