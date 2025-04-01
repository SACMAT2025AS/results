package cryptoac.mm.local

import cryptoac.Constants.ADMIN
import cryptoac.OutcomeCode
import cryptoac.OutcomeCode.*
import cryptoac.code.CodeBoolean
import cryptoac.code.CodeServiceParameters
import cryptoac.core.CoreParameters
import cryptoac.crypto.AsymKeysType
import cryptoac.decodeBase64
import cryptoac.mm.MMServiceCACRBAC
import cryptoac.tuple.*
import mu.KotlinLogging
import java.security.PublicKey
import kotlin.collections.HashSet

private var logger = KotlinLogging.logger {}

open class MMServiceCACRBACLocal : MMServiceCACRBAC {

     companion object {
        protected var users = TransactionMutableHashSet<User>()
        protected var roles = TransactionMutableHashSet<Role>()
        protected var resources = TransactionMutableHashSet<Resource>()
        protected var userRoles = TransactionMutableHashSet<UserRole>()
        protected var rolePermissions = TransactionMutableHashSet<RolePermission>()
     }


    override fun alreadyConfigured(): CodeBoolean {
        return CodeBoolean(boolean = getUsers(username = ADMIN).isNotEmpty())
    }

    override fun configure(parameters: CoreParameters?): OutcomeCode {
        logger.info("No action required to configure the MM RBAC Local")
        return CODE_000_SUCCESS
    }

    override fun init() {
        logger.debug { "Resetting the MM RBAC Local" }
        reset()
    }

    override fun deinit() {
        logger.info("No action required to de-initialize the MM RBAC Local")
    }

    override fun addAdmin(newAdmin: User): OutcomeCode {
        logger.info("Adding admin to the MM")

        // check admin name
        if(newAdmin.name != ADMIN) {
            logger.warn { "Admin user has name ${newAdmin.name}, but admin name should be $ADMIN" }
            return CODE_036_ADMIN_NAME
        }

        // check admin already exists
        if(getUsers(username = ADMIN).isNotEmpty()) {
            logger.warn { "The database was already initialized" }
            return CODE_035_ADMIN_ALREADY_INITIALIZED
        }

        // add admin
        users.add(newAdmin)
        return CODE_000_SUCCESS
    }

    override fun initUser(user: User): OutcomeCode {
//        val streamExist = users.stream()
//            .filter { u -> u.name == user.name }
//            .filter { u -> u.status != TupleStatus.DELETED }
//
//        if(streamExist.count().toInt() == 0) {
//            return CODE_004_USER_NOT_FOUND
//        }

        val oldUserOpt = users.stream()
            .filter { u -> u.name == user.name }
            .findFirst()

        if(oldUserOpt.isEmpty) {
            return CODE_004_USER_NOT_FOUND
        }
        val oldUser = oldUserOpt.get()
//        if(oldUser.status != TupleStatus.INCOMPLETE) {
//            return CODE_052_USER_ALREADY_INITIALIZED
//        }

        users.remove(oldUser)
        users.add(user)
        return CODE_000_SUCCESS
    }

    override fun addUser(newUser: User): CodeServiceParameters {
        val username = newUser.name

        // empty name
        if(username.isBlank()) {
            logger.warn { "username cannot be empty" }
            return CodeServiceParameters(code = CODE_020_INVALID_PARAMETER)
        }

        // already exists
        val matchedUsers = getUsers(username = username)
        if(matchedUsers.isNotEmpty()) {
            if(matchedUsers.stream().anyMatch { e -> e.status == TupleStatus.DELETED }) {
                logger.warn { "User $username was previously deleted" }
                return CodeServiceParameters(CODE_013_USER_WAS_DELETED)
            } else {
                logger.warn { "The user $username already exists" }
                return CodeServiceParameters(CODE_001_USER_ALREADY_EXISTS)
            }
        }

        // add user
        users.add(newUser)
        return CodeServiceParameters(CODE_000_SUCCESS)
    }

