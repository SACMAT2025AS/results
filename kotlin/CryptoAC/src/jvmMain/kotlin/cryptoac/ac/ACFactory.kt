package cryptoac.ac

import cryptoac.ac.dynsec.ACServiceRBACDynSec
import cryptoac.ac.dynsec.ACServiceRBACDynSecParameters
import cryptoac.ac.opa.ACServiceRBACOPA
import cryptoac.ac.opa.ACServiceRBACOPAParameters
import cryptoac.ac.xacmlauthzforce.ACServiceRBACXACMLAuthzForce
import cryptoac.ac.xacmlauthzforce.ACServiceRBACXACMLAuthzForceParameters
import cryptoac.core.mqtt.CryptoACMQTTClient
import cryptoac.generateRandomString
import mu.KotlinLogging
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence

private val logger = KotlinLogging.logger {}

/** Factory for creating AC objects */
class ACFactory {

    companion object {
        /** Return an AC service configured with the given [acParameters] */
        fun getAC(acParameters: ACServiceParameters): ACService? {
            logger.debug { "Creating AC object of type ${acParameters.acType}" }
            return when (acParameters.acType) {
                ACType.RBAC_OPA -> {
                    if (acParameters is ACServiceRBACOPAParameters) {
                        ACServiceRBACOPA(acParameters)
                    } else {
                        val message = "Received wrong parameters for AC type ${acParameters.acType}"
                        logger.error { message }
                        throw IllegalArgumentException(message)
                    }
                }
                ACType.RBAC_DYNSEC -> {
                    if (acParameters is ACServiceRBACDynSecParameters) {
                        val brokerBaseAPI = (if (acParameters.tls) "ssl" else "tcp") +
                            "://${acParameters.url}:${acParameters.port}"
                        val client = CryptoACMQTTClient(
                            serverURI = brokerBaseAPI,
                            clientId = generateRandomString(),
                            persistence = MemoryPersistence(),
                            tls = acParameters.tls,
                            username = acParameters.username,
                            password = acParameters.password,
                        )
                        ACServiceRBACDynSec(acParameters, client).apply {
                            client.setCallback(this)
                        }

                    } else {
                        val message = "Received wrong parameters for AC type ${acParameters.acType}"
                        logger.error { message }
                        throw IllegalArgumentException(message)
                    }
                }
                ACType.RBAC_XACML_AUTHZFORCE -> {
                    if (acParameters is ACServiceRBACXACMLAuthzForceParameters) {
                        ACServiceRBACXACMLAuthzForce(acParameters)
                    } else {
                        val message = "Received wrong parameters for AC type ${acParameters.acType}"
                        logger.error { message }
                        throw IllegalArgumentException(message)
                    }
                }
                ACType.NONE -> null
            }
        }
    }
}
