package cryptoac.core.mqtt

import cryptoac.core.mqtt.MqttMessageCryptoACType.RESOURCE_UPDATE
import cryptoac.tuple.Resource
import kotlinx.serialization.Serializable

/**
 * A class representing a [Resource] corresponding to the
 * [RESOURCE_UPDATE] [MqttMessageCryptoACType] consisting of a [resource]
 */
@Serializable
data class AdminMqttMessageCryptoAC(
    val resource: Resource
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AdminMqttMessageCryptoAC

        return resource == other.resource
    }

    override fun hashCode(): Int {
        return resource.hashCode()
    }
}
