package cryptoac.ac

import cryptoac.OutcomeCode
import cryptoac.Service
import cryptoac.tuple.Operation

/**
 * Interface defining the methods for invoking the APIs of
 * an AC entity providing the functions of a traditional
 * access control enforcement mechanism
 */
interface ACService : Service {

    /**
     * Check whether the [username] has the permission
     * to perform the [operation] over the [resourceName],
     * and return either CODE_000_SUCCESS (if the user is
     * authorized) or CODE_037_FORBIDDEN (if the user is
     * not authorized). The [operation] can be either READ or WRITE
     */
    fun canDo(
        username: String,
        operation: Operation,
        resourceName: String,
    ): OutcomeCode
}
