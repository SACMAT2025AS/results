package cryptoac.mm.local

open class TransactionMutableHashSet<E> : MutableSet<E> {

    private val contents = HashSet<E>()
    private val backup = HashSet<E>()
    private var autocommit = true

    override val size: Int
        get() = contents.size

    override fun add(element: E): Boolean {
        try {
            return contents.add(element)
        } finally {
            triggerAutocommit()
        }
    }

    override fun addAll(elements: Collection<E>): Boolean {
        try {
            return contents.addAll(elements)
        } finally {
            triggerAutocommit()
        }
    }

    override fun clear() {
        try {
            contents.clear()
        } finally {
            triggerAutocommit()
        }
    }

    override fun isEmpty(): Boolean {
        return contents.isEmpty()
    }

    override fun iterator(): MutableIterator<E> {
        return contents.iterator()
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        try {
            // 'toSet()' is introduces to improve performance as suggested by IntelliJ
            return contents.retainAll(elements.toSet())
        } finally {
            triggerAutocommit()
        }
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        try {
            return contents.removeAll(elements.toSet())
        } finally {
            triggerAutocommit()
        }
    }

    override fun remove(element: E): Boolean {
        try {
            return contents.remove(element)
        } finally {
            triggerAutocommit()
        }
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return contents.containsAll(elements.toSet())
    }

    override fun contains(element: E): Boolean {
        return contents.contains(element)
    }

    private fun triggerAutocommit() {
        if(autocommit) {
            commit()
        }
    }

    fun setAutocommit(autocommit: Boolean) {
        this.autocommit = autocommit
    }

    fun commit() {
        backup.clear()
        backup.addAll(contents)
    }

    fun rollback() {
        contents.clear()
        contents.addAll(backup)
    }

}