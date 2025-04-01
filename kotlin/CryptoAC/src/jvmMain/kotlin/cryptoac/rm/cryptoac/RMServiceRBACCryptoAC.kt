package cryptoac.rm.cryptoac

import cryptoac.server.API
import cryptoac.server.API.HTTPS
import cryptoac.Constants
import cryptoac.OutcomeCode
import cryptoac.OutcomeCode.*
import cryptoac.ac.opa.ACServiceRBACOPAParameters
import cryptoac.core.CoreParameters
import cryptoac.core.CoreParametersRBAC
import cryptoac.core.CoreType
import cryptoac.core.myJson
import cryptoac.dm.cryptoac.DMServiceCryptoACParameters
import cryptoac.encodeBase64
import cryptoac.code.CodeBoolean
import cryptoac.code.CodeServiceParameters
import cryptoac.tuple.Resource
import cryptoac.tuple.RolePermission
import cryptoac.tuple.User
import cryptoac.rm.RMServiceRBAC
import cryptoac.rm.model.AddResourceRBACRequest
import cryptoac.rm.model.WriteResourceRBACRequest
import cryptoac.tuple.TupleStatus
import io.ktor.client.*
import io.ktor.client.engine.jetty.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import java.lang.IllegalStateException
import java.net.ConnectException
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Class implementing the methods for invoking the APIs of an RM
 * configured with the given [rmServiceParameters] for RB-CAC
 */
