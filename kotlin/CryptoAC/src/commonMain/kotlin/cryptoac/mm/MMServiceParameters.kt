package cryptoac.mm

import cryptoac.parameters.ServiceParameters

/**
 * Interface defining the parameters required
 * to configure an MM object:
 * - [mmType]: the implementation type of the MM object.
 */
interface MMServiceParameters : ServiceParameters {

    /** The type of this service */
    val mmType: MMType
}