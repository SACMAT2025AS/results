package cryptoac.core

import cryptoac.Constants.ADMIN
import cryptoac.OutcomeCode.*
import cryptoac.tuple.TupleStatus
import cryptoac.tuple.Enforcement
import cryptoac.tuple.Operation
import cryptoac.inputStream
import io.ktor.server.routing.*
import org.junit.jupiter.api.*
import kotlin.test.assertEquals

abstract class CoreCACRBACTest : CoreTest() {

    private val coreCACRBAC: CoreCACRBAC by lazy { core as CoreCACRBAC }

    // TODO test that, when revoking a user from a role, the tokens
    //  of the role and the involved files change. Similarly, test
    //  that, when revoking a role from a file, the token of the file
    //  changes

    @Test
    fun `add role of non-existing role works`() {
        /** add role of non-existing role */
        run {
            val student = "student"
            assert(coreCACRBAC.addRole(student) == CODE_000_SUCCESS)
            val adminRoles = coreCACRBAC.getUsersRoles(username = ADMIN)
            assert(adminRoles.code == CODE_000_SUCCESS)
            assert(adminRoles.usersRoles!!.size == 2)
            assert(adminRoles.usersRoles!!.filter { it.username == ADMIN && it.roleName == ADMIN }.size == 1)
            assert(adminRoles.usersRoles!!.filter { it.username == ADMIN && it.roleName == student }.size == 1)

            val roles = coreCACRBAC.getRoles().roles!!.filter { it.name == student }
            assert(roles.size == 1)
            assert(roles.filter { it.name == student }.size == 1)
            val studentRole = roles.first { it.name == student }
            assert(studentRole.versionNumber == 1)
            assert(studentRole.status == TupleStatus.OPERATIONAL)
        }
    }

    @Test
    fun `add role of blank, admin, operational or deleted role fails`() {
        /** add role with blank name */
        run {
            assert(coreCACRBAC.addRole("") == CODE_020_INVALID_PARAMETER)
            assert(coreCACRBAC.addRole("    ") == CODE_020_INVALID_PARAMETER)
        }

        /** add role with admin role name */
        run {
            // assert(coreCACRBAC.addRole(ADMIN) == CODE_002_ROLE_ALREADY_EXISTS)
        }

        /** add role of operational role */
        run {
            assert(coreCACRBAC.addRole("alice") == CODE_000_SUCCESS)
            assert(coreCACRBAC.addRole("alice") == CODE_002_ROLE_ALREADY_EXISTS)
        }

        /** add role of deleted role */
        run {
            val studentName = "student"
            assert(coreCACRBAC.addRole(studentName) == CODE_000_SUCCESS)
            assert(coreCACRBAC.deleteRole(studentName) == CODE_000_SUCCESS)
//             assert(coreCACRBAC.addRole(studentName) == CODE_014_ROLE_WAS_DELETED)
        }
    }

    @Test
    fun `delete role of operational role works`() {
        /** delete role operational role */
        run {
            val studentName = "student"
            assert(coreCACRBAC.addRole(studentName) == CODE_000_SUCCESS)
            assert(coreCACRBAC.deleteRole(studentName) == CODE_000_SUCCESS)
            val students = coreCACRBAC.getUsersRoles(studentName)
            assert(students.code == CODE_000_SUCCESS)
            assert(students.usersRoles!!.size == 0)
        }
    }

    @Test
    fun `delete role of blank, admin, non-existing or deleted role fails`() {
        /** delete role of blank role */
        run {
            assert(coreCACRBAC.deleteRole("") == CODE_020_INVALID_PARAMETER)
            assert(coreCACRBAC.deleteRole("   ") == CODE_020_INVALID_PARAMETER)
        }

        /** delete role of admin role */
        run {
            assert(coreCACRBAC.deleteRole(ADMIN) == CODE_022_ADMIN_CANNOT_BE_MODIFIED)
        }

        /** delete role of non-existing role */
        run {
            assert(coreCACRBAC.deleteRole("student") == CODE_005_ROLE_NOT_FOUND)
        }

        /** delete role of deleted role */
        run {
            assert(coreCACRBAC.addRole("student") == CODE_000_SUCCESS)
            assert(coreCACRBAC.deleteRole("student") == CODE_000_SUCCESS)
//            assert(coreCACRBAC.deleteRole("student") == CODE_014_ROLE_WAS_DELETED)
        }
    }

    @Test
    fun `add resource of non-existing resource works`() {
        /** add resource of non-existing resource */
        val testResource = "testResource"
        run {
            assert(
                coreCACRBAC.addResource(
                    resourceName = testResource,
                    resourceContent = testResource.inputStream(),
                    enforcement = Enforcement.COMBINED
                ) ==
                    CODE_000_SUCCESS)
            assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(testResource, testResource.inputStream()))

            val adminPermissions = coreCACRBAC.getRolesPermissions(username = ADMIN)
            assert(adminPermissions.code == CODE_000_SUCCESS)
            assert(adminPermissions.rolesPermissions!!.size == 1)
            assert(adminPermissions.rolesPermissions!!.filter { it.roleName == ADMIN && it.resourceName == testResource }.size == 1)
            val rolePermission = adminPermissions.rolesPermissions!!.first { it.roleName == ADMIN && it.resourceName == testResource }
            assert(rolePermission.operation == Operation.READWRITE)
            assert(rolePermission.resourceVersionNumber == 1)
            assert(rolePermission.roleVersionNumber == 1)

            val resources = coreCACRBAC.getResources().resources!!.filter { it.name == testResource }
            assert(resources.size == 1)
            assert(resources.filter { it.name == testResource }.size == 1)
            val resource = resources.first { it.name == testResource }
            assert(resource.versionNumber == 1)
            assert(resource.status == TupleStatus.OPERATIONAL)
        }
    }

    @Test
    fun `add resource of blank, operational or deleted resource fails`() {
        val content = "content".inputStream()

        /** add resource with blank name */
        run {
            assert(coreCACRBAC.addResource("", content, Enforcement.COMBINED) == CODE_020_INVALID_PARAMETER)
            assert(coreCACRBAC.addResource("    ", content, Enforcement.COMBINED) == CODE_020_INVALID_PARAMETER)
        }

        /** add resource of operational resource */
        run {
            assert(coreCACRBAC.addResource("alice", content, Enforcement.COMBINED) == CODE_000_SUCCESS)
            assert(coreCACRBAC.addResource("alice", content, Enforcement.COMBINED) == CODE_003_RESOURCE_ALREADY_EXISTS)
        }

        /** add resource of deleted resource */
        run {
            val exam = "exam"
            assert(coreCACRBAC.addResource(exam, content, Enforcement.COMBINED) == CODE_000_SUCCESS)
            assert(coreCACRBAC.deleteResource(exam) == CODE_000_SUCCESS)
            assert(coreCACRBAC.addResource(exam, content, Enforcement.COMBINED) == CODE_000_SUCCESS)
        }
    }

    @Test
    fun `delete resource of operational resource works`() {

        /** delete resource of operational resource */
        run {
            val exam = "exam"
            val examContent = "exam".inputStream()
            assert(coreCACRBAC.addResource(exam, examContent, Enforcement.COMBINED) == CODE_000_SUCCESS)
            assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent))
            assert(coreCACRBAC.deleteResource(exam) == CODE_000_SUCCESS)
            val exams = coreCACRBAC.getRolesPermissions(resourceName = exam)
            assert(exams.code == CODE_000_SUCCESS)
            assert(exams.rolesPermissions!!.size == 0)
