package cryptoac

import cryptoac.Constants.ADMIN
import cryptoac.ac.dynsec.ACServiceRBACDynSecParameters
import cryptoac.ac.opa.ACServiceRBACOPAParameters
import cryptoac.ac.xacmlauthzforce.ACServiceRBACXACMLAuthzForceParameters
import cryptoac.core.CoreParametersABAC
import cryptoac.core.CoreParametersRBAC
import cryptoac.core.CoreType
import cryptoac.crypto.*
import cryptoac.crypto.openabe.CryptoOpenABE
import cryptoac.dm.cryptoac.DMServiceCryptoACParameters
import cryptoac.dm.mqtt.DMServiceMQTTParameters
import cryptoac.mm.MMServiceParameters
import cryptoac.mm.MMType
import cryptoac.mm.local.MMServiceRBACLocalParameters
import cryptoac.mm.mysql.MMServiceMySQLParameters
import cryptoac.mm.redis.MMServiceRedisParameters
import cryptoac.parameters.ServiceParameters
import cryptoac.rm.cryptoac.RMServiceRBACCryptoACParameters
import cryptoac.tuple.*

/**
 * This object contains test parameters. Change the implementation of the cryptoObject
 * and coreObject to test different implementations of the services
 */
object Parameters {


    /** Change these parameters to test different combinations */
    // TODO delete this, we should not have to rely on this
    val cryptoType = CryptoType.SODIUM
    val cryptoABEType = CryptoABEType.OPENABE

    private const val cryptoACURL = "10.1.0.2"
    private const val cryptoACPORT = "8443"
    const val cryptoACBaseAPI = "$cryptoACURL:$cryptoACPORT"

    private val cryptoPKEObjectJava = CryptoPKEFactory.getCrypto(CryptoType.JAVA)
    private val cryptoPKEObjectSodium = CryptoPKEFactory.getCrypto(CryptoType.SODIUM)
    private val cryptoPKEObjectOpenABE = CryptoPKEFactory.getCrypto(CryptoType.OPENABE)
    val cryptoPKEObject = when (cryptoType) {
        CryptoType.JAVA -> cryptoPKEObjectJava
        CryptoType.SODIUM -> cryptoPKEObjectSodium
        CryptoType.OPENABE -> cryptoPKEObjectOpenABE
    }
    private val cryptoSKEObjectJava = CryptoSKEFactory.getCrypto(CryptoType.JAVA)
    private val cryptoSKEObjectSodium = CryptoSKEFactory.getCrypto(CryptoType.SODIUM)
    private val cryptoSKEObjectOpenABE = CryptoSKEFactory.getCrypto(CryptoType.OPENABE)
    val cryptoSKEObject = when (cryptoType) {
        CryptoType.JAVA -> cryptoSKEObjectJava
        CryptoType.SODIUM -> cryptoSKEObjectSodium
        CryptoType.OPENABE -> cryptoSKEObjectOpenABE
    }
    val cryptoABEObject = when (cryptoABEType) {
        CryptoABEType.OPENABE -> cryptoPKEObjectOpenABE as CryptoOpenABE
    }
    val abePublicParameters = cryptoABEObject.exportABEPublicParams()


