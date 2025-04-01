package cryptoac.crypto.openabe

import cryptoac.crypto.AsymKeysType
import cryptoac.crypto.KeyPairCryptoAC
import cryptoac.crypto.PrivateKeyCryptoAC
import cryptoac.crypto.PublicKeyCryptoAC

/**
 * A pair of cryptographic keys composed of
 * a [public] key, a [private] key and a [keyType]
 * for the OpenABE library
 */
class KeyPairOpenABE(
    override val public: PublicKeyOpenABE,
    override val private: PrivateKeyOpenABE,
    override val keyType: AsymKeysType,
) : KeyPairCryptoAC(public, private, keyType)

/** A public cryptographic key for the OpenABE library */
class PublicKeyOpenABE(
    val public: String,
    override val keyID: String?,
) : PublicKeyCryptoAC {
    override fun getAlgorithm() = "OpenABE"
    override fun getFormat() = "OpenABE"
    override fun getEncoded() = public.encodeToByteArray()
}

/** A private cryptographic key for the OpenABE library */
class PrivateKeyOpenABE(
    val private: String,
    override var keyID: String?,
) : PrivateKeyCryptoAC {
    override fun getAlgorithm() = "OpenABE"
    override fun getFormat() = "OpenABE"
    override fun getEncoded() = private.encodeToByteArray()
}