//            assert(coreCACRBAC.getResources().resources!!.first { it.status == TupleStatus.DELETED }.name == exam)
        }
    }

    @Test
    fun `delete resource of blank, non-existing or deleted resource fails`() {
        /** delete resource of blank resource */
        run {
            assert(coreCACRBAC.deleteResource("") == CODE_020_INVALID_PARAMETER)
            assert(coreCACRBAC.deleteResource("   ") == CODE_020_INVALID_PARAMETER)
        }

        /** delete resource of non-existing resource */
        run {
//             assert(coreCACRBAC.deleteResource("exam") == CODE_006_RESOURCE_NOT_FOUND)
        }

        /** delete resource of deleted resource */
        run {
            val exam = "exam"
            val examContent = "exam".inputStream()
            assert(coreCACRBAC.addResource(exam, examContent, Enforcement.COMBINED) == CODE_000_SUCCESS)
            assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent))
            assert(coreCACRBAC.deleteResource(exam) == CODE_000_SUCCESS)
//             assert(coreCACRBAC.deleteResource(exam) == CODE_015_RESOURCE_WAS_DELETED)
        }
    }

    @Test
    fun `assign operational user to operational role works`() {
        val alice = "alice"
        val employee = "employee"
        addAndInitUser(coreCACRBAC, alice)
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)

        /** assign operational user to operational role */
        run {
            assert(coreCACRBAC.assignUserToRole(alice, employee) == CODE_000_SUCCESS)

            val aliceRoles = coreCACRBAC.getUsersRoles(alice)
            assert(aliceRoles.code == CODE_000_SUCCESS)
            assert(aliceRoles.usersRoles!!.size == 1)
            assert(aliceRoles.usersRoles!!.filter { it.username == alice && it.roleName == employee }.size == 1)
            val aliceUserRole = aliceRoles.usersRoles!!.first { it.username == alice && it.roleName == employee }
            assert(aliceUserRole.roleVersionNumber == 1)
        }
    }

    @Test
    fun `assign blank, non-existing, incomplete or deleted user to blank, non-existing or deleted role fails`() {
        val userNonExisting = "userNonExisting"
        val userIncomplete = "userIncomplete"
        assert(coreCACRBAC.addUser(userIncomplete).code == CODE_000_SUCCESS)
        val userOperational = "userOperational"
        addAndInitUser(coreCACRBAC, userOperational)
        val userDeleted = "userDeleted"
        assert(coreCACRBAC.addUser(userDeleted).code == CODE_000_SUCCESS)
        assert(coreCACRBAC.deleteUser(userDeleted) == CODE_000_SUCCESS)

        val roleNonExisting = "roleNonExisting"
        val roleOperational = "roleOperational"
        assert(coreCACRBAC.addRole(roleOperational) == CODE_000_SUCCESS)
        val roleDeleted = "roleDeleted"
        assert(coreCACRBAC.addRole(roleDeleted) == CODE_000_SUCCESS)
        assert(coreCACRBAC.deleteRole(roleDeleted) == CODE_000_SUCCESS)

        /** assign non-existing user to non-existing role */
//        run {
//            assert(
//                coreCACRBAC.assignUserToRole(userNonExisting, roleNonExisting) ==
//                    CODE_005_ROLE_NOT_FOUND
//            )
//        }

        /** assign non-existing user to operational role */
        run {
            assert(
                coreCACRBAC.assignUserToRole(userNonExisting, roleOperational) ==
                    CODE_004_USER_NOT_FOUND
            )
        }

        /** assign non-existing user to deleted role */
        run {
            assert(
                coreCACRBAC.assignUserToRole(userNonExisting, roleDeleted) ==
                    CODE_004_USER_NOT_FOUND
            )
        }

        /** assign incomplete user to non-existing role */
        run {
            assert(
                coreCACRBAC.assignUserToRole(userIncomplete, roleNonExisting) ==
                    CODE_053_USER_IS_INCOMPLETE
            )
        }

        /** assign incomplete user to operational role */
        run {
            assert(
                coreCACRBAC.assignUserToRole(userIncomplete, roleOperational) ==
                    CODE_053_USER_IS_INCOMPLETE
            )
        }

        /** assign incomplete user to deleted role */
        run {
            assert(
                coreCACRBAC.assignUserToRole(userIncomplete, roleDeleted) ==
                    CODE_053_USER_IS_INCOMPLETE
            )
        }

        /** assign operational user to non-existing role */
        run {
            assert(
                coreCACRBAC.assignUserToRole(userOperational, roleNonExisting) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** assign operational user to deleted role */
        run {
            assert(
                coreCACRBAC.assignUserToRole(userOperational, roleDeleted) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** assign deleted user to non-existing role */
        run {
            assert(
                coreCACRBAC.assignUserToRole(userDeleted, roleNonExisting) ==
                    CODE_004_USER_NOT_FOUND
            )
        }

        /** assign deleted user to operational role */
        run {
            assert(
                coreCACRBAC.assignUserToRole(userDeleted, roleOperational) ==
                    CODE_004_USER_NOT_FOUND
            )
        }

        /** assign deleted user to deleted role */
        run {
            assert(
                coreCACRBAC.assignUserToRole(userDeleted, roleDeleted) ==
                    CODE_004_USER_NOT_FOUND
            )
        }

        /** assign blank user to operational role */
        run {
            assert(
                coreCACRBAC.assignUserToRole("  ", roleOperational) ==
                    CODE_020_INVALID_PARAMETER
            )
        }

        /** assign operational user to blank role */
        run {
            assert(
                coreCACRBAC.assignUserToRole(userOperational, "  ") ==
                    CODE_020_INVALID_PARAMETER
            )
        }
    }

    @Test
    fun `assign operational user to admin role fails`() {
        /** assign operational user to admin role */
        run {
            val alice = "alice"
            addAndInitUser(coreCACRBAC, alice)
            assert(coreCACRBAC.assignUserToRole(alice, ADMIN) == CODE_022_ADMIN_CANNOT_BE_MODIFIED)
        }
    }

    @Test
    fun `assign operational user to operational role twice fails`() {
        val alice = "alice"
        val employee = "employee"
        addAndInitUser(coreCACRBAC, alice)
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)

        /** assign operational user to operational role twice */
        run {
            assert(coreCACRBAC.assignUserToRole(alice, employee) == CODE_000_SUCCESS)
            assert(coreCACRBAC.assignUserToRole(alice, employee) == CODE_010_USER_ROLE_ASSIGNMENT_ALREADY_EXISTS)
        }
    }

    @Test
    fun `revoke user from assigned role works`() {
        val alice = "alice"
        val employee = "employee"
        val excel = "excel"
        addAndInitUser(coreCACRBAC, alice)
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)
        assert(coreCACRBAC.assignUserToRole(alice, employee) == CODE_000_SUCCESS)
        assert(
            coreCACRBAC.addResource(
                excel,
                excel.inputStream(),
                Enforcement.COMBINED
            ) == CODE_000_SUCCESS
        )
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(excel, excel.inputStream()))
        assert(coreCACRBAC.assignPermissionToRole(employee, excel, Operation.READ) == CODE_000_SUCCESS)

        /** revoke user from assigned role */
        run {
            /** get the role and the role/role-permission assignments before the revoke operation */
            val beforeEmployeeRole = coreCACRBAC.getRoles().roles!!.first { it.name == employee }
            val beforeAdminRoles = coreCACRBAC.getUsersRoles(username = ADMIN)
            assert(beforeAdminRoles.code == CODE_000_SUCCESS)
            assert(beforeAdminRoles.usersRoles!!.size == 2)
            assert(beforeAdminRoles.usersRoles!!.filter { it.username == ADMIN && it.roleName == employee }.size == 1)
            val beforeAdminUserRole =
                beforeAdminRoles.usersRoles!!.first { it.username == ADMIN && it.roleName == employee }
            assert(beforeAdminUserRole.roleVersionNumber == 1)
            val beforeRolesPermissions = coreCACRBAC.getRolesPermissions(resourceName = excel)
            assert(beforeRolesPermissions.code == CODE_000_SUCCESS)
            assert(beforeRolesPermissions.rolesPermissions!!.size == 2)
            beforeRolesPermissions.rolesPermissions!!.filter { it.roleName == employee }.apply {
                assert(size == 1)
                assert(first().resourceVersionNumber == 1)
            }

            /** revoke operation */
            assert(coreCACRBAC.revokeUserFromRole(alice, employee) == CODE_000_SUCCESS)
            assertEquals(CODE_000_SUCCESS, coreCACRBAC.rotateRoleKeyUserRoles(employee))
            assertEquals(CODE_000_SUCCESS, coreCACRBAC.rotateRoleKeyPermissions(employee))
            assertEquals(CODE_000_SUCCESS, coreCACRBAC.rotateResourceKey(excel))
            assertEquals(CODE_000_SUCCESS, coreCACRBAC.eagerReencryption(excel))
            val aliceRoles = coreCACRBAC.getUsersRoles(alice)
            assert(aliceRoles.code == CODE_000_SUCCESS)
            assert(aliceRoles.usersRoles!!.size == 1) // admin

            /** get the role and the role/role-permission assignments after the revoke operation */
            val afterEmployeeRole = coreCACRBAC.getRoles().roles!!.first { it.name == employee }
            val afterAdminRoles = coreCACRBAC.getUsersRoles(ADMIN, status = arrayOf(TupleStatus.OPERATIONAL))
            assert(afterAdminRoles.code == CODE_000_SUCCESS)
            assert(afterAdminRoles.usersRoles!!.size == 2)
            assert(afterAdminRoles.usersRoles!!.filter { it.username == ADMIN && it.roleName == employee }.size == 1)
            val afterAdminUserRole =
                afterAdminRoles.usersRoles!!.first { it.username == ADMIN && it.roleName == employee }
            assert(afterAdminUserRole.roleVersionNumber == 2)
            val afterRolesPermissions = coreCACRBAC.getRolesPermissions(resourceName = excel, status = arrayOf(TupleStatus.OPERATIONAL))
            assert(afterRolesPermissions.code == CODE_000_SUCCESS)
//            assert(afterRolesPermissions.rolesPermissions!!.size == 2)
            afterRolesPermissions.rolesPermissions!!.filter { it.roleName == employee && it.resourceVersionNumber == 2 }.apply {
                assertEquals(1, size)
                assert(first().resourceVersionNumber == 2)
            }

            /** check the difference between the role and the role/role-permission assignments before and after the revoke operation */
//            assert(beforeEmployeeRole.versionNumber == 1)
//            assert(afterEmployeeRole.versionNumber == 2)
//            assert(!beforeAdminUserRole.encryptedAsymEncKeys!!.private.contentEquals(afterAdminUserRole.encryptedAsymEncKeys!!.private))
//            assert(!beforeAdminUserRole.encryptedAsymEncKeys!!.public.contentEquals(afterAdminUserRole.encryptedAsymEncKeys!!.public))
//            assert(!beforeAdminUserRole.encryptedAsymSigKeys!!.private.contentEquals(afterAdminUserRole.encryptedAsymSigKeys!!.private))
//            assert(!beforeAdminUserRole.encryptedAsymSigKeys!!.public.contentEquals(afterAdminUserRole.encryptedAsymSigKeys!!.public))
//            assert(
//                !beforeRolesPermissions.rolesPermissions!!.first { it.roleName == employee }.encryptedSymKey!!.key.contentEquals(
//                    afterRolesPermissions.rolesPermissions!!.first { it.roleName == employee }.encryptedSymKey!!.key
//                )
//            )
        }
    }

    @Test
    fun `revoke blank, non-existing, incomplete or deleted user from blank, non-existing or deleted role fails`() {
        val userNonExisting = "userNonExisting"
        val userIncomplete = "userIncomplete"
        assert(coreCACRBAC.addUser(userIncomplete).code == CODE_000_SUCCESS)
        val userOperational = "userOperational"
        addAndInitUser(coreCACRBAC, userOperational)
        val userDeleted = "userDeleted"
        assert(coreCACRBAC.addUser(userDeleted).code == CODE_000_SUCCESS)
        assert(coreCACRBAC.deleteUser(userDeleted) == CODE_000_SUCCESS)

        val roleNonExisting = "roleNonExisting"
        val roleOperational = "roleOperational"
        assert(coreCACRBAC.addRole(roleOperational) == CODE_000_SUCCESS)
        val roleDeleted = "roleDeleted"
        assert(coreCACRBAC.addRole(roleDeleted) == CODE_000_SUCCESS)
        assert(coreCACRBAC.deleteRole(roleDeleted) == CODE_000_SUCCESS)

        /** revoke non-existing user from non-existing role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userNonExisting, roleNonExisting) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** revoke non-existing user from operational role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userNonExisting, roleOperational) in listOf(
                    CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND,
                    CODE_004_USER_NOT_FOUND
                )
            )
        }

        /** revoke non-existing user from deleted role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userNonExisting, roleDeleted) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** revoke incomplete user from non-existing role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userIncomplete, roleNonExisting) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** revoke incomplete user from operational role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userIncomplete, roleOperational) ==
                    CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND
            )
        }

        /** revoke incomplete user from deleted role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userIncomplete, roleDeleted) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** revoke operational user from non-existing role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userOperational, roleNonExisting) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** revoke operational user from deleted role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userOperational, roleDeleted) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** revoke deleted user from non-existing role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userDeleted, roleNonExisting) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** revoke deleted user from operational role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userDeleted, roleOperational) ==
                    CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND
            )
        }

        /** revoke deleted user from deleted role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userDeleted, roleDeleted) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** revoke blank user from operational role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole("  ", roleOperational) ==
                    CODE_020_INVALID_PARAMETER
            )
        }

        /** revoke operational user from blank role */
        run {
            assert(
                coreCACRBAC.revokeUserFromRole(userOperational, "  ") ==
                    CODE_020_INVALID_PARAMETER
            )
        }
    }

    @Test
    fun `revoke admin user from admin or assigned role fails`() {
        /** revoke user from admin role */
        run {
            val alice = "alice"
            addAndInitUser(coreCACRBAC, alice)
            assert(coreCACRBAC.revokeUserFromRole(alice, ADMIN) == CODE_022_ADMIN_CANNOT_BE_MODIFIED)
        }

        /** revoke admin user from admin role */
        run {
            assert(coreCACRBAC.revokeUserFromRole(ADMIN, ADMIN) == CODE_022_ADMIN_CANNOT_BE_MODIFIED)
        }

        /** revoke admin user from assigned role */
        run {
            val student = "student"
            assert(coreCACRBAC.addRole(student) == CODE_000_SUCCESS)
            assert(coreCACRBAC.revokeUserFromRole(ADMIN, student) == CODE_022_ADMIN_CANNOT_BE_MODIFIED)
        }
    }

    @Test
    fun `revoke user to assigned role twice fails`() {
        val alice = "alice"
        val employee = "employee"
        addAndInitUser(coreCACRBAC, alice)
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)
        assert(coreCACRBAC.assignUserToRole(alice, employee) == CODE_000_SUCCESS)

        /** revoke user to assigned role twice */
        run {
            assert(coreCACRBAC.revokeUserFromRole(alice, employee) == CODE_000_SUCCESS)
            assert(coreCACRBAC.revokeUserFromRole(alice, employee) == CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND)
        }
    }

    @Test
    fun `assign permission over operational resource to operational role works`() {
        val employee = "employee"
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)
        val exam = "exam"
        val examContent = "exam content".inputStream()
        assert(coreCACRBAC.addResource(exam, examContent, Enforcement.COMBINED) == CODE_000_SUCCESS)
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent))

        /** assign read permission over operational resource to operational role */
        run {
            assert(coreCACRBAC.assignPermissionToRole(
                roleName = employee,
                resourceName = exam,
                operation = Operation.READ
            ) == CODE_000_SUCCESS)

            val employeeResources = coreCACRBAC.getRolesPermissions(roleName = employee)
            assert(employeeResources.code == CODE_000_SUCCESS)
            assert(employeeResources.rolesPermissions!!.size == 1)
            assert(employeeResources.rolesPermissions!!.filter { it.roleName == employee && it.resourceName == exam }.size == 1)
            val employeeRolePermission = employeeResources.rolesPermissions!!.first { it.roleName == employee && it.resourceName == exam }
            assert(employeeRolePermission.roleVersionNumber == 1)
            assert(employeeRolePermission.resourceVersionNumber == 1)
            assert(employeeRolePermission.operation == Operation.READ)
        }

        /** adding write permission to operational role already having read permission */
        run {
            assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READWRITE) == CODE_000_SUCCESS)

            val employeeResources = coreCACRBAC.getRolesPermissions(roleName = employee)
            assert(employeeResources.code == CODE_000_SUCCESS)
            val employeeRolePermission = employeeResources.rolesPermissions!!.first { it.roleName == employee && it.resourceName == exam }
            assert(employeeRolePermission.operation == Operation.READWRITE)
        }

        /** assign read and write permission over operational resource to operational role */
        run {
            val student = "student"
            assert(coreCACRBAC.addRole(student) == CODE_000_SUCCESS)
            assert(coreCACRBAC.assignPermissionToRole(student, exam, Operation.READWRITE) == CODE_000_SUCCESS)

            val studentResources = coreCACRBAC.getRolesPermissions(roleName = student)
            assert(studentResources.code == CODE_000_SUCCESS)
            val studentRolePermission = studentResources.rolesPermissions!!.first { it.roleName == student && it.resourceName == exam }
            assert(studentRolePermission.operation == Operation.READWRITE)
        }
    }

    @Test
    fun `assign read permission over operational resource to operational role with already read or read write permission fails`() {
        val exam = "exam"
        val examContent = "exam content".inputStream()
        assert(coreCACRBAC.addResource(exam, examContent, Enforcement.COMBINED) == CODE_000_SUCCESS)
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent))

        /** assign read permission over operational resource to operational role with already read permission */
        run {
            val employee = "employee"
            assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)
            assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READ) == CODE_000_SUCCESS)
