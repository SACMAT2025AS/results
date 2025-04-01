package cryptoac.rm

import cryptoac.Constants
import cryptoac.OutcomeCode
import cryptoac.TestUtilities
import cryptoac.inputStream
import cryptoac.tuple.Operation
import cryptoac.tuple.Enforcement
import cryptoac.tuple.Resource
import cryptoac.rm.model.AddResourceRBACRequest
import cryptoac.rm.model.WriteResourceRBACRequest
import org.junit.jupiter.api.Test

internal abstract class RMServiceRBACTest : RMServiceTest() {

    abstract val rmRBAC: RMServiceRBAC


    @Test
    open fun check_add_resource_once_works() {
        /** check add resource once */
        run {
            val newResource = Resource(name = "exam")
            assert(dm!!.addResource(
                newResource = newResource,
                resourceContent = "exam content".inputStream()
            ) == OutcomeCode.CODE_000_SUCCESS)
            val addResourceRequest = createAddResourceRequest(newResource.name, Constants.ADMIN)
            assert(
                rmRBAC.checkAddResource(
                    newResource = addResourceRequest.resource,
                    adminRolePermission = addResourceRequest.rolePermission
                ) == OutcomeCode.CODE_000_SUCCESS)
        }
    }

    @Test
    open fun check_add_resource_twice_non_existing_or_deleted_resource_fail() {
        val newResource = Resource(name = "exam")
        val addResourceRequest = createAddResourceRequest(newResource.name, Constants.ADMIN)

        /** check add resource twice */
        run {
            assert(dm!!.addResource(
                newResource = newResource,
                resourceContent = "exam content".inputStream()
            ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                rmRBAC.checkAddResource(
                    newResource = addResourceRequest.resource,
                    adminRolePermission = addResourceRequest.rolePermission
                ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                rmRBAC.checkAddResource(
                    newResource = addResourceRequest.resource,
                    adminRolePermission = addResourceRequest.rolePermission
                ) == OutcomeCode.CODE_003_RESOURCE_ALREADY_EXISTS
            )
        }

        /** check non-existing resource */
        run {
            val nonExistingResourceRequest = createAddResourceRequest(
                resourceName = "non-existing",
                roleName = Constants.ADMIN
            )
            assert(
                rmRBAC.checkAddResource(
                    newResource = nonExistingResourceRequest.resource,
                    adminRolePermission = nonExistingResourceRequest.rolePermission
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** check deleted resource */
        run {
            assert(dm!!.deleteResource(
                resourceName = newResource.name,
//                resourceVersionNumber = 1
            ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                rmRBAC.checkAddResource(
                    newResource = addResourceRequest.resource,
                    adminRolePermission = addResourceRequest.rolePermission
                ) == OutcomeCode.CODE_003_RESOURCE_ALREADY_EXISTS
            )
        }
    }

    @Test
    open fun check_write_resource_once_works() {
        val newResource = Resource(
            name = "exam",
//            enforcement = Enforcement.COMBINED
        )
        val addResourceRequest = createAddResourceRequest(newResource.name, Constants.ADMIN)
        assert(dm!!.addResource(
            newResource = newResource,
            resourceContent = "exam content".inputStream()
        ) == OutcomeCode.CODE_000_SUCCESS)
        assert(
            rmRBAC.checkAddResource(
                newResource = addResourceRequest.resource,
                adminRolePermission = addResourceRequest.rolePermission
            ) == OutcomeCode.CODE_000_SUCCESS
        )

        /** check write resource once */
        run {
            val updatedResource = Resource(name = "exam")
            assert(dm!!.addResource(
                newResource = updatedResource,
                resourceContent = "updated exam content".inputStream()
            ) == OutcomeCode.CODE_000_SUCCESS)
            val writeResourceRequest = createWriteResourceRequest(
                resourceName = newResource.name,
                resourceToken = addResourceRequest.resource.token,
                symKeyVersionNumber = 1
            )
            assert(
                rmRBAC.checkWriteResource(
                    Constants.ADMIN,
                    writeResourceRequest.resource.versionNumber,
                    writeResourceRequest.resource,
                ) == OutcomeCode.CODE_000_SUCCESS)
        }
    }

    @Test
    open fun check_write_resource_twice_non_existing_or_deleted_resource_fail() {
        val newResource = Resource(name = "exam", )
        val addResourceRequest = createAddResourceRequest(newResource.name, Constants.ADMIN)
        assert(dm!!.addResource(
            newResource = newResource,
            resourceContent = "exam content".inputStream()
        ) == OutcomeCode.CODE_000_SUCCESS)
        assert(
            rmRBAC.checkAddResource(
                newResource = addResourceRequest.resource,
                adminRolePermission = addResourceRequest.rolePermission
            ) == OutcomeCode.CODE_000_SUCCESS
        )

        val updatedResource = Resource(
            name = "exam",
//            enforcement = Enforcement.COMBINED
        )
        assert(dm!!.addResource(
            newResource = updatedResource,
            resourceContent = "updated exam content".inputStream()
        ) == OutcomeCode.CODE_000_SUCCESS)
        val writeResourceRequest = createWriteResourceRequest(
            resourceName = newResource.name,
            resourceToken = addResourceRequest.resource.token,
            symKeyVersionNumber = 1
        )
        assert(
            rmRBAC.checkWriteResource(
                Constants.ADMIN,
                writeResourceRequest.resource.versionNumber,
                writeResourceRequest.resource,
            ) == OutcomeCode.CODE_000_SUCCESS
        )

        /** check write resource twice */
        run {
            assert(
                rmRBAC.checkWriteResource(
                    Constants.ADMIN,
                    writeResourceRequest.resource.versionNumber,
                    writeResourceRequest.resource,
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** check write non-existing resource */
        run {
            val nonExistingWriteResourceRequest = createWriteResourceRequest(
                resourceName = "non-existing",
                resourceToken = "non-existing",
                symKeyVersionNumber = 1
            )
            assert(
                rmRBAC.checkWriteResource(
                    Constants.ADMIN,
                    nonExistingWriteResourceRequest.resource.versionNumber,
                    nonExistingWriteResourceRequest.resource,
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND
            )
        }

        /** check write deleted resource */
        run {
            assert(dm!!.deleteResource(
                resourceName = newResource.name,
//                resourceVersionNumber = 1
            ) == OutcomeCode.CODE_000_SUCCESS)
            assert(
                rmRBAC.checkWriteResource(
                    Constants.ADMIN,
                    writeResourceRequest.resource.versionNumber,
                    writeResourceRequest.resource,
                ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND
            )
        }
    }



    fun createAddResourceRequest(
        resourceName: String,
        roleName: String
    ): AddResourceRBACRequest {
        val role = TestUtilities.createRole(roleName = roleName)
        val resource = TestUtilities.createResource(
            resourceName = resourceName,
            enforcement = Enforcement.COMBINED
        )
        val rolePermission = TestUtilities.createRolePermission(
            role = role,
            resource = resource,
            operation = Operation.READWRITE
        )
        return AddResourceRBACRequest(resource, rolePermission)
    }
    fun createWriteResourceRequest(
        resourceName: String,
        resourceToken: String,
        symKeyVersionNumber: Int
    ): WriteResourceRBACRequest {
        val resource = TestUtilities.createResource(
            resourceName = resourceName,
            symKeyVersionNumber = symKeyVersionNumber,
            enforcement = Enforcement.COMBINED
        )
        resource.token = resourceToken
        return WriteResourceRBACRequest(
            username = Constants.ADMIN,
            roleName = Constants.ADMIN,
            resource = resource,
        )
    }
}