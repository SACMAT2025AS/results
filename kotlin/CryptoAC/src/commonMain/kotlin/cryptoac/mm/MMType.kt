package cryptoac.mm

/**
 * An MM service can be implemented by:
 * - [RBAC_MYSQL]: MySQL8+ (for RBAC);
 * - [ABAC_MYSQL]: MySQL8+ (for ABAC);
 * - [RBAC_REDIS]: Redis (for RBAC);
 * - [ABAC_REDIS]: Redis (for ABAC);
 * - [RBAC_LOCAL]: Local testing (for RBAC).
 */
enum class MMType {
    RBAC_MYSQL,
    ABAC_MYSQL,
    RBAC_REDIS,
    ABAC_REDIS,
    RBAC_LOCAL,
}