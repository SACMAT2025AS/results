package cryptoac.ac

import cryptoac.parameters.ServiceParameters

/**
 * Interface defining the parameters required
 * to configure an AC object:
 * - [acType]: the implementation type of the AC object.
 */
interface ACServiceParameters : ServiceParameters {

    /** The type of this service */
    val acType: ACType
}
