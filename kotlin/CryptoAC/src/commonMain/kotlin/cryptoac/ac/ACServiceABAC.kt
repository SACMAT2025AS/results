package cryptoac.ac

import cryptoac.OutcomeCode
import cryptoac.tuple.Operation

/**
 * Interface defining the methods for invoking the APIs of
 * an AC entity providing the functions of a traditional
 * attribute-based access control enforcement mechanism
 */
interface ACServiceABAC : ACService {

    /**
     * The status of an ABAC policy is composed of the following sets:
     * U: set of users;
     * A: set of attributes;
     * P: set of resources;
     * UA: set of user-attribute assignments;
     * AP: set of access structure-permission assignments.
     */

    /** Add [attributeName] to A */
    fun addAttribute(
        attributeName: String
    ): OutcomeCode?

    /**
     * Delete [attributeName] from A and
     * [attributeName]'s assignments from
     * UA and AP
     */
    fun deleteAttribute(
        attributeName: String
    ): OutcomeCode?

    /** Add [resourceName] to P */
    fun addResource(
        resourceName: String
    ): OutcomeCode

    /**
     * Delete [resourceName] from P and
     * [resourceName]'s assignments from AP
     */
    fun deleteResource(
        resourceName: String
    ): OutcomeCode?

    /**
     * Add ([username], attribute) to UA
     * for each attribute in [attributes]
     */
    fun assignUserToAttributes(
        username: String,
        attributes: HashSet<String>
    ): OutcomeCode

    /**
     * Remove ([username], attribute) from UA
     * for each attribute in [attributes]
     */
    fun revokeUserFromAttributes(
        username: String,
        attributes: HashSet<String>
    ): OutcomeCode

    /**
     * Add ([accessStructure], ([operation],
     * [resourceName])) to AP
     */
    fun assignPermissionToAccessStructure(
        resourceName: String,
        accessStructure: String,
        operation: Operation
    ): OutcomeCode

    /** Delete ([accessStructure], (-, [resourceName])) from AP */
    fun revokePermissionFromAccessStructure(
        resourceName: String,
        accessStructure: String,
    ): OutcomeCode

    /**
     * Replace (-, (-, [resourceName])) with
     * ([newAccessStructure], (-, [resourceName]))
     * in AP
     */
    fun updateAccessStructureToPermission(
        resourceName: String,
        newAccessStructure: String
    ): OutcomeCode?
}
