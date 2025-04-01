package cryptoac.crypto.openabe

import cryptoac.crypto.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

internal class CryptoOpenABEPKETest : CryptoPKETest() {

    lateinit var cryptoABEObject: CryptoOpenABE
    override lateinit var cryptoPKEObject: CryptoOpenABE
    override lateinit var cryptoSKEObject: CryptoOpenABE

    @BeforeEach
    fun setUp() {
        cryptoABEObject = CryptoABEFactory.getCrypto(CryptoABEType.OPENABE) as CryptoOpenABE
        cryptoPKEObject = cryptoABEObject
        cryptoSKEObject = cryptoABEObject
    }

    @AfterEach
    fun tearDown() {
        cryptoABEObject.deinit()
    }
}
