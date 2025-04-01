package cryptoac

import cryptoac.OutcomeCode.*
import cryptoac.TestUtilities.Companion.assertLock
import cryptoac.TestUtilities.Companion.assertRollbackAndLock
import cryptoac.TestUtilities.Companion.assertUnlockAndLock
import cryptoac.TestUtilities.Companion.assertUnlock
import cryptoac.TestUtilities.Companion.createUser
import cryptoac.tuple.TupleStatus
import cryptoac.tuple.User
import org.junit.jupiter.api.*
import java.lang.AssertionError
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class ServiceTest {

    abstract val service: Service



    @BeforeAll
    abstract fun setUpAll()

    @BeforeEach
    open fun setUp() {
        assert(service.locks == 0)
        service.init()
        assertLock(service)
    }

    @AfterEach
    open fun tearDown() {
        assertUnlock(service)
        service.deinit()
        assert(service.locks == 0)
    }

    @AfterAll
    abstract fun tearDownAll()



    @Test
    open fun add_admin_once_works() {
        /** add admin */
        run {
            assert(service.addAdmin(
                newAdmin = Parameters.adminUser
            ) == CODE_000_SUCCESS)
        }
    }

    @Test
    open fun add_admin_with_wrong_or_blank_name_or_twice_fails() {
        /** add admin with wrong or blank name */
        run {
            assert(service.addAdmin(
                newAdmin = User(
                    name = "notAdmin",
                    isAdmin = true,
                )
            ) == CODE_036_ADMIN_NAME)

            assert(service.addAdmin(
                newAdmin = User(
                    name = "",
                    isAdmin = true,
                )
            ) == CODE_036_ADMIN_NAME)
        }

        /** add admin twice */
        run {
            assert(service.addAdmin(
                newAdmin = Parameters.adminUser
            ) == CODE_000_SUCCESS)
            assertUnlockAndLock(service)

            assert(service.addAdmin(
                newAdmin = Parameters.adminUser
            ) == CODE_035_ADMIN_ALREADY_INITIALIZED)
        }
    }

    @Test
    open fun init_user_once_works() {
        /** init user of existing user */
        myRun {
            addAndInitUser(Parameters.aliceUser)
        }
    }

    @Test
    open fun init_user_not_existing_or_already_initialized_or_deleted_fail() {
        /** init user not existing */
        run {
            assert(service.initUser(
                user = Parameters.aliceUser
            ) in listOf(CODE_055_ACCESS_DENIED_TO_MM, CODE_004_USER_NOT_FOUND))
        }

        /** init user already initialized */
        run {
            assert(service.addUser(
                newUser = Parameters.aliceUser
            ).code == CODE_000_SUCCESS)

            assertEquals(CODE_000_SUCCESS, service.initUser(
                user = Parameters.aliceUser
            ))
            assertUnlockAndLock(service)

            assert(service.initUser(
                user = Parameters.aliceUser
            ) == CODE_052_USER_ALREADY_INITIALIZED)
        }

        /** init user deleted */
        run {
            assert(service.deleteUser(
                username = Parameters.aliceUser.name
            ) == CODE_000_SUCCESS)
            assertUnlockAndLock(service)

            assert(service.initUser(
                user = Parameters.aliceUser
            ) in listOf(CODE_004_USER_NOT_FOUND, CODE_013_USER_WAS_DELETED))
        }
    }

    @Test
    open fun add_user_once_works() {
        /** add user */
        run {
            assert(service.addUser(
                newUser = Parameters.aliceUser
            ).code == CODE_000_SUCCESS)
        }
    }

    @Test
    open fun add_user_twice_or_with_admin_or_blank_name_or_previously_deleted_fail() {
        /** add user twice */
        run {
            assert(service.addUser(
                newUser = Parameters.aliceUser
            ).code == CODE_000_SUCCESS)
            assertUnlockAndLock(service)

            assertEquals(CODE_001_USER_ALREADY_EXISTS, service.addUser(
                newUser = Parameters.aliceUser
            ).code)
        }

        /** add user with admin or blank name */
        myRun {
            assert(service.addUser(
                Parameters.adminUser
            ).code == CODE_001_USER_ALREADY_EXISTS)

            assert(service.addUser(
                User(name = " ")
            ).code == CODE_020_INVALID_PARAMETER)
        }

        /** add user previously deleted */
        myRun {
            addAndInitUser(Parameters.bobUser)
            assert(service.deleteUser(
                Parameters.bobUser.name
            ) == CODE_000_SUCCESS)
            assert(service.addUser(
                Parameters.bobUser
            ).code == CODE_013_USER_WAS_DELETED)
        }
    }

    @Test
    open fun get_not_existing_incomplete_operational_and_deleted_user_by_name_works() {
        val incompleteUser = Parameters.aliceUser
        service.addUser(incompleteUser)
        val operationalUser = Parameters.bobUser
        addAndInitUser(operationalUser)
        val deletedUser = Parameters.carlUser
        addAndInitUser(deletedUser)
        assert(service.deleteUser(deletedUser.name) == CODE_000_SUCCESS)

        /** get not-existing user by name */
        myRun {
            assert(service.getUsers(username = "non-existing").isEmpty())
        }

        /** get incomplete user by name */
        myRun {
            val incompleteUserByName = service.getUsers(username = incompleteUser.name)
            assert(incompleteUserByName.size == 1)
        }

        /** get operational user by name */
        myRun {
            val operationalUserByName = service.getUsers(username = operationalUser.name)
            assertEquals(1, operationalUserByName.size)
            assertEquals(operationalUser.token, operationalUserByName.firstOrNull()!!.token)
        }

        /** get deleted user by name */
        myRun {
            val deletedUserByName = service.getUsers(username = deletedUser.name, status = TupleStatus.DELETED)
            assert(deletedUserByName.size == 1)
            assert(deletedUserByName.firstOrNull()!!.name == deletedUser.name)
        }
    }

    @Test
    open fun get_all_users_works() {
        /** get all users */
        myRun {
            addAndInitUser(Parameters.aliceUser)
            addAndInitUser(Parameters.bobUser)
            addAndInitUser(Parameters.carlUser)

            val allUsers = service.getUsers()
            assert(allUsers.size == 4)
            assert(allUsers.filter { it.name == Parameters.aliceUser.name }.size == 1)
            assert(allUsers.filter { it.name == Parameters.bobUser.name }.size == 1)
            assert(allUsers.filter { it.name == Parameters.carlUser.name }.size == 1)
            assert(allUsers.filter { it.name == Constants.ADMIN }.size == 1)
        }
    }

    @Test
    open fun delete_the_admin_user_by_name_fails() {
        /** delete the admin user */
        myRun {
            assert(service.deleteUser(Constants.ADMIN) == CODE_022_ADMIN_CANNOT_BE_MODIFIED)
        }
    }

    @Test
    open fun delete_incomplete_and_operational_users_by_name_works() {
        val incompleteUser = Parameters.aliceUser
        service.addUser(incompleteUser)
        val operationalUser = Parameters.bobUser
        addAndInitUser(operationalUser)

        /** delete incomplete users */
        myRun {
            assert(service.deleteUser(incompleteUser.name) == CODE_000_SUCCESS)
        }

        /** delete operational users */
        myRun {
            assert(service.deleteUser(operationalUser.name) == CODE_000_SUCCESS)
        }
    }

    @Test
    open fun delete_non_existing_and_deleted_users_by_name_or_blank_name_fails() {
        val nonExistingUser = Parameters.aliceUser
        val deletedUser = Parameters.bobUser
        addAndInitUser(deletedUser)
        assert(service.deleteUser(deletedUser.name) == CODE_000_SUCCESS)

        /** delete user twice */
        myRun {
            assert(service.deleteUser(
                username = deletedUser.name
            ) in listOf(CODE_004_USER_NOT_FOUND, CODE_013_USER_WAS_DELETED))
        }

        /** delete non-existing users */
        myRun {
            assert(service.deleteUser(nonExistingUser.name) == CODE_004_USER_NOT_FOUND)
        }

        /** delete user with blank username */
        myRun {
            assert(service.deleteUser(
                username = " "
            ) == CODE_020_INVALID_PARAMETER)
        }
    }



    @Test
    open fun single_and_multiple_lock_and_unlock_works() {
        // TODO method of Service interface
    }

    @Test
    open fun single_and_multiple_lock_and_rollback_works() {
        // TODO method of Service interface
    }

    @Test
    open fun unlock_without_locking_fails() {
        // TODO method of Service interface
    }

    @Test
    open fun rollback_without_locking_fails() {
        // TODO method of Service interface
    }



    /** Add an incomplete user in the service and return an instance of the service */
    open fun addUser(
        username: String,
        userVersionNumber: Int = 1
    ): User {
        val newUser = createUser(
            username = username,
            status = TupleStatus.INCOMPLETE,
            isAdmin = false
        )
        addUser(newUser)
        return newUser
    }

    /** Add a user in the service and return an instance of the service */
    open fun addAndInitUser(
        username: String,
    ): User {
        val newUser = createUser(
            username = username,
            status = TupleStatus.INCOMPLETE,
            isAdmin = false
        )
        val userService = addUser(newUser)
        assertUnlock(service)
        assertLock(userService)
        userService.initUser(newUser)
        assertUnlock(userService)
        assertLock(service)
        return newUser
    }

    /** Add a user in the service and return an instance of the service */
    open fun addAndInitUser(user: User): Service {
        val userService = addUser(user)
        assertUnlock(service)
        assertLock(userService)
        userService.initUser(user)
        assertUnlock(userService)
        assertLock(service)
        return userService
    }

    /** Add an incomplete user in the service and return an instance of the service */
    protected abstract fun addUser(user: User): Service

    /** Before executing each block, commit the service status */
    protected fun myRun(block: () -> Unit) {
        assertUnlockAndLock(service)
        try {
            block.invoke()
            assertUnlockAndLock(service)
        } catch (e: AssertionError) {
            assertRollbackAndLock(service)
            e.printStackTrace()
            throw e
        }
    }
}