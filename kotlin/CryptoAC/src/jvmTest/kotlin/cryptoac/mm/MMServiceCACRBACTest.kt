package cryptoac.mm

import cryptoac.*
import cryptoac.Constants.ADMIN
import cryptoac.OutcomeCode.*
import cryptoac.Parameters.aliceUser
import cryptoac.Parameters.bobUser
import cryptoac.Parameters.carlUser
import cryptoac.TestUtilities.Companion.assertUnlockAndLock
import cryptoac.TestUtilities.Companion.createResource
import cryptoac.TestUtilities.Companion.createRole
import cryptoac.tuple.*
import cryptoac.crypto.AsymKeysType
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal abstract class MMServiceCACRBACTest : MMServiceTest() {

    abstract override val mm: MMServiceCACRBAC



    @Test
    open fun `add role once works`() {
        /** add role once */
        myRun {
            addRole("employee")
        }
    }

    @Test
    open fun `add role twice or with admin name or blank name or same name as previously deleted role fails`() {
        val studentRole = addRole("student")

        /** add role twice */
        myRun {
            assertEquals(CODE_002_ROLE_ALREADY_EXISTS, mm.addRole(studentRole))
        }

        /** add role with admin name */
        myRun {
            assertEquals(CODE_002_ROLE_ALREADY_EXISTS, mm.addRole(Parameters.adminRole))
        }

        /** add role with blank name */
        myRun {
            assertEquals(CODE_020_INVALID_PARAMETER, mm.addRole(createRole("")))
        }

        /** add role with same name as previously deleted role */
        myRun {
            assertEquals(CODE_000_SUCCESS, mm.deleteUsersRoles(roleName = studentRole.name, status = TupleStatus.OPERATIONAL))
            assertEquals(CODE_000_SUCCESS, mm.deleteRole(studentRole.name))
            assertEquals(CODE_014_ROLE_WAS_DELETED, mm.addRole(studentRole))
        }
    }

    @Test
    open fun `add resource once works`() {
        /** add resource once */
        myRun {
            addResource("exam")
        }
    }

    @Test
    open fun `add resource twice or with blank name or same name as previously deleted resource fails`() {
        val exam = addResource("exam")

        /** add resource twice */
        myRun {
            assertEquals(CODE_003_RESOURCE_ALREADY_EXISTS, mm.addResource(exam))
        }

        /** add resource with blank name */
        myRun {
            assertEquals(CODE_020_INVALID_PARAMETER, mm.addResource(createResource("")))
        }

        /** add resource with same name as previously deleted resource */
        myRun {
            assertEquals(CODE_000_SUCCESS, mm.deleteResource(exam.name))
            assertEquals(CODE_015_RESOURCE_WAS_DELETED, mm.addResource(exam))
        }
    }

    @Test
    open fun `add no, one or multiple user-role assignments work`() {
        addAndInitUser(aliceUser)
        addAndInitUser(bobUser)
        addAndInitUser(carlUser)
        val roleEmployee = addRole("employee")

        /** add no user-role assignments */
        myRun {
            assertEquals(CODE_000_SUCCESS, mm.addUsersRoles(HashSet()))
        }

        /** add one user-role assignment */
        myRun {
            addUserRole(aliceUser.name, roleEmployee)
        }

        /** add multiple user-role assignments */
        myRun {
            val bobUserRole = TestUtilities.createUserRole(bobUser.name, roleEmployee)
            val carlUserRole = TestUtilities.createUserRole(carlUser.name, roleEmployee)
            assertEquals(
                CODE_000_SUCCESS,
                mm.addUsersRoles(hashSetOf(
                    bobUserRole,
                    carlUserRole
                )))
        }
    }

    @Test
    open fun `add user-role assignment twice fails`() {
        addAndInitUser(aliceUser)
        val roleEmployee = addRole("employee")

        /** add user-role assignment twice */
        myRun {
            val aliceUserRole = addUserRole(aliceUser.name, roleEmployee)
            assertEquals(
                CODE_010_USER_ROLE_ASSIGNMENT_ALREADY_EXISTS,
                mm.addUsersRoles(hashSetOf(aliceUserRole))
            )
        }
    }

    @Test
    open fun `add user-role assignment with non-existing or deleted user or role fails`() {
        addAndInitUser(aliceUser)
        val userDeleted = bobUser
        addAndInitUser(userDeleted)
        assertEquals(CODE_000_SUCCESS, mm.deleteUser(userDeleted.name))
        val roleDeleted = addRole("role")
        assertEquals(CODE_000_SUCCESS, mm.deleteUsersRoles(roleName = roleDeleted.name, status = TupleStatus.OPERATIONAL))
        assertEquals(CODE_000_SUCCESS, mm.deleteRole(roleDeleted.name))
        val roleEmployee = addRole("employee")
        val roleNonExisting = TestUtilities.createRole("non-existing")

        /** add user-role assignment with non-existing user */
        myRun {
            val nonExistingUserUserRole = TestUtilities.createUserRole("non-existing", roleEmployee)
            assertEquals(
                CODE_004_USER_NOT_FOUND,
                mm.addUsersRoles(hashSetOf(nonExistingUserUserRole))
            )
        }

        /** add user-role assignment with deleted user */
        myRun {
            val deleteUserUserRole = TestUtilities.createUserRole(userDeleted.name, roleEmployee)
            assertEquals(
                CODE_013_USER_WAS_DELETED,
                mm.addUsersRoles(hashSetOf(deleteUserUserRole))
            )
        }

        /** add user-role assignment with non-existing role */
        myRun {
            val nonExistingRoleUserRole = TestUtilities.createUserRole(aliceUser.name, roleNonExisting)
            assertEquals(
                CODE_005_ROLE_NOT_FOUND,
                mm.addUsersRoles(hashSetOf(nonExistingRoleUserRole))
            )
        }

        /** add user-role assignment with deleted role */
        myRun {
            val deletedRoleUserRole = TestUtilities.createUserRole(aliceUser.name, roleDeleted)
            assertEquals(
                CODE_014_ROLE_WAS_DELETED,
                mm.addUsersRoles(hashSetOf(deletedRoleUserRole))
            )
        }
    }

    @Test
    open fun `add no, one or multiple role-permission assignments work`() {
        val roleEmployee = addRole("employee")
        val roleStudent = addRole("student")
        val roleDirector = addRole("director")

        val resourceExam = addResource("exam")

        /** add no role-permission assignments*/
        myRun {
            assertEquals(CODE_000_SUCCESS, mm.addRolesPermissions(HashSet()))
        }

        /** add one role-permission assignment */
        myRun {
            addRolePermission(roleEmployee, resourceExam)
        }

        /** add multiple role-permission assignments*/
        myRun {
            val studentRolePermission = TestUtilities.createRolePermission(roleStudent, resourceExam)
            val directoryRolePermission = TestUtilities.createRolePermission(roleDirector, resourceExam)
            assertEquals(
                CODE_000_SUCCESS,
                mm.addRolesPermissions(hashSetOf(
                    studentRolePermission,
                    directoryRolePermission
                )))
        }
    }

    @Test
    open fun `add role-permission assignment twice fails`() {
        val roleEmployee = addRole("employee")
        val resourceExam = addResource("exam")

        /** add role-permission assignment twice */
        myRun {
            val employeeRolePermission = addRolePermission(roleEmployee, resourceExam)
            assertEquals(
                CODE_011_ROLE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS,
                mm.addRolesPermissions(hashSetOf(employeeRolePermission))
            )
        }
    }

    @Test
    open fun `add role-permission assignment with non-existing or deleted role or resource fails`() {
        val roleEmployee = addRole("employee")
        val roleDeleted = addRole("role-deleted")
        assertEquals(CODE_000_SUCCESS, mm.deleteUsersRoles(roleName = roleDeleted.name))
        assertEquals(CODE_000_SUCCESS, mm.deleteRole(roleDeleted.name))
        val roleNonExisting = createRole("non-existing-role")

        val resourceExam = addResource("exam")
        val resourceNonExisting = createResource(
            resourceName = "non-existing-resource",
            enforcement = Enforcement.COMBINED
        )
        val resourceDeleted = addResource("resource-deleted")
        assertEquals(CODE_000_SUCCESS, mm.deleteResource(resourceDeleted.name))

        /** add role-permission assignment with non-existing role */
        myRun {
            val nonExistingRoleRolePermission = TestUtilities.createRolePermission(roleNonExisting, resourceExam)
            assertEquals(
                CODE_005_ROLE_NOT_FOUND,
                mm.addRolesPermissions(hashSetOf(nonExistingRoleRolePermission))
            )
        }

        /** add role-permission assignment with deleted role */
        myRun {
            val deletedRoleRolePermission = TestUtilities.createRolePermission(roleDeleted, resourceExam)
            assertEquals(
                CODE_014_ROLE_WAS_DELETED,
                mm.addRolesPermissions(hashSetOf(deletedRoleRolePermission))
            )
        }

        /** add role-permission assignment with non-existing resource */
        myRun {
            val nonExistingResourceRolePermission =
                TestUtilities.createRolePermission(roleEmployee, resourceNonExisting)
            assertEquals(
                CODE_006_RESOURCE_NOT_FOUND,
                mm.addRolesPermissions(hashSetOf(nonExistingResourceRolePermission))
            )
        }

        /** add role-permission assignment with deleted resource */
        myRun {
            val deletedResourceRolePermission = TestUtilities.createRolePermission(roleEmployee, resourceDeleted)
            assertEquals(
                CODE_015_RESOURCE_WAS_DELETED,
                mm.addRolesPermissions(hashSetOf(deletedResourceRolePermission))
            )
        }
    }

    @Test
    open fun `get all roles works`() {
        val student = addRole("student")
        val employee = addRole("employee")
        val director = addRole("director")

        /** get all roles */
        myRun {
            val allRoles = mm.getRoles()
            /** there is also the admin */
            assertEquals(4, allRoles.size)
            assertEquals(1, allRoles.filter { it.name == student.name }.size)
            assertEquals(1, allRoles.filter { it.name == employee.name }.size)
            assertEquals(1, allRoles.filter { it.name == director.name }.size)
            assertEquals(1, allRoles.filter { it.name == ADMIN }.size)
        }
    }

    @Test
    open fun `get not-existing, operational and deleted role by name works`() {
        val operational = addRole("operational")
        val deleted = addRole("deleted")
        assertEquals(CODE_000_SUCCESS, mm.deleteUsersRoles(roleName = deleted.name))
        assertEquals(CODE_000_SUCCESS, mm.deleteRole(deleted.name))

        /** get not-existing role by name */
        myRun {
            assert(mm.getRoles(roleName = "non-existing").isEmpty())
        }

        /** get operational role by name */
        myRun {
            val operationalRoleByName = mm.getRoles(roleName = operational.name)
            assertEquals(1, operationalRoleByName.size)
            assertEquals(operational.token, operationalRoleByName.firstOrNull()!!.token)
        }

        /** get deleted role by name */
        myRun {
            val deletedRoleByName = mm.getRoles(roleName = deleted.name, status = TupleStatus.DELETED)
            assertEquals(1, deletedRoleByName.size)
            assert(deletedRoleByName.firstOrNull()!!.name == deleted.name)
        }
    }

    @Test
    open fun `get all resources works`() {
        val exam = addResource("exam")
        val document = addResource("document")
        val excel = addResource("excel")

        /** get all resources */
        myRun {
            val allResources = mm.getResources()
            assert(allResources.size == 3)
            assert(allResources.filter { it.name == exam.name }.size == 1)
            assert(allResources.filter { it.name == document.name }.size == 1)
            assert(allResources.filter { it.name == excel.name }.size == 1)
        }
    }

    @Test
    open fun `get not-existing, operational and deleted resource by name works`() {
        val operational = addResource("operational")
        val deleted = addResource("deleted")
        assertUnlockAndLock(mm)
        assert(mm.deleteResource(deleted.name) == CODE_000_SUCCESS)

        /** get not-existing resource by name */
        myRun {
            assert(mm.getResources(resourceName = "non-existing").isEmpty())
        }

        /** get operational resource by name */
        myRun {
            val operationalResourceByName = mm.getResources(resourceName = operational.name)
            assert(operationalResourceByName.size == 1)
            assert(operationalResourceByName.firstOrNull()!!.token == operational.token)
        }

        /** get deleted resource by name */
        myRun {
            val deletedResourceByName = mm.getResources(resourceName = deleted.name, status = TupleStatus.DELETED)
            assert(deletedResourceByName.size == 1)
            assertEquals(deleted.token, deletedResourceByName.firstOrNull()!!.token)
        }
    }

    @Test
    open fun `get user-role assignments of the admin or of users by username or role name works`() {
        addAndInitUser(aliceUser)
        val student = addRole("student")
        addUserRole("alice", student)

        /** get user-role assignment of the admin by username or role name */
        myRun {
            val adminUsersRolesByUsername = mm.getUsersRoles(username = ADMIN)
            assertEquals(2, adminUsersRolesByUsername.size)
            assert(adminUsersRolesByUsername.filter { it.roleName == ADMIN }.size == 1)
            assert(adminUsersRolesByUsername.filter { it.roleName == student.name }.size == 1)

            val adminUsersRolesByRoleName = mm.getUsersRoles(roleName = ADMIN)
            assert(adminUsersRolesByRoleName.size == 1)
            assert(adminUsersRolesByRoleName.firstOrNull()!!.username == ADMIN)
        }

        /** get user-role assignments of users by username or role name */
        myRun {
            val aliceUsersRolesByUsername = mm.getUsersRoles(username = aliceUser.name)
            assert(aliceUsersRolesByUsername.size == 1)
            assert(aliceUsersRolesByUsername.filter { it.roleName == student.name }.size == 1)

            val studentUsersRolesByRoleName = mm.getUsersRoles(roleName = student.name)
            assert(studentUsersRolesByRoleName.size == 2)
            assert(studentUsersRolesByRoleName.filter { it.username == ADMIN }.size == 1)
            assert(studentUsersRolesByRoleName.filter { it.username == aliceUser.name }.size == 1)
        }
    }

    @Test
    open fun `get role-permission assignments by role or resource name works`() {
        val roleEmployee = addRole("employee")
        val roleStudent = addRole("student")

        val resourceExam = addResource("exam")

        addRolePermission(roleEmployee, resourceExam)
        addRolePermission(roleStudent, resourceExam)

        /** get role-permission assignments by role or resource name */
        myRun {
            val examRolesPermissionsByName = mm.getRolesPermissions(resourceName = resourceExam.name)
            assert(examRolesPermissionsByName.size == 2)
            assert(examRolesPermissionsByName.filter { it.roleName == roleEmployee.name }.size == 1)
            assert(examRolesPermissionsByName.filter { it.roleName == roleStudent.name }.size == 1)
        }
    }

    @Test
    open fun `get public key of non-existing, incomplete, operational and deleted users and roles by name or token works`() {
        val nonExistingUser = aliceUser
        val incompleteUser = bobUser
        val operationalUser = carlUser
        val deleteUser = Parameters.deniseUser
        assert(mm.addUser(incompleteUser).code == CODE_000_SUCCESS)
        addAndInitUser(operationalUser)
        addAndInitUser(deleteUser)
        assert(mm.deleteUser(deleteUser.name) == CODE_000_SUCCESS)

        val nonExistingRole = Role("nonExistingRole")
        val operationalRole = addRole("operationalRole")
        val deletedRole = addRole("deletedRole")
        assert(mm.deleteUsersRoles(roleName = deletedRole.name) == CODE_000_SUCCESS)
        assert(mm.deleteRole(deletedRole.name) == CODE_000_SUCCESS)

        /** get public key of non-existing users by name or token */
        myRun {
            assert(
                mm.getPublicKey(
                    name = nonExistingUser.name,
                    elementType = RBACElementType.USER,
                    asymKeyType = AsymKeysType.ENC
                ) == null
            )
            assert(
                mm.getPublicKey(
                    token = nonExistingUser.token,
                    elementType = RBACElementType.USER,
                    asymKeyType = AsymKeysType.ENC
                ) == null
            )
            assert(
                mm.getPublicKey(
                    name = nonExistingUser.name,
                    elementType = RBACElementType.USER,
                    asymKeyType = AsymKeysType.SIG
                ) == null
            )
            assert(
                mm.getPublicKey(
                    token = nonExistingUser.token,
                    elementType = RBACElementType.USER,
                    asymKeyType = AsymKeysType.SIG
                ) == null
            )
        }

        /** get public key of incomplete users by name or token */
        myRun {
            assertNull(
                mm.getPublicKey(
                    name = incompleteUser.name,
                    elementType = RBACElementType.USER,
                    asymKeyType = AsymKeysType.ENC
                )
            )
            assertNull(
                mm.getPublicKey(
                    token = incompleteUser.token,
                    elementType = RBACElementType.USER,
                    asymKeyType = AsymKeysType.ENC
                )
            )
            assertNull(
                mm.getPublicKey(
                    name = incompleteUser.name,
                    elementType = RBACElementType.USER,
                    asymKeyType = AsymKeysType.SIG
                )
            )
            assert(
                mm.getPublicKey(
                    token = incompleteUser.token,
                    elementType = RBACElementType.USER,
                    asymKeyType = AsymKeysType.SIG
                ) == null
            )
        }

        /** get public key of operational users by name or token */
        myRun {
            val asymEncKeysBytesByName = mm.getPublicKey(
                name = operationalUser.name,
                elementType = RBACElementType.USER,
                asymKeyType = AsymKeysType.ENC
            )
            assertNotNull(asymEncKeysBytesByName)
            assert(asymEncKeysBytesByName.contentEquals(operationalUser.asymEncKeys!!.public.decodeBase64()))

            val asymEncKeysBytesByToken = mm.getPublicKey(
                token = operationalUser.token,
                elementType = RBACElementType.USER,
                asymKeyType = AsymKeysType.ENC
            )
            assertNotNull(asymEncKeysBytesByToken)
            assert(asymEncKeysBytesByToken.contentEquals(operationalUser.asymEncKeys!!.public.decodeBase64()))

            val asymSigKeysBytesByName = mm.getPublicKey(
                name = operationalUser.name,
                elementType = RBACElementType.USER,
                asymKeyType = AsymKeysType.SIG
            )
            assert(asymSigKeysBytesByName != null)
            assert(asymSigKeysBytesByName.contentEquals(operationalUser.asymSigKeys!!.public.decodeBase64()))

            val asymSigKeysBytesByToken = mm.getPublicKey(
                token = operationalUser.token,
                elementType = RBACElementType.USER,
                asymKeyType = AsymKeysType.SIG
            )
            assert(asymSigKeysBytesByToken != null)
            assert(asymSigKeysBytesByToken.contentEquals(operationalUser.asymSigKeys!!.public.decodeBase64()))
        }

        /** get public key of deleted users by name or token */
        myRun {
            mm.getPublicKey(
                name = deleteUser.name,
                elementType = RBACElementType.USER,
                asymKeyType = AsymKeysType.ENC
            ).apply {
                assert(this != null)
                assert(this.contentEquals(deleteUser.asymEncKeys!!.public.decodeBase64()))
            }
            mm.getPublicKey(
                token = deleteUser.token,
                elementType = RBACElementType.USER,
                asymKeyType = AsymKeysType.ENC
            ).apply {
                assert(this != null)
                assert(this.contentEquals(deleteUser.asymEncKeys!!.public.decodeBase64()))
            }
            mm.getPublicKey(
                name = deleteUser.name,
                elementType = RBACElementType.USER,
                asymKeyType = AsymKeysType.SIG
            ).apply {
                assert(this != null)
                assert(this.contentEquals(deleteUser.asymSigKeys!!.public.decodeBase64()))
            }
            mm.getPublicKey(
                token = deleteUser.token,
                elementType = RBACElementType.USER,
                asymKeyType = AsymKeysType.SIG
            ).apply {
                assert(this != null)
                assert(this.contentEquals(deleteUser.asymSigKeys!!.public.decodeBase64()))
            }
        }

        /** get public key of non-existing roles by name or token */
        myRun {
            assert(
                mm.getPublicKey(
                    name = nonExistingRole.name,
                    elementType = RBACElementType.ROLE,
                    asymKeyType = AsymKeysType.ENC
                ) == null
            )
            assert(
                mm.getPublicKey(
                    token = nonExistingRole.token,
                    elementType = RBACElementType.ROLE,
                    asymKeyType = AsymKeysType.ENC
                ) == null
            )
            assert(
                mm.getPublicKey(
                    name = nonExistingRole.name,
                    elementType = RBACElementType.ROLE,
                    asymKeyType = AsymKeysType.SIG
                ) == null
            )
            assert(
                mm.getPublicKey(
                    token = nonExistingRole.token,
                    elementType = RBACElementType.ROLE,
                    asymKeyType = AsymKeysType.SIG
                ) == null
            )
        }

        /** get public key of operational roles by name or token */
        myRun {
            val asymEncKeysBytesByName = mm.getPublicKey(
                name = operationalRole.name,
                elementType = RBACElementType.ROLE,
                asymKeyType = AsymKeysType.ENC
            )
            assert(asymEncKeysBytesByName != null)
            assert(asymEncKeysBytesByName.contentEquals(operationalRole.asymEncKeys!!.public.decodeBase64()))

            val asymEncKeysBytesByToken = mm.getPublicKey(
                token = operationalRole.token,
                elementType = RBACElementType.ROLE,
                asymKeyType = AsymKeysType.ENC
            )
            assert(asymEncKeysBytesByToken != null)
            assert(asymEncKeysBytesByToken.contentEquals(operationalRole.asymEncKeys!!.public.decodeBase64()))

            val asymSigKeysBytesByName = mm.getPublicKey(
                name = operationalRole.name,
                elementType = RBACElementType.ROLE,
                asymKeyType = AsymKeysType.SIG
            )
            assert(asymSigKeysBytesByName != null)
            assert(asymSigKeysBytesByName.contentEquals(operationalRole.asymSigKeys!!.public.decodeBase64()))

            val asymSigKeysBytesByToken = mm.getPublicKey(
                token = operationalRole.token,
                elementType = RBACElementType.ROLE,
                asymKeyType = AsymKeysType.SIG
            )
            assert(asymSigKeysBytesByToken != null)
            assert(asymSigKeysBytesByToken.contentEquals(operationalRole.asymSigKeys!!.public.decodeBase64()))
        }

        /** get public key of deleted roles by name or token */
        myRun {
            mm.getPublicKey(
                name = deletedRole.name,
                elementType = RBACElementType.ROLE,
                asymKeyType = AsymKeysType.ENC
            ).apply {
                assert(this != null)
                assert(this.contentEquals(deletedRole.asymEncKeys!!.public.decodeBase64()))
            }
            mm.getPublicKey(
                token = deletedRole.token,
                elementType = RBACElementType.ROLE,
                asymKeyType = AsymKeysType.ENC
            ).apply {
                assert(this != null)
                assert(this.contentEquals(deletedRole.asymEncKeys!!.public.decodeBase64()))
            }
            mm.getPublicKey(
                name = deletedRole.name,
                elementType = RBACElementType.ROLE,
                asymKeyType = AsymKeysType.SIG
            ).apply {
                assert(this != null)
                assert(this.contentEquals(deletedRole.asymSigKeys!!.public.decodeBase64()))
            }
            mm.getPublicKey(
                token = deletedRole.token,
                elementType = RBACElementType.ROLE,
                asymKeyType = AsymKeysType.SIG
            ).apply {
                assert(this != null)
                assert(this.contentEquals(deletedRole.asymSigKeys!!.public.decodeBase64()))
            }
        }
    }

    @Test
    open fun `get version number of non-existing, incomplete, operational and deleted users, roles, and resources by name or token works`() {
        val userNonExisting = User("userNonExisting")
        val userIncomplete = addUser("userIncomplete", )
        val userOperational = addAndInitUser("userOperational", )
        val userDeleted = addUser("userDeleted")
        assert(mm.deleteUser(userDeleted.name) == CODE_000_SUCCESS)

        val roleNonExisting = Role("roleNonExisting")
        val roleOperational = addRole("roleOperational", 2)
        val roleDeleted = addRole("roleDeleted")
        assert(mm.deleteUsersRoles(roleName = roleDeleted.name) == CODE_000_SUCCESS)
        assert(mm.deleteRole(roleDeleted.name) == CODE_000_SUCCESS)

        val resourceNonExisting = Resource(
            name = "resourceNonExisting",
//            enforcement = Enforcement.COMBINED
        )
        val resourceOperational = addResource("resourceOperational", 3)
        val resourceDeleted = addResource("resourceDeleted")
        assert(mm.deleteResource(resourceDeleted.name) == CODE_000_SUCCESS)

        /** get version number of non-existing users by name or token */
        myRun {
            assert(
                mm.getVersionNumber(
                    name = userNonExisting.name,
                    elementType = RBACElementType.USER,
                ) == null
            )
            assert(
                mm.getVersionNumber(
                    token = userNonExisting.token,
                    elementType = RBACElementType.USER,
                ) == null
            )
        }

        /** get version number of incomplete users by name or token */
        myRun {
            assertNull(
                mm.getVersionNumber(
                    name = userIncomplete.name,
                    elementType = RBACElementType.USER,
                )
            )
            assertNull(
                mm.getVersionNumber(
                    token = userIncomplete.token,
                    elementType = RBACElementType.USER,
                )
            )
        }

        /** get version number of operational users by name or token */
        myRun {
            val versionNumbersByName = mm.getVersionNumber(
                name = userOperational.name,
                elementType = RBACElementType.USER
            )
            assert(versionNumbersByName != null)
//            assert(versionNumbersByName == userOperational.versionNumber)

            val versionNumbersByToken = mm.getVersionNumber(
                token = userOperational.token,
                elementType = RBACElementType.USER
            )
            assertNotNull(versionNumbersByToken)
//            assertEquals(userOperational.versionNumber, versionNumbersByToken)
        }

        /** get version number of deleted users by name or token */
        myRun {
            assert(
                mm.getVersionNumber(
                    name = userDeleted.name,
                    elementType = RBACElementType.USER,
                ) == null
            )
            assert(
                mm.getVersionNumber(
                    token = userDeleted.token,
                    elementType = RBACElementType.USER,
                ) == null
            )
        }

        /** get version number of non-existing roles by name or token */
        myRun {
            assert(
                mm.getVersionNumber(
                    name = roleNonExisting.name,
                    elementType = RBACElementType.ROLE,
                ) == null
            )
            assert(
                mm.getVersionNumber(
                    token = roleNonExisting.token,
                    elementType = RBACElementType.ROLE,
                ) == null
            )
        }

        /** get version number of operational roles by name or token */
        myRun {
            val versionNumbersByName = mm.getVersionNumber(
                name = roleOperational.name,
                elementType = RBACElementType.ROLE
            )
            assert(versionNumbersByName != null)
            assert(versionNumbersByName == roleOperational.versionNumber)

            val versionNumbersByToken = mm.getVersionNumber(
                token = roleOperational.token,
                elementType = RBACElementType.ROLE
            )
            assert(versionNumbersByToken != null)
            assert(versionNumbersByToken == roleOperational.versionNumber)
        }

        /** get version number of deleted roles by name or token */
        myRun {
            assert(
                mm.getVersionNumber(
                    name = roleDeleted.name,
                    elementType = RBACElementType.ROLE,
                ) == null
            )
            assert(
                mm.getVersionNumber(
                    token = roleDeleted.token,
                    elementType = RBACElementType.ROLE,
                ) == null
            )
        }

        /** get version number of non-existing resources by name or token */
        myRun {
            assert(
                mm.getVersionNumber(
                    name = resourceNonExisting.name,
                    elementType = RBACElementType.RESOURCE,
                ) == null
            )
            assert(
                mm.getVersionNumber(
                    token = resourceNonExisting.token,
                    elementType = RBACElementType.RESOURCE,
                ) == null
            )
        }

        /** get version number of operational resources by name or token */
        myRun {
            val versionNumbersByName = mm.getVersionNumber(
                name = resourceOperational.name,
                elementType = RBACElementType.RESOURCE
            )
            assert(versionNumbersByName != null)
            assert(versionNumbersByName == resourceOperational.versionNumber)

            val versionNumbersByToken = mm.getVersionNumber(
                token = resourceOperational.token,
                elementType = RBACElementType.RESOURCE
            )
            assert(versionNumbersByToken != null)
            assert(versionNumbersByToken == resourceOperational.versionNumber)
        }

        /** get version number of deleted resources by name or token */
        myRun {
            assert(
                mm.getVersionNumber(
                    name = resourceDeleted.name,
                    elementType = RBACElementType.RESOURCE,
                ) == null
            )
            assert(
                mm.getVersionNumber(
                    token = resourceDeleted.token,
                    elementType = RBACElementType.RESOURCE,
                ) == null
            )
        }
    }

    @Test
    open fun `get token of non-existing, incomplete, operational and deleted users, roles, and resources by name works`() {
        val userNonExisting = User("userNonExisting")
        val userIncomplete = addUser("userIncomplete", )
        val userOperational = addAndInitUser("userOperational", )
        val userDeleted = addUser("userDeleted")
        assert(mm.deleteUser(userDeleted.name) == CODE_000_SUCCESS)

        val roleNonExisting = Role("roleNonExisting")
        val roleOperational = addRole("roleOperational", 2)
        val roleDeleted = addRole("roleDeleted")
        assert(mm.deleteUsersRoles(roleName = roleDeleted.name) == CODE_000_SUCCESS)
        assert(mm.deleteRole(roleDeleted.name) == CODE_000_SUCCESS)

        val resourceNonExisting = Resource(
            name = "resourceNonExisting",
//            enforcement = Enforcement.COMBINED
        )
        val resourceOperational = addResource("resourceOperational", 3)
        val resourceDeleted = addResource("resourceDeleted")
        assert(mm.deleteResource(resourceDeleted.name) == CODE_000_SUCCESS)

        /** get token of non-existing users by name */
        myRun {
            assert(
                mm.getToken(
                    name = userNonExisting.name,
                    type = RBACElementType.USER,
                ) == null
            )
        }
        /** get token of incomplete users by name */
        myRun {
            val operationalUserToken = mm.getToken(
                name = userIncomplete.name,
                type = RBACElementType.USER
            )
            assert(operationalUserToken != null)
            assert(operationalUserToken == userIncomplete.token)
        }

        /** get token of operational users by name */
        myRun {
            val operationalUserToken = mm.getToken(
                name = userOperational.name,
                type = RBACElementType.USER
            )
            assertNotNull(operationalUserToken)
            assertEquals(userOperational.token, operationalUserToken)
        }

        /** get token of deleted users by name */
        myRun {
            val deletedUserToken = mm.getToken(
                name = userDeleted.name,
                type = RBACElementType.USER,
            )
            assert(deletedUserToken != null)
            assert(deletedUserToken == userDeleted.token)
        }

        /** get token of non-existing roles by name */
        myRun {
            assert(
                mm.getToken(
                    name = roleNonExisting.name,
                    type = RBACElementType.ROLE,
                ) == null
            )
        }

        /** get token of operational roles by name */
        myRun {
            val operationalRoleToken = mm.getToken(
                name = roleOperational.name,
                type = RBACElementType.ROLE
            )
            assert(operationalRoleToken != null)
            assert(operationalRoleToken == roleOperational.token)
        }

        /** get token of deleted roles by name */
        myRun {
            val deletedRoleToken = mm.getToken(
                name = roleDeleted.name,
                type = RBACElementType.ROLE,
            )
            assert(deletedRoleToken != null)
            assert(deletedRoleToken == roleDeleted.token)
        }

        /** get token of non-existing resources by name */
        myRun {
            assert(
                mm.getToken(
                    name = resourceNonExisting.name,
                    type = RBACElementType.RESOURCE,
                ) == null
            )
        }

        /** get token of operational resources by name */
        myRun {
            val operationalResourceToken = mm.getToken(
                name = resourceOperational.name,
                type = RBACElementType.RESOURCE
            )
            assert(operationalResourceToken != null)
            assert(operationalResourceToken == resourceOperational.token)
        }

        /** get token of deleted resources by name */
        myRun {
            val deletedResourceToken = mm.getToken(
                name = resourceDeleted.name,
                type = RBACElementType.RESOURCE
            )
            assert(deletedResourceToken != null)
            assert(deletedResourceToken == resourceDeleted.token)
        }
    }

    @Test
    open fun `get status of non-existing, incomplete, operational and deleted users, roles, and resources by name or token works`()  {

        /** get status of admin user by name or token */
        myRun {
            val statusByName = mm.getStatus(
                name = ADMIN,
                type = RBACElementType.USER
            )
            assertEquals(TupleStatus.OPERATIONAL, statusByName)
            val statusByToken = mm.getStatus(
                token = ADMIN,
                type = RBACElementType.USER
            )
            assertEquals(TupleStatus.OPERATIONAL, statusByToken)
        }

        /** get status of admin role by name or token */
        myRun {
            val statusByName = mm.getStatus(
                name = ADMIN,
                type = RBACElementType.ROLE
            )
            assert(statusByName == TupleStatus.OPERATIONAL)
            val statusByToken = mm.getStatus(
                token = ADMIN,
                type = RBACElementType.ROLE
            )
            assert(statusByToken == TupleStatus.OPERATIONAL)
        }

        /** get status of non-existing user by name or token */
        myRun {
            val statusByName = mm.getStatus(
                name = "not-existing",
                type = RBACElementType.USER
            )
            assert(statusByName == null)
            val statusByToken = mm.getStatus(
                token = "not-existing",
                type = RBACElementType.USER
            )
            assert(statusByToken == null)
        }

        /**
         * get status of existing but incomplete user by name
         * (not by token, as incomplete users still lack a token)
         */
        myRun {
            assert(mm.addUser(aliceUser).code == CODE_000_SUCCESS)
            val aliceName = aliceUser.name
            val statusByName = mm.getStatus(
                name = aliceName,
                type = RBACElementType.USER
            )
            assert(statusByName == TupleStatus.INCOMPLETE)
        }

        /** get status of operational user by name or token */
        myRun {
            addAndInitUser(bobUser)
            val bobName = bobUser.name
            val bobToken = bobUser.token
            val statusByName = mm.getStatus(
                name = bobName,
                type = RBACElementType.USER
            )
            assert(statusByName == TupleStatus.OPERATIONAL)
            val statusByToken = mm.getStatus(
                token = bobToken,
                type = RBACElementType.USER
            )
            assert(statusByToken == TupleStatus.OPERATIONAL)
        }

        /** get status of deleted user by name or token */
        myRun {
            addAndInitUser(carlUser)
            val carlName = carlUser.name
            val carlToken = carlUser.token
            assert(mm.deleteUser(carlName) == CODE_000_SUCCESS)
            val statusByName = mm.getStatus(
                name = carlName,
                type = RBACElementType.USER
            )
            assert(statusByName == TupleStatus.DELETED)
            val statusByToken = mm.getStatus(
                token = carlToken,
                type = RBACElementType.USER
            )
            assert(statusByToken == TupleStatus.DELETED)
        }

        /** get status of non-existing role by name or token */
        myRun {
            val statusByName = mm.getStatus(
                name = "not-existing",
                type = RBACElementType.ROLE
            )
            assert(statusByName == null)
            val statusByToken = mm.getStatus(
                token = "not-existing",
                type = RBACElementType.ROLE
            )
            assert(statusByToken == null)
        }

        /** get status of operational role by name or token */
        myRun {
            val roleOperational = addRole("roleOperational")
            val roleName = roleOperational.name
            val roleToken = roleOperational.token
            val statusByName = mm.getStatus(
                name = roleName,
                type = RBACElementType.ROLE
            )
            assert(statusByName == TupleStatus.OPERATIONAL)
            val statusByToken = mm.getStatus(
                token = roleToken,
                type = RBACElementType.ROLE
            )
            assert(statusByToken == TupleStatus.OPERATIONAL)
        }

        /** get status of deleted role by name or token */
        myRun {
            val roleDeleted = addRole("roleDeleted")
            assert(mm.deleteUsersRoles(roleName = roleDeleted.name) == CODE_000_SUCCESS)
            assert(mm.deleteRole(roleDeleted.name) == CODE_000_SUCCESS)
            val roleName = roleDeleted.name
            val roleToken = roleDeleted.token
            val statusByName = mm.getStatus(
                name = roleName,
                type = RBACElementType.ROLE
            )
            assert(statusByName == TupleStatus.DELETED)
            val statusByToken = mm.getStatus(
                token = roleToken,
                type = RBACElementType.ROLE
            )
            assert(statusByToken == TupleStatus.DELETED)
        }

        /** get status of non-existing resource by name or token */
        myRun {
            val statusByName = mm.getStatus(
                name = "not-existing",
                type = RBACElementType.RESOURCE
            )
            assert(statusByName == null)
            val statusByToken = mm.getStatus(
                token = "not-existing",
                type = RBACElementType.RESOURCE
            )
            assert(statusByToken == null)
        }

        /** get status of operational resource by name or token */
        myRun {
            val resourceOperational = addResource("resourceOperational")
            val resourceName = resourceOperational.name
            val resourceToken = resourceOperational.token
            val statusByName = mm.getStatus(
                name = resourceName,
                type = RBACElementType.RESOURCE
            )
            assert(statusByName == TupleStatus.OPERATIONAL)
            val statusByToken = mm.getStatus(
                token = resourceToken,
                type = RBACElementType.RESOURCE
            )
            assert(statusByToken == TupleStatus.OPERATIONAL)
        }

        /** get status of deleted resource by name or token */
        myRun {
            val resourceDeleted = addResource("resourceDeleted")
            assert(mm.deleteResource(resourceDeleted.name) == CODE_000_SUCCESS)
            val resourceName = resourceDeleted.name
            val resourceToken = resourceDeleted.token
            val statusByName = mm.getStatus(
                name = resourceName,
                type = RBACElementType.RESOURCE
            )
            assert(statusByName == TupleStatus.DELETED)
            val statusByToken = mm.getStatus(
                token = resourceToken,
                type = RBACElementType.RESOURCE
            )
            assert(statusByToken == TupleStatus.DELETED)
        }
    }

    @Test
    open fun `delete operational role by name works`() {
        val operational = addRole("operational")

        /** delete operational roles */
        myRun {
            assert(mm.deleteUsersRoles(roleName = operational.name) == CODE_000_SUCCESS)
            assert(mm.deleteRole(operational.name) == CODE_000_SUCCESS)
            val deleteRoles = mm.getRoles(
                roleName = operational.name,
                status = TupleStatus.DELETED
            )
            assert(deleteRoles.size == 1)
            assert(deleteRoles.firstOrNull()!!.name == operational.name)
        }
    }

    @Test
    open fun `delete non-existing and deleted roles by name or blank name fails`() {
        val nonExisting = Role("nonExisting")
        val deleted = addRole("operational")
        assert(mm.deleteUsersRoles(roleName = deleted.name) == CODE_000_SUCCESS)
        assert(mm.deleteRole(deleted.name) == CODE_000_SUCCESS)

        /** delete non-existing roles */
        myRun {
            assert(mm.deleteRole(nonExisting.name) == CODE_005_ROLE_NOT_FOUND)
        }

        /** delete deleted roles */
        myRun {
            assertEquals(CODE_014_ROLE_WAS_DELETED, mm.deleteRole(deleted.name))
        }

        /** delete roles by blank name */
        myRun {
            assert(mm.deleteRole("") == CODE_020_INVALID_PARAMETER)
        }
    }

    @Test
    open fun `delete the admin role by name fails`() {
        /** delete the admin role */
        myRun {
            assert(mm.deleteRole(ADMIN) == CODE_022_ADMIN_CANNOT_BE_MODIFIED)
        }
    }

    @Test
    open fun `delete operational resources by name works`() {
        val operational = addResource("operational")

        /** delete operational resources */
        myRun {
            assert(mm.deleteResource(operational.name) == CODE_000_SUCCESS)
            val deleteResources = mm.getResources(
                resourceName = operational.name,
                status = TupleStatus.DELETED
            )
            assert(deleteResources.size == 1)
            assert(deleteResources.firstOrNull()!!.name == operational.name)
        }
    }

    @Test
    open fun `delete non-existing and deleted resources by name or blank name fails`() {
        val nonExisting = Resource(
            name = "nonExisting",
//            enforcement = Enforcement.COMBINED
        )
        val deleted = addResource("operational")
        assert(mm.deleteResource(deleted.name) == CODE_000_SUCCESS)

        /** delete non-existing resources */
        myRun {
            assert(mm.deleteResource(nonExisting.name) == CODE_006_RESOURCE_NOT_FOUND)
        }

        /** delete deleted resources */
        myRun {
            assertEquals(CODE_015_RESOURCE_WAS_DELETED, mm.deleteResource(deleted.name))
        }

        /** delete resources by blank name */
        myRun {
            assert(mm.deleteResource(
                resourceName = " "
            ) == CODE_020_INVALID_PARAMETER)
        }
    }

    @Test
    open fun `delete existing user-role assignments by role name works`() {
        addAndInitUser(aliceUser)
        addAndInitUser(bobUser)
        val student = addRole("student")
        addUserRole(aliceUser.name, student)
        addUserRole(bobUser.name, student)

        /** delete existing user-role assignments by role name */
        myRun {
            assert(mm.getUsersRoles(roleName = student.name).size == 3)
            assert(mm.deleteUsersRoles(roleName = student.name) == CODE_000_SUCCESS)
            assert(mm.getUsersRoles(roleName = student.name).size == 0)
        }
    }

    @Test
    open fun `delete non-existing or the admin's user-role assignments by role name fails`() {

        /** delete non-existing user-role assignments */
        myRun {
            assertEquals(CODE_005_ROLE_NOT_FOUND, mm.deleteUsersRoles(roleName = "non-existing"))
        }

        /** delete the admin's user-role assignments */
        myRun {
            assertEquals(CODE_022_ADMIN_CANNOT_BE_MODIFIED, mm.deleteUsersRoles(roleName = ADMIN))
        }
    }

    @Test
    open fun `delete existing role-permission assignments by role or resource name works`() {
        val student = addRole("student", 1)
        val director = addRole("director", 1)
        val exam = addResource("exam")
        addRolePermission(student, exam)
        addRolePermission(director, exam)

        /** delete existing role-permission assignments by role name */
        myRun {
            assert(mm.getRolesPermissions(resourceName = exam.name).size == 2)
            assert(mm.deleteRolesPermissions(roleName = student.name) == CODE_000_SUCCESS)
        }
        assert(mm.getRolesPermissions(resourceName = exam.name).size == 1)

        /** delete existing role-permission assignments by resource name */
        myRun {
            assert(mm.deleteRolesPermissions(resourceName = exam.name) == CODE_000_SUCCESS)
        }
        assert(mm.getRolesPermissions(resourceName = exam.name).size == 0)
    }

    @Test
    open fun `delete non-existing or the admin's role-permission assignments by role or resource name fails`() {

        /** delete non-existing role-permission assignments by role or resource name */
        myRun {
            assertEquals(CODE_005_ROLE_NOT_FOUND, mm.deleteRolesPermissions(roleName = "non-existing"))
            assertEquals(CODE_006_RESOURCE_NOT_FOUND, mm.deleteRolesPermissions(resourceName = "non-existing"))
        }

        /** delete the admin's user-role assignments */
        val exam = addResource("exam")
        addRolePermission(Parameters.adminRole, exam)
        myRun {
            assertEquals(CODE_022_ADMIN_CANNOT_BE_MODIFIED, mm.deleteRolesPermissions(roleName = ADMIN))
        }
    }

    @Test
    open fun `update symmetric encryption key version number and token for operational resource works`() {
        val exam = addResource("exam", 1)
        val examToken = exam.token

        /** update symmetric encryption key version number and token for operational resource */
        myRun {
            assert(
                mm.getVersionNumber(name = exam.name, elementType = RBACElementType.RESOURCE) == 1
            )
            assert(
                mm.getToken(name = exam.name, type = RBACElementType.RESOURCE) == examToken
            )

            val newExamToken = Element.generateRandomToken()
            assert(mm.updateResourceTokenAndVersionNumber(
                resourceName = exam.name,
                oldResourceToken = examToken,
                newResourceToken = newExamToken,
                newVersionNumber = 2
            ) == CODE_000_SUCCESS)

            /**
             * We should also update the resource's token in
             * the resource's role-permission assignments(but here it
             * would be useless)
             */

            assert(
                mm.getVersionNumber(name = exam.name, elementType = RBACElementType.RESOURCE) == 2
            )
            assert(
                mm.getToken(name = exam.name, type = RBACElementType.RESOURCE) == newExamToken
            )
        }
    }

    @Test
    open fun `update symmetric encryption key version number and token for non-existing or deleted resource fails`() {
        val exam = addResource("exam", 1)
        val examToken = exam.token
        assert(mm.deleteResource(exam.name) == CODE_000_SUCCESS)

        /** update symmetric encryption key version number and token for non-existing resource */
        myRun {
            assert(mm.updateResourceTokenAndVersionNumber(
                resourceName = "non-existing",
                oldResourceToken = "non-existing-token",
                newResourceToken = "newExamToken",
                newVersionNumber = 2
            ) == CODE_006_RESOURCE_NOT_FOUND)

            /**
             * We should also update the resource's token in
             * the resource's role-permission assignments(but here it
             * would be useless)
             */
        }

        /** update symmetric encryption key version number and token for deleted resource */
        myRun {
            assert(mm.updateResourceTokenAndVersionNumber(
                resourceName = exam.name,
                oldResourceToken = examToken,
                newResourceToken = "newExamToken",
                newVersionNumber = 2
            ) == CODE_015_RESOURCE_WAS_DELETED)

            /**
             * We should also update the resource's token in
             * the resource's role-permission assignments(but here it
             * would be useless)
             */
        }
    }

    @Test
    open fun `update public keys and token and version number of operational roles by name works`() {
        val roleOperational = addRole("roleOperational")
        val roleToken = roleOperational.token

        /** update public keys and token and version number of operational roles by name */
        myRun {
            val asymEncKeysBytesByName = mm.getPublicKey(
                name = roleOperational.name, elementType = RBACElementType.ROLE, asymKeyType = AsymKeysType.ENC
            )
            assert(asymEncKeysBytesByName != null)
            assert(asymEncKeysBytesByName.contentEquals(roleOperational.asymEncKeys!!.public.decodeBase64()))

            val asymSigKeysBytesByName = mm.getPublicKey(
                name = roleOperational.name, elementType = RBACElementType.ROLE, asymKeyType = AsymKeysType.SIG
            )
            assert(asymSigKeysBytesByName != null)
            assert(asymSigKeysBytesByName.contentEquals(roleOperational.asymSigKeys!!.public.decodeBase64()))

            val newRoleToken = Element.generateRandomToken()
            val newAsymEncKeys = Parameters.cryptoPKEObject.generateAsymEncKeys()
            val newAsymSigKeys = Parameters.cryptoPKEObject.generateAsymSigKeys()
            assert(
                mm.updateRoleTokenAndVersionNumberAndAsymKeys(
                    roleName = roleOperational.name,
                    oldRoleVersionNumber = 1,
                    oldRoleToken = roleToken,
                    newRoleToken = newRoleToken,
                    newAsymEncPublicKey = newAsymEncKeys.public,
                    newAsymSigPublicKey = newAsymSigKeys.public
                ) == CODE_000_SUCCESS)

            /**
             * We should also update the role's token in
             * the role's role-permission assignments(but here it
             * would be useless)
             */

            val newAsymEncKeysBytesByName = mm.getPublicKey(
                name = roleOperational.name,
                elementType = RBACElementType.ROLE,
                asymKeyType = AsymKeysType.ENC
            )
            assert(newAsymEncKeysBytesByName != null)
            assert(newAsymEncKeysBytesByName.contentEquals(newAsymEncKeys.public.encoded))

            val newAsymSigKeysBytesByName = mm.getPublicKey(
                name = roleOperational.name,
                elementType = RBACElementType.ROLE,
                asymKeyType = AsymKeysType.SIG
            )
            assert(newAsymSigKeysBytesByName != null)
            assert(newAsymSigKeysBytesByName.contentEquals(newAsymSigKeys.public.encoded))

            assert(
                mm.getToken(
                    name = roleOperational.name,
                    type = RBACElementType.ROLE
                ) == newRoleToken
            )
        }
    }

    @Test
    open fun `update public keys and token and version number of deleted roles by name fails`() {
        val roleDeleted = addRole("roleDeleted")
        val roleDeletedToken = roleDeleted.token
        assert(mm.deleteUsersRoles(roleName = roleDeleted.name) == CODE_000_SUCCESS)
        assert(mm.deleteRole(roleDeleted.name) == CODE_000_SUCCESS)

        /** update public keys and token and version number of deleted roles by name */
        myRun {
            val newRoleToken = Element.generateRandomToken()
            val newAsymEncKeys = Parameters.cryptoPKEObject.generateAsymEncKeys()
            val newAsymSigKeys = Parameters.cryptoPKEObject.generateAsymSigKeys()

            assert(
                mm.updateRoleTokenAndVersionNumberAndAsymKeys(
                    roleName = roleDeleted.name,
                    oldRoleVersionNumber = 2,
                    oldRoleToken = roleDeletedToken,
                    newRoleToken = newRoleToken,
                    newAsymEncPublicKey = newAsymEncKeys.public,
                    newAsymSigPublicKey = newAsymSigKeys.public,
                ) == CODE_014_ROLE_WAS_DELETED
            )

            /**
             * We should also update the role's token in
             * the role's role-permission assignments(but here it
             * would be useless)
             */
        }
    }

    @Test
    open fun `update existing role-permission assignment works`() {
        val student = addRole("student")
        val exam = addResource("exam")
        val studentExamRolePermission = addRolePermission(student, exam, Operation.READ)

        /** update existing role-permission assignment */
        myRun {
            val getBefore = mm.getRolesPermissions(roleName = student.name, resourceName = exam.name)
            assert(getBefore.filter { it.roleName == student.name }.size == 1)
            assert(getBefore.firstOrNull { it.roleName == student.name }!!.operation == Operation.READ)

            val newRolePermission = RolePermission(
                roleName = student.name,
                resourceName = exam.name,
                roleToken = student.token,
                resourceToken = exam.token,
                operation = Operation.READWRITE,
                roleVersionNumber = student.versionNumber,
                resourceVersionNumber = exam.versionNumber,
                encryptedSymKey = studentExamRolePermission.encryptedSymKey,
            )
            val signature = Parameters.cryptoPKEObject.createSignature(
                bytes = newRolePermission.getBytesForSignature(),
                signingKey = Parameters.adminAsymSigKeys.private
            )
            newRolePermission.updateSignature(
                newSignature = signature,
                newSigner = ADMIN
            )
            assert(mm.updateRolePermission(newRolePermission) == CODE_000_SUCCESS)

            val getAfter = mm.getRolesPermissions(roleName = student.name, resourceName = exam.name)
            assert(getAfter.filter { it.roleName == student.name }.size == 1)
            assert(getAfter.firstOrNull { it.roleName == student.name }!!.operation == Operation.READWRITE)
        }
    }

    @Test
    open fun `update non-existing role-permission assignment fails`() {
        /** update non-existing role-permission assignment */
        myRun {
            val roleNonExisting = TestUtilities.createRole("non-existing-role")
            val resourceNonExisting = TestUtilities.createResource("non-existing-resource", enforcement = Enforcement.COMBINED)
            val nonExistingRolePermission =
                TestUtilities.createRolePermission(roleNonExisting, resourceNonExisting)
            assertEquals(CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND, mm.updateRolePermission(nonExistingRolePermission))
        }
    }

    // TODO test:
    //  - updateRoleTokenAndVersionNumberAndAsymKeys
    //  - updateResourceVersionNumber
    //  - updateResourceTokenAndVersionNumber
    // TODO test paginazione (limit e offset)



    private fun addRole(
        roleName: String,
        roleVersionNumber: Int = 1
    ): Role {
        val newRole = createRole(roleName, roleVersionNumber)
        val newUserRole = TestUtilities.createUserRole(ADMIN, newRole)
        assert(mm.addRole(newRole) == CODE_000_SUCCESS)
        assert(mm.addUsersRoles(hashSetOf(newUserRole)) == CODE_000_SUCCESS)
        assertUnlockAndLock(mm)
        return newRole
    }

    private fun addResource(
        resourceName: String,
        symKeyVersionNumber: Int = 1
    ): Resource {
        val newResource = TestUtilities.createResource(resourceName, symKeyVersionNumber, enforcement = Enforcement.COMBINED)
        assert(mm.addResource(newResource) == CODE_000_SUCCESS)
        assertUnlockAndLock(mm)
        return newResource
    }

    private fun addUserRole(
        username: String,
        role: Role
    ): UserRole {
        val userRole = TestUtilities.createUserRole(username, role)
        assert(mm.addUsersRoles(hashSetOf(userRole)) == CODE_000_SUCCESS)
        assertUnlockAndLock(mm)
        return userRole
    }

    private fun addRolePermission(
        role: Role,
        resource: Resource,
        operation: Operation = Operation.READ
    ): RolePermission {
        val rolePermission = TestUtilities.createRolePermission(role, resource, operation)
        assertEquals(CODE_000_SUCCESS, mm.addRolesPermissions(hashSetOf(rolePermission)))
        assertUnlockAndLock(mm)
        return rolePermission
    }
}
