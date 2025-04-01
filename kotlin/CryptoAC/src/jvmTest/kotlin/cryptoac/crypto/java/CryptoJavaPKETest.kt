package cryptoac.crypto.java

import cryptoac.crypto.*

internal class CryptoJavaPKETest : CryptoPKETest() {

    override val cryptoPKEObject: CryptoJava =
        CryptoPKEFactory.getCrypto(CryptoType.JAVA) as CryptoJava
    override val cryptoSKEObject: CryptoJava = cryptoPKEObject

}
