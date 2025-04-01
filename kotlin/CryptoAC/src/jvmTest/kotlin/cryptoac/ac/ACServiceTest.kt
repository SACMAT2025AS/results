package cryptoac.ac

import cryptoac.OutcomeCode
import cryptoac.Service
import cryptoac.ServiceTest
import cryptoac.TestUtilities
import cryptoac.TestUtilities.Companion.assertUnlockAndLock
import cryptoac.tuple.User
import org.junit.jupiter.api.*

internal abstract class ACServiceTest : ServiceTest() {

    abstract val ac: ACService



    @Test
    abstract fun check_authorized_user_can_do_works()

    @Test
    abstract fun check_not_authorized_user_can_do_fails()



    override fun addUser(user: User): Service {
        val addUserResult = ac.addUser(user)
        assert(addUserResult.code == OutcomeCode.CODE_000_SUCCESS)
        val userAC = ACFactory.getAC(
            acParameters = addUserResult.serviceParameters as ACServiceParameters
        ) as ACService
        return userAC
    }
}