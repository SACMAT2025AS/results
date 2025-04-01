package cryptoac.dm

import cryptoac.ac.ACType.NONE
import cryptoac.mm.MMType.RBAC_LOCAL

/**
 * A DM service can be implemented by:
 * - [CRYPTOAC]: CryptoAC test implementation;
 * - [MQTT]: any message broker supporting MQTT;
 * - [LOCAL]: Local testing;
 * - [NONE]: no DM chosen.
 */
enum class DMType {
    CRYPTOAC,
    MQTT,
    LOCAL,
    NONE,
}