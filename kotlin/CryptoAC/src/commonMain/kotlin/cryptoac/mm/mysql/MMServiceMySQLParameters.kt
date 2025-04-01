package cryptoac.mm.mysql

import cryptoac.mm.MMServiceParameters
import cryptoac.mm.MMType
import cryptoac.parameters.RemoteServiceParameters
import cryptoac.parameters.ServiceParameters
import cryptoac.parameters.UPServiceParameters
import cryptoac.server.SERVER
import cryptoac.view.CryptoACFormField
import cryptoac.view.InputType
import kotlinx.serialization.Serializable

/**
 * Class defining the parameters required to configure an MM object
 * of type either [MMType.RBAC_MYSQL] or [MMType.ABAC_MYSQL]
 */
@Serializable
class MMServiceMySQLParameters(
    override var username: String,
    override var password: String,
    override var port: Int,
    override var url: String,
    override val mmType: MMType,
) : MMServiceParameters,
    RemoteServiceParameters,
    UPServiceParameters {

    companion object {
        /**
         * Create a MMServiceMySQLParameters object of
         * type [mmType] from the given map of [parameters].
         * Missing values will cause a NPE
         */
        // TODO improve behaviour, here but also in all other
        //  modules that have the fromMap method, which they
        //  say that "Missing values will cause the return
        //  object to be null", but I do think that they
        //  throw an NPE as well instead
        fun fromMap(
            parameters: HashMap<String, String>,
            mmType: MMType
        ): MMServiceMySQLParameters {
            return MMServiceMySQLParameters(
                username = parameters[SERVER.USERNAME]!!,
                port = parameters[SERVER.MM_PORT]!!.toInt(),
                url = parameters[SERVER.MM_URL]!!,
                password = parameters[SERVER.MM_PASSWORD]!!,
                mmType = mmType
            )
        }

        /**
         * Create a list of CryptoAC form
         * fields from the given [parameters]
         */
        fun toMap(parameters: MMServiceMySQLParameters? = null) = listOf(
            listOf(
                CryptoACFormField(
                    SERVER.MM_URL,
                    SERVER.MM_URL.replace("_", " "),
                    InputType.text,
                    className = "darkTextField",
                    defaultValue = parameters?.url
                ),
                CryptoACFormField(
                    SERVER.MM_PASSWORD,
                    SERVER.MM_PASSWORD.replace("_", " "),
                    InputType.password,
                    className = "darkTextField",
                    defaultValue = parameters?.password
                ),
                CryptoACFormField(
                    SERVER.MM_PORT,
                    SERVER.MM_PORT.replace("_", " "),
                    InputType.number,
                    className = "darkTextField",
                    defaultValue = parameters?.port.toString()
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
        if (other !is MMServiceMySQLParameters) return false

        if (username != other.username) return false
        if (password != other.password) return false
        if (port != other.port) return false
        if (url != other.url) return false
        if (mmType != other.mmType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + port
        result = 31 * result + url.hashCode()
        result = 31 * result + mmType.hashCode()
        return result
    }
}
