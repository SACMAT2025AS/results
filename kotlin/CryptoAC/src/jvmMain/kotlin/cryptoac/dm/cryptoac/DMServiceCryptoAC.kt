package cryptoac.dm.cryptoac

import cryptoac.*
import cryptoac.OutcomeCode.*
import cryptoac.core.CoreParameters
import cryptoac.core.CoreParametersRBAC
import cryptoac.core.CoreType
import cryptoac.tuple.Resource
import cryptoac.core.myJson
import cryptoac.dm.DMServiceABAC
import cryptoac.dm.DMServiceRBAC
import cryptoac.code.CodeResource
import cryptoac.code.CodeBoolean
import cryptoac.code.CodeServiceParameters
import cryptoac.server.API
import cryptoac.server.SERVER
import cryptoac.tuple.TupleStatus
import cryptoac.tuple.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.jetty.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.net.ConnectException
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Class implementing the methods for invoking the APIs of
 * a simple storage service for files configured with the given
 * [dmServiceParameters]
 */
class DMServiceCryptoAC(
    private val dmServiceParameters: DMServiceCryptoACParameters,
) : DMServiceRBAC, DMServiceABAC {

    /** The base API path (url + port) of the DM */
    private val dmBaseAPI = "${dmServiceParameters.url}:${dmServiceParameters.port}"

    /** The Http client */
    private var client: HttpClient? = null

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
            throw IllegalArgumentException(message)
        }

        logger.info { "Initializing the DM" }

        val serviceStatus = alreadyConfigured()
        if (serviceStatus.code != CODE_000_SUCCESS) {
            return serviceStatus.code
        } else if (serviceStatus.boolean) {
            logger.warn { "The DM was already initialized" }
            return CODE_077_SERVICE_ALREADY_CONFIGURED
        }

        var code: OutcomeCode = CODE_000_SUCCESS
        runBlocking {
            val dmURL = "${API.HTTPS}${dmBaseAPI}${API.DM}${CoreType.RBAC_FILES}/"
            logger.info { "Configuring the DM sending a POST request to $dmURL" }
            try {
                val dmResponse = client!!.post {
                    url(dmURL)
                    contentType(ContentType.Application.Json)
                    setBody(parameters.acServiceParameters)
                }
                logger.debug { "Received response from the DM" }
                if (dmResponse.status != HttpStatusCode.OK) {
                    code = myJson.decodeFromString(dmResponse.bodyAsText())
                    logger.warn {
                        "Received error code from the DM (code: $code, status: ${dmResponse.status})"
                    }
                }
            } catch (e: ConnectException) {
                if (e.message == "Connection refused") {
                    logger.warn { "The connection was refused" }
                    code = CODE_044_DM_CONNECTION_TIMEOUT
                } else {
                    throw e
                }
            }
        }
        return code
    }

    override fun init() {
        logger.info { "No action required to initialize the DM RBAC CryptoAC service" }
    }

    override fun deinit() {
        logger.info { "No action required to de-initialize the DM RBAC CryptoAC service" }
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
            serviceParameters = DMServiceCryptoACParameters(
                username = username,
                password = newPassword,
                port = dmServiceParameters.port,
                url = dmServiceParameters.url,
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

        /** Guard clauses */
        if (username.isBlank()) {
            logger.warn { "Username is blank" }
            return CODE_020_INVALID_PARAMETER
        }
        if (username == Constants.ADMIN) {
            logger.warn { "Cannot delete the ${Constants.ADMIN} user" }
            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
        }

        // TODO to implement when we will have login with users

        return CODE_000_SUCCESS
    }



    override fun addResource(
        newResource: Resource,
        resourceContent: InputStream
    ): OutcomeCode {

        /** Guard clauses */
        if (newResource.name.isBlank()) {
            logger.warn { "Resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }
        if (newResource.versionNumber != 1) {
            logger.warn { "Resource version number is not 1 (${newResource.versionNumber})" }
            return CODE_020_INVALID_PARAMETER
        }

        logger.info {
            "Adding a resource with name ${newResource.name} " +
                "and version number ${newResource.versionNumber}"
        }

        var code = CODE_000_SUCCESS
        runBlocking {
            val dmURL = "${API.HTTPS}$dmBaseAPI${API.DM}resources/${CoreType.RBAC_FILES}/"
            logger.info { "Uploading the resource ${newResource.name} in the DM sending a post request to $dmURL" }
            try {
                val dmResponse: HttpResponse = client!!.submitFormWithBinaryData(
                    url = dmURL,
                    formData = formData {
                        // TODO find better method to send resource without loading the stream into memory
                        append(
                            SERVER.RESOURCE_NAME, resourceContent.readBytes(),
                            Headers.build {
                                append(HttpHeaders.ContentType, "application/octet-stream")
                                append(HttpHeaders.ContentDisposition, "filename=${newResource.name}")
                            }
                        )
                        append(
                            key = SERVER.RESOURCE_VERSION_NUMBER,
                            value = newResource.versionNumber,
                        )
                    }
                ) {
                    method = HttpMethod.Post
                }
                logger.debug { "Received response from the DM" }
                if (dmResponse.status != HttpStatusCode.OK) {
                    code = myJson.decodeFromString(dmResponse.bodyAsText())
                    logger.warn {
                        "Received error code from the DM (code: $code, status: ${dmResponse.status})"
                    }
                }
            } catch (e: ConnectException) {
                if (e.message == "Connection refused") {
                    logger.warn { "The connection was refused" }
                    code = CODE_044_DM_CONNECTION_TIMEOUT
                } else {
                    throw e
                }
            }
        }
        return code
    }

    /**
     * TODO DOC
     */
    override fun readResource(
        resourceName: String,
        //resourceVersionNumber: Int
    ): CodeResource {

        /** Guard clauses */
        if (resourceName.isBlank()) {
            logger.warn { "Resource name is blank" }
            return CodeResource(CODE_020_INVALID_PARAMETER)
        }

        logger.info {
            "Reading resource with name $resourceName "
                    // "and version number $resourceVersionNumber"
        }

        var code = CODE_000_SUCCESS
        var stream: InputStream? = null
        runBlocking {
            var dmURL = "${API.HTTPS}$dmBaseAPI${API.DM}resources/${CoreType.RBAC_FILES}/$resourceName/1"

            // TODO temporarily, we add the username query parameter to tell the DM what is the username.
            //  In the future, this should be replaced by the login/authentication method used by the DM
            dmURL += "?${SERVER.USERNAME}=${dmServiceParameters.username}"

            logger.info { "Downloading the resource $resourceName from the DM sending a get request to $dmURL" }
            try {
                client!!.prepareGet(dmURL).execute { response: HttpResponse ->
                    logger.debug { "Received response from the DM" }
                    if (response.status != HttpStatusCode.OK) {
                        code = myJson.decodeFromString(response.bodyAsText())
                        logger.warn {
                            "Received error code from the DM (code: $code, status: ${response.status})"
                        }
                    } else {

                        var byteArray = byteArrayOf()
                        val channel: ByteReadChannel = response.body()
                        while (!channel.isClosedForRead) {
                            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                            while (!packet.isEmpty) {
                                byteArray += packet.readBytes()
                            }
                        }
                        stream = byteArray.inputStream()
                        // TODO this is extremely inefficient, as we read the whole file into memory and
                        //  then create an ad-hoc stream. It would be better to directly stream the response
                        //  from the DM. However, how to do it?
                    }
                }
            } catch (e: ConnectException) {
                if (e.message == "Connection refused") {
                    logger.warn { "The connection was refused" }
                    code = CODE_044_DM_CONNECTION_TIMEOUT
                } else {
                    throw e
                }
            }
        }
        return CodeResource(code, stream)
    }


    /**
     * In this implementation, ask the DM to move the
     * new resource from the upload folder to the download
     * folder. The [resourceContent] argument is not used.
     */
    override fun writeResource(
        updatedResource: Resource,
        resourceContent: InputStream
    ): OutcomeCode {

        /** Guard clauses */
        if (updatedResource.name.isBlank()) {
            logger.warn { "Resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        logger.info {
            "Writing a resource with name ${updatedResource.name} " +
                    "and version number ${updatedResource.versionNumber}"
        }

        var code = CODE_000_SUCCESS
        runBlocking {
            var dmURL = "${API.HTTPS}$dmBaseAPI${API.DM}resources/${CoreType.RBAC_FILES}/${updatedResource.name}/1"

            // TODO temporarily, we add the username query parameter to tell the DM what is the username.
            //  In the future, this should be replaced by the login/authentication method used by the DM
            dmURL += "?${SERVER.USERNAME}=${dmServiceParameters.username}"

            logger.info { "Overwriting the resource ${updatedResource.name} in the DM sending a put request to $dmURL" }
            try {
                val dmResponse: HttpResponse = client!!.put {
                    url(dmURL)
                }
                logger.debug { "Received response from the DM" }
                if (dmResponse.status != HttpStatusCode.OK) {
                    code = myJson.decodeFromString(dmResponse.bodyAsText())
                    logger.warn {
                        "Received error code from the DM (code: $code, status: ${dmResponse.status})"
                    }
                }
            } catch (e: ConnectException) {
                if (e.message == "Connection refused") {
                    logger.warn { "The connection was refused" }
                    code = CODE_044_DM_CONNECTION_TIMEOUT
                } else {
                    throw e
                }
            }
        }
        return code
    }

    /**
     *  TODO DOC
     */
    override fun deleteResource(
        resourceName: String
    ): OutcomeCode {

        /** Guard clauses */
        if (resourceName.isBlank()) {
            logger.warn { "Resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        logger.info {
            "Deleting resource with name $resourceName "
        }

        var code = CODE_000_SUCCESS
        runBlocking {
            var dmURL = "${API.HTTPS}$dmBaseAPI${API.DM}resources/${CoreType.RBAC_FILES}/$resourceName/1"

            // TODO temporarily, we add the username query parameter to tell the DM what is the username.
            //  In the future, this should be replaced by the login/authentication method used by the DM
            dmURL += "?${SERVER.USERNAME}=${dmServiceParameters.username}"

            logger.info { "Delete the resource $resourceName from the DM sending a delete request to $dmURL" }
            try {
                val dmResponse = client!!.delete {
                    url(dmURL)
                }
                logger.debug { "Received response from the DM" }
                if (dmResponse.status != HttpStatusCode.OK) {
                    code = myJson.decodeFromString(dmResponse.bodyAsText())
                    logger.warn {
                        "Received error code from the DM (code: $code, status: ${dmResponse.status})"
                    }
                }
            } catch (e: ConnectException) {
                if (e.message == "Connection refused") {
                    logger.warn { "The connection was refused" }
                    code = CODE_044_DM_CONNECTION_TIMEOUT
                } else {
                    throw e
                }
            }
        }
        return code
    }

    /**
     * Delete the [resourceName] from the temporary
     * storage and return the outcome code.
     * Check that the resource exists
     */
    fun deleteTemporaryResource(
        resourceName: String
    ): OutcomeCode {

        var code = CODE_000_SUCCESS
        runBlocking {
            var dmURL = "${API.HTTPS}$dmBaseAPI${API.DM}temporaryResources/${CoreType.RBAC_FILES}/$resourceName"

            // TODO temporarily, we add the username query parameter to tell the DM what is the username.
            //  In the future, this should be replaced by the login/authentication method used by the DM
            dmURL += "?${SERVER.USERNAME}=${dmServiceParameters.username}"

            logger.info { "Delete the temporary resource $resourceName from the DM sending a delete request to $dmURL" }
            try {
                val dmResponse = client!!.delete {
                    url(dmURL)
                }
                logger.debug { "Received response from the DM" }
                if (dmResponse.status != HttpStatusCode.OK) {
                    code = myJson.decodeFromString(dmResponse.bodyAsText())
                    logger.warn {
                        "Received error code from the DM (code: $code, status: ${dmResponse.status})"
                    }
                }
            } catch (e: ConnectException) {
                if (e.message == "Connection refused") {
                    logger.warn { "The connection was refused" }
                    code = CODE_044_DM_CONNECTION_TIMEOUT
                } else {
                    throw e
                }
            }
        }
        return code
    }



    /**
     * In this implementation, the lock-unlock-rollback mechanism is not needed,
     * as the RM is the entity which validates users' operations. However, to keep
     * streaming when downloading resources (and avoid the "Channel has been cancelled"
     * exception), we need to store a reference to the server and the HTTP response
     */
    override fun lock(): OutcomeCode {
        return if (locks == 0) {
            logger.info { "Locking the status of the DM" }
            locks++
            client = HttpClient(Jetty) {
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
            CODE_000_SUCCESS
        } else if (locks > 0) {
            locks++
            logger.debug { "Increment lock number to $locks" }
            CODE_000_SUCCESS
        } else {
            logger.warn { "Lock number is negative ($locks)" }
            CODE_031_LOCK_CALLED_IN_INCONSISTENT_STATUS
        }
    }

    /** In this implementation, close the Http client */
    override fun rollback(): OutcomeCode {
        return if (locks == 1) {
            logger.info { "Rollback the status of the DM" }
            locks--
            client!!.close()
            CODE_000_SUCCESS
        } else if (locks > 1) {
            locks--
            logger.debug { "Decrement lock number to $locks" }
            CODE_000_SUCCESS
        } else {
            logger.warn { "DM rollback number is zero or negative ($locks)" }
            CODE_033_ROLLBACK_CALLED_IN_INCONSISTENT_STATUS
        }
    }

    /** In this implementation, close the Http client */
    override fun unlock(): OutcomeCode {
        return if (locks == 1) {
            logger.info { "Unlock the status of the DM" }
            locks--
            client!!.close()
            CODE_000_SUCCESS
        } else if (locks > 1) {
            locks--
            logger.debug { "Decrement lock number to $locks" }
            CODE_000_SUCCESS
        } else {
            logger.warn { "DM unlock number is zero or negative ($locks)" }
            CODE_032_UNLOCK_CALLED_IN_INCONSISTENT_STATUS
        }
    }
}
