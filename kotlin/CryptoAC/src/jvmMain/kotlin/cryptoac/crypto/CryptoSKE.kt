package cryptoac.crypto

import java.io.InputStream

/**
 * Interface defining the methods of a cryptographic
 * provider for symmetric cryptography
 */
interface CryptoSKE : Crypto {

    /** Return a freshly generated symmetric key */
    fun generateSymKey(): SymmetricKeyCryptoAC

    /**
     * Encrypt the content of the [stream] with
     * the [encryptingKey] and return it as a stream.
     * Empty streams are allowed due to the difficulty
     * of checking whether a stream is empty without
     * consuming it. Ensure that authenticated encryption
     * only is used
     */
    fun encryptStream(
        encryptingKey: SymmetricKeyCryptoAC,
        stream: InputStream
    ): InputStream

    /**
     * Decrypt the content of the [stream] with
     * the [decryptingKey] and return it as a stream.
     * Empty streams are allowed due to the difficulty
     * of checking whether a stream is empty without
     * consuming it. Ensure that authenticated encryption
     * only is used
     */
    fun decryptStream(
        decryptingKey: SymmetricKeyCryptoAC,
        stream: InputStream
    ): InputStream

    fun symDecryptSymKey(
        encryptedKey: EncryptedSymKey,
        decryptingKey: SymmetricKeyCryptoAC,
    ): SymmetricKeyCryptoAC

    fun symEncryptSymKey(
        key: SymmetricKeyCryptoAC,
        encryptingKey: SymmetricKeyCryptoAC,
    ): EncryptedSymKey
}
