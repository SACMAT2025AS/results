package cryptoac.dm

import cryptoac.ac.dynsec.ACServiceRBACDynSec
import cryptoac.core.mqtt.CryptoACMQTTClient
import cryptoac.dm.cryptoac.DMServiceCryptoACParameters
import cryptoac.dm.cryptoac.DMServiceCryptoAC
import cryptoac.dm.local.DMLocal
import cryptoac.dm.local.DMServiceLocalParameters
import cryptoac.dm.mqtt.DMServiceMQTTParameters
//import cryptoac.dm.mqtt.DMServiceMQTT
import cryptoac.generateRandomString
import mu.KotlinLogging
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence

private val logger = KotlinLogging.logger {}

/** Factory for creating DM objects */
class DMFactory {

    companion object {
        /** Return a DM service configured with the given [dmParameters] */
        fun getDM(dmParameters: DMServiceParameters): DMService {
            logger.debug { "Creating DM object of type ${dmParameters.dmType}" }
            return when (dmParameters.dmType) {
                DMType.CRYPTOAC -> {
                    if (dmParameters is DMServiceCryptoACParameters) {
                        DMServiceCryptoAC(dmParameters)
                    } else {
                        val message = "Received wrong parameters for DM type ${DMType.CRYPTOAC}"
                        logger.error { message }
                        throw IllegalArgumentException(message)
                    }
                }
                DMType.MQTT -> TODO()
                DMType.LOCAL -> {
                    if (dmParameters is DMServiceLocalParameters) {
                        DMLocal(dmParameters)
                    } else {
                        val message = "Received wrong parameters for DM type ${DMType.LOCAL}"
                        logger.error { message }
                        throw IllegalArgumentException(message)
                    }
                }
//                DMType.MQTT -> {
//                    if (dmParameters is DMServiceMQTTParameters) {
//                        val brokerBaseAPI = if (dmParameters.tls) {
//                            "ssl"
//                        } else {
//                            "tcp"
//                        } + "://${dmParameters.url}:${dmParameters.port}"
//                        val client = CryptoACMQTTClient(
//                            serverURI = brokerBaseAPI,
//                            clientId = generateRandomString(),
//                            persistence = MemoryPersistence(),
//                            tls = dmParameters.tls,
//                            username = dmParameters.username,
//                            password = dmParameters.password,
//                        )
//                        DMServiceMQTT(dmParameters, client).apply {
//                            client.setCallback(this)
//                        }
//                    } else {
//                        val message = "Received wrong parameters for DM type ${DMType.MQTT}"
//                        logger.error { message }
//                        throw IllegalArgumentException(message)
//                    }
//                }
                DMType.NONE -> TODO()
            }
        }
    }
}
