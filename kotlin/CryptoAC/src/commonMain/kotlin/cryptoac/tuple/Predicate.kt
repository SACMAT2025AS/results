package cryptoac.tuple

import kotlinx.serialization.Serializable

/**
 * A predicate can be one among the following:
 * - [COLLUDING]: referring to a user, it means that the user is colluding prone;
 * - ... TODO the other predicates
 */
@Serializable
enum class Predicate {
    COLLUDING,
}