package cryptoac.core.mqtt.model

import cryptoac.core.mqtt.ProcessedUserMqttMessageCryptoAC
import cryptoac.core.mqtt.UserMqttMessageCryptoAC
import cryptoac.crypto.SymmetricKeyCryptoAC
import cryptoac.tuple.Resource


/**
 * Data class holding, for a given resource that is an MQTT topic, the
 * (latest) [symKey] for the resource data received as [retainedMessage].
 * Then, [decryptedMessages] (i.e., MQTT messages received and already
 * decrypted) and [messagesToDecrypt] (i.e., MQTT messages received
 * but not decrypted already due to, e.g., the fact that the
 * [retainedMessage] has still to be received
 */
data class SymmetricKeysAndCachedMessages(
    var symKey: SymmetricKeyCryptoAC? = null,
    var retainedMessage: Resource? = null,
    var decryptedMessages: MutableList<ProcessedUserMqttMessageCryptoAC> = mutableListOf(),
    var messagesToDecrypt: MutableList<UserMqttMessageCryptoAC> = mutableListOf()
)