//             assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READ) == CODE_016_INVALID_PERMISSION)
        }

        /** adding write permission to operational role already having read permission */
        run {
            val student = "student"
            assert(coreCACRBAC.addRole(student) == CODE_000_SUCCESS)
            assert(coreCACRBAC.assignPermissionToRole(student, exam, Operation.READWRITE) == CODE_000_SUCCESS)
//             assert(coreCACRBAC.assignPermissionToRole(student, exam, Operation.READ) == CODE_016_INVALID_PERMISSION)
        }
    }

    @Test
    fun `assign blank, non-existing or deleted role to blank, non-existing or deleted resource fails`() {
        val roleNonExisting = "roleNonExisting"
        val roleOperational = "roleOperational"
        assert(coreCACRBAC.addRole(roleOperational) == CODE_000_SUCCESS)
        val roleDeleted = "roleDeleted"
        assert(coreCACRBAC.addRole(roleDeleted) == CODE_000_SUCCESS)
        assert(coreCACRBAC.deleteRole(roleDeleted) == CODE_000_SUCCESS)

        val resourceNonExisting = "resourceNonExisting"
        val resourceOperational = "resourceOperational"
        assert(
            coreCACRBAC.addResource(
                resourceOperational, resourceOperational.inputStream(), Enforcement.COMBINED
            ) ==
                CODE_000_SUCCESS
        )
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(resourceOperational, resourceOperational.inputStream()))
        val resourceDeleted = "resourceDeleted"
        assert(
            coreCACRBAC.addResource(
                resourceDeleted, resourceDeleted.inputStream(), Enforcement.COMBINED
            ) ==
                CODE_000_SUCCESS
        )
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(resourceDeleted, resourceDeleted.inputStream()))
        assert(coreCACRBAC.deleteResource(resourceDeleted) == CODE_000_SUCCESS)

        /** assign non-existing role to non-existing resource */
        run {
            assertEquals(
                CODE_005_ROLE_NOT_FOUND,
                coreCACRBAC.assignPermissionToRole(roleNonExisting, resourceNonExisting, Operation.READ)
            )
        }

        /** assign non-existing role to operational resource */
        run {
            assert(
                coreCACRBAC.assignPermissionToRole(roleNonExisting, resourceOperational, Operation.READ) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** assign non-existing role to deleted resource */
        run {
            assert(
                coreCACRBAC.assignPermissionToRole(roleNonExisting, resourceDeleted, Operation.READ) ==
                        CODE_005_ROLE_NOT_FOUND
            )
        }

        /** assign operational role to non-existing resource */
        run {
            assertEquals(
                CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND,
                coreCACRBAC.assignPermissionToRole(roleOperational, resourceNonExisting, Operation.READ)
            )
        }

        /** assign operational role to deleted resource */
        run {
            assert(
                coreCACRBAC.assignPermissionToRole(roleOperational, resourceDeleted, Operation.READ) ==
                        CODE_007_USER_ROLE_ASSIGNMENT_NOT_FOUND
            )
        }

        /** assign deleted role to non-existing resource */
        run {
            assert(
                coreCACRBAC.assignPermissionToRole(roleDeleted, resourceNonExisting, Operation.READ) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** assign deleted role to operational resource */
        run {
            assert(
                coreCACRBAC.assignPermissionToRole(roleDeleted, resourceOperational, Operation.READ) ==
                    CODE_005_ROLE_NOT_FOUND
            )
        }

        /** assign deleted role to deleted resource */
        run {
            assert(
                coreCACRBAC.assignPermissionToRole(roleDeleted, resourceDeleted, Operation.READ) ==
                        CODE_005_ROLE_NOT_FOUND
            )
        }

        /** assign blank role to operational resource */
        run {
            assert(
                coreCACRBAC.assignPermissionToRole("  ", resourceOperational, Operation.READ) ==
                    CODE_020_INVALID_PARAMETER
            )
        }

        /** assign operational role to blank resource */
        run {
            assert(
                coreCACRBAC.assignPermissionToRole(roleOperational, "  ", Operation.READ) ==
                    CODE_020_INVALID_PARAMETER
            )
        }
    }

    @Test
    fun `assign operational role to operational resource twice fails`() {
        val employee = "employee"
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)
        val exam = "exam"
        val examContent = "exam content".inputStream()
        assert(coreCACRBAC.addResource(exam, examContent, Enforcement.COMBINED) == CODE_000_SUCCESS)
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent))

        /** assign operational role to operational resource twice */
        run {
            assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READ) == CODE_000_SUCCESS)
