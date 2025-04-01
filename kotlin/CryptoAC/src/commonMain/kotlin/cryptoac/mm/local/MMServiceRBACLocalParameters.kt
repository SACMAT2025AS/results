package cryptoac.mm.local

import cryptoac.SafeRegex
import cryptoac.mm.MMServiceParameters
import cryptoac.mm.MMType
import cryptoac.parameters.RemoteServiceParameters
import cryptoac.parameters.ServiceParameters
import cryptoac.parameters.UPServiceParameters
import cryptoac.server.SERVER
import cryptoac.view.CryptoACFormField
import cryptoac.view.InputType
import io.ktor.utils.io.core.*
import kotlinx.serialization.Serializable

/**
 * Class defining the parameters required to configure an MM object
 * of type [MMType.RBAC_LOCAL]. Beside
 * the inherited fields, a MMServiceRBACLocalParameters is defined by
 * the [mmType]
 */
@Serializable
class MMServiceRBACLocalParameters(
    override val mmType: MMType
) : MMServiceParameters {

    companion object {
        /**
         * Create a MMServiceRBACLocalParameters object of
         * type [mmType] from the given map of [parameters].
         * Missing values will cause a NPE // TODO improve behaviour
         */
        fun fromMap(
            parameters: HashMap<String, String>,
            mmType: MMType
        ): MMServiceRBACLocalParameters {
            return MMServiceRBACLocalParameters(
                mmType = mmType
            )
        }

        /**
         * Create a list of CryptoAC form
         * fields from the given [parameters]
         */
        fun toMap(parameters: MMServiceRBACLocalParameters? = null) = listOf(
            listOf<CryptoACFormField>()
        )
    }

    override fun checkParameters(): Boolean = true

    override fun update(updatedParameters: ServiceParameters) {

    }

    override fun obscureSensitiveFields() {

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MMServiceRBACLocalParameters

        return mmType == other.mmType
    }

    override fun hashCode(): Int {
        return mmType.hashCode()
    }

}