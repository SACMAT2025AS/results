package cryptoac.rm.cryptoac

import cryptoac.parameters.RemoteServiceParameters
import cryptoac.parameters.ServiceParameters
import cryptoac.parameters.UPServiceParameters
import cryptoac.rm.RMServiceParameters
import cryptoac.rm.RMType
import cryptoac.server.SERVER
import cryptoac.view.CryptoACFormField
import cryptoac.view.InputType
import kotlinx.serialization.Serializable

/**
 * Class defining the parameters required to configure an RM object
 * of type [RMType.RBAC_CRYPTOAC]
 */
@Serializable
class RMServiceRBACCryptoACParameters(
    override var username: String,
    override var password: String,
    override var port: Int,
    override var url: String,
) : RMServiceParameters,
    RemoteServiceParameters,
    UPServiceParameters {

    override val rmType: RMType = RMType.RBAC_CRYPTOAC

    companion object {
        /**
         * Create a RMServiceRBACCryptoACParameters object from the given map of
         * [parameters]. Missing values will cause the return object to be null
         */
        fun fromMap(parameters: HashMap<String, String>): RMServiceRBACCryptoACParameters {
            return RMServiceRBACCryptoACParameters(
                username = parameters[SERVER.USERNAME]!!,
                port = parameters[SERVER.RM_PORT]!!.toInt(),
                url = parameters[SERVER.RM_URL]!!,
                password = parameters[SERVER.RM_PASSWORD]!!,
            )
        }

        /**
         * Create a list of CryptoAC form
         * fields from the given [parameters]
         */
        fun toMap(parameters: RMServiceRBACCryptoACParameters? = null) = listOf(
            listOf(
                CryptoACFormField(
                    SERVER.RM_URL,
                    SERVER.RM_URL.replace("_", " "),
                    InputType.text,
                    className = "darkTextField",
                    defaultValue = parameters?.url
                ),
                CryptoACFormField(
                    SERVER.RM_PASSWORD,
                    SERVER.RM_PASSWORD.replace("_", " "),
                    InputType.password,
                    className = "darkTextField",
                    defaultValue = parameters?.password
                ),
                CryptoACFormField(
                    SERVER.RM_PORT,
                    SERVER.RM_PORT.replace("_", " "),
                    InputType.number,
                    className = "darkTextField",
                    defaultValue = parameters?.port.toString()
                )
            )
        )
    }

    override fun checkParameters(): Boolean =
        (super<UPServiceParameters>.checkParameters()
                &&
                super<RemoteServiceParameters>.checkParameters())

    override fun update(updatedParameters: ServiceParameters) {
        super<UPServiceParameters>.update(updatedParameters)
        super<RemoteServiceParameters>.update(updatedParameters)
    }

    override fun obscureSensitiveFields() {
        super<UPServiceParameters>.obscureSensitiveFields()
        super<RemoteServiceParameters>.obscureSensitiveFields()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RMServiceRBACCryptoACParameters) return false

        if (username != other.username) return false
        if (password != other.password) return false
        if (port != other.port) return false
        if (url != other.url) return false
        if (rmType != other.rmType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + port
        result = 31 * result + url.hashCode()
        result = 31 * result + rmType.hashCode()
        return result
    }
}
