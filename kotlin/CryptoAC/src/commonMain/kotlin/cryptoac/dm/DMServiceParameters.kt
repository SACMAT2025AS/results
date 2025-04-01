package cryptoac.dm

import cryptoac.parameters.ServiceParameters

/**
 * Interface defining the parameters required
 * to configure a DM object:
 * - [dmType]: the implementation type of the DM object.
 */
interface DMServiceParameters : ServiceParameters {

    /** The type of this service */
    val dmType: DMType
}