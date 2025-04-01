package cryptoac.rm

import cryptoac.*
import cryptoac.ServiceTest
import cryptoac.TestUtilities.Companion.assertLock
import cryptoac.TestUtilities.Companion.assertUnlock
import cryptoac.dm.DMService
import cryptoac.tuple.User
import org.junit.jupiter.api.*

internal abstract class RMServiceTest : ServiceTest() {

    abstract val rm: RMService
    abstract val dm: DMService?



    @BeforeEach
    override fun setUp() {
        assert(rm.locks == 0)
        if (dm != null) {
            assert(dm!!.locks == 0)
            dm!!.init()
            assertLock(dm!!)
            assert(dm!!.configure(
                Parameters.adminCoreCACRBACCryptoACParametersWithOPA
            ) == OutcomeCode.CODE_000_SUCCESS)
            assert(dm!!.addAdmin(
                Parameters.adminUser
            ) == OutcomeCode.CODE_000_SUCCESS)
        }
        rm.init()
        assertLock(rm)
        assert(rm.configure(
            Parameters.adminCoreCACRBACCryptoACParametersWithOPA
        ) == OutcomeCode.CODE_000_SUCCESS)
        assert(rm.addAdmin(
            Parameters.adminUser
        ) == OutcomeCode.CODE_000_SUCCESS)
    }

    @AfterEach
    override fun tearDown() {
        dm?.let {
            assertUnlock(it)
            it.deinit()
            assert(it.locks == 0)
        }
        assertUnlock(rm)
        rm.deinit()
        assert(rm.locks == 0)
    }



    @Test
    override fun add_admin_once_works() {
        /** Already done during the 'setUp()' function */
    }

    @Test
    override fun add_admin_with_wrong_or_blank_name_or_twice_fails() {
        /** add admin with wrong or blank name */
        run {
            assert(rm.addAdmin(
                newAdmin = User(
                    name = "notAdmin",
                    isAdmin = true,
                )
            ) == OutcomeCode.CODE_036_ADMIN_NAME)

            assert(rm.addAdmin(
                newAdmin = User(
                    name = "",
                    isAdmin = true,
                )
            ) == OutcomeCode.CODE_036_ADMIN_NAME)
        }

        /** add admin twice */
        run {
            /** Admin is already added once during the 'setUp()' function */

            assert(rm.addAdmin(
                newAdmin = Parameters.adminUser
            ) == OutcomeCode.CODE_035_ADMIN_ALREADY_INITIALIZED
            )
        }
    }



    override fun addUser(user: User): Service {
        val addUserResult = rm.addUser(user)
        assert(addUserResult.code == OutcomeCode.CODE_000_SUCCESS)
        val userRM = RMFactory.getRM(
            rmParameters = addUserResult.serviceParameters as RMServiceParameters
        )
        return userRM!!
    }
}