//             assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READ) == CODE_016_INVALID_PERMISSION)
        }
    }

    @Test
    fun `assign read permission to operational role with already write permission over operational resource fails`() {
        val employee = "employee"
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)
        val exam = "exam"
        val examContent = "exam content".inputStream()
        assert(coreCACRBAC.addResource(exam, examContent, Enforcement.COMBINED) == CODE_000_SUCCESS)
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent))

        /** assign operational role to operational resource twice */
        run {
            assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READWRITE) == CODE_000_SUCCESS)
            assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READ) == CODE_000_SUCCESS)
        }
    }

    @Test
    fun `revoke assigned permission from role over resource works`() {
        val employee = "employee"
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)
        val exam = "exam"
        val examContent = "exam content".inputStream()
        assert(coreCACRBAC.addResource(exam, examContent, Enforcement.COMBINED) == CODE_000_SUCCESS)
        assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READWRITE) == CODE_000_SUCCESS)

//        /** revoke write permission from role over resource */
//        run {
//            assert(coreCACRBAC.revokePermissionFromRole(employee, exam, Operation.WRITE) == CODE_000_SUCCESS)
//            val employeeResources = coreCACRBAC.getRolesPermissions(roleName = employee)
//            assert(employeeResources.code == CODE_000_SUCCESS)
//            assert(employeeResources.rolesPermissions!!.size == 1)
//            assert(employeeResources.rolesPermissions!!.filter { it.roleName == employee && it.resourceName == exam }.size == 1)
//            val employeeRolePermission = employeeResources.rolesPermissions!!.first { it.roleName == employee && it.resourceName == exam && it.status == TupleStatus.OPERATIONAL }
//            assert(employeeRolePermission.roleVersionNumber == 1)
//            assert(employeeRolePermission.resourceVersionNumber == 1)
//            assert(employeeRolePermission.operation == Operation.READ)
//        }

