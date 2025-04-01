package cryptoac.rm

/**
 * An RM service can be implemented by:
 * - [RBAC_CRYPTOAC]: CryptoAC test implementation (for RBAC);
 * - [NONE]: no RM chosen.
 */
enum class RMType {
    RBAC_CRYPTOAC,
    NONE,
}
