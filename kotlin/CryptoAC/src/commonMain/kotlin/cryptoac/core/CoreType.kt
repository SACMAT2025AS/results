package cryptoac.core

/**
 * A core interface (whose name is composed of the
 * policy model and the scenario) can be implemented by:
 * - [RBAC_FILES]: RBAC for stored files;
 * - [RBAC_MQTT]: RBAC for MQTT;
 * - [ABAC_FILES]: ABAC for stored files;
 * - [ABAC_MQTT]: ABAC for MQTT.
 */
enum class CoreType {
    RBAC_FILES,
    RBAC_MQTT,
    ABAC_FILES,
    ABAC_MQTT;

    /** Return a pretty representation of the core type */
    fun toPrettyString(): String = when (this) {
        RBAC_FILES -> "RBAC for stored files"
        RBAC_MQTT -> "RBAC for MQTT"
        ABAC_FILES -> "ABAC for stored files"
        ABAC_MQTT -> "ABAC for MQTT"
    }
}