    val adminAsymEncKeys = cryptoPKEObject.generateAsymEncKeys(
        keyID = "${ADMIN}PKE" // TODO check this ID is fine
    )
    val adminAsymSigKeys = cryptoPKEObject.generateAsymSigKeys(
        keyID = "${ADMIN}PKSIG" // TODO check this ID is fine
    )
    val adminUser = User(
        name = ADMIN,
        isAdmin = true,
        asymEncKeys = AsymKeys(
            private = adminAsymEncKeys.private.encoded.encodeBase64(),
            public = adminAsymEncKeys.public.encoded.encodeBase64(),
            AsymKeysType.ENC
        ),
        asymSigKeys = AsymKeys(
            private = adminAsymSigKeys.private.encoded.encodeBase64(),
            public = adminAsymSigKeys.public.encoded.encodeBase64(),
            AsymKeysType.SIG
        ),
    )
    val adminRole = Role(
        ADMIN,
        asymEncKeys = AsymKeys(
            private = adminAsymEncKeys.private.encoded.encodeBase64(),
            public = adminAsymEncKeys.public.encoded.encodeBase64(),
            AsymKeysType.ENC
        ),
        asymSigKeys = AsymKeys(
            private = adminAsymSigKeys.private.encoded.encodeBase64(),
            public = adminAsymSigKeys.public.encoded.encodeBase64(),
            AsymKeysType.SIG
        ),
    )
    val adminUserRole = UserRole(
        username = ADMIN,
        roleName = ADMIN,
        encryptedAsymEncKeys = cryptoPKEObject.encryptAsymKeys(
            encryptingKey = adminAsymEncKeys.public,
            asymKeys = adminAsymEncKeys,
            type = AsymKeysType.ENC
        ),
        encryptedAsymSigKeys = cryptoPKEObject.encryptAsymKeys(
            encryptingKey = adminAsymEncKeys.public,
            asymKeys = adminAsymSigKeys,
            type = AsymKeysType.SIG
        )
    ).apply {
        updateSignature(
            newSignature = cryptoPKEObject.createSignature(
                bytes = getBytesForSignature(),
                signingKey = adminAsymSigKeys.private
            ),
            newSigner = ADMIN,
        )
    }
    val adminAttribute = Attribute(
        ADMIN,
    )
//    val adminUserAttribute = UserAttribute(
//        username = ADMIN,
//        attributeName = ADMIN,
//    ).apply {
//        updateSignature(
//            newSignature = cryptoPKEObject.createSignature(
//                bytes = getBytesForSignature(),
//                signingKey = adminAsymSigKeys.private
//            ),
//            newSigner = ADMIN,
//        )
//    }

    /** CryptoAC Parameters */
    val dmServiceCryptoACParameters = DMServiceCryptoACParameters(
        password = "password",
        username = ADMIN,
        port = 8443,
        url = "10.1.0.5"
    )
    val rmServiceRBACCryptoACParameters = RMServiceRBACCryptoACParameters(
        password = "password",
        username = ADMIN,
        port = 8443,
        url = "10.1.0.4"
    )
    val mmServiceRBACMySQLParameters = MMServiceMySQLParameters(
        port = 3306,
        url = "10.1.0.3",
        password = "password",
        username = ADMIN,
        mmType = MMType.RBAC_MYSQL
    )
    val mmServiceABACMySQLParameters = MMServiceMySQLParameters(
        port = 3306,
        url = "10.1.0.3",
        password = "password",
        username = ADMIN,
        mmType = MMType.ABAC_MYSQL
    )
    val mmServiceRBACLocalParameters = MMServiceRBACLocalParameters(
        mmType = MMType.RBAC_LOCAL
    )
    val acServiceRBACOPAParameters = ACServiceRBACOPAParameters(
        port = 8181, url = "10.1.0.6"
    )
    val acServiceRBACDynSecParameters = ACServiceRBACDynSecParameters(
        port = 1883,
        url = "10.1.0.8",
        password = "password",
        username = ADMIN,
        tls = false,
    )
    val acServiceRBACXACMLAuthzForceParameters = ACServiceRBACXACMLAuthzForceParameters(
        port = 8443, url = "10.1.0.9"
    )
    val adminCoreCACRBACCryptoACParametersWithOPA = CoreParametersRBAC(
        user = adminUser,
        coreType = CoreType.RBAC_FILES,
        cryptoType = cryptoType,
        mmServiceParameters = mmServiceRBACMySQLParameters,
        rmServiceParameters = rmServiceRBACCryptoACParameters,
        dmServiceParameters = dmServiceCryptoACParameters,
        acServiceParameters = acServiceRBACOPAParameters
    )
    val adminCoreCACRBACCryptoACParametersNoOPA = CoreParametersRBAC(
        user = adminUser,
        coreType = CoreType.RBAC_FILES,
        cryptoType = cryptoType,
        mmServiceParameters = mmServiceRBACLocalParameters,
        rmServiceParameters = rmServiceRBACCryptoACParameters,
        dmServiceParameters = dmServiceCryptoACParameters,
        acServiceParameters = null
    )

