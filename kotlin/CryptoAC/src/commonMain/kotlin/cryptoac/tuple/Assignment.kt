package cryptoac.tuple

import kotlinx.serialization.Serializable

/**
 * An [Assignment] is a [Tuple] representing an assignment between
 * (usually two) elements (e.g., user-role assignment, role-permission
 * assignment, user-attribute assignment, access structure-permission
 * assignment) with additional metadata (besides the [signature] and
 * the [signer])
 */
@Serializable
abstract class Assignment : Tuple()
