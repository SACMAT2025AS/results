package cryptoac.crypto

import cryptoac.crypto.java.CryptoJava
import cryptoac.crypto.openabe.CryptoOpenABE
import cryptoac.crypto.sodium.CryptoSodium
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/** Factory for creating PKE Crypto objects */
class CryptoPKEFactory {

    companion object {
        /** Return a Crypto PKE object of the given [type] */
        fun getCrypto(type: CryptoType): CryptoPKE {
            logger.debug { "Creating PKE crypto object of type $type" }
            return when (type) {
                CryptoType.JAVA -> CryptoJava()
                CryptoType.SODIUM -> CryptoSodium()
                CryptoType.OPENABE -> CryptoOpenABE()
            }
        }
    }
}
