package cryptoac.crypto

import cryptoac.crypto.java.CryptoJava
import cryptoac.crypto.openabe.CryptoOpenABE
import cryptoac.crypto.sodium.CryptoSodium
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/** Factory for creating SKE Crypto objects */
class CryptoSKEFactory {

    companion object {
        /** Return a Crypto SKE object of the given [type] */
        fun getCrypto(type: CryptoType): CryptoSKE {
            logger.debug { "Creating SKE crypto object of type $type" }
            return when (type) {
                CryptoType.JAVA -> CryptoJava()
                CryptoType.SODIUM -> CryptoSodium()
                CryptoType.OPENABE -> CryptoOpenABE()
            }
        }
    }
}
