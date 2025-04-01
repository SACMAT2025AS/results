package cryptoac.dm.cryptoac

import cryptoac.*
import cryptoac.Parameters.adminCoreCACRBACCryptoACParametersNoOPA
import cryptoac.TestUtilities.Companion.assertLock
import cryptoac.TestUtilities.Companion.dir
import cryptoac.dm.DMFactory
import cryptoac.dm.DMServiceABACTest
import cryptoac.dm.DMServiceRBACTest
import cryptoac.tuple.Enforcement
import cryptoac.tuple.Resource
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class DMServiceABACCryptoACTest : DMServiceABACTest() {

    // TODO "internal abstract class" -> "internal class" e implementa

}