    override fun deleteUser(username: String): OutcomeCode {
        // empty name
        if(username.isBlank()) {
            logger.warn { "username cannot be empty" }
            return CODE_020_INVALID_PARAMETER
        }

        // delete admin
        if(username == ADMIN) {
            logger.warn { "admin user cannot be deleted" }
            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
        }

        logger.info { "deleting user $username" }


        // delete tuple
        val oldUserOpt = users.stream()
            .filter { u -> u.name == username }
            .filter { u -> u.status != TupleStatus.DELETED }
            .findFirst()
        if(oldUserOpt.isPresent) {
//            val oldUser = oldUserOpt.get()
//            val newUser = User(
//                name = oldUser.name,
//                status = TupleStatus.DELETED,
//                // versionNumber = oldUser.versionNumber,
//                asymEncKeys = oldUser.asymEncKeys,
//                asymSigKeys = oldUser.asymSigKeys,
//                isAdmin = oldUser.isAdmin,
//            ).apply {
//                token = oldUser.token
//            }
//            users.remove(oldUser)
//            users.add(newUser.apply {
//                token = newUser.token
//            })
            users.remove(oldUserOpt.get())
            return CODE_000_SUCCESS
        } else {
            return CODE_004_USER_NOT_FOUND
        }
    }

    override fun addRole(newRole: Role): OutcomeCode {
        if(newRole.status != TupleStatus.OPERATIONAL) {
            return CODE_000_SUCCESS
        }

        // check empty
        if(newRole.name.isBlank()) {
            logger.warn { "role cannot be empty" }
            return CODE_020_INVALID_PARAMETER
        }

        // already exists
//        val oldRoles = getRoles(roleName = newRole.name)
//        if(oldRoles.isNotEmpty()) {
//            if(oldRoles.stream().anyMatch { r -> r.status == TupleStatus.DELETED }) {
//                logger.warn { "Role ${newRole.name} was previously deleted" }
//                return CODE_014_ROLE_WAS_DELETED
//            } else {
//                logger.warn { "Role ${newRole.name} already exists" }
//                return CODE_002_ROLE_ALREADY_EXISTS
//            }
//        }

        // add role
        roles.add(newRole.apply {
            token = newRole.token
        })
        return CODE_000_SUCCESS
    }

