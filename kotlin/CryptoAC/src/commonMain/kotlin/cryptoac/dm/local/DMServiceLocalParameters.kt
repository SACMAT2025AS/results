package cryptoac.dm.local

import cryptoac.dm.DMServiceParameters
import cryptoac.dm.DMType
import cryptoac.parameters.RemoteServiceParameters
import cryptoac.parameters.ServiceParameters
import cryptoac.parameters.UPServiceParameters
import cryptoac.server.SERVER
import cryptoac.view.CryptoACFormField
import cryptoac.view.InputType
import kotlinx.serialization.Serializable

/**
 * Class defining the parameters required to configure a DM object
 * of type [DMType.LOCAL]
 */
@Serializable
class DMServiceLocalParameters : DMServiceParameters {

    override val dmType: DMType = DMType.LOCAL

    companion object {
        /**
         * Create a DMServiceLocalParameters object from the given map of
         * [parameters]. Missing values will cause the return object to be null
         */
        fun fromMap(parameters: HashMap<String, String>): DMServiceLocalParameters {
            return DMServiceLocalParameters()
        }

        /**
         * Create a list of CryptoAC form
         * fields from the given [parameters]
         */
        fun toMap(parameters: DMServiceLocalParameters? = null) = listOf(
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
        if (other !is DMServiceLocalParameters) return false

        if (dmType != other.dmType) return false

        return true
    }

    override fun hashCode(): Int {
        val result = dmType.hashCode()
        return result
    }
}