//        /** revoke read permission from role over resource */
//        run {
//            /** get the resource and the role-permission assignment before the revoke operation */
//            val beforeExamResource = coreCACRBAC.getResources().resources!!.first { it.name == exam }
//            val beforeAdminResources = coreCACRBAC.getRolesPermissions(roleName = ADMIN)
//            assert(beforeAdminResources.code == CODE_000_SUCCESS)
//            assert(beforeAdminResources.rolesPermissions!!.size == 1)
//            assert(beforeAdminResources.rolesPermissions!!.filter { it.roleName == ADMIN && it.resourceName == exam }.size == 1)
//            val beforeAdminRolePermission = beforeAdminResources.rolesPermissions!!.first { it.roleName == ADMIN && it.resourceName == exam }
//            assert(beforeAdminRolePermission.resourceVersionNumber == 1)
//
//            /** revoke operation */
//            assert(coreCACRBAC.revokePermissionFromRole(employee, exam, Operation.READWRITE) ==
//                    CODE_000_SUCCESS)
//            val employeeResources = coreCACRBAC.getRolesPermissions(roleName = employee)
//            assert(employeeResources.code == CODE_000_SUCCESS)
//            assert(employeeResources.rolesPermissions!!.size == 0)
//
//            val afterAdminResources = coreCACRBAC.getRolesPermissions(roleName = ADMIN)
//            /** get the resource and the role-permission assignment after the revoke operation */
//            val afterExamResource = coreCACRBAC.getResources().resources!!.first { it.name == exam }
//            assert(afterAdminResources.code == CODE_000_SUCCESS)
//            assert(afterAdminResources.rolesPermissions!!.size == 1)
//            assert(afterAdminResources.rolesPermissions!!.filter { it.roleName == ADMIN && it.resourceName == exam }.size == 1)
//            val afterAdminRolePermission = afterAdminResources.rolesPermissions!!.first { it.roleName == ADMIN && it.resourceName == exam && it.resourceVersionNumber == 2 }
//            assert(afterAdminRolePermission.resourceVersionNumber == 2)
//
//            /** check the difference between the resource and role-permission assignments before and after the revoke operation */
//            assert(beforeExamResource.versionNumber == 1)
//            assert(afterExamResource.versionNumber == 2)
//            assert(!beforeAdminRolePermission.encryptedSymKey!!.key.contentEquals(afterAdminRolePermission.encryptedSymKey!!.key))
//        }
    }

    @Test
    fun `revoke read or write permission of blank, non-existing or deleted role from blank, non-existing or deleted resource fails`() {
        val roleNonExisting = "roleNonExisting"
        val roleOperational = "roleOperational"
        assert(coreCACRBAC.addRole(roleOperational) == CODE_000_SUCCESS)
        val roleDeleted = "roleDeleted"
        assert(coreCACRBAC.addRole(roleDeleted) == CODE_000_SUCCESS)
        assert(coreCACRBAC.deleteRole(roleDeleted) == CODE_000_SUCCESS)

        val resourceNonExisting = "resourceNonExisting"
        val resourceOperational = "resourceOperational"
        assert(
            coreCACRBAC.addResource(
                resourceOperational, resourceOperational.inputStream(), Enforcement.COMBINED
            ) ==
                CODE_000_SUCCESS
        )
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(resourceOperational, resourceOperational.inputStream()))
        val resourceDeleted = "resourceDeleted"
        assert(
            coreCACRBAC.addResource(
                resourceDeleted, resourceDeleted.inputStream(), Enforcement.COMBINED
            ) ==
                CODE_000_SUCCESS
        )
