package cryptoac.core

import cryptoac.OutcomeCode
import cryptoac.crypto.CryptoPKE
import cryptoac.crypto.CryptoSKE
import org.eclipse.jetty.http.HttpStatus.Code

/**
 * A CoreCAC extends the [Core] class as a cryptographic
 * enforcement mechanism for an access control model/scheme
 * (e.g., RBAC, ABAC). The CoreCAC uses the [cryptoPKE] and
 * [cryptoSKE] objects to perform cryptographic computations
 */
abstract class CoreCAC(
    open val cryptoPKE: CryptoPKE,
    open val cryptoSKE: CryptoSKE,
    override val coreParameters: CoreParameters
) : Core(coreParameters) {

    override fun initCore() : OutcomeCode {
        super.initCore() // TODO check return code

        var code = startOfMethod()
        if (code != OutcomeCode.CODE_000_SUCCESS) {
            return code
        }
        cryptoPKE.init()
        cryptoSKE.init()

        return endOfMethod(code) // TODO return code
    }

    override fun deinitCore() {
        cryptoPKE.deinit()
        cryptoSKE.deinit()
        // TODO wipe crypto material from user object
    }
}