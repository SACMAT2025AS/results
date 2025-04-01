package cryptoac.tuple

import cryptoac.Constants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

internal class ElementTest {

    @Test
    fun token_creation_for_positive_values_works() {
        assertTrue(Element.generateRandomToken(1).length == 1)
    }

    @Test
    fun token_creation_with_zero_or_negative_values_fails() {
        assertFails { Element.generateRandomToken(0) }
        assertFails { Element.generateRandomToken(-1) }
    }

    @Test
    fun the_admin_token_is_the_name_of_the_admin() {
        assertEquals(User(Constants.ADMIN).token, Constants.ADMIN)
        assertEquals(Role(Constants.ADMIN).token, Constants.ADMIN)
    }

    @Test
    fun the_token_of_a_generic_element_is_not_the_name_of_the_admin() {
        assertTrue(User("Alice").token != Constants.ADMIN)
        assertTrue(Role("Student").token != Constants.ADMIN)
    }
}