//        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(resourceDeleted, resourceDeleted.inputStream()))
        assert(coreCACRBAC.deleteResource(resourceDeleted) == CODE_000_SUCCESS)

        /** revoke non-existing role from non-existing resource */
        run {
//            assert(
//                coreCACRBAC.revokePermissionFromRole(roleNonExisting, resourceNonExisting, Operation.WRITE) ==
//                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//            )
            assert(
                coreCACRBAC.revokePermissionFromRole(roleNonExisting, resourceNonExisting, Operation.READWRITE) ==
                    CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** revoke non-existing role from operational resource */
        run {
            assert(
                coreCACRBAC.revokePermissionFromRole(roleNonExisting, resourceOperational, Operation.WRITE) ==
                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
            )
            assert(
                coreCACRBAC.revokePermissionFromRole(roleNonExisting, resourceOperational, Operation.READWRITE) ==
                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
            )
        }

        /** revoke non-existing role from deleted resource */
        run {
//            assert(
//                coreCACRBAC.revokePermissionFromRole(roleNonExisting, resourceDeleted, Operation.WRITE) ==
//                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//            )
            assert(
                coreCACRBAC.revokePermissionFromRole(roleNonExisting, resourceDeleted, Operation.READWRITE) ==
                    CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** revoke operational role from non-existing resource */
        run {
//            assert(
//                coreCACRBAC.revokePermissionFromRole(roleOperational, resourceNonExisting, Operation.WRITE) ==
//                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//            )
            assert(
                coreCACRBAC.revokePermissionFromRole(roleOperational, resourceNonExisting, Operation.READWRITE) ==
                    CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** revoke operational role from deleted resource */
        run {
//            assert(
//                coreCACRBAC.revokePermissionFromRole(roleOperational, resourceDeleted, Operation.WRITE) ==
//                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//            )
            assert(
                coreCACRBAC.revokePermissionFromRole(roleOperational, resourceDeleted, Operation.READWRITE) ==
                    CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** revoke deleted role from non-existing resource */
        run {
//            assert(
//                coreCACRBAC.revokePermissionFromRole(roleDeleted, resourceNonExisting, Operation.WRITE) ==
//                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//            )
            assert(
                coreCACRBAC.revokePermissionFromRole(roleDeleted, resourceNonExisting, Operation.READWRITE) ==
                    CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** revoke deleted role from operational resource */
        run {
//            assert(
//                coreCACRBAC.revokePermissionFromRole(roleDeleted, resourceOperational, Operation.WRITE) ==
//                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//            )
            assert(
                coreCACRBAC.revokePermissionFromRole(roleDeleted, resourceOperational, Operation.READWRITE) ==
                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
            )
        }

        /** revoke deleted role from deleted resource */
        run {
//            assert(
//                coreCACRBAC.revokePermissionFromRole(roleDeleted, resourceDeleted, Operation.WRITE) ==
//                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
//            )
            assert(
                coreCACRBAC.revokePermissionFromRole(roleDeleted, resourceDeleted, Operation.READWRITE) ==
                    CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** revoke blank role from operational resource */
        run {
//            assert(
//                coreCACRBAC.revokePermissionFromRole("   ", resourceOperational, Operation.WRITE) ==
//                    CODE_020_INVALID_PARAMETER
//            )
            assert(
                coreCACRBAC.revokePermissionFromRole("   ", resourceOperational, Operation.READWRITE) ==
                    CODE_020_INVALID_PARAMETER
            )
        }

        /** revoke operational role from blank resource */
        run {
//            assert(
//                coreCACRBAC.revokePermissionFromRole(roleOperational, "   ", Operation.WRITE) ==
//                    CODE_020_INVALID_PARAMETER
//            )
            assert(
                coreCACRBAC.revokePermissionFromRole(roleOperational, "   ", Operation.READWRITE) ==
                    CODE_020_INVALID_PARAMETER
            )
        }
    }

    @Test
    fun `revoke admin role permission over assigned resource fails`() {
        val exam = "exam"
        val examContent = "exam content".inputStream()
        assert(coreCACRBAC.addResource(
            resourceName = exam,
            resourceContent = examContent,
            enforcement = Enforcement.COMBINED
        ) == CODE_000_SUCCESS)
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent))

        /** revoke admin role permission over assigned resource */
        run {
            assert(
                coreCACRBAC.revokePermissionFromRole(ADMIN, exam, Operation.WRITE) ==
                    CODE_022_ADMIN_CANNOT_BE_MODIFIED
            )
            assert(
                coreCACRBAC.revokePermissionFromRole(ADMIN, exam, Operation.READWRITE) ==
                    CODE_022_ADMIN_CANNOT_BE_MODIFIED
            )
        }
    }

    @Test
    fun `revoke assigned permission from role over resource twice fails`() {
        val exam = "exam"
        val examContent = "exam content".inputStream()
        assert(coreCACRBAC.addResource(exam, examContent, Enforcement.COMBINED) == CODE_000_SUCCESS)
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent))

        /** revoke read-write assigned permission from role over resource twice */
        run {
            val employee = "employee"
            assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)
            assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READWRITE) ==
                    CODE_000_SUCCESS)
            assert(coreCACRBAC.revokePermissionFromRole(employee, exam, Operation.READWRITE) ==
                    CODE_000_SUCCESS)
            assert(coreCACRBAC.revokePermissionFromRole(employee, exam, Operation.READWRITE) ==
                    CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND
            )
        }

        /** revoke write assigned permission from role over resource twice */
        run {
            val student = "student"
            assert(coreCACRBAC.addRole(student) == CODE_000_SUCCESS)
            assert(coreCACRBAC.assignPermissionToRole(student, exam, Operation.READWRITE) == CODE_000_SUCCESS)
            assert(
                coreCACRBAC.revokePermissionFromRole(student, exam, Operation.WRITE) ==
                    CODE_000_SUCCESS)
//            assert(
//                coreCACRBAC.revokePermissionFromRole(student, exam, Operation.WRITE) ==
//                    CODE_016_INVALID_PERMISSION
//            )
        }
    }

    @Test
    open fun `revoke assigned permission and reassign lower permission works`() {
        val alice = "alice"
        val aliceCoreCACRBAC = addAndInitUser(coreCACRBAC, alice)

        val employee = "employee"
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)

        val exam = "exam"
        val examContent = "exam content"
        assert(
            coreCACRBAC.addResource(exam, examContent.inputStream(), Enforcement.COMBINED) ==
                CODE_000_SUCCESS
        )
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent.inputStream()))

        /** revoke assigned permission and reassign lower permission */
        run {
            assert(coreCACRBAC.assignUserToRole(alice, employee) == CODE_000_SUCCESS)
            assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READWRITE) == CODE_000_SUCCESS)
            assert(coreCACRBAC.revokePermissionFromRole(employee, exam, Operation.WRITE) == CODE_000_SUCCESS)
            val downloadedResourceResult = aliceCoreCACRBAC.readResource(exam)
            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == examContent)
        }
    }

    @Test
    open fun `admin or user read resource having permission over resource works`() {
        val alice = "alice"
        val aliceCoreCACRBAC = addAndInitUser(coreCACRBAC, alice)

        val employee = "employee"
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)

        val exam = "exam"
        val examContent = "exam content"
        assert(
            coreCACRBAC.addResource(exam, examContent.inputStream(), Enforcement.COMBINED) ==
                CODE_000_SUCCESS
        )
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent.inputStream()))

        /** admin read resource having permission over resource */
        run {
            val downloadedResourceResult = coreCACRBAC.readResource(exam)
            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == examContent)
        }

        /** user read resource having permission over resource */
        run {
            assert(coreCACRBAC.assignUserToRole(alice, employee) == CODE_000_SUCCESS)
            assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READ) == CODE_000_SUCCESS)
            val downloadedResourceResult = aliceCoreCACRBAC.readResource(exam)
            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == examContent)
        }
    }

    @Test
    open fun `not assigned or revoked user read resource fails`() {
        val alice = "alice"
        val aliceCoreCACRBAC = addAndInitUser(coreCACRBAC, alice)

        val employee = "employee"
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)

        val exam = "exam"
        val examContent = "exam content"
        assert(
            coreCACRBAC.addResource(exam, examContent.inputStream(), Enforcement.COMBINED) ==
                CODE_000_SUCCESS
        )
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent.inputStream()))

        /** not assigned user read resource */
        run {
            assert(coreCACRBAC.assignUserToRole(alice, employee) == CODE_000_SUCCESS)
            assert(aliceCoreCACRBAC.readResource(exam).code == CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND)
        }

        /** revoked user read resource */
        run {
            assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READ) == CODE_000_SUCCESS)
            val downloadedResourceResult = aliceCoreCACRBAC.readResource(exam)
            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == examContent)

            assert(coreCACRBAC.revokePermissionFromRole(employee, exam, Operation.READWRITE) == CODE_000_SUCCESS)
            assertEquals(CODE_008_ROLE_PERMISSION_ASSIGNMENT_NOT_FOUND, aliceCoreCACRBAC.readResource(exam).code)
        }
    }

    @Test
    open fun `admin or user write resource having permission over resource works`() {

        val alice = "alice"
        val aliceCoreCACRBAC = addAndInitUser(coreCACRBAC, alice)
        val employee = "employee"
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)
        val exam = "exam"
        val examContent = "exam content"
        assert(
            coreCACRBAC.addResource(exam, examContent.inputStream(), Enforcement.COMBINED) ==
                CODE_000_SUCCESS
        )
        assertEquals(
            CODE_000_SUCCESS,
            coreCACRBAC.writeResource(exam, examContent.inputStream())
        )
        val readResourceResult = coreCACRBAC.readResource(exam)
        assertEquals(CODE_000_SUCCESS, readResourceResult.code)
        assertEquals(examContent, String(readResourceResult.stream!!.readAllBytes()))

        /** admin write resource having permission over resource */
        run {
            val updatedExamContent = "updated exam content by admin"
            val updateResourceCode = coreCACRBAC.writeResource(exam, updatedExamContent.inputStream())
            assert(updateResourceCode == CODE_000_SUCCESS)
            val downloadedResourceResult = coreCACRBAC.readResource(exam)
            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == updatedExamContent)
        }

        /** user write resource having permission over resource */
        run {
            assert(coreCACRBAC.assignUserToRole(alice, employee) == CODE_000_SUCCESS)
            assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READWRITE) == CODE_000_SUCCESS)
            val updatedExamContent = "updated exam content by user"
            val updateResourceCode = aliceCoreCACRBAC.writeResource(exam, updatedExamContent.inputStream())
            assertEquals(CODE_000_SUCCESS, updateResourceCode)
            val downloadedResourceResult = aliceCoreCACRBAC.readResource(exam)
            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == updatedExamContent)
        }
    }

    @Test
    open fun `not assigned or revoked user write resource fails`() {
        val alice = "alice"
        val aliceCoreCACRBAC = addAndInitUser(coreCACRBAC, alice)

        val employee = "employee"
        assert(coreCACRBAC.addRole(employee) == CODE_000_SUCCESS)

        val exam = "exam"
        val examContent = "exam content"
        assert(
            coreCACRBAC.addResource(exam, examContent.inputStream(), Enforcement.COMBINED) ==
                CODE_000_SUCCESS
        )
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource(exam, examContent.inputStream()))

        /** not assigned user write resource */
        run {
            assert(coreCACRBAC.assignUserToRole(alice, employee) == CODE_000_SUCCESS)
//            assert(aliceCoreCACRBAC.writeResource(exam, exam.inputStream()) == CODE_006_RESOURCE_NOT_FOUND)
        }

        /** revoked user write resource */
        run {
            assert(coreCACRBAC.assignPermissionToRole(employee, exam, Operation.READWRITE) == CODE_000_SUCCESS)
//            assert(aliceCoreCACRBAC.writeResource(exam, exam.inputStream()) == CODE_000_SUCCESS)
            assert(coreCACRBAC.revokePermissionFromRole(employee, exam, Operation.WRITE) == CODE_000_SUCCESS)
//            assert(aliceCoreCACRBAC.writeResource(exam, exam.inputStream()) == CODE_006_RESOURCE_NOT_FOUND)
        }
    }



    @Test
    fun `get role of operational or deleted role works`() {
        assert(coreCACRBAC.addRole("operational") == CODE_000_SUCCESS)
        assert(coreCACRBAC.addRole("deleted") == CODE_000_SUCCESS)
        assert(coreCACRBAC.deleteRole("deleted") == CODE_000_SUCCESS)

        /** get role of operational role */
        run {
            assert(coreCACRBAC.getRoles().roles!!.filter { it.name == "operational" }.size == 1)
        }

//        /** get role of deleted role */
//        run {
//            assert(coreCACRBAC.getRoles(
//                status =
//            ).roles!!.first { it.status == TupleStatus.DELETED }.name == "deleted")
//        }
    }

    @Test
    fun `get role of non-existing fails`() {

        /** get role of non-existing role */
        run {
            assert(coreCACRBAC.getRoles().roles!!.none { it.name == "not-existing" })
        }
    }

    @Test
    fun `get resource of operational or deleted resource works`() {
        assert(
            coreCACRBAC.addResource(
                "operational", "operational".inputStream(), Enforcement.COMBINED
            ) == CODE_000_SUCCESS
        )
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource("operational", "operational".inputStream()))
        assert(
            coreCACRBAC.addResource(
                "deleted", "deleted".inputStream(), Enforcement.COMBINED
            ) == CODE_000_SUCCESS
        )
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource("deleted", "deleted".inputStream()))
        assert(coreCACRBAC.deleteResource("deleted") == CODE_000_SUCCESS)

        /** get resource of operational resource */
        run {
            assert(coreCACRBAC.getResources().resources!!.first { it.status == TupleStatus.OPERATIONAL }.name == "operational")
        }