    override fun addResource(newResource: Resource): OutcomeCode {
        if(newResource.status != TupleStatus.OPERATIONAL) {
            return CODE_000_SUCCESS
        }

        // check resource name
        if(newResource.name.isBlank()) {
            logger.warn { "Resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

//        val oldResource = getResources(resourceName = newResource.name)
//        if(oldResource.isNotEmpty()) {
//            if(oldResource.stream().anyMatch { r -> r.status == TupleStatus.DELETED }) {
//                logger.warn { "Resource ${newResource.name} was deletes" }
//                return CODE_015_RESOURCE_WAS_DELETED
//            } else {
//                logger.warn { "Resource ${newResource.name} already exists" }
//                return CODE_003_RESOURCE_ALREADY_EXISTS
//            }
//            if(oldResource.stream().anyMatch { r -> r.status == TupleStatus.OPERATIONAL }) {
//                return CODE_003_RESOURCE_ALREADY_EXISTS
//            }
//        }

        resources.add(newResource.apply {
            token = newResource.token
        })
        return CODE_000_SUCCESS
    }

    override fun addUsersRoles(newUsersRoles: HashSet<UserRole>): OutcomeCode {
        for(ur in newUsersRoles) {
            if(ur.status != TupleStatus.OPERATIONAL) {
                continue
            }
//            val userOpt = users.stream()
//                .filter { u -> u.name == ur.username }
//                .findFirst()
//            if(userOpt.isEmpty) {
//                return CODE_004_USER_NOT_FOUND
//            }
            /* if(userOpt.get().status == ElementStatus.INCOMPLETE) {
                return CODE_053_USER_IS_INCOMPLETE
            }*/
//            if(userOpt.get().status == TupleStatus.DELETED) {
//                return CODE_013_USER_WAS_DELETED
//            }

            val roleOpt = roles.stream()
                .filter { r -> r.name == ur.roleName }
                .findFirst()
            if(roleOpt.isEmpty) {
                return CODE_005_ROLE_NOT_FOUND
            }
            if(roleOpt.get().status == TupleStatus.DELETED) {
                return CODE_014_ROLE_WAS_DELETED
            }

            userRoles.add(ur)

//            if(!userRoles.add(ur)) {
//                // TODO: non è specificato il comportamento quando solo uno degli userrole è già presente
//                return CODE_010_USER_ROLE_ASSIGNMENT_ALREADY_EXISTS
//            }
        }
        return CODE_000_SUCCESS
    }

    override fun addRolesPermissions(newRolesPermissions: HashSet<RolePermission>): OutcomeCode {
        for(rp in newRolesPermissions) {
            if(rp.status != TupleStatus.OPERATIONAL) {
                continue
            }

            val role = roles.stream().filter { r -> r.name == rp.roleName }.findFirst()
            if(role.isEmpty) {
                return CODE_005_ROLE_NOT_FOUND
            }
//            if(role.get().status == TupleStatus.DELETED) {
//                return CODE_014_ROLE_WAS_DELETED
//            }
            val resource = resources.stream().filter { r -> r.name == rp.resourceName }.findFirst()
            if(resource.isEmpty) {
                return CODE_006_RESOURCE_NOT_FOUND
            }
//            if(resource.get().status == TupleStatus.DELETED) {
//                return CODE_015_RESOURCE_WAS_DELETED
//            }
//            if(!rolePermissions.add(rp)) {
//                return CODE_011_ROLE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS
//            }
            rolePermissions.add(rp)
        }
        return CODE_000_SUCCESS
    }

    override fun getUsers(
        username: String?,
        status: TupleStatus?,
        isAdmin: Boolean,
        offset: Int,
        limit: Int
    ): HashSet<User> {
        var stream = users.stream()

        if(username != null) {
            stream = stream.filter { u -> u.name == username }
        }

        if(status != null) {
            stream = stream.filter { u -> u.status == status }
        }

        return HashSet(stream
            .skip(offset.toLong())
            .limit(limit.toLong())
            .toList())
    }

    override fun getRoles(
        roleName: String?,
        status: TupleStatus?,
        isAdmin: Boolean,
        offset: Int,
        limit: Int
    ): HashSet<Role> {
        var stream = roles.stream()

        if(roleName != null) {
            stream = stream.filter { r -> r.name == roleName }
        }

        if(status != null) {
            stream = stream.filter { r -> r.status == status }
        }

        return HashSet(stream
            .skip(offset.toLong())
            .limit(limit.toLong())
            .toList())
    }

    override fun getResources(
        resourceName: String?,
        status: TupleStatus?,
        isAdmin: Boolean,
        offset: Int,
        limit: Int
    ): HashSet<Resource> {
        var stream = resources.stream()

        if(resourceName != null) {
            stream = stream.filter { r -> r.name == resourceName }
        }

        if(status != null) {
            stream = stream.filter { r -> r.status == status }
        }

        stream = stream.skip(offset.toLong())
        stream = stream.limit(limit.toLong())

        return HashSet(stream.toList())
    }

    override fun getUsersRoles(
        username: String?,
        roleName: String?,
        isAdmin: Boolean,
        status: TupleStatus?,
        offset: Int,
        limit: Int
    ): HashSet<UserRole> {
        var stream = userRoles.stream()

        if(username != null) {
            stream = stream.filter { ur -> ur.username == username }
        }

        if(roleName != null) {
            stream = stream.filter { ur -> ur.roleName == roleName }
        }

        if (status != null) {
            stream = stream.filter { ur -> ur.status == status }
        }

        stream = stream.skip(offset.toLong())
        stream = stream.limit(limit.toLong())

        return HashSet(stream.toList())
    }

    override fun getRolesPermissions(
        roleName: String?,
        resourceName: String?,
        isAdmin: Boolean,
        status: TupleStatus?,
        offset: Int,
        limit: Int
    ): HashSet<RolePermission> {
        var stream = rolePermissions.stream()

        if(roleName != null) {
            stream = stream.filter { rp -> rp.roleName == roleName }
        }

        if(resourceName != null) {
            stream = stream.filter { rp -> rp.resourceName == resourceName }
        }

        if(status != null) {
            stream = stream.filter { rp -> rp.status == status }
        }

        stream = stream.skip(offset.toLong())
        stream = stream.limit(limit.toLong())

        return HashSet(stream.toList())
    }

    private fun retrieveSetFromElementType(elementType: RBACElementType) : MutableSet<out Element> {
        return when(elementType) {
            RBACElementType.USER -> users
            RBACElementType.ROLE -> roles
            RBACElementType.RESOURCE -> resources
            else -> HashSet()
        }
    }

    private fun retrievePublicKey(e: Element, asymKeyType: AsymKeysType) : ByteArray? {
        return when(e) {
            is User -> when(asymKeyType) {
                AsymKeysType.ENC -> e.asymEncKeys?.public?.decodeBase64()
                AsymKeysType.SIG -> e.asymSigKeys?.public?.decodeBase64()
            }

            is Role -> when(asymKeyType) {
                AsymKeysType.ENC -> e.asymEncKeys?.public?.decodeBase64()
                AsymKeysType.SIG -> e.asymSigKeys?.public?.decodeBase64()
            }

            else -> null
        }
    }

    override fun getPublicKey(
        name: String?,
        token: String?,
        elementType: RBACElementType,
        asymKeyType: AsymKeysType
    ): ByteArray? {
        val elements = retrieveSetFromElementType(elementType)
        var stream = elements.stream()
            .filter { e -> e.status == TupleStatus.OPERATIONAL || e.status == TupleStatus.DELETED }
        
        if(name != null) {
            stream = stream.filter { e -> e.name == name }
        }

        if(token != null) {
            stream = stream.filter { e -> e.token == token }
        }

        val resultOpt = stream.findFirst()

        if(resultOpt.isEmpty) {
            return null
        }

        return retrievePublicKey(resultOpt.get(), asymKeyType)
    }

    override fun getVersionNumber(name: String?, token: String?, elementType: RBACElementType): Int? {
        val elements = retrieveSetFromElementType(elementType)
        var stream = elements.stream()
            .filter({ e -> e.status == TupleStatus.OPERATIONAL })

        if(name != null) {
            stream = stream.filter { e -> e.name == name }
        }

        if(token != null) {
            stream = stream.filter { e -> e.token == token }
        }

        val opt = stream.findFirst()

        if(opt.isEmpty) {
            return null
        }

        // return opt.get().versionNumber
        return 1
    }

    override fun getToken(name: String, type: RBACElementType): String? {
        val elements = retrieveSetFromElementType(type)
        val stream = elements.stream().filter { e -> e.name == name }
        val tokenOpt = stream.findFirst()

        if(tokenOpt.isEmpty) {
            return null
        }

        return tokenOpt.get().token
    }

    override fun getStatus(name: String?, token: String?, type: RBACElementType): TupleStatus? {
        val elements = retrieveSetFromElementType(type)
        var stream = elements.stream()

        if(name != null) {
            stream = stream.filter { e -> e.name == name }
        }

        if(token != null) {
            stream = stream.filter { e -> e.token == token }
        }

        val opt = stream.findFirst()

        if(opt.isEmpty) {
            return null
        }

        return opt.get().status
    }

    override fun deleteRole(roleName: String): OutcomeCode {
        if(roleName == ADMIN) {
            logger.warn { "Cannot delete the $ADMIN role" }
            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
        }

        if(roleName.isBlank()) {
            logger.warn { "Role name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        val stream = roles.stream().filter { r -> r.name == roleName }


        val oldRoleOpt = stream.findFirst()
        if(oldRoleOpt.isEmpty) {
            logger.warn { "The role $roleName does not exist" }
            return CODE_005_ROLE_NOT_FOUND
        }
        val oldRole = oldRoleOpt.get()

        if(oldRole.status == TupleStatus.DELETED) {
            return CODE_014_ROLE_WAS_DELETED
        }

//        val newRole = Role(
//            name = oldRole.name,
//            status = TupleStatus.DELETED,
//            versionNumber = oldRole.versionNumber,
//            asymEncKeys = oldRole.asymEncKeys,
//            asymSigKeys = oldRole.asymSigKeys,
//        ).apply {
//            token = oldRole.token
//        }

        roles.remove(oldRole)
//        roles.add(newRole.apply {
//            token = newRole.token
//        })

        return CODE_000_SUCCESS
    }

    override fun deleteResource(resourceName: String): OutcomeCode {
        if(resourceName.isBlank()) {
            logger.warn { "Resource name is blank" }
            return CODE_020_INVALID_PARAMETER
        }

        val stream = resources.stream().filter { r -> r.name == resourceName }
            .filter{ it.status == TupleStatus.OPERATIONAL }
        val oldResourceOpt = stream.findFirst()

        if(oldResourceOpt.isEmpty) {
            logger.warn { "The resource $resourceName does not exist" }
            return CODE_006_RESOURCE_NOT_FOUND
        }

        val oldResource = oldResourceOpt.get()
        if(oldResource.status == TupleStatus.DELETED) {
            logger.warn { "Trying to delete deleted resource $resourceName" }
            return CODE_015_RESOURCE_WAS_DELETED
        }

//        val newResource = Resource(
//            name = oldResource.name,
//            status = TupleStatus.DELETED,
//            versionNumber = oldResource.versionNumber,
//            // reEncryptionThresholdNumber = oldResource.reEncryptionThresholdNumber,
//            // enforcement = oldResource.enforcement,
//            encryptedSymKey = oldResource.encryptedSymKey,
//        ).apply {
//            token = oldResource.token
//        }

        resources.remove(oldResource)
//        resources.add(newResource.apply {
//            token = newResource.token
//        })
        return CODE_000_SUCCESS
    }

    override fun deleteUsersRoles(username: String?, roleName: String, status: TupleStatus?): OutcomeCode {
        if(roleName == ADMIN) {
            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
        }

        if(roles.none { r -> r.name == roleName }) {
            return CODE_005_ROLE_NOT_FOUND
        }

        /* val userRoleOpt = userRoles.stream()
            .filter { ur -> ur.roleName == roleName }
            .*/


        userRoles.removeIf { ur -> ur.roleName == roleName && (username == null || username == ur.username) && (status == null || status == ur.status) }
        return CODE_000_SUCCESS
    }

    override fun deleteRolesPermissions(roleName: String?, resourceName: String?): OutcomeCode {
        var stream = rolePermissions.stream()

        if(roleName != null) {
            stream = stream.filter { rp -> rp.roleName == roleName }
            if(roles.filter { r -> r.name == roleName }.isEmpty()) {
                return CODE_005_ROLE_NOT_FOUND
            }
        }

        if(resourceName != null) {
            stream = stream.filter { rp -> rp.resourceName == resourceName }
            if(resources.filter { r -> r.name == resourceName }.isEmpty()) {
                return CODE_006_RESOURCE_NOT_FOUND
            }
        }

        val rolePermOpt = stream.toList()
        if(rolePermOpt.isEmpty()) {
            return CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
        }
//        if(rolePermOpt.get().roleName == ADMIN) {
//            return CODE_022_ADMIN_CANNOT_BE_MODIFIED
//        }

        rolePermissions.removeAll(rolePermOpt);
        return CODE_000_SUCCESS;
    }

    override fun updateRoleTokenAndVersionNumberAndAsymKeys(
        roleName: String,
        oldRoleVersionNumber: Int,
        oldRoleToken: String,
        newRoleToken: String,
        newAsymEncPublicKey: PublicKey,
        newAsymSigPublicKey: PublicKey
    ): OutcomeCode {
        val oldRoleOpt = roles.stream()
            .filter { r -> r.name == roleName }
            .findFirst()

        if(oldRoleOpt.isEmpty) {
            return CODE_005_ROLE_NOT_FOUND
        }
        val oldRole = oldRoleOpt.get()
        if(oldRole.status == TupleStatus.DELETED) {
            return CODE_014_ROLE_WAS_DELETED
        }

        val newRole = Role(
            name = oldRole.name,
            status = TupleStatus.OPERATIONAL,
            versionNumber = oldRoleVersionNumber + 1,
            asymEncKeys = oldRole.asymEncKeys,
            asymSigKeys = oldRole.asymSigKeys,
        ).apply {
            token = newRoleToken
        }

        roles.remove(oldRole)
        roles.add(newRole.apply {
            token = newRole.token
        })
        return CODE_000_SUCCESS
    }

    override fun updateResourceVersionNumber(updatedResource: Resource): OutcomeCode {
        val oldResOpt = resources.stream()
            .filter { r -> r.name == updatedResource.name }
            .findFirst()

        if(oldResOpt.isEmpty) {
            return CODE_006_RESOURCE_NOT_FOUND
        }
        if(oldResOpt.get().status == TupleStatus.DELETED) {
            return CODE_015_RESOURCE_WAS_DELETED
        }

        resources.remove(oldResOpt.get())
        resources.add(updatedResource.apply {
            token = updatedResource.token
        })
        return CODE_000_SUCCESS
    }

    override fun updateResourceTokenAndVersionNumber(
        resourceName: String,
        oldResourceToken: String,
        newResourceToken: String,
        newVersionNumber: Int
    ): OutcomeCode {
        val oldResOpt = resources.stream()
            .filter { r -> r.name == resourceName }
            .findFirst()

        if(oldResOpt.isEmpty) {
            return CODE_006_RESOURCE_NOT_FOUND
        }
        val oldRes = oldResOpt.get()
        if(oldRes.status == TupleStatus.DELETED) {
            return CODE_015_RESOURCE_WAS_DELETED
        }

        val newRes = Resource(
            name = resourceName,
            status = TupleStatus.OPERATIONAL,
            versionNumber = newVersionNumber,
            // reEncryptionThresholdNumber = oldRes.reEncryptionThresholdNumber,
            // enforcement = oldRes.enforcement,
        )
        newRes.token = newResourceToken

        resources.remove(oldRes)
        resources.add(newRes.apply {
            token = newRes.token
        })
        return CODE_000_SUCCESS
    }

    override fun updateRolePermission(updatedRolePermission: RolePermission): OutcomeCode {
        val oldRolePermissionOpt = rolePermissions.stream()
            .filter { rp -> rp.roleName == updatedRolePermission.roleName }
            .filter { rp -> rp.resourceName == updatedRolePermission.resourceName }
            .findFirst()

        if(oldRolePermissionOpt.isEmpty) {
            return CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND;
        }

        rolePermissions.remove(oldRolePermissionOpt.get())
        rolePermissions.add(updatedRolePermission)
        return CODE_000_SUCCESS
    }

    override var locks: Int = 0

    override fun lock(): OutcomeCode {
        users.setAutocommit(false)
        roles.setAutocommit(false)
        resources.setAutocommit(false)
        userRoles.setAutocommit(false)
        rolePermissions.setAutocommit(false)
        return CODE_000_SUCCESS
    }

    override fun rollback(): OutcomeCode {
        users.rollback()
        roles.rollback()
        resources.rollback()
        userRoles.rollback()
        rolePermissions.rollback()
        return CODE_000_SUCCESS
    }

    override fun unlock(): OutcomeCode {
        users.commit()
        roles.commit()
        resources.commit()
        userRoles.commit()
        rolePermissions.commit()
        users.setAutocommit(true)
        roles.setAutocommit(true)
        resources.setAutocommit(true)
        userRoles.setAutocommit(true)
        rolePermissions.setAutocommit(true)
        return CODE_000_SUCCESS
    }

    fun reset() {
        users.clear()
        roles.clear()
        resources.clear()
        userRoles.clear()
        rolePermissions.clear()
        users.commit()
        roles.commit()
        resources.commit()
        userRoles.commit()
        rolePermissions.commit()
    }

}