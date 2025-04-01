package cryptoac.ac.opa

import cryptoac.ac.ACServiceParameters
import cryptoac.ac.ACType
import cryptoac.parameters.RemoteServiceParameters
import cryptoac.server.SERVER
import cryptoac.view.CryptoACFormField
import cryptoac.view.InputType
import kotlinx.serialization.Serializable

/**
 * Class defining the parameters required to configure an AC object
 * of type [ACType.RBAC_OPA]
 */
@Serializable
class ACServiceRBACOPAParameters(
    override var port: Int,
    override var url: String,
) : RemoteServiceParameters,
    ACServiceParameters {

    override val acType: ACType = ACType.RBAC_OPA

    companion object {
        /**
         * Create a ACServiceOPAParameters object from the given map of
         * [parameters]. Missing values will cause the return object to be null
         */
        fun fromMap(parameters: HashMap<String, String>): ACServiceRBACOPAParameters {
            return ACServiceRBACOPAParameters(
                url = parameters[SERVER.AC_URL]!!,
                port = parameters[SERVER.AC_PORT]!!.toInt()
            )
        }

        /**
         * Create a list of CryptoAC form
         * fields from the given [parameters]
         */
        fun toMap(parameters: ACServiceRBACOPAParameters? = null) = listOf(
            listOf(
                CryptoACFormField(
                    SERVER.AC_URL,
                    SERVER.AC_URL.replace("_", " "),
                    InputType.text,
                    className = "darkTextField",
                    defaultValue = parameters?.url
                ),
                CryptoACFormField(
                    SERVER.AC_PORT,
                    SERVER.AC_PORT.replace("_", " "),
                    InputType.number,
                    className = "darkTextField",
                    defaultValue = parameters?.port.toString()
                ),
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ACServiceRBACOPAParameters

        if (port != other.port) return false
        if (url != other.url) return false
        if (acType != other.acType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = port
        result = 31 * result + url.hashCode()
        result = 31 * result + acType.hashCode()
        return result
    }
}
