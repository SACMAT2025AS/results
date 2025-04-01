package cryptoac.crypto.sodium

import cryptoac.crypto.*
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CryptoSodiumPKETest : CryptoPKETest() {

    override val cryptoPKEObject: CryptoSodium =
        CryptoPKEFactory.getCrypto(CryptoType.SODIUM) as CryptoSodium
    override val cryptoSKEObject: CryptoSodium = cryptoPKEObject
}