    /** MQTT Parameters */
    val dmServiceMQTTWithACParameters = DMServiceMQTTParameters(
        port = 1883,
        url = "10.1.0.8",
        password = "password",
        username = ADMIN,
        tls = false,
    )
    val dmServiceMQTTNoACParameters = DMServiceMQTTParameters(
        port = 1883,
        url = "10.1.0.10",
        password = "password",
        username = ADMIN,
        tls = false,
    )
    val mmServiceRBACRedisParameters = MMServiceRedisParameters(
        port = 6379,
        url = "10.1.0.7",
        password = "password",
        username = ADMIN,
        token = ADMIN,
        mmType = MMType.RBAC_REDIS
    )

    val adminCoreCACRBACMQTTParameters = CoreParametersRBAC(
        user = adminUser,
        coreType = CoreType.RBAC_MQTT,
        cryptoType = cryptoType,
        mmServiceParameters = mmServiceRBACRedisParameters,
        dmServiceParameters = dmServiceMQTTWithACParameters,
        acServiceParameters = acServiceRBACDynSecParameters,
        rmServiceParameters = null
    )
    val adminCoreCACABACMQTTParameters = CoreParametersABAC(
        user = adminUser,
        coreType = CoreType.ABAC_MQTT,
        rmServiceParameters = null,
        cryptoABEType = cryptoABEType,
        cryptoType = cryptoType,
        mmServiceParameters = mmServiceABACMySQLParameters,
        dmServiceParameters = dmServiceMQTTNoACParameters,
        acServiceParameters = null,
        abePublicParameters = abePublicParameters
    )

    val opaBaseAPI = "${acServiceRBACOPAParameters.url}:${acServiceRBACOPAParameters.port}"
    val xacmlAuthzForceBaseAPI = "${acServiceRBACXACMLAuthzForceParameters.url}:${acServiceRBACXACMLAuthzForceParameters.port}"

    private const val aliceName = "alice"
    private val aliceAsymEncKeys = cryptoPKEObject.generateAsymEncKeys(
        keyID = "${aliceName}PKE" // TODO check this ID is fine
    )
    private val aliceAsymSigKeys = cryptoPKEObject.generateAsymSigKeys(
        keyID = "${aliceName}PKSIG" // TODO check this ID is fine
    )
    val aliceUser = User(
        name = aliceName,
        isAdmin = false,
        asymEncKeys = AsymKeys(
            public = aliceAsymEncKeys.public.encoded.encodeBase64(),
            private = aliceAsymEncKeys.private.encoded.encodeBase64(),
            keysType = AsymKeysType.ENC
        ),
        asymSigKeys = AsymKeys(
            public = aliceAsymSigKeys.public.encoded.encodeBase64(),
            private = aliceAsymSigKeys.private.encoded.encodeBase64(),
            keysType = AsymKeysType.SIG
        )
    )
    val aliceCoreCACRBACMQTTParameters = CoreParametersRBAC(
        user = aliceUser,
        coreType = CoreType.RBAC_MQTT,
        cryptoType = cryptoType,
        mmServiceParameters = MMServiceRedisParameters(
            port = 6379,
            url = "10.1.0.7",
            password = "alicePassword",
            username = aliceName,
            token = aliceName,
            mmType = MMType.RBAC_REDIS
        ),
        dmServiceParameters = DMServiceMQTTParameters(
            port = 1883,
            url = "10.1.0.8",
            password = "alicePassword",
            username = aliceName,
            tls = false,
        ),
        acServiceParameters = ACServiceRBACDynSecParameters(
            port = 1883,
            url = "10.1.0.8",
            password = "alicePassword",
            username = aliceName,
            tls = false,
        ),
        rmServiceParameters = null
    )

