package cryptoac

import cryptoac.Constants.DEFAULT_LIMIT
import cryptoac.Constants.DEFAULT_OFFSET
import cryptoac.core.CoreParameters
import cryptoac.code.CodeBoolean
import cryptoac.code.CodeServiceParameters
import cryptoac.tuple.TupleStatus
import cryptoac.tuple.User

/**
 * Interface defining the methods for invoking the APIs of an entity.
 * Such methods include the lock-unlock-rollback methods to avoid
 * inconsistent statuses. A service should be locked when in use
 * for an operation, and then either unlocked or rollbacked when
 * the operation concluded successfully or not, respectively
 */
interface Service {

    /**
     * The number of locks for the
     * lock-rollback-unlock mechanism
     */
    var locks: Int



    /**
     * Check whether the service was
     * already configured (at least once)
     */
    fun alreadyConfigured(): CodeBoolean

    /**
     * This function is invoked after the 'init' function to
     * configure the service with the given [parameters]
     * and return the outcome code. When implementing this
     * function, remember to decide how to handle (e.g.,
     * reject or allow) subsequent invocations
     */
    fun configure(
        parameters: CoreParameters? = null
    ): OutcomeCode

    /**
     * This function is invoked after the 'lock' function once after
     * the (interface toward the) service is created, and it should
     * contain the code to initialize the interface (e.g., possibly
     * connect to remote services like MQTT brokers, databases, etc.)
     */
    fun init() // TODO should we return the code in case of error?

    /**
     * This function is invoked after the 'unlock' or 'rollback'
     * function once just before the (interface toward the) service
     * is destroyed, and it should contain the code to de-initialize
     * the interface (e.g., possibly disconnect from remote services
     * like MQTT brokers, databases, etc.)
     */
    fun deinit() // TODO should we return the code in case of error?

    /**
     * Add (and initialize) the [newAdmin] in the
     * service and return the outcome code. Check that
     * the name of the admin is the expected one and
     * that the admin was not already added (or initialized)
     */
    fun addAdmin(
        newAdmin: User
    ): OutcomeCode

     /**
     * Initialize the [user] in the service and
     * return the outcome code. Check that the user
     * is present and was not already initialized or
     * deleted. This method should support invocations
     * by non-admin users
     */
    fun initUser(
         user: User
    ): OutcomeCode

    /**
     * Add the [newUser] in the service by, e.g.,
     * creating an account for the user. Check that
     * the user was not already added. Finally,
     * return the user's configuration parameters
     * together with the outcome code
     */
    fun addUser(
        newUser: User
    ): CodeServiceParameters

    /**
     * Retrieve the users matching the specified
     * [username] and [status], if given, starting from
     * the [offset] limiting the number of users to return
     * to the given [limit] and with the (possibly) relevant
     * information of whether the user invoking this function
     * [isAdmin]. If no users are found, return an empty set.
     * This method should support invocations by non-admin users
     */
    fun getUsers(
        username: String? = null,
        status: TupleStatus? = null,
        isAdmin: Boolean = true,
        offset: Int = DEFAULT_OFFSET,
        limit: Int = DEFAULT_LIMIT,
    ): HashSet<User>

    /**
     * Delete [username] from the service. Check
     * that the user exists
     */
    fun deleteUser(
        username: String
    ): OutcomeCode



    /**
     * Signal the start of a new atomic transaction so to rollback to the
     * previous status in case of errors. As this method could be invoked
     * multiple times before committing or rollbacking the transactions,
     * increment the number of [locks] by 1 at each invocation, effectively
     * starting a new transaction only when [locks] is 0. Finally, return
     * the outcome code
     */
    fun lock(): OutcomeCode

    /**
     * Signal an error during an atomic transaction so to restore the
     * previous status. As this method could be invoked multiple times,
     * decrement the number of [locks] by 1 at each invocation, effectively
     * rollback to the previous status only when [locks] becomes 0.
     * Finally, return the outcome code
     */
    fun rollback(): OutcomeCode

    /**
     * Signal the end of an atomic transaction so commit the changes.
     * As this method could be invoked multiple times, decrement the
     * number of [locks] by 1 at each invocation, effectively committing
     * the transaction only when [locks] becomes 0. Finally, return the
     * outcome code
     */
    fun unlock(): OutcomeCode
}