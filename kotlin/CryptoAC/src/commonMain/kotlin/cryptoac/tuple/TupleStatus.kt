package cryptoac.tuple

/**
 * An Element has one among the following statuses:
 * - [INCOMPLETE]: the Tuple is present in the access
 *     control policy state but is not fully configured yet;
 * - [OPERATIONAL]: the Tuple is present in the access
 *     control policy state and ready for use;
 * - [CACHED] the Tuple was deleted from the access
 *      control policy state but can be used by a user
 *      to access some resources;
 * - [DELETED]: the Element was deleted from the access
 *      control policy state.
 */
enum class TupleStatus {
    INCOMPLETE, OPERATIONAL, CACHED, DELETED,
}
