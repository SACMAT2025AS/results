package cryptoac.core.mqtt

import kotlinx.serialization.Serializable


/**
 * An MQTT message sent by either admin or users
 * in CryptoAC defined by a [type] and a [content]
 */
@Serializable
class MqttMessageCryptoAC(
    val type: MqttMessageCryptoACType,
    val content: ByteArray
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MqttMessageCryptoAC

        if (type != other.type) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}

/**
 * A CryptoAC MQTT message can be:
 * - [RESOURCE_UPDATE]: message sent by the admin to broadcast
 *                      an updated version of the resource
 * - [RESOURCE_DELETE]: message sent by the admin to notify
 *                      that the resource was deleted
 * - [RESOURCE_CONTENT]: message sent by users to broadcast
 *                       resource content (i.e., normal message)
 */
@Serializable
enum class MqttMessageCryptoACType {
    RESOURCE_UPDATE,
    RESOURCE_DELETE,
    RESOURCE_CONTENT,
}