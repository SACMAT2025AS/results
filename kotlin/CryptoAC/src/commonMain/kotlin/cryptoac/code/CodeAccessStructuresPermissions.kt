package cryptoac.code

import cryptoac.OutcomeCode
// import cryptoac.tuple.AccessStructurePermission

/** Wrapper binding a [code] with a set of [accessStructuresPermissions] */
data class CodeAccessStructuresPermissions(
    val code: OutcomeCode = OutcomeCode.CODE_000_SUCCESS,
    // val accessStructuresPermissions: HashSet<AccessStructurePermission>? = null
)
