package cryptoac.parameters

import cryptoac.SafeRegex
import io.ktor.utils.io.core.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Interface defining the parameters required to configure a
 * remote service to connect to over a network:
 * - [url]: the url;
 * - [port]: the port.
 */
interface RemoteServiceParameters : ServiceParameters {

    /** The URL of this interface */
    var url: String

    /** The port of this interface */
    var port: Int

    override fun checkParameters(): Boolean =
        if (!SafeRegex.URL_OR_IPV4.matches(url)) {
            logger.warn { "URL ${url.toByteArray()} does not respect URL_OR_IPV4 regex" }
            false
        } else if (port <= 0 || port >= 65535) {
            logger.warn { "Port number $port is not between 0 and 65535" }
            false
        } else {
            true
        }

    override fun update(updatedParameters: ServiceParameters) {
        if (updatedParameters is RemoteServiceParameters) {
            port = updatedParameters.port
            url = updatedParameters.url
        } else {
            val message = "Given a non-subclass of ${this::class} for update"
            logger.error { message }
            throw IllegalStateException(message)
        }
    }

    override fun obscureSensitiveFields() {
        /** No sensitive fields to obscure */
    }
}

