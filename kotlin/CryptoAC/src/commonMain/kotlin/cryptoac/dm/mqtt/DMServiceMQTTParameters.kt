package cryptoac.dm.mqtt

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
 * of type [DMType.MQTT]. Beside the inherited fields, a
 * DMServiceMQTTParameters is defined by a boolean flag
 * [tls] (true if the Mosquitto broker is using tls)
 */
@Serializable
class DMServiceMQTTParameters(
    override var username: String,
    override var password: String,
    override var port: Int,
    override var url: String,
    var tls: Boolean
) : DMServiceParameters,
    RemoteServiceParameters,
    UPServiceParameters {

    override val dmType: DMType = DMType.MQTT

    companion object {
        /**
         * Create a DMServiceMQTTParameters object from the given map of
         * [parameters]. Missing values will cause the return object to be null
         */
        fun fromMap(parameters: HashMap<String, String>): DMServiceMQTTParameters {
            return DMServiceMQTTParameters(
                username = parameters[SERVER.USERNAME]!!,
                port = parameters[SERVER.DM_PORT]!!.toInt(),
                url = parameters[SERVER.DM_URL]!!,
                password = parameters[SERVER.DM_PASSWORD]!!,
                tls = parameters[SERVER.DM_TLS]!!.toBooleanStrict()
            )
        }

        /**
         * Create a list of CryptoAC form
         * fields from the given [parameters]
         */
        fun toMap(parameters: DMServiceMQTTParameters? = null) = listOf(
            listOf(
                CryptoACFormField(
                    SERVER.DM_URL,
                    SERVER.DM_URL.replace("_", " "),
                    InputType.text,
                    className = "darkTextField",
                    defaultValue = parameters?.url
                ),
                CryptoACFormField(
                    SERVER.DM_PASSWORD,
                    SERVER.DM_PASSWORD.replace("_", " "),
                    InputType.password,
                    className = "darkTextField",
                    defaultValue = parameters?.password
                ),
                CryptoACFormField(
                    SERVER.DM_PORT,
                    SERVER.DM_PORT.replace("_", " "),
                    InputType.number,
                    className = "darkTextField",
                    defaultValue = parameters?.port.toString()
                ),
                CryptoACFormField(
                    SERVER.DM_TLS,
                    SERVER.DM_TLS,
                    InputType.checkBox,
                    className = "darkTextField",
                    defaultValue = parameters?.tls.toString()
                ),
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
        if (other !is DMServiceMQTTParameters) return false

        if (username != other.username) return false
        if (password != other.password) return false
        if (port != other.port) return false
        if (url != other.url) return false
        if (tls != other.tls) return false
        if (dmType != other.dmType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + port
        result = 31 * result + url.hashCode()
        result = 31 * result + tls.hashCode()
        result = 31 * result + dmType.hashCode()
        return result
    }
}
