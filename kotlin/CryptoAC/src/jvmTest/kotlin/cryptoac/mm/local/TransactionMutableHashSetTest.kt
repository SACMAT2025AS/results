package cryptoac.mm.local

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.RepeatedTest
import java.util.Random

class TransactionMutableHashSetTest {

    private val random = Random()

    @RepeatedTest(100)
    fun checkTransactions() {
        val previous = ArrayList<Int>(50)
        val last = ArrayList<Int>(100)

        val set = TransactionMutableHashSet<Int>()

        for(i in 1..random.nextInt(50)) {
            val value = random.nextInt()
            previous.add(value)
            last.add(value)
            set.add(value)
        }

        assertEquals(previous.sorted(), last.sorted())
        assertEquals(previous.sorted(), set.sorted())

        set.setAutocommit(false)

        for(i in 1..random.nextInt(50)) {
            val value = random.nextInt()
            last.add(value)
            set.add(value)
        }

        assertEquals(last.sorted(), set.sorted())

        set.rollback()

        assertEquals(previous.sorted(), set.sorted())
    }

    @RepeatedTest(100)
    fun checkRepeatedTest() {
        val set = TransactionMutableHashSet<Int>()
        set.setAutocommit(true)

        for(i in 1..100) {
            val value = random.nextInt()
            set.add(value)
        }

        assertNotEquals(ArrayList<Int>(), set.sorted())

        set.clear()
        set.commit()

        assertEquals(ArrayList<Int>(), set.sorted())
    }

}