    private const val bobName = "bob"
    private val bobAsymEncKeys = cryptoPKEObject.generateAsymEncKeys(
        keyID = "${bobName}PKE" // TODO check this ID is fine
    )
    private val bobAsymSigKeys = cryptoPKEObject.generateAsymSigKeys(
        keyID = "${bobName}PKSIG" // TODO check this ID is fine
    )
    val bobUser = User(
        name = bobName,
        isAdmin = false,
        asymEncKeys = AsymKeys(
            public = bobAsymEncKeys.public.encoded.encodeBase64(),
            private = bobAsymEncKeys.private.encoded.encodeBase64(),
            keysType = AsymKeysType.ENC
        ),
        asymSigKeys = AsymKeys(
            public = bobAsymSigKeys.public.encoded.encodeBase64(),
            private = bobAsymSigKeys.private.encoded.encodeBase64(),
            keysType = AsymKeysType.SIG
        )
    )
    val bobCoreCACRBACMQTTParameters = CoreParametersRBAC(
        user = bobUser,
        coreType = CoreType.RBAC_MQTT,
        cryptoType = cryptoType,
        mmServiceParameters = MMServiceRedisParameters(
            port = 6379,
            url = "10.1.0.7",
            password = "bobPassword",
            username = bobName,
            token = bobName,
            mmType = MMType.RBAC_REDIS
        ),
        dmServiceParameters = DMServiceMQTTParameters(
            port = 1883,
            url = "10.1.0.8",
            password = "bobPassword",
            username = bobName,
            tls = false,
        ),
        acServiceParameters = ACServiceRBACDynSecParameters(
            port = 1883,
            url = "10.1.0.8",
            password = "bobPassword",
            username = bobName,
            tls = false,
        ),
        rmServiceParameters = null
    )

    private const val carlName = "carl"
    private val carlAsymEncKeys = cryptoPKEObject.generateAsymEncKeys(
        keyID = "${carlName}PKE" // TODO check this ID is fine
    )
    private val carlAsymSigKeys = cryptoPKEObject.generateAsymSigKeys(
        keyID = "${carlName}PKSIG" // TODO check this ID is fine
    )
    val carlUser = User(
        name = carlName,
        isAdmin = false,
        asymEncKeys = AsymKeys(
            public = carlAsymEncKeys.public.encoded.encodeBase64(),
            private = carlAsymEncKeys.private.encoded.encodeBase64(),
            keysType = AsymKeysType.ENC
        ),
        asymSigKeys = AsymKeys(
            public = carlAsymSigKeys.public.encoded.encodeBase64(),
            private = carlAsymSigKeys.private.encoded.encodeBase64(),
            keysType = AsymKeysType.SIG
        )
    )

    private const val deniseName = "denise"
    private val deniseAsymEncKeys = cryptoPKEObject.generateAsymEncKeys(
        keyID = "${deniseName}PKE" // TODO check this ID is fine
    )
    private val deniseAsymSigKeys = cryptoPKEObject.generateAsymSigKeys(
        keyID = "${deniseName}PKSIG" // TODO check this ID is fine
    )
    val deniseUser = User(
        name = deniseName,
        isAdmin = false,
        asymEncKeys = AsymKeys(
            public = deniseAsymEncKeys.public.encoded.encodeBase64(),
            private = deniseAsymEncKeys.private.encoded.encodeBase64(),
            keysType = AsymKeysType.ENC
        ),
        asymSigKeys = AsymKeys(
            public = deniseAsymSigKeys.public.encoded.encodeBase64(),
            private = deniseAsymSigKeys.private.encoded.encodeBase64(),
            keysType = AsymKeysType.SIG
        )
    )
}
