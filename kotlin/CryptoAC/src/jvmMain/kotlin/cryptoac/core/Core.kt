package cryptoac.core

import cryptoac.OutcomeCode
import cryptoac.OutcomeCode.*
import cryptoac.ServiceType
import cryptoac.ac.ACService
import cryptoac.dm.DMService
import cryptoac.mm.MMService
import cryptoac.code.CodeCoreParameters
import cryptoac.code.CodeUsers
import cryptoac.tuple.User
import cryptoac.rm.RMService
import cryptoac.tuple.TupleStatus
import kotlinx.coroutines.sync.Mutex
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A Core is the enforcement mechanism for an access control
 * model/scheme (e.g., RBAC, ABAC). The core is configured with
 * the [coreParameters] and is used by a [user]. Finally, a core
 * has a [mutex] for regulating concurrent execution
 */
abstract class Core(
    open val coreParameters: CoreParameters
) {

    /** The mutex to regulate concurrent execution */
    val mutex = Mutex()

    /** The user of the core */
    abstract val user: User

    /** The MM service */
    abstract val mm: MMService?

    /** The RM service */
    abstract val rm: RMService?

    /** The DM service */
    abstract val dm: DMService?

    /** The AC service */
    abstract val ac: ACService?

    /**
     * This function is invoked each time the core object
     * is created (not only the first time), and it
     * should contain the code to set up the core
     * (e.g., possibly connect to remote services like
     * MQTT brokers, databases, etc.)
     */
    open fun initCore() : OutcomeCode {
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }
        mm?.init()
        rm?.init()
        dm?.init()
        ac?.init()

        return endOfMethod(code)
    }

    /**
     * This function is invoked each time the core object
     * is destroyed, and it should contain the code to
     * de-initialize the core (e.g., possibly disconnect
     * remote services like MQTT brokers, databases, etc.)
     */
    open fun deinitCore() {
        mm?.deinit()
        rm?.deinit()
        dm?.deinit()
        ac?.deinit()
    }

    /**
     * This function is invoked by the admin, and
     * it should configure the services, initialize
     * the admin's status in the various services,
     * set the value of relevant parameters and return
     * the outcome code. Ideally, this function should
     * be invoked only once after the 'initCore'
     * function. When implementing this function,
     * remember to decide how to handle (e.g.,
     * reject or allow) subsequent invocations
     */
    abstract fun configureServices(): OutcomeCode

    /**
     * This function is invoked by the user, and it
     * should initialize the user's status in the
     * various services and return the outcome code.
     * Ideally, this function should be invoked only
     * once after the 'initCore' function. When
     * implementing this function, remember to decide
     * how to handle (e.g., reject or allow) subsequent
     * invocations
     */
    abstract fun initUser(): OutcomeCode
