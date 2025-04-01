package cryptoac.mm

import cryptoac.*
import cryptoac.OutcomeCode.*
import cryptoac.tuple.TupleStatus
import cryptoac.tuple.User
import org.junit.jupiter.api.*
import kotlin.test.assertEquals

internal abstract class MMServiceTest : ServiceTest() {

    abstract val mm: MMService



    @BeforeEach
    override fun setUp() {
        super.setUp()
        assertEquals(CODE_000_SUCCESS, mm.configure())
        assertEquals(CODE_000_SUCCESS, mm.addAdmin(Parameters.adminUser))
    }


    @Test
    override fun add_admin_once_works() {
        /** Already done during the 'setUp()' function */
    }

    @Test
    override fun add_admin_with_wrong_or_blank_name_or_twice_fails() {
        /** add admin with wrong or blank name */
        run {
            assert(mm.addAdmin(
                newAdmin = User(
                    name = "notAdmin",
                    isAdmin = true,
                )
            ) == CODE_036_ADMIN_NAME
            )

            assert(mm.addAdmin(
                newAdmin = User(
                    name = "",
                    isAdmin = true,
                )
            ) == CODE_036_ADMIN_NAME
            )
        }

        /** add admin twice */
        run {
            /** Admin is already added once during the 'setUp()' function */

            assert(mm.addAdmin(
                newAdmin = Parameters.adminUser
            ) == CODE_035_ADMIN_ALREADY_INITIALIZED
            )
        }
    }

    @Test
    override fun delete_incomplete_and_operational_users_by_name_works() {
        val incompleteUser = Parameters.aliceUser
        service.addUser(incompleteUser)
        val operationalUser = Parameters.bobUser
        addAndInitUser(operationalUser)

        /** delete incomplete users */
        myRun {
            assert(service.deleteUser(incompleteUser.name) == CODE_000_SUCCESS)
            val deleteUsers = service.getUsers(username = incompleteUser.name, status = TupleStatus.DELETED)
            assert(deleteUsers.size == 1)
            assert(deleteUsers.firstOrNull()!!.name == incompleteUser.name)
        }

        /** delete operational users */
        myRun {
            assert(service.deleteUser(operationalUser.name) == CODE_000_SUCCESS)
            val deleteUsers = service.getUsers(username = operationalUser.name, status = TupleStatus.DELETED)
            assert(deleteUsers.size == 1)
            assert(deleteUsers.firstOrNull()!!.name == operationalUser.name)
        }
    }
}
