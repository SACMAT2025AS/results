package cryptoac.core.mqtt

import kotlinx.serialization.Serializable

/**
 * A class representing an (already encrypted) MQTT message,
 * consisting of a [message] in a [topic] and a boolean flag
 * to determining whether the message is an [error]
 */
@Serializable
data class ProcessedUserMqttMessageCryptoAC(
    val message: String,
    val topic: String,
    val error: Boolean
)
