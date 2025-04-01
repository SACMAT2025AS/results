package cryptoac.view.content.dashboard

import cryptoac.server.API
import cryptoac.OutcomeCode
import cryptoac.server.SERVER
import cryptoac.core.CoreType
import cryptoac.tuple.Role
import cryptoac.tuple.User
import cryptoac.core.myJson
import cryptoac.tuple.RBACType
import cryptoac.tuple.RolePermission
import cryptoac.tuple.Resource
import cryptoac.tuple.UserRole
import cryptoac.view.baseURL
import cryptoac.view.components.custom.CryptoACAlertSeverity
import cryptoac.view.components.custom.table.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import react.*

private val logger = KotlinLogging.logger {}

external interface RBACDashboardProps : Props {
    // TODO docs
    var httpClientProp: HttpClient
    var coreTypeProp: CoreType
    var userIsAdminProp: Boolean
    var handleChangeBackdropIsOpenProp: (Boolean) -> Unit
    var handleDisplayAlertProp: (OutcomeCode, CryptoACAlertSeverity) -> Unit
}

external interface RBACDashboardState : State {
    /** The list of users */
    var usersState: List<Array<String>>

    /** The list of roles */
    var rolesState: List<Array<String>>

    /** The list of resources */
    var resourcesState: List<Array<String>>

    /** The list of user-role assignments */
    var assignmentsState: List<Array<String>>

    /** The list of role-permission assignments */
    var permissionsState: List<Array<String>>

    /** The list of tables opened by the user by clicking on table elements */
    var openedTablesState: MutableList<CryptoACTableData>
}


/** Get the list of users */
suspend fun getUsers(props: RBACDashboardProps): List<Array<String>>? {
    val actualEndpoint = "$baseURL${API.CRYPTOAC}${API.USERS.replace("{Core}", props.coreTypeProp.toString())}"
    return getElements(
        props = props,
        endpoint = actualEndpoint,
        errorCode = OutcomeCode.CODE_004_USER_NOT_FOUND,
        type = RBACType.USER
    )
}

/** Get the list of roles */
suspend fun getRoles(props: RBACDashboardProps): List<Array<String>>? {
    val actualEndpoint = "$baseURL${API.CRYPTOAC}${API.ROLES.replace("{Core}", props.coreTypeProp.toString())}"
    return getElements(
        props = props,
        endpoint = actualEndpoint,
        errorCode = OutcomeCode.CODE_005_ROLE_NOT_FOUND,
        type = RBACType.ROLE
    )
}

/** Get the list of resources */
suspend fun getResources(props: RBACDashboardProps): List<Array<String>>? {
    val actualEndpoint = "$baseURL${API.CRYPTOAC}${API.RESOURCES.replace("{Core}", props.coreTypeProp.toString())}"
    return getElements(
        props = props,
        endpoint = actualEndpoint,
        errorCode = OutcomeCode.CODE_006_RESOURCE_NOT_FOUND,
        type = RBACType.RESOURCE
    )
}

/** Get the list of assignments filtering, if given, by [username] and [roleName] */
suspend fun getAssignments(
    props: RBACDashboardProps,
    username: String? = null,
    roleName: String? = null
): List<Array<String>>? {
    val actualEndpoint =
        "$baseURL${API.CRYPTOAC}${API.USERSROLES.replace("{Core}", props.coreTypeProp.toString())}?" +
                (if (username != null) "${SERVER.USERNAME}=$username" else "") +
                if (roleName != null) "${SERVER.ROLE_NAME}=$roleName" else ""
    return getElements(
        props = props,
        endpoint = actualEndpoint,
        errorCode = OutcomeCode.CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND,
        type = RBACType.USERROLE
    )
}

/** Get the list of permissions filtering, if given, by [roleName] and [resourceName] */
suspend fun getPermissions(
    props: RBACDashboardProps,
    roleName: String? = null,
    resourceName: String? = null
): List<Array<String>>? {
    val actualEndpoint =
        "$baseURL${API.CRYPTOAC}${API.ROLESPERMISSIONS.replace("{Core}", props.coreTypeProp.toString())}?" +
                (if (roleName != null) "${SERVER.ROLE_NAME}=$roleName" else "") +
                if (resourceName != null) "${SERVER.RESOURCE_NAME}=$resourceName" else ""
    return getElements(
        props = props,
        endpoint = actualEndpoint,
        errorCode = OutcomeCode.CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND,
        type = RBACType.ROLEPERMISSION
    )
}

