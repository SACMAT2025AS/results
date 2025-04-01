package cryptoac

import cryptoac.tuple.Tuple
import kotlin.test.Test
import kotlin.test.assertFails

internal class TupleTest {

    @Test
    fun enforcing_positive_numbers_positive_values_works() {
        Tuple.requirePositiveNumber(1)
    }

    @Test
    fun enforcing_positive_numbers_with_zero_or_negative_values_throws_exception() {
        assertFails { Tuple.requirePositiveNumber(0) }
        assertFails { Tuple.requirePositiveNumber(-1) }
    }
}
