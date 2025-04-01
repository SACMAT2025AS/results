package cryptoac.mm

import cryptoac.mm.local.MMServiceCACRBACLocal
//import cryptoac.mm.mysql.MMServiceCACABACMySQL
// import cryptoac.mm.mysql.MMServiceMySQLParameters
// import cryptoac.mm.mysql.MMServiceCACRBACMySQL
// import cryptoac.mm.redis.MMServiceCACRBACRedis
// import cryptoac.mm.redis.MMServiceRedisParameters
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/** Factory for creating MM objects */
class MMFactory {

    companion object {
        /** Return an MM service configured with the given [mmParameters] */
        fun getMM(mmParameters: MMServiceParameters): MMService {
            logger.debug { "Creating MM object of type ${mmParameters.mmType}" }
            return when (mmParameters.mmType) {
                MMType.RBAC_MYSQL -> TODO()
//                {
//                    if (mmParameters is MMServiceMySQLParameters) {
//                        MMServiceCACRBACMySQL(mmParameters)
//                    } else {
//                        val message = "Received wrong parameters for MM type ${MMType.RBAC_MYSQL}"
//                        logger.error { message }
//                        throw IllegalArgumentException(message)
//                    }
//                }
                MMType.ABAC_MYSQL -> TODO()// {
//                    if (mmParameters is MMServiceMySQLParameters) {
//                        MMServiceCACABACMySQL(mmParameters)
//                    } else {
//                        val message = "Received wrong parameters for MM type ${MMType.ABAC_MYSQL}"
//                        logger.error { message }
//                        throw IllegalArgumentException(message)
//                    }
//                }
                MMType.RBAC_REDIS -> TODO()
//                {
//                    if (mmParameters is MMServiceRedisParameters) {
//                        MMServiceCACRBACRedis(mmParameters)
//                    } else {
//                        val message = "Received wrong parameters for MM type ${MMType.ABAC_MYSQL}"
//                        logger.error { message }
//                        throw IllegalArgumentException(message)
//                    }
//                }
                MMType.ABAC_REDIS -> TODO()
//                {
//                    if (mmParameters is MMServiceRedisParameters) {
//                        TODO("first need to implement MMServiceABACRedis")
//                    } else {
//                        val message = "Received wrong parameters for MM type ${MMType.ABAC_REDIS}"
//                        logger.error { message }
//                        throw IllegalArgumentException(message)
//                    }
//                }
                MMType.RBAC_LOCAL -> MMServiceCACRBACLocal()
            }
        }
    }
}
