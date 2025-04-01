package cryptoac.dm.local

import cryptoac.*
import cryptoac.OutcomeCode.*
import cryptoac.code.CodeBoolean
import cryptoac.code.CodeResource
import cryptoac.code.CodeServiceParameters
import cryptoac.core.CoreParameters
import cryptoac.core.CoreParametersRBAC
import cryptoac.core.CoreType
import cryptoac.core.myJson
import cryptoac.dm.DMServiceABAC
import cryptoac.dm.DMServiceRBAC
import cryptoac.dm.cryptoac.DMServiceCryptoACParameters
import cryptoac.server.API
import cryptoac.server.FileSaveMode
import cryptoac.server.FileSystemManager
import cryptoac.server.SERVER
import cryptoac.tuple.Resource
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
import java.io.File
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.net.ConnectException
import java.util.*

private val logger = KotlinLogging.logger { }

/**
 * Class implementing the methods for invoking the APIs of
 * a local storage service for files configured with the given
 * [dmServiceParameters]
 */
class DMLocal(
    private val dmServiceParameters: DMServiceLocalParameters,
) : DMServiceRBAC, DMServiceABAC {

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
            return OutcomeCode.CODE_077_SERVICE_ALREADY_CONFIGURED
        }

        return CODE_000_SUCCESS
    }

    override fun init() {
        logger.info { "No action required to initialize the DM local service" }
    }

    override fun deinit() {
        logger.info { "No action required to de-initialize the DM local service" }
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
            return OutcomeCode.CODE_036_ADMIN_NAME
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
            return CodeServiceParameters(OutcomeCode.CODE_020_INVALID_PARAMETER)
        }

        /** TODO check password generation */
        val passwordBytes = ByteArray(20)
        Random().nextBytes(passwordBytes)
        val newPassword = passwordBytes.encodeBase64()

        // TODO to implement when we will have login with users

        return CodeServiceParameters(
            serviceParameters = DMServiceLocalParameters()
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
            return OutcomeCode.CODE_020_INVALID_PARAMETER
        }
        if (username == Constants.ADMIN) {
            logger.warn { "Cannot delete the ${Constants.ADMIN} user" }
            return OutcomeCode.CODE_022_ADMIN_CANNOT_BE_MODIFIED
        }

        // TODO to implement when we will have login with users

        return CODE_000_SUCCESS
    }



    override fun addResource(
        newResource: Resource,
        resourceContent: InputStream
    ): OutcomeCode {
        val versionNumber = 1

        /** Guard clauses */
        if (newResource.name.isBlank()) {
            logger.warn { "Resource name is blank" }
            return OutcomeCode.CODE_020_INVALID_PARAMETER
        }
        if (versionNumber != 1) {
            logger.warn { "Resource version number is not 1 (${versionNumber})" }
            return OutcomeCode.CODE_020_INVALID_PARAMETER
        }

        logger.info {
            "Adding a resource with name ${newResource.name} " +
                "and version number ${versionNumber}"
        }

        var code = CODE_000_SUCCESS

        logger.info { "Uploading the resource ${newResource.name}" }

        try {
            FileSystemManager.saveFile(
                path = "${UPLOAD_DIRECTORY.absolutePath}/${
                    FileSystemManager.getFileKey(
                        fileName = newResource.name,
                        fileVersionNumber = 1,
                        core = CoreType.RBAC_FILES
                    )
                }",
                content = "default".inputStream(),
                saveMode = FileSaveMode.THROW_EXCEPTION,
            )
            CODE_000_SUCCESS
        } catch (e: FileAlreadyExistsException) {
            logger.warn {
                "Resource with name ${newResource.name} " +
                        "and version number ${versionNumber} " +
                        "already exists in the upload folder"
            }
            CODE_003_RESOURCE_ALREADY_EXISTS
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
            return CodeResource(OutcomeCode.CODE_020_INVALID_PARAMETER)
        }

        logger.info {
            "Reading resource with name $resourceName "
                    // "and version number $resourceVersionNumber"
        }

        var stream: InputStream? = null

        logger.info { "Downloading the resource $resourceName" }

        /** Get the resource */
        val resource = File(
            "${UPLOAD_DIRECTORY.absolutePath}/" +
                    FileSystemManager.getFileKey(resourceName, 1, CoreType.RBAC_FILES)
        )
        stream = resource.inputStream()

        return CodeResource(CODE_000_SUCCESS, stream)
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

        logger.info { "Overwriting the resource ${updatedResource.name}" }

        val newResource = File(
            "${UPLOAD_DIRECTORY.absolutePath}/" +
                    FileSystemManager.getFileKey(updatedResource.name, 1, CoreType.RBAC_FILES)
        )

        return if (!newResource.exists()) {
            logger.warn { "New content of resource ${updatedResource.name} not found" }
            CODE_006_RESOURCE_NOT_FOUND
        } else {
            FileSystemManager.saveFile(
                path = "${UPLOAD_DIRECTORY.absolutePath}/${
                    FileSystemManager.getFileKey(
                        fileName = updatedResource.name,
                        fileVersionNumber = 1,
                        core = CoreType.RBAC_FILES
                    )
                }",
                content = resourceContent,
                saveMode = FileSaveMode.OVERWRITE,
            )
            CODE_000_SUCCESS
        }
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
            return OutcomeCode.CODE_020_INVALID_PARAMETER
        }

        logger.info {
            "Deleting resource with name $resourceName "
        }

        logger.info { "Delete the resource $resourceName" }

        val resourceToDelete = File(
            "${DOWNLOAD_DIRECTORY.absolutePath}/" +
                    FileSystemManager.getFileKey(resourceName, 1, CoreType.RBAC_FILES)
        )

        /** Delete the resource */
        return if (!resourceToDelete.exists()) {
            logger.warn { "Resource $resourceName not found" }
            CODE_006_RESOURCE_NOT_FOUND
        } else {
            if (!resourceToDelete.delete()) {
                logger.warn { "Error while deleting resource $resourceName" }
                CODE_024_RESOURCE_DELETE
            } else {
                CODE_000_SUCCESS
            }
        }
    }

    /**
     * In this implementation, the lock-unlock-rollback mechanism is not needed,
     * as the RM is the entity which validates users' operations. However, to keep
     * streaming when downloading resources (and avoid the "Channel has been cancelled"
     * exception), we need to store a reference to the server and the HTTP response
     */
    override fun lock(): OutcomeCode {
        return CODE_000_SUCCESS
//        return if (locks == 0) {
//            logger.info { "Locking the status of the DM" }
//            locks++
//            CODE_000_SUCCESS
//        } else if (locks > 0) {
//            locks++
//            logger.debug { "Increment lock number to $locks" }
//            CODE_000_SUCCESS
//        } else {
//            logger.warn { "Lock number is negative ($locks)" }
//            CODE_031_LOCK_CALLED_IN_INCONSISTENT_STATUS
//        }
    }

    /** In this implementation, close the Http client */
    override fun rollback(): OutcomeCode {
        return CODE_000_SUCCESS

//        return if (locks == 1) {
//            logger.info { "Rollback the status of the DM" }
//            locks--
//            CODE_000_SUCCESS
//        } else if (locks > 1) {
//            locks--
//            logger.debug { "Decrement lock number to $locks" }
//            CODE_000_SUCCESS
//        } else {
//            logger.warn { "DM rollback number is zero or negative ($locks)" }
//            CODE_033_ROLLBACK_CALLED_IN_INCONSISTENT_STATUS
//        }
    }

    /** In this implementation, close the Http client */
    override fun unlock(): OutcomeCode {
        return CODE_000_SUCCESS

//        return if (locks == 1) {
//            logger.info { "Unlock the status of the DM" }
//            locks--
//            CODE_000_SUCCESS
//        } else if (locks > 1) {
//            locks--
//            logger.debug { "Decrement lock number to $locks" }
//            CODE_000_SUCCESS
//        } else {
//            logger.warn { "DM unlock number is zero or negative ($locks)" }
//            CODE_032_UNLOCK_CALLED_IN_INCONSISTENT_STATUS
//        }
    }
}