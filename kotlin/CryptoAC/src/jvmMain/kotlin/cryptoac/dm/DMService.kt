package cryptoac.dm

import cryptoac.OutcomeCode
import cryptoac.Service
import cryptoac.code.CodeResource
import cryptoac.tuple.Resource
import java.io.InputStream

/**
 * Interface defining the methods for invoking the APIs of
 * a DM entity providing the functions of the DM
 */
interface DMService : Service {

    /**
     * Create a [newResource], possibly initializing it
     * with the given [resourceContent] in the DM and return
     * the outcome code. Note that the version number of the
     * resource (which should be stored as a metadata of the
     * [resourceContent]) should be 1
     */
    fun addResource(
        newResource: Resource,
        resourceContent: InputStream
    ): OutcomeCode

    /**
     * Require read access to the [resourceName], possibly returning
     * an input stream from the DM along with the outcome code.
     * This function should return the content of the resource
     * corresponding to the [resourceVersionNumber]
     */
    fun readResource(
        resourceName: String,
        // resourceVersionNumber: Int
    ): CodeResource

    /**
     * Require write access to the resource [updatedResource],
     * writing the [resourceContent] in the DM and return the outcome code.
     * Note that the version number of the resource should be stored
     * as a metadata of the [resourceContent]
     */
    fun writeResource(
        updatedResource: Resource,
        resourceContent: InputStream
    ): OutcomeCode

    /**
     * Delete the content of the [resourceName] from the DM
     * and return the outcome code. This function should
     * delete the content of the resource corresponding to the
     * [resourceVersionNumber]
     */
    fun deleteResource(
        resourceName: String
    ): OutcomeCode
}