// TODO refactor this function
/**
 * Get the list of elements of the given [type] at the specified [endpoint],
 * comparing the [endpoint] response with the provided [errorCode] for when
 * no elements are available
 */
private suspend fun getElements(
    props: RBACDashboardProps,
    endpoint: String,
    errorCode: OutcomeCode,
    type: RBACType
): List<Array<String>>? {
    logger.info { "Getting the list of elements of type $type" }

    logger.info { "Sending get request to $endpoint" }
    props.handleChangeBackdropIsOpenProp(true)
    val httpResponse: HttpResponse? = try {
        props.httpClientProp.get {
            url(endpoint)
        }
    } catch (e: Error) {
        if (e.message == "Fail to fetch") {
            logger.error { "CryptoAC is unreachable" }
            props.handleDisplayAlertProp(OutcomeCode.CODE_046_CRYPTOAC_CONNECTION_TIMEOUT, CryptoACAlertSeverity.ERROR)
        } else {
            logger.error { "Error during HTTP request (${e.message}), see console log for details" }
            console.log(e)
            props.handleDisplayAlertProp(OutcomeCode.CODE_049_UNEXPECTED, CryptoACAlertSeverity.ERROR)
        }
        null
    } finally {
        /** Close the backdrop loading screen */
        props.handleChangeBackdropIsOpenProp(false)
    }
    return if (httpResponse != null) {
        if (httpResponse.status == HttpStatusCode.OK) {
            when (type) {
                RBACType.USER -> {
                    val users: HashSet<User> = myJson.decodeFromString(httpResponse.bodyAsText())
                    users.map { it.toArray() }
                }
                RBACType.ROLE -> {
                    val roles: HashSet<Role> = myJson.decodeFromString(httpResponse.bodyAsText())
                    roles.map { it.toArray() }
                }
                RBACType.RESOURCE -> {
                    val resources: HashSet<Resource> = myJson.decodeFromString(httpResponse.bodyAsText())
                    resources.map { it.toArray() }
                }
                RBACType.USERROLE -> {
                    val assignments: HashSet<UserRole> = myJson.decodeFromString(httpResponse.bodyAsText())
                    assignments.map { it.toArray() }
                }
                RBACType.ROLEPERMISSION -> {
                    val permissions: HashSet<RolePermission> = myJson.decodeFromString(httpResponse.bodyAsText())
                    permissions.map { it.toArray() }
                }
            }
        } else {
            val text = httpResponse.bodyAsText()
            val outcomeCode: OutcomeCode = myJson.decodeFromString(text)
            if (outcomeCode == errorCode) {
                logger.info { "Got 0 elements of type $type" }
                listOf()
            } else {
                logger.warn { "Error while getting elements of type $type: $outcomeCode" }
                props.handleDisplayAlertProp(outcomeCode, CryptoACAlertSeverity.ERROR)
                null
            }
        }
    } else {
        null
    }
}

/**
 * Compute the list of columns spans (in the 12-columns grid reference system)
 * depending on the given [numberOfTables]. For instance, the input 8 returns
 * the list <4, 4, 6> (i.e., in the first row three tables spanning 4 columns,
 * in the second row three tables spanning 4 columns and in the last row two
 * tables spanning 6 columns)
 */
fun getColumnsForTables(numberOfTables: Int): MutableList<Int> {
    var tablesLeft = numberOfTables
    val columnsForRow = mutableListOf<Int>()
    while (tablesLeft > 0) {
        tablesLeft = when (tablesLeft) {
            1 -> {
                columnsForRow.add(12)
                0
            }
            2 -> {
                columnsForRow.add(6)
                0
            }
            else -> {
                columnsForRow.add(4)
                tablesLeft - 3
            }
        }
    }
    return columnsForRow
}

/** Get the number of items in a row depending on the column span */
fun getNumberOfItemsFromSpan(span: Int) = when (span) {
    12 -> 1
    6 -> 2
    else -> 3
}
