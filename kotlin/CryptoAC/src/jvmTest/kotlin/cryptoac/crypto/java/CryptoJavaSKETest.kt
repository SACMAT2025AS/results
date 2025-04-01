package cryptoac.crypto.java

import cryptoac.crypto.*

internal class CryptoJavaSKETest : CryptoSKETest() {

    override val cryptoSKEObject: CryptoJava =
        CryptoPKEFactory.getCrypto(CryptoType.JAVA) as CryptoJava
}
