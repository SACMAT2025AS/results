package cryptoac.parameters

import cryptoac.SafeRegex
import io.ktor.utils.io.core.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Interface defining the parameters required to configure a
 * service that requires a username and a password for authentication:
 * - [username]: the username;
 * - [password]: the password.
 */
interface UPServiceParameters : ServiceParameters {

    /** The username */
    var username: String

    /** The password */
    var password: String

    override fun checkParameters(): Boolean =
        if (!SafeRegex.TEXT.matches(username)) {
            logger.warn {
                "Username ${username.toByteArray()} " +
                "does not respect TEXT regex"
            }
            false
        } else if (!SafeRegex.TEXT.matches(password)) {
            logger.warn { "Password does not respect TEXT regex" }
            false
        } else {
            true
        }

    override fun update(updatedParameters: ServiceParameters) {
        if (updatedParameters is UPServiceParameters) {
            // password = updatedParameters.password TODO decide whether to update or not the password
        } else {
            val message = "Given a non-subclass of ${this::class} for update"
            logger.error { message }
            throw IllegalStateException(message)
        }
    }

    override fun obscureSensitiveFields() {
        password = "***"
    }
}