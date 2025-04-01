package cryptoac.rm

import cryptoac.parameters.ServiceParameters

/**
 * Interface defining the parameters required
 * to configure an RM object:
 * - [rmType]: the implementation type of the RM object.
 */
interface RMServiceParameters : ServiceParameters {

    /** The type of this service */
    val rmType: RMType
}
