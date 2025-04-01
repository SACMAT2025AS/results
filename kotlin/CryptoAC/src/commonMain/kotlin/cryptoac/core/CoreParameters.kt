package cryptoac.core

import cryptoac.parameters.ServiceParameters
import cryptoac.SafeRegex
import cryptoac.tuple.User
import cryptoac.crypto.CryptoType
import cryptoac.ac.opa.ACServiceRBACOPAParameters
import cryptoac.ac.ACServiceParameters
import cryptoac.ac.dynsec.ACServiceRBACDynSecParameters
import cryptoac.ac.xacmlauthzforce.ACServiceRBACXACMLAuthzForceParameters
import cryptoac.dm.cryptoac.DMServiceCryptoACParameters
import cryptoac.dm.mqtt.DMServiceMQTTParameters
import cryptoac.dm.DMServiceParameters
import cryptoac.dm.local.DMServiceLocalParameters
import cryptoac.rm.cryptoac.RMServiceRBACCryptoACParameters
import cryptoac.rm.RMServiceParameters
import cryptoac.mm.MMServiceParameters
import cryptoac.mm.local.MMServiceRBACLocalParameters
import cryptoac.mm.redis.MMServiceRedisParameters
import cryptoac.mm.mysql.MMServiceMySQLParameters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A CoreParameters defines the parameters required to configure a core object:
 * - [user]: the user to whom the core object belongs;
 * - [coreType]: the type of the core object;
 * - [cryptoType]: the cryptographic provider used by the core object;
 * - [versionNumber]: the version number of the core object;
 * - [rmServiceParameters]: parameters of the RM object, if any;
 * - [mmServiceParameters]: parameters of the MM object;
 * - [dmServiceParameters]: parameters of the DM object;
 * - [acServiceParameters]: parameters of the AC object.
 */
@Serializable
abstract class CoreParameters : ServiceParameters {
    abstract var user: User
    abstract val coreType: CoreType
    abstract val cryptoType: CryptoType
    abstract val versionNumber: Int
    abstract val rmServiceParameters: RMServiceParameters?
    abstract val mmServiceParameters: MMServiceParameters
    abstract val dmServiceParameters: DMServiceParameters?
    abstract val acServiceParameters: ACServiceParameters?

    override fun checkParameters(): Boolean =
        if (!SafeRegex.TEXT.matches(user.name)) {
            logger.warn { "Username ${user.name} does not respect TEXT regex" }
            false
        } else if (
            user.asymEncKeys != null && (
                !SafeRegex.BASE64.matches(user.asymEncKeys!!.public) ||
                !SafeRegex.BASE64.matches(user.asymEncKeys!!.private)
            )
        ) {
            logger.warn { "Encryption keys does not respect BASE64 regex" }
            false
        } else if (
            user.asymSigKeys != null && (
                !SafeRegex.BASE64.matches(user.asymSigKeys!!.public) ||
                !SafeRegex.BASE64.matches(user.asymSigKeys!!.private)
            )
        ) {
            logger.warn { "Signing keys does not respect BASE64 regex" }
            false
        } else if (versionNumber !in 1..1) {
            logger.warn {
                "Parameters version is not supported (version" +
                    " number is $versionNumber, supported are 1..1"
            }
            false
        } else {
            rmServiceParameters?.checkParameters() ?: true &&
            mmServiceParameters.checkParameters() &&
            dmServiceParameters?.checkParameters() ?: true
            acServiceParameters?.checkParameters() ?: true
        }

    override fun update(updatedParameters: ServiceParameters) {
        if (updatedParameters is CoreParameters) {
            updatedParameters.rmServiceParameters?.let { rmServiceParameters?.update(it) }
            mmServiceParameters.update(updatedParameters.mmServiceParameters)
            updatedParameters.dmServiceParameters?.let { dmServiceParameters?.update(it) }
            updatedParameters.acServiceParameters?.let { acServiceParameters?.update(it) }
        } else {
            val message = "Given a non-subclass of ${this::class} for update"
            logger.error { message }
            throw IllegalStateException(message)
        }
    }

    override fun obscureSensitiveFields() {
        user.asymEncKeys?.private = "***"
        user.asymSigKeys?.private = "***"
        rmServiceParameters?.obscureSensitiveFields()
        mmServiceParameters.obscureSensitiveFields()
        dmServiceParameters?.obscureSensitiveFields()
        acServiceParameters?.obscureSensitiveFields()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CoreParameters) return false

        if (user != other.user) return false
        if (coreType != other.coreType) return false
        if (cryptoType != other.cryptoType) return false
        if (versionNumber != other.versionNumber) return false
        if (rmServiceParameters != other.rmServiceParameters) return false
        if (mmServiceParameters != other.mmServiceParameters) return false
        if (dmServiceParameters != other.dmServiceParameters) return false
        if (acServiceParameters != other.acServiceParameters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = user.hashCode()
        result = 31 * result + coreType.hashCode()
        result = 31 * result + cryptoType.hashCode()
        result = 31 * result + versionNumber
        result = 31 * result + (rmServiceParameters?.hashCode() ?: 0)
        result = 31 * result + mmServiceParameters.hashCode()
        result = 31 * result + dmServiceParameters.hashCode()
        result = 31 * result + (acServiceParameters?.hashCode() ?: 0)
        return result
    }
}

val myJson = Json {
    encodeDefaults = true
    serializersModule = SerializersModule {
        /** AC */
        polymorphic(ACServiceParameters::class) {
            subclass(ACServiceRBACXACMLAuthzForceParameters::class)
            subclass(ACServiceRBACOPAParameters::class)
            subclass(ACServiceRBACDynSecParameters::class)
        }


        /** DM */
        polymorphic(DMServiceParameters::class) {
            subclass(DMServiceCryptoACParameters::class)
            subclass(DMServiceMQTTParameters::class)
            subclass(DMServiceLocalParameters::class)
        }


        /** MM */
        polymorphic(MMServiceParameters::class) {
            subclass(MMServiceMySQLParameters::class)
            subclass(MMServiceRedisParameters::class)
            subclass(MMServiceRBACLocalParameters::class)
        }


        /** RM */
        polymorphic(RMServiceParameters::class) {
            subclass(RMServiceRBACCryptoACParameters::class)
        }


        /** Core */
        polymorphic(CoreParameters::class) {
            subclass(CoreParametersRBAC::class)
        }
    }
}