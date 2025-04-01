package cryptoac.ac.opa

import cryptoac.tuple.Operation
import kotlinx.serialization.Serializable

/**
 * Describe an OPA permission:
 * - [resource] the name of the resource;
 * - [operation] the operation.
 */
@Serializable
data class OPAPermission(
    val resource: String,
    val operation: Operation,
)