//        /** get resource of deleted resource */
//        run {
//            assert(coreCACRBAC.getResources().resources!!.first { it.status == TupleStatus.DELETED }.name == "deleted")
//        }
    }

    @Test
    fun `get resource of non-existing or deleted resource fails`() {

        /** get resource of non-existing resource */
        run {
            assert(coreCACRBAC.getResources().resources!!.none { it.name == "not-existing" })
        }
    }

    @Test
    fun `get existing assignment specifying any combination of username and role name works`() {
        addAndInitUser(coreCACRBAC, "alice")
        assert(coreCACRBAC.addRole("student") == CODE_000_SUCCESS)
        assert(coreCACRBAC.assignUserToRole("alice", "student") == CODE_000_SUCCESS)

        /** get existing assignment specifying the username */
        run {
            assert(coreCACRBAC.getUsersRoles(username = "alice").usersRoles!!.filter { it.roleName == "student" }.size == 1)
        }

        /** get existing assignment specifying the role name */
        run {
            assert(coreCACRBAC.getUsersRoles(roleName = "student").usersRoles!!.filter { it.username == "alice" }.size == 1)
        }

        /** get existing assignment specifying both the username and the role name */
        run {
            assert(coreCACRBAC.getUsersRoles(username = "alice", roleName = "student").usersRoles!!.size == 1)
        }
    }

    @Test
    fun `get non-existing or deleted assignment fails`() {

        /** get non-existing assignment */
        run {
            assert(coreCACRBAC.getUsersRoles(username = "alice").usersRoles!!.none { it.roleName == "student" })
            assert(coreCACRBAC.getUsersRoles(roleName = "student").usersRoles!!.none { it.username == "alice" })
            assert(coreCACRBAC.getUsersRoles(username = "alice", roleName = "student").usersRoles!!.isEmpty())
        }

        /** get deleted assignment */
        run {
            addAndInitUser(coreCACRBAC, "alice")
            assert(coreCACRBAC.addRole("student") == CODE_000_SUCCESS)
            assert(coreCACRBAC.assignUserToRole("alice", "student") == CODE_000_SUCCESS)
            assert(coreCACRBAC.revokeUserFromRole("alice", "student") == CODE_000_SUCCESS)

            assert(coreCACRBAC.getUsersRoles(username = "alice", status = arrayOf(TupleStatus.OPERATIONAL)).usersRoles!!.none { it.roleName == "student" })
            assert(coreCACRBAC.getUsersRoles(roleName = "student", status = arrayOf(TupleStatus.OPERATIONAL)).usersRoles!!.none { it.username == "alice" })
            assert(coreCACRBAC.getUsersRoles(username = "alice", roleName = "student", status = arrayOf(TupleStatus.OPERATIONAL)).usersRoles!!.isEmpty())
        }
    }

    @Test
    fun `get existing permission specifying any combination of username, role name and resource name works`() {
        addAndInitUser(coreCACRBAC, "alice")
        assert(coreCACRBAC.addRole("student") == CODE_000_SUCCESS)
        assert(
            coreCACRBAC.addResource(
                "exam", "exam".inputStream(), Enforcement.COMBINED
            ) == CODE_000_SUCCESS
        )
        assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource("exam", "exam".inputStream()))
        assert(coreCACRBAC.assignUserToRole("alice", "student") == CODE_000_SUCCESS)
        assert(
            coreCACRBAC.assignPermissionToRole("student", "exam", Operation.READWRITE)
                == CODE_000_SUCCESS
        )

        /** get existing permission specifying the username */
        run {
            assert(
                coreCACRBAC.getRolesPermissions(username = "alice").rolesPermissions!!.filter {
                    it.roleName == "student" && it.resourceName == "exam"
                }.size == 1
            )
        }

        /** get existing permission specifying the role name */
        run {
            assert(
                coreCACRBAC.getRolesPermissions(roleName = "student").rolesPermissions!!.filter {
                    it.resourceName == "exam"
                }.size == 1
            )
        }

        /** get existing permission specifying the resource name */
        run {
            assert(
                coreCACRBAC.getRolesPermissions(resourceName = "exam").rolesPermissions!!.filter {
                    it.roleName == "student"
                }.size == 1
            )
        }

        /** get existing assignment specifying the username, the role and the resource name */
        run {
            assert(
                coreCACRBAC.getRolesPermissions(username = "alice", roleName = "student", resourceName = "exam").rolesPermissions!!.filter {
                    it.roleName == "student"
                }.size == 1
            )
        }
    }

    @Test
    fun `get non-existing or deleted permission fails`() {

        /** get non-existing permission */
        run {
            assert(coreCACRBAC.getRolesPermissions(username = "alice").rolesPermissions!!.isEmpty())
            assert(coreCACRBAC.getRolesPermissions(roleName = "student").rolesPermissions!!.isEmpty())
            assert(coreCACRBAC.getRolesPermissions(resourceName = "exam").rolesPermissions!!.isEmpty())
            assert(coreCACRBAC.getRolesPermissions(username = "alice", roleName = "student", resourceName = "exam").rolesPermissions!!.isEmpty())
        }

        /** get deleted permission */
        run {
            addAndInitUser(coreCACRBAC, "alice")
            assert(coreCACRBAC.addRole("student") == CODE_000_SUCCESS)
            assert(
                coreCACRBAC.addResource(
                    "exam", "exam".inputStream(), Enforcement.COMBINED
                ) == CODE_000_SUCCESS)
            assertEquals(CODE_000_SUCCESS, coreCACRBAC.writeResource("exam", "exam".inputStream()))
            assert(coreCACRBAC.assignUserToRole("alice", "student") == CODE_000_SUCCESS)
            assert(
                coreCACRBAC.assignPermissionToRole("student", "exam", Operation.READWRITE)
                    == CODE_000_SUCCESS)
            assert(coreCACRBAC.revokePermissionFromRole("student", "exam", Operation.READWRITE)
                    == CODE_000_SUCCESS)

            assert(coreCACRBAC.getRolesPermissions(username = "alice", status = arrayOf(TupleStatus.OPERATIONAL)).rolesPermissions!!.isEmpty())
            assert(coreCACRBAC.getRolesPermissions(roleName = "student", status = arrayOf(TupleStatus.OPERATIONAL)).rolesPermissions!!.isEmpty())
            assert(coreCACRBAC.getRolesPermissions(resourceName = "exam", status = arrayOf(TupleStatus.OPERATIONAL)).rolesPermissions!!.none { it.roleName == "student" })
            assert(coreCACRBAC.getRolesPermissions(
                username = "alice",
                roleName = "student",
                resourceName = "exam",
                status = arrayOf(TupleStatus.OPERATIONAL)
            ).rolesPermissions!!.isEmpty())
        }
    }



    override fun addAndInitUser(core: Core, username: String): CoreCACRBAC {
        return super.addAndInitUser(core, username) as CoreCACRBAC
    }
}
