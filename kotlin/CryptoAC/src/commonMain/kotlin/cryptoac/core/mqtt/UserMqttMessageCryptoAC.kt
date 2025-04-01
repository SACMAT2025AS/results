package cryptoac.core.mqtt

import cryptoac.core.mqtt.MqttMessageCryptoACType.RESOURCE_CONTENT
import kotlinx.serialization.Serializable

/**
 * A class representing a (possibly encrypted) MQTT message
 * corresponding to the [RESOURCE_CONTENT] [MqttMessageCryptoACType]
 * consisting of a [resourceContent] and the [resourceVersionNumber]
 */
@Serializable
data class UserMqttMessageCryptoAC(
    val resourceContent: ByteArray,
    val resourceVersionNumber: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UserMqttMessageCryptoAC

        if (!resourceContent.contentEquals(other.resourceContent)) return false
        if (resourceVersionNumber != other.resourceVersionNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = resourceContent.contentHashCode()
        result = 31 * result + resourceVersionNumber
        return result
    }
}