//    {
//
//        logger.info { "Initializing token and public key of user ${user.name}" }
//
//        /** Lock the status of the services */
//        var code = startOfMethod()
//        if (code != CODE_000_SUCCESS) {
//            return code
//        }
//
//        /** Init the user in the MM */
//        code = mm?.initUser(user)
//            ?: CODE_000_SUCCESS
//        if (code != CODE_000_SUCCESS) {
//            return endOfMethod(
//                code = code
//            )
//        }
//
//        /** Init the user in the RM */
//        code = rm?.initUser(user)
//            ?: CODE_000_SUCCESS
//        if (code != CODE_000_SUCCESS) {
//            return endOfMethod(
//                code = code
//            )
//        }
//
//        /** Init the user in the DM */
//        code = dm?.initUser(user)
//            ?: CODE_000_SUCCESS
//        if (code != CODE_000_SUCCESS) {
//            return endOfMethod(
//                code = code
//            )
//        }
//
//        /** Init the user in the AC */
//        return endOfMethod(
//            code = ac?.initUser(user)
//                ?: CODE_000_SUCCESS
//        )
//    }

    /**
     * Add a user with the given [username] to the
     * policy and return eventual configuration
     * parameters along with the outcome code
     * (if an error occurred, the configuration
     * parameters will be null). Note that users
     * cannot be assigned (to, e.g., attributes or
     * roles) until they invoke the initUser method
     */
    abstract fun addUser(
        username: String
    ): CodeCoreParameters

    /**
     * Delete the user with the matching [username] from
     * the policy and revoke the user from all assignments
     * (of, e.g., attributes or roles). Finally, return
     * the outcome code
     */
    abstract fun deleteUser(
        username: String
    ): OutcomeCode

    /**
     * Return the set of users, along with the
     * outcome code (if an error occurred, the
     * set of users will be null)
     */
    abstract fun getUsers(
        statuses: Array<TupleStatus>
    ): CodeUsers



    /**
     * Lock the specified services
     * and return the outcome code
     */
    protected fun startOfMethod(
        mmLock: Boolean = true,
        dmLock: Boolean = true,
        acLock: Boolean = true,
    ): OutcomeCode {
        logger.info {
            "Locking the following services: " +
            (if (acLock && ac != null) "AC " else "") +
            (if (mmLock && ac != null) "MM " else "") +
            (if (dmLock && ac != null) "DM " else "")
        }
        val mmLockCode = if (mmLock && mm != null) mm!!.lock() else CODE_000_SUCCESS
        return if (mmLockCode == CODE_000_SUCCESS) {
            val acLockCode = if (acLock && ac != null) ac!!.lock() else CODE_000_SUCCESS
            if (acLockCode == CODE_000_SUCCESS) {
                val dmLockCode = if (dmLock && dm != null) dm!!.lock() else CODE_000_SUCCESS
                if (dmLockCode == CODE_000_SUCCESS) {
                    CODE_000_SUCCESS
                } else {
                    logger.warn { "DM lock failed, code is $dmLockCode" }
                    if (acLock && ac != null) unlockOrRollbackService(ServiceType.AC)
                    if (mmLock) unlockOrRollbackService(ServiceType.MM)
                    dmLockCode
                }
            } else {
                logger.warn { "AC lock failed, code is $acLockCode" }
                if (mmLock) unlockOrRollbackService(ServiceType.MM)
                acLockCode
            }
        } else {
            logger.warn { "MM lock failed, code is $mmLockCode" }
            mmLockCode
        }
    }

    /**
     * If the [code] is a success, unlock the
     * specified services (i.e., commit the
     * changes). Otherwise, rollback to the
     * previous status. In both cases, return
     * the outcome code
     */
    protected fun endOfMethod(
        code: OutcomeCode,
        mmLocked: Boolean = true,
        dmLocked: Boolean = true,
        acLocked: Boolean = true,
    ): OutcomeCode {

        if (code == CODE_000_SUCCESS) {
            logger.info {
                "Operation successful, unlocking the following services: " +
                        (if (acLocked && ac != null) "AC " else "") +
                        (if (mmLocked && mm != null) "MM " else "") +
                        (if (dmLocked && dm != null) "DM " else "")
            }
            if (mmLocked && mm != null) unlockOrRollbackService(ServiceType.MM)
            if (acLocked && ac != null) unlockOrRollbackService(ServiceType.AC)
            if (dmLocked && dm != null) unlockOrRollbackService(ServiceType.DM)
        } else {
            logger.info {
                "Operation unsuccessful (code $code), rollback the following services: " +
                        (if (acLocked && ac != null) "AC " else "") +
                        (if (mmLocked && mm != null) "MM " else "") +
                        if (dmLocked && dm != null) "DM " else ""
            }
            if (mmLocked && mm != null) unlockOrRollbackService(ServiceType.MM, true)
            if (acLocked && ac != null) unlockOrRollbackService(ServiceType.AC, true)
            if (dmLocked && dm != null) unlockOrRollbackService(ServiceType.DM, true)
        }
        return code
    }

    /**
     * Unlock or rollback the specified [serviceType],
     * depending on the [rollback] flag
     */
    private fun unlockOrRollbackService(
        serviceType: ServiceType,
        rollback: Boolean = false
    ) {
        val code = when (serviceType) {
            ServiceType.MM -> if (rollback) mm!!.rollback() else mm!!.unlock()
            ServiceType.DM -> if (rollback) dm!!.rollback() else dm!!.unlock()
            ServiceType.AC -> if (rollback) ac!!.rollback() else ac!!.unlock()
            ServiceType.RM -> if (rollback) rm!!.rollback() else rm!!.unlock()
        }
        if (code != CODE_000_SUCCESS) {
            val message =
                "$serviceType lock was fine but " +
                (if (rollback) "rollback" else "unlock") +
                " failed, code is $code"
            logger.error { message }
            throw IllegalStateException(message)
        }
    }
}
