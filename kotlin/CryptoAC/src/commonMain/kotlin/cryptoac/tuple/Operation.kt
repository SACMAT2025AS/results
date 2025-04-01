package cryptoac.tuple

import io.ktor.utils.io.*
import kotlinx.serialization.Serializable

/**
 * An operation can be one among the following:
 * - [READ]: the operation to read a resource;
 * - [WRITE]: the operation to write a resource;
 * - [READWRITE]: the operation to read and write a resource.
 */
@Serializable
enum class Operation {
    READ, WRITE, READWRITE;

    fun merge(op: Operation): Operation {
        return when(this) {
            READ -> when(op) {
                READ -> READ
                WRITE -> READWRITE
                READWRITE -> READWRITE
            }
            WRITE -> when(op) {
                READ -> READWRITE
                WRITE -> WRITE
                READWRITE -> READWRITE
            }
            READWRITE -> READWRITE
        }
    }

    fun subtract(op: Operation): Operation? {
        return when(this) {
            READ -> when(op) {
                READ -> null
                WRITE -> READ
                READWRITE -> null
            }
            WRITE -> when(op) {
                READ -> WRITE
                WRITE -> null
                READWRITE -> null
            }
            READWRITE -> when(op) {
                READ -> WRITE
                WRITE -> READ
                READWRITE -> null
            }
        }
    }

    fun includes(op: Operation): Boolean {
        return when(this) {
            READ -> when(op) {
                READ -> true
                WRITE -> false
                READWRITE -> false
            }
            WRITE -> when(op) {
                READ -> false
                WRITE -> true
                READWRITE -> false
            }
            READWRITE -> true
        }
    }

    companion object {
        fun fromString(name: String): Operation? {
            for(v in Operation.values()) {
                if(v.name.equals(name, ignoreCase = true)) {
                    return v
                }
            }
            return null
        }
    }
}