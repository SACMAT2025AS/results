package cryptoac.code

import cryptoac.OutcomeCode
import cryptoac.tuple.RolePermission

/** Wrapper binding a [code] with a set of [rolesPermissions] */
data class CodeRolesPermissions(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    val rolesPermissions: HashSet<RolePermission>? = null
)
