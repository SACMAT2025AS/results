package cryptoac

/** Constant values */
object Constants {

    /** The ID of the admin user */
    const val ADMIN = "admin"

    /** The default offset for rows in queries */
    const val DEFAULT_OFFSET = 0

    /** The default limit for rows in queries */
    const val DEFAULT_LIMIT = 100

    /** The unlimited value for rows in queries */
    // TODO check the usage of the "NO_LIMIT" variable, it breaks pagination
    const val NO_LIMIT = -1
}
