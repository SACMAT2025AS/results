package cryptoac.mm.local

import cryptoac.OutcomeCode
import cryptoac.Parameters
import cryptoac.Service
import cryptoac.TestUtilities
import cryptoac.mm.MMFactory
import cryptoac.mm.MMServiceCACRBACTest
import cryptoac.tuple.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

internal class MMServiceCACRBACLocalTest : MMServiceCACRBACTest() {

    override val mm = MMFactory.getMM(
        Parameters.mmServiceRBACLocalParameters
    ) as MMServiceCACRBACLocal

    override val service: Service = mm

    override fun setUpAll() {

    }

    @BeforeEach
    override fun setUp() {
        super.setUp()
        assert(mm.addRole(Parameters.adminRole) == OutcomeCode.CODE_000_SUCCESS)
        assert(mm.addUsersRoles((hashSetOf(Parameters.adminUserRole))) == OutcomeCode.CODE_000_SUCCESS)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        TestUtilities.resetMMServiceRBACLocal()
    }

    override fun tearDownAll() {

    }

    override fun addUser(user: User): Service {
        val userMM = MMFactory.getMM(Parameters.mmServiceRBACLocalParameters)
        mm.addUser(user)
//        userMM.addUser(user)
//        mm.addUser(user)
        return userMM
    }

}