class RMServiceRBACCryptoAC(
    private val rmServiceParameters: RMServiceRBACCryptoACParameters,
) : RMServiceRBAC {

    /** The base API path (url + port) of the RM */
    private val rmBaseAPI = "${rmServiceParameters.url}:${rmServiceParameters.port}"

    override var locks = 0


    override fun alreadyConfigured(): CodeBoolean {
        // TODO to implement
        return CodeBoolean(boolean = false)
    }

    /** In this implementation, deny subsequent invocations */
    override fun configure(
        parameters: CoreParameters?
    ): OutcomeCode {

        /** Guard clauses */
        if (parameters !is CoreParametersRBAC) {
            val message = "Given wrong parameters for update"
            logger.error { message }
            throw IllegalStateException(message)
        }

        logger.info { "Initializing the RM" }

        val serviceStatus = alreadyConfigured()
        if (serviceStatus.code != CODE_000_SUCCESS) {
            return serviceStatus.code
        } else if (serviceStatus.boolean) {
            logger.warn { "The RM was already initialized" }
            return OutcomeCode.CODE_077_SERVICE_ALREADY_CONFIGURED
        }

        var code: OutcomeCode = CODE_000_SUCCESS
        runBlocking {
            val rmURL = "$HTTPS${rmBaseAPI}${API.RM}${CoreType.RBAC_FILES}/"
            logger.info { "Configuring the RM sending a POST request to $rmURL" }
            try {
                getKtorClient().use {
                    val rmResponse = it.post {
                        url(rmURL)
                        contentType(ContentType.Application.Json)
                        setBody(
                            RMRBACCryptoACParameters(
                                crypto = parameters.cryptoType,
                                mmServiceParameters = parameters.mmServiceParameters,
                                acServiceParameters = parameters.acServiceParameters as ACServiceRBACOPAParameters?,
                                dmServiceCryptoACParameters = parameters.dmServiceParameters as DMServiceCryptoACParameters
                            )
                        )
                    }
                    logger.debug { "Received response from the RM" }
                    if (rmResponse.status != HttpStatusCode.OK) {
                        code = myJson.decodeFromString(rmResponse.bodyAsText())
                        logger.warn {(
                            "Received error code from the RM (code:"
                            + " $code, status: ${rmResponse.status})"
                        )}
                    }
                }
            } catch (e: ConnectException) {
                if (e.message == "Connection refused") {
                    logger.warn { "The connection was refused" }
                    code = OutcomeCode.CODE_043_RM_CONNECTION_TIMEOUT
                } else {
                    throw e
                }
            }
        }
        return code
    }

    override fun init() {
        logger.info { "No action required to initialize the RM RBAC CryptoAC service" }
    }

    override fun deinit() {
        logger.info { "No action required to de-initialize the RM RBAC CryptoAC service" }
    }

    override fun addAdmin(
        newAdmin: User
    ): OutcomeCode {
        // TODO to implement when we will have login with users
        /** Guard clauses */
        if (newAdmin.name != Constants.ADMIN) {
            logger.warn {
                "Admin user has name ${newAdmin.name}" +
                ", but admin name should be ${Constants.ADMIN}"
            }
            return CODE_036_ADMIN_NAME
        }

        return CODE_000_SUCCESS
    }

    override fun initUser(
        user: User
    ): OutcomeCode {
        // TODO to implement when we will have login with users
        return CODE_000_SUCCESS
    }

    override fun addUser(
        newUser: User
    ): CodeServiceParameters {
        val username = newUser.name

        /** Guard clauses */
        if (username.isBlank() ) {
            logger.warn { "Username is blank" }
            return CodeServiceParameters(CODE_020_INVALID_PARAMETER)
        }

        /** TODO check password generation */
        val passwordBytes = ByteArray(20)
        Random().nextBytes(passwordBytes)
        val newPassword = passwordBytes.encodeBase64()

        // TODO to implement when we will have login with users

        return CodeServiceParameters(
            serviceParameters = RMServiceRBACCryptoACParameters(
                username = username,
                password = newPassword,
                port = rmServiceParameters.port,
                url = rmServiceParameters.url,
            )
        )
    }

    override fun getUsers(
        username: String?,
        status: TupleStatus?,
        isAdmin: Boolean,
        offset: Int,
        limit: Int
    ): HashSet<User> {
        TODO("Not yet implemented")
    }

    override fun deleteUser(
        username: String
    ): OutcomeCode {
        // TODO to implement when we will have login with users
        return CODE_000_SUCCESS
    }



    override fun checkAddResource(
        newResource: Resource,
        adminRolePermission: RolePermission
    ): OutcomeCode {

        var code = CODE_000_SUCCESS
        runBlocking {
            val rmURL = "$HTTPS$rmBaseAPI${API.RM}resources/${CoreType.RBAC_FILES}/"
            logger.info {(
                "Asking the RM to check an add resource "
                + "operation sending a POST request to $rmURL"
            )}
            val addResourceRBACRequest = AddResourceRBACRequest(
                resource = newResource,
                rolePermission = adminRolePermission,
            )
            try {
                getKtorClient().use {
                    val rmResponse = it.post {
                        url(rmURL)
                        contentType(ContentType.Application.Json)
                        setBody(addResourceRBACRequest)
                    }
                    logger.debug { "Received response from the RM" }
                    if (rmResponse.status != HttpStatusCode.OK) {
                        code = myJson.decodeFromString(rmResponse.bodyAsText())
                        logger.warn {(
                            "Received error code from the RM (code: "
                            + "$code, status: ${rmResponse.status})"
                        )}
                    }
                }
            } catch (e: ConnectException) {
                if (e.message == "Connection refused") {
                    logger.warn { "The connection was refused" }
                    code = OutcomeCode.CODE_043_RM_CONNECTION_TIMEOUT
                } else {
                    throw e
                }
            }
        }
        return code
    }

    override fun checkWriteResource(
        roleName: String,
        symKeyVersionNumber: Int,
        newResource: Resource
    ): OutcomeCode {

        var code = CODE_000_SUCCESS
        runBlocking {
            val rmURL = "$HTTPS$rmBaseAPI${API.RM}resources/${CoreType.RBAC_FILES}/"
            logger.info {(
                "Asking the RM to check a write resource "
                + "operation sending a PATCH request to $rmURL"
            )}
            val writeResourceRBACRequest = WriteResourceRBACRequest(
                username = rmServiceParameters.username,
                roleName = roleName,
                resource = newResource,
            )
            try {
                getKtorClient().use {
                    val rmResponse = it.patch {
                        url(rmURL)
                        contentType(ContentType.Application.Json)
                        setBody(writeResourceRBACRequest)
                    }
                    logger.debug { "Received response from the RM" }
                    if (rmResponse.status != HttpStatusCode.OK) {
                        code = myJson.decodeFromString(rmResponse.bodyAsText())
                        logger.warn {(
                            "Received error code from the RM (code: "
                            + "$code, status: ${rmResponse.status})"
                        )}
                    }
                }
            } catch (e: ConnectException) {
                if (e.message == "Connection refused") {
                    logger.warn { "The connection was refused" }
                    code = CODE_043_RM_CONNECTION_TIMEOUT
                } else {
                    throw e
                }
            }
        }
        return code
    }



    /** Return the Ktor Http client */
    private fun getKtorClient(): HttpClient = HttpClient(Jetty) {
        expectSuccess = false
        install(HttpCookies) /** To accept all cookies */
        install(ContentNegotiation) {
            json(json = myJson)
        }
        // TODO configure this, as for now the client accepts all certificates
        engine {
            sslContextFactory.isTrustAll = true
        }
    }



    /** TODO this function has to be designed and then implemented */
    override fun lock(): OutcomeCode {
        return CODE_000_SUCCESS
    }

    /** TODO this function has to be designed and then implemented */
    override fun rollback(): OutcomeCode {
        return CODE_000_SUCCESS
    }

    /** TODO this function has to be designed and then implemented */
    override fun unlock(): OutcomeCode {
        return CODE_000_SUCCESS
    }
}
