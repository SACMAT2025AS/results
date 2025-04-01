package cryptoac.server

import cryptoac.Parameters.aliceCoreCACRBACMQTTParameters
import cryptoac.Parameters.aliceUser
import cryptoac.Parameters.bobCoreCACRBACMQTTParameters
import cryptoac.TestUtilities
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.io.FileNotFoundException

internal class ProfileManagerTest {

    @AfterEach
    fun tearDown() {
        TestUtilities.deleteLocalCryptoACUsersProfiles()
    }

    @Test
    fun `save profile works`() {
        /** save profile */
        run {
            assertDoesNotThrow {
                ProfileManager.saveProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters)
            }
        }
    }

    @Test
    fun `save profile twice fails`() {
        /** save profile twice */
        run {
            ProfileManager.saveProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters)
            assertThrows<FileAlreadyExistsException> {
                ProfileManager.saveProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters)
            }
        }
    }

    @Test
    fun `load profile works`() {
        /** load profile */
        run {
            ProfileManager.saveProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters)
            val loadedProfile = ProfileManager.loadProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters.coreType)
            assert(aliceCoreCACRBACMQTTParameters == loadedProfile)
        }
    }

    @Test
    fun `load non-existing profile fails`() {
        /** load non-existing profile */
        run {
            assert(ProfileManager.loadProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters.coreType) == null)
        }
    }

    @Test
    fun `update profile works`() {
        /** update profile */
        run {
            ProfileManager.saveProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters)
            ProfileManager.updateProfile(aliceUser.name, bobCoreCACRBACMQTTParameters)
            val loadedProfile = ProfileManager.loadProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters.coreType)
            assert(aliceCoreCACRBACMQTTParameters != loadedProfile)
        }
    }

    @Test
    fun `update non-existing profile fails`() {
        /** update non-existing profile */
        run {
            assertThrows<FileNotFoundException> {
                ProfileManager.updateProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters)
            }
        }
    }

    @Test
    fun `delete profile works`() {
        /** delete profile */
        run {
            ProfileManager.saveProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters)
            assert(ProfileManager.loadProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters.coreType) != null)
            assert(ProfileManager.deleteProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters.coreType))
            assert(ProfileManager.loadProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters.coreType) == null)
        }
    }

    @Test
    fun `delete non-existing profile fails`() {
        /** delete non-existing profile */
        run {
            assert(!ProfileManager.deleteProfile(aliceUser.name, aliceCoreCACRBACMQTTParameters.coreType))
        }
    }
}
