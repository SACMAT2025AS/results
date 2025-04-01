package cryptoac.core

import cryptoac.OutcomeCode
import cryptoac.OutcomeCode.*
import cryptoac.ac.ACServiceRBAC
import cryptoac.code.*
import cryptoac.dm.DMServiceRBAC
import cryptoac.mm.MMServiceRBACPredicates
import cryptoac.rm.RMServiceRBAC
import cryptoac.tuple.Operation
import cryptoac.tuple.Predicate
import cryptoac.tuple.TupleStatus
import cryptoac.tuple.User
import mu.KotlinLogging
import java.io.InputStream

private val logger = KotlinLogging.logger {}

/**
 * A CoreRBACPredicates extends the [Core] class as an enforcement
 * mechanism for an extended role-based access control model/scheme
 */
class CoreRBACPredicates(
    override val coreParameters: CoreParameters
) : Core(coreParameters) {

    override val mm: MMServiceRBACPredicates = TODO()

    override val rm: RMServiceRBAC = TODO()

    override val dm: DMServiceRBAC = TODO()

    override val ac: ACServiceRBAC = TODO()

    val cac: CoreCACRBACTuples = TODO()

    override val user: User = coreParameters.user



    /**
     * In this implementation, add the admin user, the
     * admin role, and the admin-admin UR assignment in
     * both the policy state and the traditional policy
     * state. Then, invoke the [configureServices]
     * method of the [cac] and return the outcome code
     */
    override fun configureServices(): OutcomeCode {

        logger.info { "CoreRBACPredicates: configureServices for admin ${user.name}" }

        /** Lock the status of the services */
        var code = startOfMethod()
        if (code != CODE_000_SUCCESS) {
            return code
        }

        logger.info { "Configuring the services for admin ${user.name}" }

        code = mm.configure()
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }
        code = rm.configure(coreParameters)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }
        code = dm.configure(coreParameters)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }
        code = ac.configure()
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }


        // TODO: E– Set state with ⟨ {adm}, {adm}, { }, { (adm, adm) }, { }, { } ⟩
        logger.info { "Initializing the admin ${user.name} in the extended policy state" }

        code = mm.addAdmin(newAdmin = user)
        if (code != CODE_000_SUCCESS) {
            return if (code == CODE_035_ADMIN_ALREADY_INITIALIZED) {
                logger.warn { "Code was $code, replacing with $CODE_077_SERVICE_ALREADY_CONFIGURED" }
                endOfMethod(CODE_077_SERVICE_ALREADY_CONFIGURED)
            } else {
                endOfMethod(code)
            }
        }

        /** Add the admin role in the metadata */
        code = mm.addRole(newRole = user.name)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        /** Add the admin user-role assignment in the metadata */
        code = mm.addUsersRoles(listOf(Pair(user.name, user.name)))
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }


        // TODO: T– Invoke addUserT(adm)
        // TODO: T– Invoke addRoleT(adm)
        // TODO: T– Invoke assignUserToRoleT(adm, adm)
        logger.info { "Adding the admin ${user.name} in the traditional policy state" }

        code = ac.addAdmin(newAdmin = user)
        if (code != CODE_000_SUCCESS) {
            return if (code == CODE_035_ADMIN_ALREADY_INITIALIZED) {
                logger.warn { "Code was $code, replacing with $CODE_077_SERVICE_ALREADY_CONFIGURED" }
                endOfMethod(CODE_077_SERVICE_ALREADY_CONFIGURED)
            } else {
                endOfMethod(code)
            }
        }

        /** Add the admin role in the AC */
        code = ac.addRole(roleName = user.name)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        /** Add the admin role assignment in the AC */
        code = ac.assignUserToRole(
            username = user.name,
            roleName = user.name
        )
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }


        // TODO: C– Invoke initC( )
        logger.info { "Adding the admin ${user.name} in the cryptographic policy state" }

        code = cac.configureServices()
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }


        logger.info { "Adding the admin ${user.name} in the DM" }
        code = dm.addAdmin(newAdmin = user)
        if (code != CODE_000_SUCCESS) {
            return endOfMethod(code)
        }

        logger.info { "Adding the admin ${user.name} in the RM" }
        return endOfMethod(
            code = rm.addAdmin(
                newAdmin = user
            ))
    }

    override fun initUser(): OutcomeCode {
        TODO("Not yet implemented")
    }

    override fun addUser(
        username: String
    ): CodeCoreParameters {
        return addUser(username, hashSetOf())
    }

    fun addUser(
        username: String,
        predicates: HashSet<Predicate>
    ): CodeCoreParameters {
        TODO("Not yet implemented")

//        addUser(u, preds = [ ] )
//        E– Update state with ⟨U ∪ {u}, R, F, UR, PA, EP ∪ { (p, u) |p ∈ preds} ⟩
//        T– Invoke addUserT(u)
//        C– Invoke addUserC(u)
//        E– Invoke updateUnderlying( )

    }

    override fun deleteUser(
        username: String
    ): OutcomeCode {
        TODO("Not yet implemented")

//        deleteUser(u)
//        C– For each r ∈ R s.t. (u, r ) ∈ UR:
//            ∗ Invoke revokeUserFromRoleC(u, r )
//            ∗ If isRoleKeyRotationNeededOnRUR(u, r ):
//                · Invoke rotateRoleKeyUserRoleC(r )
//        T– Invoke deleteUserT(u)
//        C– Invoke deleteUserC(u)
//        C– For each (f , ∗) ∈ F s.t. ∃op ∈ OP ∧ canDo (u, op, f ) ∧ isEncryptionNeeded (f ):
//            ∗ If ∃r ∈ R, op ∈ OP s.t. canUserDoViaRoleC (u, r, op, f ) ∧ isReencryptionNeededOnRUR(u, r, ⟨op, f ⟩ ):
//                · Invoke prepareReencryption(f )
//            ∗ If ∃r ∈ R, op ∈ OP s.t. canUserDoViaRoleC (u, r, op, f ) ∧ isEagerReencNeededOnRUR(u, r, ⟨op, f ⟩ ):
//                · Invoke reencryptResource(f )
//        C– For each r ∈ R s.t. (u, r ) ∈ UR ∧ isRoleKeyRotationNeededOnRUR(u, r ):
//            ∗ Invoke rotateRoleKeyPermissionsC(r )
//        E– Update state with ⟨U \ {u}, R, F, UR \ { (u, r ) |r ∈ R}, PA, EP \ { (p, u) |p ∈ P } ⟩
//        E– Invoke updateUnderlying( )
    }

    fun addRole(
        roleName: String,
        predicates: HashSet<Predicate>
    ): OutcomeCode {
        TODO("Not yet implemented")

//        addRole(r, preds = [ ] )
//        E– Update state with ⟨U, R∪ {r }, F, UR∪ { (adm, r ) }, PA, EP ∪ { (p, r ) |p ∈ preds} ⟩
//        T– Invoke addRoleT(r )
//        C– Invoke addRoleC(r )
//        T– Invoke assignUserToRoleT(adm, r )
//        E– Invoke updateUnderlying( )
    }

    fun deleteRole(
        roleName: String
    ): OutcomeCode {
        TODO("Not yet implemented")

//        deleteRole(r )
//        C– For each (f , ∗) ∈ F s.t. ∃ops ⊆ OP ∧ (r, ⟨ops, f ⟩ ) ∈ PA ∧ isEncryptionNeeded (f ):
//            ∗ Invoke revokePermissionFromRoleC(r, ⟨ops, f ⟩ )
//            ∗ If ∃op ∈ ops s.t. isReencryptionNeededOnRP (r, op, f ):
//                · Invoke prepareReencryption(f )
//            ∗ If ∃op ∈ ops s.t. isEagerReencNeededOnRP (r, op, f ):
//                · Invoke reencryptResource(f )
//        C– For each u ∈ U s.t. (u, r ) ∈ UR:
//            ∗ Invoke revokeUserFromRoleC(u, r )
//        T– Invoke deleteRoleT(r )
//        C– Invoke deleteRoleC(r )
//        E– Update state with ⟨U, R \ {r }, F, UR \ { (u, r ) |u ∈ U }, PA \ { (r, ⟨ops, f ⟩ ) |ops ⊆ OP, (f , ∗) ∈ F }, EP \ { (p, r ) |p ∈ P } ⟩
//        E– Invoke updateUnderlying( )
    }

    fun addResource(
        resourceName: String,
        resourceContent: InputStream,
        predicates: HashSet<Predicate>,
        threshold: Int,
    ): OutcomeCode {
        TODO("Not yet implemented")

//        addResourceu (f , fc, preds = [ ], tf = 1)
//        E– Update state with ⟨U, R, F ∪ { (f , tf ) }, UR, PA ∪ { (adm, ⟨OP, f ⟩ ) }, EP ∪ { (p, f ) |p ∈ preds} ⟩
//        C– If isEncryptionNeeded (f ):
//            ∗ Invoke addResourceCu (f )
//            ∗ Invoke fc enc ← writeResourceCu (f , fc)
//        C– Else:
//            ∗ Set fc enc ← fc
//        T– Invoke addResourceT(f )
//        T– invoke assignPermissionToRoleT(adm, ⟨OP, f ⟩ )
//        E– Send fc^enc to the D.S.
//        E– Invoke updateUnderlying( )
    }

    fun deleteResource(
        resourceName: String
    ): OutcomeCode {
        TODO("Not yet implemented")

//        deleteResource(f )
//        T– Invoke deleteResourceT(f )
//        C– If isEncryptionNeeded (f ):
//        ∗ For each r ∈ R s.t. ∃ops ⊆ OP ∧ (r, ⟨ops, f ⟩ ) ∈ PA:
//        · Invoke revokePermissionFromRoleC(r, ⟨ops, f ⟩ )
//        ∗ Invoke deleteResourceC(f )
//        E– Update state with ⟨U, R, F \ { (f , tf ) |tf ∈ N}, UR, PA \ { (r, ⟨ops, f ⟩ ) |r ∈ R, ops ⊆ OP }, EP \ { (p, f ) |p ∈ P } ⟩
//        E– Invoke updateUnderlying( )
//        E– Delete the content associated with f in the D.S.
    }

    fun assignUserToRole(
        username: String,
        roleName: String
    ): OutcomeCode {
        TODO("Not yet implemented")

//        assignUserToRole(u, r )
//        E– Update state with ⟨U, R, F, UR ∪ { (u, r ) }, PA, EP ⟩
//        T– Invoke assignUserToRoleT(u, r )
//        C– Invoke assignUserToRoleC(u, r )
//        E– Invoke updateUnderlying( )
    }

    fun revokeUserFromRole(
        username: String,
        roleName: String
    ): OutcomeCode {
        TODO("Not yet implemented")

//        revokeUserFromRole(u, r )
//        T– Invoke revokeUserFromRoleT(u, r )
//        C– Invoke revokeUserFromRoleC(u, r )
//        C– If isRoleKeyRotationNeededOnRUR(u, r ):
//        ∗ Invoke rotateRoleKeyUserRoleC(r )
//        C– For each (f , ∗) ∈ F s.t. ∃ops ⊆ OP ∧ (r, ⟨ops, f ⟩ ) ∈ PA ∧ isEncryptionNeeded (𝑓 ):
//        ∗ If ∃op ∈ ops s.t. isReencryptionNeededOnRUR(u, r, ⟨op, f ⟩ ):
//        · Invoke prepareReencryption(f )
//        ∗ If ∃op ∈ ops s.t. isEagerReencNeededOnRP (u, r, ⟨op, f ⟩ ):
//        · Invoke reencryptResource(f )
//        C– If isRoleKeyRotationNeededOnRUR(u, r ):
//        ∗ Invoke rotateRoleKeyPermissionsC(r )
//        E– Update status with ⟨U, R, F, UR \ { (u, r ) }, PA, EP ⟩
    }

    fun assignPermissionToRole(
        username: String,
        roleName: String,
        operation: Operation
    ): OutcomeCode {
        TODO("Not yet implemented")

//        assignPermissionToRole(r, ⟨ops, f ⟩ ):
//        E– Check and find (r, ⟨ops , f ⟩ ) ∈ PA, otherwise set ops ← { }
//        E– Update state with ⟨U, R, F, UR, PA \ { (u, ⟨ops , f ⟩ ) } ∪ {r, ⟨ops ∪ ops, f ⟩ }, EP ⟩
//        T– Invoke assignPermissionToRoleT(r, ⟨ops, f ⟩ )
//        C– Invoke assignPermissionToRoleC(r, ⟨ops, f ⟩ )
//        E– Invoke updateUnderlying( )
    }

    fun revokePermissionFromRole(
        username: String,
        roleName: String,
        operation: Operation
    ): OutcomeCode {
        TODO("Not yet implemented")

//        revokePermissionFromRole(r, ⟨ops, f ⟩ )
//        T– Invoke revokePermissionFromRoleT(r, ⟨ops, f ⟩ )
//        C– If isEncryptionNeeded (f ):
//            ∗ Invoke revokePermissionFromRoleC(r, ⟨ops, f ⟩ )
//        C– Set encrypted ← isEncryptionNeeded (f )
//        E– Find (r, ⟨ops , f ⟩ ) ∈ PA
//        E– If ops \ ops = ∅:
//            ∗ Update state with ⟨U, R, F, UR, PA \ { (r, ⟨ops′ , f ⟩ ) }, EP ⟩
//        E– Else:
//            ∗ Update state with ⟨U, R, F, UR, PA \ { (r, ⟨ops′ , f ⟩ ) } ∪ { (r, ops′ \ ops, f ) }, EP ⟩
//        E– Invoke updateUnderlying( )
//        C– If isEncryptionNeeded (f ) = encrypted = true:
//            ∗ If ∃op ∈ ops s.t. isReencryptionNeededOnRP (r, op, f ):
//                · Invoke prepareReencryption(f )
//            ∗ If ∃op ∈ ops s.t. isEagerReencNeededOnRP (r, op, f ):
//                · Invoke reencryptResource(f )
    }

    fun assignPredicateToUser(
        username: String,
        predicate: Predicate
    ): OutcomeCode {
        TODO("Not yet implemented")

//        assignPredicate(p, e)
//        E– Update state with ⟨U, R, F, UR, PA, EP ∪ { (p, e) } ⟩
//        E– Invoke updateUnderlying( )

    }

    fun revokePredicateFromUser(
        username: String,
        predicate: Predicate
    ): OutcomeCode {
        TODO("Not yet implemented")

//        revokePredicate(p, e)
//        E– Update state with ⟨U, R, F, UR, PA, EP \ { (p, e) } ⟩
//        E– Invoke updateUnderlying( )
    }

    fun assignPredicateToRole(
        roleName: String,
        predicate: Predicate
    ): OutcomeCode {
        TODO("Not yet implemented")

//        assignPredicate(p, e)
//        E– Update state with ⟨U, R, F, UR, PA, EP ∪ { (p, e) } ⟩
//        E– Invoke updateUnderlying( )

    }

    fun revokePredicateFromRole(
        roleName: String,
        predicate: Predicate
    ): OutcomeCode {
        TODO("Not yet implemented")

//        revokePredicate(p, e)
//        E– Update state with ⟨U, R, F, UR, PA, EP \ { (p, e) } ⟩
//        E– Invoke updateUnderlying( )
    }

    fun assignPredicateToResource(
        resourceName: String,
        predicate: Predicate
    ): OutcomeCode {
        TODO("Not yet implemented")

//        assignPredicate(p, e)
//        E– Update state with ⟨U, R, F, UR, PA, EP ∪ { (p, e) } ⟩
//        E– Invoke updateUnderlying( )

    }

    fun revokePredicateFromResource(
        resourceName: String,
        predicate: Predicate
    ): OutcomeCode {
        TODO("Not yet implemented")

//        revokePredicate(p, e)
//        E– Update state with ⟨U, R, F, UR, PA, EP \ { (p, e) } ⟩
//        E– Invoke updateUnderlying( )
    }

    fun readResource(
        resourceName: String
    ): CodeResource {
        TODO("Not yet implemented")

//        readResourceu (f )
//        E– Retrieve fc^enC from the D.S.
//        T– If ¬canDoT (u, read, f ):
//        ∗ Return ⊥
//        C– If isEncryptionNeeded (f ):
//        ∗ Invoke fc ← readResourceCu (f , fc enc )
//        C– Else:
//        ∗ Set fc ← fc enc
//        E– Return fc
    }

    fun writeResource(
        resourceName: String,
        resourceContent: InputStream
    ): OutcomeCode {
        TODO("Not yet implemented")

//        writeResourceu (f , fc)
//        C– If isEncryptionNeeded (f )
//        ∗ Invoke fc enc ← writeResourceCu (f , fc)
//        C– Else:
//        ∗ Set fc enc ← fc
//        E– Send fc^enC to the R.M.
//        E– The R.M. checks whether canDoT (u, write, f ):
//        ∗ Send fc enc to the D.S.
//        ∗ Return ⊤
//        E– Else:
//        ∗ Return ⊥
    }

    fun prepareReEncryption(
        resourceName: String
    ): OutcomeCode {
        TODO("Not yet implemented")

//        prepareReencryption(f )
//        E– Retrieve fc^enC from the D.S.
//        C– Invoke prepareReencryptionC(f, fc enc )
    }

    fun reEncryptResource(
        resourceName: String
    ): OutcomeCode {
        TODO("Not yet implemented")

//        reencryptResource(f )
//        E– Retrieve fc^enC from the D.S.
//        C– Invoke fc^enc_new ← reencryptResourceC(f , fc^enC)
//        E– Send fc^enc_new to the D.S.
    }

    fun appendResource(
        resourceName: String,
        resourceContent: InputStream
    ): OutcomeCode {
        TODO("Not yet implemented")

//        appendResourceu (f , fc)
//        E– Find (f , tf ) ∈ F
//        E– Retrieve fc^enC from the D.S.
//        C– Invoke fc^enc_new ← appendResourceCu (f , fc^enc , fc, tf )
    }

    fun setThreshold(
        resourceName: String,
        threshold: Int
    ): OutcomeCode {
        TODO("Not yet implemented")

//        setThreshold(f , tf )
//        E– Update state with ⟨U, R, F \ { (f , tf ) |tf ∈ N} ∪ { (f , tf ) }, UR, PA, EP ⟩
//        E– Invoke appendReourceadm (f , □)
    }

    fun updateUnderlying(): OutcomeCode {
        TODO("Not yet implemented")

//        updateUnderlying( )
//        C– For each (f , ∗) ∈ F s.t. isEncryptionNeeded (f ) ∧ ¬isProtected-
//        WithCACC (f ):
//        ∗ Retrieve and delete fc from the D.S.
//        ∗ Invoke addResourceCadm (f )
//        ∗ For each r ∈ R s.t. ∃ops ⊆ OP ∧ (r, ⟨ops, f ⟩ ) ∈ PA:
//        · Invoke assignPermissionToRoleC(r, ⟨ops, f ⟩ )
//        ∗ Invoke fc enc ← writeResourceCadm (f , fc)
//        ∗ Send fc enc to the D.S.
//        C
//        For
//        each (f , ∗) ∈ F s.t. ¬isEncryptionNeeded (f ) ∧ isProtected-
//        –
//        WithCACC (f ):
//        ∗ Retrieve and delete fc enc from the D.S.
//        ∗ Invoke fc ← readResourceCadm (f , fc enc )
//        ∗ For each r ∈ R s.t. ∃ops ⊆ OP ∧ (r, ⟨ops, f ⟩ ) ∈ PA:
//        · Invoke revokePermissionFromRoleC(r, ⟨ops, f ⟩ )
//        ∗ Invoke deleteResourceC(f )
//        ∗ Send fc to the D.S.
//        – Create set rotating_roles
//        – For each u ∈ U, r ∈ R s.t. ¬canUserBeC (u, r )∧isRoleKeyRotation-
//        NeededOnRUR(u, r ) ∧ canUserBeCacheC (u, r ):
//        ∗ Invoke rotateRoleKeyUserRoleC(r )
//        ∗ Add r to rotating_roles
//        – For each u ∈ U, (f , ∗) ∈ F s.t. ∃r ∈ R ∧ isReencryptionNeeded-
//        OnRUR(u, r, ⟨op, f ⟩ ) ∧ ¬canUserDoC (u, op, f ) ∧ canUserDoVia-
//        RoleCacheLastC (u, r, op, f ) ∧ ∃op ∈ OP:
//        ∗ Invoke prepareReencryption(f )
//        – For each u ∈ U, (f , ∗) ∈ F s.t. ∃r ∈ R ∧ isEagerReencNeeded-
//        OnRUR(u, r, ⟨op, f ⟩ ) ∧ ¬canUserDoC (u, op, f ) ∧ canUserDoVia-
//        RoleCacheC (u, r, op, f ) ∧ ∃op ∈ OP:
//        ∗ Invoke reencryptResource(f )
//        – For each r ∈ R, (f , ∗) ∈ F s.t. isReencryptionNeededOnRP (r, op,
//        f ) ∧ ¬canRoleDoC (r, op, f ) ∧ canRoleDoCacheLastC (r, op, f ) ∧
//        ∃op ∈ OP:
//        ∗ Invoke prepareReencryption(f )
//        – For each r ∈ R, (f , ∗) ∈ F s.t. isEagerReencNeededOnRP (r, op,
//        f ) ∧ ¬canRoleDoCacheC (r, op, f ) ∧ canRoleDoCacheC (r, op, f ) ∧
//        ∃op ∈ OP:
//        ∗ Invoke reencryptResource(f )
//        – For each r ∈ rotating_roles:
//        ∗ Invoke rotateRoleKeyPermissionsC(r )
    }

    override fun getUsers(
        statuses: Array<TupleStatus>
    ): CodeUsers {
        TODO("Not yet implemented")
    }

    fun getRoles(): CodeRoles {
        TODO("Not yet implemented")
    }

    fun getResources(): CodeResources {
        TODO("Not yet implemented")
    }

    fun getUsersRoles(): CodeUsersRoles {
        TODO("Not yet implemented")
    }

    fun getRolesPermissions(): CodeRolesPermissions {
        TODO("Not yet implemented")
    }

    fun getUsersPredicates(): CodeElementsPredicates {
        TODO("Not yet implemented")
    }

    fun getRolesPredicates(): CodeElementsPredicates {
        TODO("Not yet implemented")
    }

    fun getResourcesPredicates(): CodeElementsPredicates {
        TODO("Not yet implemented")
    }
}