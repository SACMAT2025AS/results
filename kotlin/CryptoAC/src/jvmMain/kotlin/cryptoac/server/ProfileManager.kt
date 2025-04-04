package cryptoac.server

import cryptoac.USERS_PROFILE_DIRECTORY_PATH
import cryptoac.core.CoreParameters
import cryptoac.core.CoreParametersRBAC
import cryptoac.core.CoreType
import cryptoac.core.myJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

/**
 * Allow saving and load users' profiles. A profile consists
 * in the parameters of a user for a specific core type
 */
class ProfileManager {

    companion object {

        /** Return the key identifying the [username]'s profile for the given [core] */
        private fun getProfileKey(username: String, core: CoreType) =
            "$USERS_PROFILE_DIRECTORY_PATH$username.$core"

        /** Save the [username]'s [coreParameters] */
        fun saveProfile(username: String, coreParameters: CoreParameters) {
            logger.debug { "Saving profile for user $username" }
            FileSystemManager.saveFile(
                path = getProfileKey(username, coreParameters.coreType),
                content = when (coreParameters.coreType) {
                    CoreType.RBAC_FILES -> myJson.encodeToString(
                        coreParameters as CoreParametersRBAC
                    ).byteInputStream()
                    CoreType.RBAC_MQTT -> myJson.encodeToString(
                        coreParameters as CoreParametersRBAC
                    ).byteInputStream()
                    CoreType.ABAC_FILES -> TODO()
                    CoreType.ABAC_MQTT -> TODO()
                },
                saveMode = FileSaveMode.THROW_EXCEPTION
            )
        }

        /** Update the [username]'s [coreParameters] */
        fun updateProfile(username: String, coreParameters: CoreParameters) {
            logger.debug { "Updating profile for user $username" }
            FileSystemManager.saveFile(
                path = getProfileKey(username, coreParameters.coreType),
                content = when (coreParameters.coreType) {
                    CoreType.RBAC_FILES -> myJson.encodeToString(
                        coreParameters as CoreParametersRBAC
                    ).byteInputStream()
                    CoreType.RBAC_MQTT -> myJson.encodeToString(
                        coreParameters as CoreParametersRBAC
                    ).byteInputStream()
                    CoreType.ABAC_FILES -> TODO()
                    CoreType.ABAC_MQTT -> TODO()
                },
                saveMode = FileSaveMode.OVERWRITE
            )
        }

        /** Load the [username]'s profile for the given [coreType] */
        fun loadProfile(username: String, coreType: CoreType): CoreParameters? {
            logger.debug { "Loading profile for user $username" }
            val profileFile = File(getProfileKey(username, coreType))
            return if (profileFile.exists()) {
                val profileString = String(profileFile.inputStream().readAllBytes())
                when (coreType) {
                    CoreType.RBAC_FILES -> myJson.decodeFromString<CoreParametersRBAC>(profileString)
                    CoreType.RBAC_MQTT -> myJson.decodeFromString<CoreParametersRBAC>(profileString)
                    CoreType.ABAC_FILES -> TODO()
                    CoreType.ABAC_MQTT -> TODO()
                }
            } else {
                null
            }
        }

        /**
         * Delete the [username]'s profile for the given [core]
         * and return true if the operation was successful, false
         * if the profile does not exist
         */
        fun deleteProfile(username: String, core: CoreType): Boolean {
            logger.info { "Deleting profile of user $username for core $core" }
            val filePath = getProfileKey(username, core)
            val profileFile = File(filePath)
            return if (profileFile.exists()) {
                if (!profileFile.delete()) {
                    val message = "IO Error while deleting the profile"
                    logger.error { message }
                    throw IOException(message)
                }
                true
            } else {
                logger.warn { "profile does not exists" }
                false
            }
        }
    }
}
