//package cryptoac.core
//
//import cryptoac.Constants.ADMIN
//import cryptoac.OutcomeCode.*
//import cryptoac.encodeBase64
//import cryptoac.inputStream
//import cryptoac.tuple.Operation
//import cryptoac.tuple.Enforcement
//import cryptoac.tuple.TupleStatus
//import org.junit.jupiter.api.*
//import kotlin.test.assertEquals
//import kotlin.test.assertNotEquals
//
//internal abstract class CoreCACABACTest : CoreTest() {
//
//    private val coreCACABAC: CoreCACABAC by lazy { core as CoreCACABAC }
//
//    // TODO test that, when revoking an attribute from a user (or when
//    //  updating the access structure of a resource), the tokens
//    //  of the attribute and the involved resources change. This should be
//    //  reflected also in the access structures and the ABE secret keys
//
//
//
//    @Test
//    fun `add attribute of non-existing or deleted attribute works`() {
//        val tall = "height"
//
//        /** add attribute of non-existing attribute */
//        run {
//            assert(coreCACABAC.addAttribute(
//                attributeName = tall,
//            ) == CODE_000_SUCCESS)
//            val adminAttributes = coreCACABAC.getAttributeAssignments(username = ADMIN)
//            assert(adminAttributes.code == CODE_000_SUCCESS)
//            assert(adminAttributes.usersAttributes!!.size == 2)
//            val adminUserAttribute = adminAttributes.usersAttributes!!.first { it.attributeName == ADMIN }
//            assert(adminUserAttribute.attributeName == ADMIN)
//            assert(adminUserAttribute.attributeValue == null)
//            val tallUserAttribute = adminAttributes.usersAttributes!!.first { it.attributeName == tall }
//            assert(tallUserAttribute.attributeName == tall)
//
//            val attributes = coreCACABAC.getAttributes().attributes!!
//            assert(attributes.size == 2)
//            assert(attributes.filter { it.name == ADMIN }.size == 1)
//            assert(attributes.filter { it.name == tall }.size == 1)
//            val adminAttribute = attributes.first { it.name == ADMIN }
//            assert(adminAttribute.versionNumber == 1)
//            assert(adminAttribute.status == TupleStatus.OPERATIONAL)
//            val tallAttribute = attributes.first { it.name == tall }
//            assert(tallAttribute.versionNumber == 1)
//            assert(tallAttribute.status == TupleStatus.OPERATIONAL)
//        }
//
//        /** add attribute of deleted attribute */
//        run {
//            assert(coreCACABAC.deleteAttributes(tall) == CODE_000_SUCCESS)
//            val deletedTallAttribute = coreCACABAC.getAttributes().attributes!!.first { it.name == tall }
//            assert(deletedTallAttribute.versionNumber == 1)
//            assert(deletedTallAttribute.status == TupleStatus.DELETED)
//            assert(coreCACABAC.addAttribute(
//                attributeName = tall,
//            ) == CODE_000_SUCCESS)
//            val tallAttribute = coreCACABAC.getAttributes().attributes!!.first { it.name == tall }
//            assert(tallAttribute.versionNumber == 2)
//            assert(tallAttribute.status == TupleStatus.OPERATIONAL)
//        }
//    }
//
//    @Test
//    fun `add attribute with blank name or operational attribute fails`() {
//        /** add attribute with blank name */
//        run {
//            assert(coreCACABAC.addAttribute("") == CODE_020_INVALID_PARAMETER)
//            assert(coreCACABAC.addAttribute("    ") == CODE_020_INVALID_PARAMETER)
//        }
//
//        /** add attribute of operational attribute */
//        run {
//            val tallName = "tall"
//            assert(coreCACABAC.addAttribute(tallName) == CODE_000_SUCCESS)
//            assert(coreCACABAC.addAttribute(tallName) == CODE_065_ATTRIBUTE_ALREADY_EXISTS)
//        }
//
//    }
//
//    @Test
//    fun `delete attribute of operational attribute works`() {
//        /** delete attribute operational attribute */
//        run {
//            val tallName = "tall"
//            assert(coreCACABAC.addAttribute(tallName) == CODE_000_SUCCESS)
//            assert(coreCACABAC.deleteAttributes(tallName) == CODE_000_SUCCESS)
//            val usersAttributes = coreCACABAC.getAttributeAssignments(attributeName = tallName)
//            assert(usersAttributes.code == CODE_000_SUCCESS)
//            assert(usersAttributes.usersAttributes!!.size == 0)
//        }
//    }
//
//    @Test
//    fun `delete attribute of blank, non-existing or deleted attribute fails`() {
//        val tallName = "tall"
//
//        /** delete attribute of blank attribute */
//        run {
//            assert(coreCACABAC.deleteAttributes(hashSetOf("", tallName)) == CODE_020_INVALID_PARAMETER)
//            assert(coreCACABAC.deleteAttributes(hashSetOf(tallName, "   ")) == CODE_020_INVALID_PARAMETER)
//        }
//
//        /** delete attribute of non-existing attribute */
//        run {
//            assert(coreCACABAC.deleteAttributes(tallName) == CODE_066_ATTRIBUTE_NOT_FOUND)
//        }
//
//        /** delete attribute of deleted attribute */
//        run {
//            assert(coreCACABAC.addAttribute(tallName) == CODE_000_SUCCESS)
//            assert(coreCACABAC.deleteAttributes(tallName) == CODE_000_SUCCESS)
//            assert(coreCACABAC.deleteAttributes(tallName) == CODE_067_ATTRIBUTE_WAS_DELETED)
//        }
//    }
//
//    @Test
//    fun `add resource of non-existing resource works`() {
//        /** add resource of non-existing resource */
//        val testResource = "testResource"
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        run {
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = testResource,
//                    resourceContent = testResource.inputStream(),
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = "Tall and From"
//                ) == CODE_000_SUCCESS)
//
//            val adminUsersAttributes = coreCACABAC.getAttributeAssignments(username = ADMIN)
//            assert(adminUsersAttributes.code == CODE_000_SUCCESS)
//            assert(adminUsersAttributes.usersAttributes!!.size == 3)
//            assert(adminUsersAttributes.usersAttributes!!.filter { it.username == ADMIN && it.attributeName == ADMIN }.size == 1)
//            assert(adminUsersAttributes.usersAttributes!!.filter { it.username == ADMIN && it.attributeName == "Tall" }.size == 1)
//            assert(adminUsersAttributes.usersAttributes!!.filter { it.username == ADMIN && it.attributeName == "From" }.size == 1)
//
//            val accessStructuresPermissions = coreCACABAC.getAccessStructures(resourceName = testResource)
//            assert(accessStructuresPermissions.code == CODE_000_SUCCESS)
//            assert(accessStructuresPermissions.accessStructuresPermissions!!.size == 1)
//            assert(accessStructuresPermissions.accessStructuresPermissions!!.filter { it.resourceName == testResource }.size == 1)
//            val accessStructurePermission = accessStructuresPermissions.accessStructuresPermissions!!.elementAt(0)
//            assert(accessStructurePermission.operation == Operation.READ)
//            assert(accessStructurePermission.resourceVersionNumber == 1)
//
//            val resources = coreCACABAC.getResources().resources!!.filter { it.name == testResource }
//            assert(resources.size == 1)
//            assert(resources.filter { it.name == testResource }.size == 1)
//            val resource = resources.first { it.name == testResource }
//            assert(resource.versionNumber == 1)
//            assert(resource.status == TupleStatus.OPERATIONAL)
//        }
//    }
//
//    @Test
//    fun `add resource of blank, operational or deleted resource fails`() {
//        val testResource = "testResource"
//        val content = testResource.inputStream()
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("Short") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//
//        /** add resource with blank name or access structure */
//        run {
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = "",
//                    resourceContent = content,
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = "Tall and From"
//                ) == CODE_020_INVALID_PARAMETER)
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = "    ",
//                    resourceContent = content,
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = "Tall and From"
//                ) == CODE_020_INVALID_PARAMETER)
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = testResource,
//                    resourceContent = content,
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = " "
//                ) == CODE_020_INVALID_PARAMETER)
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = testResource,
//                    resourceContent = content,
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = "    "
//                ) == CODE_020_INVALID_PARAMETER)
//        }
//
//        /** add resource of operational resource */
//        run {
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = testResource,
//                    resourceContent = content,
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = "Tall and From"
//                ) == CODE_000_SUCCESS)
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = testResource,
//                    resourceContent = content,
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = "Tall and From"
//                ) == CODE_003_RESOURCE_ALREADY_EXISTS)
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = testResource,
//                    resourceContent = content,
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READWRITE,
//                    accessStructure = "Tall and From"
//                ) == CODE_003_RESOURCE_ALREADY_EXISTS)
//        }
//
//        /** add resource of deleted resource */
//        run {
//            val exam = "exam"
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = exam,
//                    resourceContent = content,
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = "Tall and From"
//                ) == CODE_000_SUCCESS)
//            assert(coreCACABAC.deleteResource(exam) == CODE_000_SUCCESS)
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = exam,
//                    resourceContent = content,
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = "Tall and From"
//                ) == CODE_015_RESOURCE_WAS_DELETED)
//        }
//    }
//
//    @Test
//    fun `update resource of operational resource works`() {
//        val alice = "alice"
//        addAndInitUser(coreCACABAC, alice)
//        assert(coreCACABAC.addAttribute("tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("short") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(coreCACABAC.assignUserToAttributes(
//            username = alice,
//            attributeName = "tall",
//            attributeValue = "200cm"
//        ) == CODE_000_SUCCESS)
//
//        val testResource = "testResource"
//        assert(
//            coreCACABAC.addResource(
//                resourceName = testResource,
//                resourceContent = testResource.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        /** update resource of operational resource */
//        run {
//            val beforeAccessStructuresPermissions = coreCACABAC.getAccessStructures(
//                resourceName = testResource
//            )
//            assert(beforeAccessStructuresPermissions.code == CODE_000_SUCCESS)
//            assert(beforeAccessStructuresPermissions.accessStructuresPermissions!!.size == 1)
//            val beforeAccessStructurePermission =
//                beforeAccessStructuresPermissions.accessStructuresPermissions!!.first()
//            beforeAccessStructurePermission.accessStructure = "short and From:alice"
//
//            assert(coreCACABAC.updateResourceAndAccessStructures(
//                resourceName = testResource,
//                accessStructuresPermissionsToUpdate = hashSetOf(
//                    beforeAccessStructurePermission
//                ),
//                newAccessStructuresAlreadyEmbedVersionNumbers = false,
//            ) == CODE_000_SUCCESS)
//
//            val accessStructuresPermissions = coreCACABAC.getAccessStructures(resourceName = testResource)
//            assert(accessStructuresPermissions.code == CODE_000_SUCCESS)
//            assert(accessStructuresPermissions.accessStructuresPermissions!!.size == 1)
//            assert(accessStructuresPermissions.accessStructuresPermissions!!.filter { it.resourceName == testResource }.size == 1)
//            val accessStructurePermission = accessStructuresPermissions.accessStructuresPermissions!!.elementAt(0)
//            assert(accessStructurePermission.operation == Operation.READ)
//            assert(accessStructurePermission.resourceVersionNumber == 2)
//            assert(accessStructurePermission.accessStructure == "admin_1 or (short_1 and From_1:alice)")
//
//            val resources = coreCACABAC.getResources().resources!!.filter { it.name == testResource }
//            assert(resources.size == 1)
//            assert(resources.filter { it.name == testResource }.size == 1)
//            val resource = resources.first { it.name == testResource }
//            assert(resource.versionNumber == 2)
//            assert(resource.status == TupleStatus.OPERATIONAL)
//        }
//    }
//
//    @Test
//    fun `update resource of blank, non-existing or deleted resource fails`() {
//        val alice = "alice"
//        addAndInitUser(coreCACABAC, alice)
//        assert(coreCACABAC.addAttribute("short") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(coreCACABAC.assignUserToAttributes(
//            username = alice,
//            attributeName = "tall",
//            attributeValue = "200cm"
//        ) == CODE_000_SUCCESS)
//
//        val testResource = "testResource"
//        assert(
//            coreCACABAC.addResource(
//                resourceName = testResource,
//                resourceContent = testResource.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        val beforeAccessStructuresPermissions = coreCACABAC.getAccessStructures(
//            resourceName = testResource
//        )
//        assert(beforeAccessStructuresPermissions.code == CODE_000_SUCCESS)
//        assert(beforeAccessStructuresPermissions.accessStructuresPermissions!!.size == 1)
//        val beforeAccessStructurePermission =
//            beforeAccessStructuresPermissions.accessStructuresPermissions!!.first()
//        beforeAccessStructurePermission.accessStructure = "short and From:bob"
//
//        /** update resource of blank name or access structure */
//        run {
//            assert(
//                coreCACABAC.updateResourceAndAccessStructures(
//                    resourceName = "",
//                    accessStructuresPermissionsToUpdate = hashSetOf(
//                        beforeAccessStructurePermission
//                    ),
//                    newAccessStructuresAlreadyEmbedVersionNumbers = false,
//                ) == CODE_020_INVALID_PARAMETER)
//
//            assert(
//                coreCACABAC.updateResourceAndAccessStructures(
//                    resourceName = testResource,
//                    accessStructuresPermissionsToUpdate = hashSetOf(
//                        beforeAccessStructurePermission.apply {
//                            accessStructure = "  "
//                        }
//                    ),
//                    newAccessStructuresAlreadyEmbedVersionNumbers = false,
//                ) == CODE_020_INVALID_PARAMETER)
//        }
//
//        /** update resource of non-existing resource */
//        run {
//            assert(
//                coreCACABAC.updateResourceAndAccessStructures(
//                    resourceName = "non-existing",
//                    accessStructuresPermissionsToUpdate = hashSetOf(
//                        beforeAccessStructurePermission.apply {
//                            accessStructure = "short and From:bob"
//                        }
//                    ),
//                    newAccessStructuresAlreadyEmbedVersionNumbers = false,
//                ) == CODE_006_RESOURCE_NOT_FOUND)
//
//            assert(
//                coreCACABAC.updateResourceAndAccessStructures(
//                    resourceName = testResource,
//                    accessStructuresPermissionsToUpdate = hashSetOf(
//                        beforeAccessStructurePermission.apply {
//                            accessStructure = "  "
//                        }
//                    ),
//                    newAccessStructuresAlreadyEmbedVersionNumbers = false,
//                ) == CODE_020_INVALID_PARAMETER)
//        }
//
//        /** update resource of deleted resource */
//        run {
//            assert(coreCACABAC.deleteResource(testResource) == CODE_000_SUCCESS)
//
//            assert(
//                coreCACABAC.updateResourceAndAccessStructures(
//                    resourceName = testResource,
//                    accessStructuresPermissionsToUpdate = hashSetOf(
//                        beforeAccessStructurePermission.apply {
//                            accessStructure = "short and From:bob"
//                        }
//                    ),
//                    newAccessStructuresAlreadyEmbedVersionNumbers = false,
//                ) in listOf(
//                    CODE_015_RESOURCE_WAS_DELETED,
//                    CODE_006_RESOURCE_NOT_FOUND
//                ))
//        }
//    }
//
//    @Test
//    fun `delete resource of operational resource works`() {
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//
//        /** delete resource of operational resource */
//        run {
//            val exam = "exam"
//            val examContent = "exam".inputStream()
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = exam,
//                    resourceContent = examContent,
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = "Tall and From"
//                ) == CODE_000_SUCCESS)
//            assert(coreCACABAC.deleteResource(exam) == CODE_000_SUCCESS)
//            val examAccessStructuresPermissions = coreCACABAC.getAccessStructures(resourceName = exam)
//            assert(examAccessStructuresPermissions.code == CODE_000_SUCCESS)
//            assert(examAccessStructuresPermissions.accessStructuresPermissions!!.size == 0)
//            assert(coreCACABAC.getResources().resources!!.first {
//                it.status == TupleStatus.DELETED
//            }.name == exam)
//        }
//    }
//
//    @Test
//    fun `delete resource of blank, non-existing or deleted resource fails`() {
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//
//        /** delete resource of blank resource */
//        run {
//            assert(coreCACABAC.deleteResource("") == CODE_020_INVALID_PARAMETER)
//            assert(coreCACABAC.deleteResource("   ") == CODE_020_INVALID_PARAMETER)
//        }
//
//        /** delete resource of non-existing resource */
//        run {
//            assert(coreCACABAC.deleteResource("exam") == CODE_006_RESOURCE_NOT_FOUND)
//        }
//
//        /** delete resource of deleted resource */
//        run {
//            val exam = "exam"
//            val examContent = "exam".inputStream()
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = exam,
//                    resourceContent = examContent,
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = "Tall and From"
//                ) == CODE_000_SUCCESS)
//            assert(coreCACABAC.deleteResource(exam) == CODE_000_SUCCESS)
//            assert(coreCACABAC.deleteResource(exam) == CODE_015_RESOURCE_WAS_DELETED)
//        }
//    }
//
//    @Test
//    fun `assign operational user to operational attribute works`() {
//        val alice = "alice"
//        val tall = "tall"
//        addAndInitUser(coreCACABAC, alice)
//        assert(coreCACABAC.addAttribute(tall) == CODE_000_SUCCESS)
//
//        /** assign operational user to operational attribute */
//        run {
//            assert(coreCACABAC.assignUserToAttributes(
//                username = alice,
//                attributeName = "tall",
//                attributeValue = "200cm"
//            ) == CODE_000_SUCCESS)
//
//            val aliceAttributes = coreCACABAC.getAttributeAssignments(alice)
//            assert(aliceAttributes.code == CODE_000_SUCCESS)
//            assert(aliceAttributes.usersAttributes!!.size == 1)
//            assert(aliceAttributes.usersAttributes!!.filter { it.username == alice && it.attributeName == tall }.size == 1)
//            val aliceUserAttribute = aliceAttributes.usersAttributes!!.first { it.username == alice && it.attributeName == tall }
//            assert(aliceUserAttribute.attributeValue == "200cm")
//        }
//    }
//
//    @Test
//    fun `assign blank, non-existing, incomplete or deleted user to blank, non-existing or deleted attribute fails`() {
//        val userNonExisting = "userNonExisting"
//        val userIncomplete = "userIncomplete"
//        assert(coreCACABAC.addUser(userIncomplete).code == CODE_000_SUCCESS)
//        val userOperational = "userOperational"
//        addAndInitUser(coreCACABAC, userOperational)
//        val userDeleted = "userDeleted"
//        assert(coreCACABAC.addUser(userDeleted).code == CODE_000_SUCCESS)
//        assert(coreCACABAC.deleteUser(userDeleted) == CODE_000_SUCCESS)
//
//        val attributeNonExisting = "attributeNonExisting"
//        val attributeOperational = "attributeOperational"
//        assert(coreCACABAC.addAttribute(attributeOperational) == CODE_000_SUCCESS)
//        val attributeDeleted = "attributeDeleted"
//        assert(coreCACABAC.addAttribute(attributeDeleted) == CODE_000_SUCCESS)
//        assert(coreCACABAC.deleteAttributes(attributeDeleted) == CODE_000_SUCCESS)
//
//        /** assign non-existing user to non-existing attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userNonExisting, attributeNonExisting) ==
//                        CODE_066_ATTRIBUTE_NOT_FOUND
//            )
//        }
//
//        /** assign non-existing user to operational attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userNonExisting, attributeOperational) ==
//                        CODE_004_USER_NOT_FOUND
//            )
//        }
//
//        /** assign non-existing user to deleted attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userNonExisting, attributeDeleted) ==
//                        CODE_067_ATTRIBUTE_WAS_DELETED
//            )
//        }
//
//        /** assign incomplete user to non-existing attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userIncomplete, attributeNonExisting) ==
//                        CODE_066_ATTRIBUTE_NOT_FOUND
//            )
//        }
//
//        /** assign incomplete user to operational attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userIncomplete, attributeOperational) ==
//                        CODE_053_USER_IS_INCOMPLETE
//            )
//        }
//
//        /** assign incomplete user to deleted attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userIncomplete, attributeDeleted) ==
//                        CODE_067_ATTRIBUTE_WAS_DELETED
//            )
//        }
//
//        /** assign operational user to non-existing attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userOperational, attributeNonExisting) ==
//                        CODE_066_ATTRIBUTE_NOT_FOUND
//            )
//        }
//
//        /** assign operational user to deleted attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userOperational, attributeDeleted) ==
//                        CODE_067_ATTRIBUTE_WAS_DELETED
//            )
//        }
//
//        /** assign deleted user to non-existing attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userDeleted, attributeNonExisting) ==
//                        CODE_066_ATTRIBUTE_NOT_FOUND
//            )
//        }
//
//        /** assign deleted user to operational attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userDeleted, attributeOperational) ==
//                        CODE_013_USER_WAS_DELETED
//            )
//        }
//
//        /** assign deleted user to deleted attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userDeleted, attributeDeleted) ==
//                        CODE_067_ATTRIBUTE_WAS_DELETED
//            )
//        }
//
//        /** assign blank user to operational attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes("  ", attributeOperational) ==
//                        CODE_020_INVALID_PARAMETER
//            )
//        }
//
//        /** assign operational user to blank attribute */
//        run {
//            assert(
//                coreCACABAC.assignUserToAttributes(userOperational, "  ") ==
//                        CODE_020_INVALID_PARAMETER
//            )
//        }
//    }
//
//    @Test
//    fun `assign operational user to operational attribute twice fails`() {
//        val alice = "alice"
//        val tall = "tall"
//        addAndInitUser(coreCACABAC, alice)
//        assert(coreCACABAC.addAttribute(tall) == CODE_000_SUCCESS)
//
//        /** assign operational user to operational attribute twice */
//        run {
//            assert(coreCACABAC.assignUserToAttributes(alice, tall) == CODE_000_SUCCESS)
//            assert(coreCACABAC.assignUserToAttributes(alice, tall) == CODE_068_USER_ATTRIBUTE_ASSIGNMENT_ALREADY_EXISTS)
//        }
//    }
//
//    @Test
//    fun `revoke user from assigned attribute works`() {
//        val alice = "alice"
//        val excel = "excel"
//        addAndInitUser(coreCACABAC, alice)
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(coreCACABAC.assignUserToAttributes(alice, "Tall") == CODE_000_SUCCESS)
//        assert(
//            coreCACABAC.addResource(
//                resourceName = excel,
//                resourceContent = excel.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        /** revoke user from assigned attribute */
//        run {
//            /** get the attribute and the access structure-permission assignments before the revoke operation */
//            val beforeTallAttribute = coreCACABAC.getAttributes().attributes!!.first { it.name == "Tall" }
//            assert(beforeTallAttribute.versionNumber == 1)
//            val beforeAccessStructuresPermissions = coreCACABAC.getAccessStructures(resourceName = excel)
//            assert(beforeAccessStructuresPermissions.code == CODE_000_SUCCESS)
//            assert(beforeAccessStructuresPermissions.accessStructuresPermissions!!.size == 1)
//            beforeAccessStructuresPermissions.accessStructuresPermissions!!.filter { it.resourceName == excel }.apply {
//                assert(size == 1)
//                assert(first().resourceVersionNumber == 1)
//            }
//            val beforeResourceExcel = coreCACABAC.getResources().resources!!.first { it.name == excel }
//            assert(beforeResourceExcel.versionNumber == 1)
//
//            /** revoke operation */
//            assert(coreCACABAC.revokeAttributesFromUser(alice, "Tall") == CODE_000_SUCCESS)
//            val aliceAttributes = coreCACABAC.getAttributeAssignments(alice)
//            assert(aliceAttributes.code == CODE_000_SUCCESS)
//            assert(aliceAttributes.usersAttributes!!.size == 0)
//
//            /** get the attribute and the access structure-permission assignments after the revoke operation */
//            val afterTallAttribute = coreCACABAC.getAttributes().attributes!!.first { it.name == "Tall" }
//            assert(afterTallAttribute.versionNumber == 2)
//            val afterAccessStructuresPermissions = coreCACABAC.getAccessStructures(resourceName = excel)
//            assert(afterAccessStructuresPermissions.code == CODE_000_SUCCESS)
//            assert(afterAccessStructuresPermissions.accessStructuresPermissions!!.size == 1)
//            afterAccessStructuresPermissions.accessStructuresPermissions!!.filter { it.resourceName == excel }.apply {
//                assert(size == 1)
//                assert(first().resourceVersionNumber == 2)
//            }
//            val afterResourceExcel = coreCACABAC.getResources().resources!!.first { it.name == excel }
//            assert(afterResourceExcel.versionNumber == 2)
//
//
//            /** check the difference between the access structure-permission assignments before and after the revoke operation */
//            val beforeAccessStructurePermission = beforeAccessStructuresPermissions.accessStructuresPermissions!!.elementAt(0)
//            val afterAccessStructurePermission = afterAccessStructuresPermissions.accessStructuresPermissions!!.elementAt(0)
//
//            assertNotEquals(
//                coreCACABAC.cryptoABE.decryptABE(
//                    keyID = coreCACABAC.user.name,
//                    ciphertext = beforeAccessStructurePermission.encryptedSymKey!!.key.encodeBase64()
//                ),
//                coreCACABAC.cryptoABE.decryptABE(
//                    keyID = coreCACABAC.user.name,
//                    ciphertext = afterAccessStructurePermission.encryptedSymKey!!.key.encodeBase64()
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `revoke blank, non-existing, incomplete or deleted user from blank, non-existing or deleted attribute fails`() {
//        val userNonExisting = "userNonExisting"
//        val userIncomplete = "userIncomplete"
//        assert(coreCACABAC.addUser(userIncomplete).code == CODE_000_SUCCESS)
//        val userOperational = "userOperational"
//        addAndInitUser(coreCACABAC, userOperational)
//        val userDeleted = "userDeleted"
//        assert(coreCACABAC.addUser(userDeleted).code == CODE_000_SUCCESS)
//        assert(coreCACABAC.deleteUser(userDeleted) == CODE_000_SUCCESS)
//
//        val attributeNonExisting = "attributeNonExisting"
//        val attributeOperational = "attributeOperational"
//        assert(coreCACABAC.addAttribute(attributeOperational) == CODE_000_SUCCESS)
//        val attributeDeleted = "attributeDeleted"
//        assert(coreCACABAC.addAttribute(attributeDeleted) == CODE_000_SUCCESS)
//        assert(coreCACABAC.deleteAttributes(attributeDeleted) == CODE_000_SUCCESS)
//
//        /** revoke non-existing user from non-existing attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userNonExisting, attributeNonExisting) ==
//                        CODE_066_ATTRIBUTE_NOT_FOUND
//            )
//        }
//
//        /** revoke non-existing user from operational attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userNonExisting, attributeOperational) ==
//                        CODE_070_USER_ATTRIBUTE_ASSIGNMENT_NOT_FOUND
//            )
//        }
//
//        /** revoke non-existing user from deleted attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userNonExisting, attributeDeleted) ==
//                        CODE_067_ATTRIBUTE_WAS_DELETED
//            )
//        }
//
//        /** revoke incomplete user from non-existing attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userIncomplete, attributeNonExisting) ==
//                        CODE_066_ATTRIBUTE_NOT_FOUND
//            )
//        }
//
//        /** revoke incomplete user from operational attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userIncomplete, attributeOperational) ==
//                        CODE_070_USER_ATTRIBUTE_ASSIGNMENT_NOT_FOUND
//            )
//        }
//
//        /** revoke incomplete user from deleted attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userIncomplete, attributeDeleted) ==
//                        CODE_067_ATTRIBUTE_WAS_DELETED
//            )
//        }
//
//        /** revoke operational user from non-existing attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userOperational, attributeNonExisting) ==
//                        CODE_066_ATTRIBUTE_NOT_FOUND
//            )
//        }
//
//        /** revoke operational user from deleted attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userOperational, attributeDeleted) ==
//                        CODE_067_ATTRIBUTE_WAS_DELETED
//            )
//        }
//
//        /** revoke deleted user from non-existing attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userDeleted, attributeNonExisting) ==
//                        CODE_066_ATTRIBUTE_NOT_FOUND
//            )
//        }
//
//        /** revoke deleted user from operational attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userDeleted, attributeOperational) ==
//                        CODE_070_USER_ATTRIBUTE_ASSIGNMENT_NOT_FOUND
//            )
//        }
//
//        /** revoke deleted user from deleted attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userDeleted, attributeDeleted) ==
//                        CODE_067_ATTRIBUTE_WAS_DELETED
//            )
//        }
//
//        /** revoke blank user from operational attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser("  ", attributeOperational) ==
//                        CODE_020_INVALID_PARAMETER
//            )
//        }
//
//        /** revoke operational user from blank attribute */
//        run {
//            assert(
//                coreCACABAC.revokeAttributesFromUser(userOperational, "  ") ==
//                        CODE_020_INVALID_PARAMETER
//            )
//        }
//    }
//
//    @Test
//    fun `revoke user to assigned attribute twice fails`() {
//        val alice = "alice"
//        val tall = "tall"
//        addAndInitUser(coreCACABAC, alice)
//        assert(coreCACABAC.addAttribute(tall) == CODE_000_SUCCESS)
//        assert(coreCACABAC.assignUserToAttributes(alice, tall) == CODE_000_SUCCESS)
//
//        /** revoke user to assigned attribute twice */
//        run {
//            assert(coreCACABAC.revokeAttributesFromUser(alice, tall) == CODE_000_SUCCESS)
//            assert(coreCACABAC.revokeAttributesFromUser(alice, tall) == CODE_070_USER_ATTRIBUTE_ASSIGNMENT_NOT_FOUND)
//        }
//    }
//
//    @Test
//    fun `assign access structure to operational resource works`() {
//        val testResource = "testResource"
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(
//            coreCACABAC.addResource(
//                resourceName = testResource,
//                resourceContent = testResource.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        /** assign access structure to operational resource */
//        run {
//            assert(coreCACABAC.assignAccessStructure(
//                resourceName = testResource,
//                accessStructure = "Tall or From:bob",
//                operation = Operation.READWRITE
//            ) == CODE_000_SUCCESS)
//
//            val resourceAccessStructures = coreCACABAC.getAccessStructures(
//                resourceName = testResource
//            )
//            assert(resourceAccessStructures.code == CODE_000_SUCCESS)
//            assert(resourceAccessStructures.accessStructuresPermissions!!.size == 2)
//            assert(resourceAccessStructures.accessStructuresPermissions!!.filter { it.operation == Operation.READWRITE }.size == 1)
//            assert(resourceAccessStructures.accessStructuresPermissions!!.filter { it.operation == Operation.READ }.size == 1)
//            val readWriteAS = resourceAccessStructures.accessStructuresPermissions!!.first { it.operation == Operation.READWRITE }
//            assert(readWriteAS.accessStructure == "admin_1 or (Tall_1 or From_1:bob)")
//            assert(readWriteAS.resourceVersionNumber == 1)
//            val readAS = resourceAccessStructures.accessStructuresPermissions!!.first { it.operation == Operation.READ }
//            assert(readAS.accessStructure == "admin_1 or (Tall_1 and From_1)")
//            assert(readAS.resourceVersionNumber == 1)
//
//            assertEquals(
//                coreCACABAC.cryptoABE.decryptABE(
//                    keyID = coreCACABAC.user.name,
//                    ciphertext = readWriteAS.encryptedSymKey!!.key.encodeBase64()
//                ),
//                coreCACABAC.cryptoABE.decryptABE(
//                    keyID = coreCACABAC.user.name,
//                    ciphertext = readAS.encryptedSymKey!!.key.encodeBase64()
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `assign blank access structure to non-existing, blank or deleted resource fails`() {
//        val deletedResource = "deletedResource"
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(
//            coreCACABAC.addResource(
//                resourceName = deletedResource,
//                resourceContent = deletedResource.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//        assert(coreCACABAC.deleteResource(deletedResource) == CODE_000_SUCCESS)
//
//        val operationalResource = "operationalResource"
//        assert(
//            coreCACABAC.addResource(
//                resourceName = operationalResource,
//                resourceContent = operationalResource.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        /** assign blank access structure to any resource */
//        run {
//            assert(
//                coreCACABAC.assignAccessStructure(
//                    resourceName = "non-existing",
//                    accessStructure = "   ",
//                    operation = Operation.READ
//                ) == CODE_020_INVALID_PARAMETER)
//
//            assert(
//                coreCACABAC.assignAccessStructure(
//                    resourceName = deletedResource,
//                    accessStructure = "   ",
//                    operation = Operation.READ
//                ) == CODE_020_INVALID_PARAMETER)
//
//            assert(
//                coreCACABAC.assignAccessStructure(
//                    resourceName = operationalResource,
//                    accessStructure = "   ",
//                    operation = Operation.READ
//                ) == CODE_020_INVALID_PARAMETER)
//        }
//
//        /** assign access structure to blank resource */
//        run {
//            assert(
//                coreCACABAC.assignAccessStructure(
//                    resourceName = "    ",
//                    accessStructure = "Tall",
//                    operation = Operation.READ
//                ) == CODE_020_INVALID_PARAMETER)
//        }
//
//        /** assign access structure to non-existing resource */
//        run {
//            assert(
//                coreCACABAC.assignAccessStructure(
//                    resourceName = "non-existing",
//                    accessStructure = "Tall",
//                    operation = Operation.READ
//                ) == CODE_006_RESOURCE_NOT_FOUND)
//        }
//
//        /** assign access structure to deleted resource */
//        run {
//            assert(
//                coreCACABAC.assignAccessStructure(
//                    resourceName = deletedResource,
//                    accessStructure = "Tall",
//                    operation = Operation.READ
//                ) == CODE_015_RESOURCE_WAS_DELETED)
//        }
//    }
//
//    @Test
//    fun `assign access structure to operational resource twice fails`() {
//        val testResource = "testResource"
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(
//            coreCACABAC.addResource(
//                resourceName = testResource,
//                resourceContent = testResource.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//        assert(
//            coreCACABAC.assignAccessStructure(
//                resourceName = testResource,
//                accessStructure = "Tall or From:bob",
//                operation = Operation.READWRITE,
//            ) == CODE_000_SUCCESS)
//
//        /** assign access structure to operational resource twice */
//        run {
//            assert(
//                coreCACABAC.assignAccessStructure(
//                    resourceName = testResource,
//                    accessStructure = "Tall or From:bob",
//                    operation = Operation.READWRITE,
//                ) == CODE_069_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS)
//            assert(
//                coreCACABAC.assignAccessStructure(
//                    resourceName = testResource,
//                    accessStructure = "Tall or From:bob",
//                    operation = Operation.READ,
//                ) == CODE_069_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS)
//        }
//    }
//
//    @Test
//    fun `revoke access structure from assigned resource works`() {
//        val testResource = "testResource"
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(
//            coreCACABAC.addResource(
//                resourceName = testResource,
//                resourceContent = testResource.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        /** revoke access structure from assigned resource */
//        run {
//            assert(coreCACABAC.assignAccessStructure(
//                resourceName = testResource,
//                accessStructure = "Tall or From:bob",
//                operation = Operation.READWRITE
//            ) == CODE_000_SUCCESS)
//
//            val beforeResourceAccessStructures = coreCACABAC.getAccessStructures(
//                resourceName = testResource
//            )
//            val beforeAS = beforeResourceAccessStructures.accessStructuresPermissions!!.first { it.operation == Operation.READ }
//            assert(beforeAS.accessStructure == "admin_1 or (Tall_1 and From_1)")
//            assert(beforeAS.resourceVersionNumber == 1)
//            val beforeResource = coreCACABAC.getResources().resources!!.first { it.name == testResource }
//            assert(beforeResource.versionNumber == 1)
//
//            assert(coreCACABAC.revokeAccessStructure(
//                resourceName = testResource,
//                operation = Operation.READWRITE
//            ) == CODE_000_SUCCESS)
//
//            val afterResourceAccessStructures = coreCACABAC.getAccessStructures(
//                resourceName = testResource
//            )
//            val afterAS = afterResourceAccessStructures.accessStructuresPermissions!!.first { it.operation == Operation.READ }
//            assert(afterAS.accessStructure == "admin_1 or (Tall_1 and From_1)")
//            assert(afterAS.resourceVersionNumber == 2)
//            val afterResource = coreCACABAC.getResources().resources!!.first { it.name == testResource }
//            assert(afterResource.versionNumber == 2)
//
//            assertNotEquals(
//                coreCACABAC.cryptoABE.decryptABE(
//                    keyID = coreCACABAC.user.name,
//                    ciphertext = beforeAS.encryptedSymKey!!.key.encodeBase64()
//                ),
//                coreCACABAC.cryptoABE.decryptABE(
//                    keyID = coreCACABAC.user.name,
//                    ciphertext = afterAS.encryptedSymKey!!.key.encodeBase64()
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `revoke access structure from blank, non-existing or deleted resource fails`() {
//        val deletedResource = "deletedResource"
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(
//            coreCACABAC.addResource(
//                resourceName = deletedResource,
//                resourceContent = deletedResource.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//        assert(coreCACABAC.deleteResource(deletedResource) == CODE_000_SUCCESS)
//
//        /** revoke access structure from non-existing resource */
//        run {
//            assert(coreCACABAC.revokeAccessStructure(
//                resourceName = "non-existing",
//                operation = Operation.READWRITE
//            ) in listOf(CODE_006_RESOURCE_NOT_FOUND,
//                CODE_071_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_NOT_FOUND,
//                CODE_006_RESOURCE_NOT_FOUND)
//            )
//        }
//
//        /** revoke access structure from deleted resource */
//        run {
//            assert(coreCACABAC.revokeAccessStructure(
//                resourceName = deletedResource,
//                operation = Operation.READWRITE
//            ) in listOf(CODE_006_RESOURCE_NOT_FOUND,
//                CODE_071_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_NOT_FOUND,
//                CODE_006_RESOURCE_NOT_FOUND)
//            )
//        }
//    }
//
//    @Test
//    fun `revoke last access structure to assigned resource or twice fails`() {
//        val operationalResource = "operationalResource"
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(
//            coreCACABAC.addResource(
//                resourceName = operationalResource,
//                resourceContent = operationalResource.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        /** revoke last access structure to assigned resource */
//        run {
//            assert(coreCACABAC.revokeAccessStructure(
//                resourceName = operationalResource,
//                operation = Operation.READ,
//            ) == CODE_074_CANNOT_REVOKE_LAST_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT)
//        }
//
//        /** revoke access structure to assigned resource twice */
//        run {
//            assert(coreCACABAC.assignAccessStructure(
//                resourceName = operationalResource,
//                accessStructure = "Tall or From:bob",
//                operation = Operation.READWRITE
//            ) == CODE_000_SUCCESS)
//
//            assert(coreCACABAC.revokeAccessStructure(
//                resourceName = operationalResource,
//                operation = Operation.READWRITE,
//            ) == CODE_000_SUCCESS)
//
//            assert(coreCACABAC.revokeAccessStructure(
//                resourceName = operationalResource,
//                operation = Operation.READWRITE,
//            ) == CODE_071_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_NOT_FOUND)
//        }
//    }
//
//    @Test
//    open fun `revoke access structure and reassign lower permission works`() {
//        val alice = "alice"
//        val aliceCoreCACRBAC = addAndInitUser(core, alice)
//
//        val testResource = "testResource"
//        assert(coreCACABAC.addAttribute("Short") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(
//            coreCACABAC.addResource(
//                resourceName = testResource,
//                resourceContent = testResource.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        /** revoke assigned permission and reassign lower permission */
//        run {
//            assert(coreCACABAC.assignAccessStructure(
//                resourceName = testResource,
//                accessStructure = "Short",
//                operation = Operation.READ
//            ) == CODE_000_SUCCESS)
//
//            assert(coreCACABAC.assignUserToAttributes(
//                username = alice,
//                attributes = hashMapOf("Tall" to null, "From" to "Student")
//            ) == CODE_000_SUCCESS)
//            assert(coreCACABAC.revokeAttributesFromUser(
//                username = alice,
//                attributes = hashSetOf("Tall", "From")
//            ) == CODE_000_SUCCESS)
//            assert(coreCACABAC.assignUserToAttributes(
//                username = alice,
//                attributes = hashMapOf("Short" to null)
//            ) == CODE_000_SUCCESS)
//
//            val downloadedResourceResult = aliceCoreCACRBAC.readResource(testResource)
//            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
//            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == testResource)
//        }
//    }
//
//    @Test
//    open fun `admin or user read resource having satisfying attributes over resource works`() {
//        val alice = "alice"
//        val aliceCoreCACABAC = addAndInitUser(coreCACABAC, alice)
//
//        val tall = "tall"
//        assert(coreCACABAC.addAttribute(tall) == CODE_000_SUCCESS)
//        val adult = "adult"
//        assert(coreCACABAC.addAttribute(adult) == CODE_000_SUCCESS)
//
//        val exam = "exam"
//        val examContent = "exam content"
//        assert(
//            coreCACABAC.addResource(
//                resourceName = exam,
//                resourceContent = examContent.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        /** admin read resource having satisfying attributes over resource works */
//        run {
//            val downloadedResourceResult = coreCACABAC.readResource(exam)
//            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
//            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == examContent)
//        }
//
//        /** user read resource having satisfying attributes over resource works */
//        run {
//            assert(coreCACABAC.assignUserToAttributes(alice, hashMapOf(
//                tall to null, adult to null
//            )) == CODE_000_SUCCESS)
//            val downloadedResourceResult = aliceCoreCACABAC.readResource(exam)
//            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
//            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == examContent)
//        }
//    }
//
//    @Test
//    open fun `user read resource not having satisfying attributes or revoked fails`() {
//        val alice = "alice"
//        val aliceCoreCACABAC = addAndInitUser(coreCACABAC, alice)
//
//        val tall = "tall"
//        assert(coreCACABAC.addAttribute(tall) == CODE_000_SUCCESS)
//        val adult = "adult"
//        assert(coreCACABAC.addAttribute(adult) == CODE_000_SUCCESS)
//
//        val exam = "exam"
//        val examContent = "exam content"
//        assert(
//            coreCACABAC.addResource(
//                resourceName = exam,
//                resourceContent = examContent.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        /** not assigned user read resource */
//        run {
//            assert(coreCACABAC.assignUserToAttributes(alice, tall) == CODE_000_SUCCESS)
//            assert(aliceCoreCACABAC.readResource(exam).code == CODE_006_RESOURCE_NOT_FOUND)
//        }
//
//        /** revoked user read resource */
//        run {
//            assert(coreCACABAC.assignUserToAttributes(alice, adult) == CODE_000_SUCCESS)
//            val downloadedResourceResult = aliceCoreCACABAC.readResource(exam)
//            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
//            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == examContent)
//
//            assert(coreCACABAC.revokeAttributesFromUser(alice, adult) == CODE_000_SUCCESS)
//            assert(aliceCoreCACABAC.readResource(exam).code == CODE_006_RESOURCE_NOT_FOUND)
//        }
//    }
//
//    @Test
//    open fun `admin or user write resource having satisfying attributes over resource works`() {
//        val alice = "alice"
//        val aliceCoreCACABAC = addAndInitUser(coreCACABAC, alice)
//
//        val tall = "tall"
//        assert(coreCACABAC.addAttribute(tall) == CODE_000_SUCCESS)
//        val adult = "adult"
//        assert(coreCACABAC.addAttribute(adult) == CODE_000_SUCCESS)
//
//        val exam = "exam"
//        val examContent = "exam content"
//        assert(
//            coreCACABAC.addResource(
//                resourceName = exam,
//                resourceContent = examContent.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//        val readResourceResult = coreCACABAC.readResource(exam)
//        assert(String(readResourceResult.stream!!.readAllBytes()) == examContent)
//
//        /** admin write resource having permission over resource */
//        run {
//            val updatedExamContent = "updated exam content by admin"
//            val updateResourceCode = coreCACABAC.writeResource(exam, updatedExamContent.inputStream())
//            assert(updateResourceCode == CODE_000_SUCCESS)
//            val downloadedResourceResult = coreCACABAC.readResource(exam)
//            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
//            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == updatedExamContent)
//        }
//
//        /** user write resource having permission over resource */
//        run {
//            assert(coreCACABAC.assignUserToAttributes(alice, hashMapOf(
//                tall to null, adult to null
//            )) == CODE_000_SUCCESS)
//            val updatedExamContent = "updated exam content by user"
//            val updateResourceCode = aliceCoreCACABAC.writeResource(exam, updatedExamContent.inputStream())
//            assert(updateResourceCode == CODE_000_SUCCESS)
//            val downloadedResourceResult = aliceCoreCACABAC.readResource(exam)
//            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
//            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == updatedExamContent)
//        }
//    }
//
//    @Test
//    open fun `user write resource not having satisfying attributes or revoked fails`() {
//        val alice = "alice"
//        val aliceCoreCACABAC = addAndInitUser(coreCACABAC, alice)
//
//        val tall = "tall"
//        assert(coreCACABAC.addAttribute(tall) == CODE_000_SUCCESS)
//        val adult = "adult"
//        assert(coreCACABAC.addAttribute(adult) == CODE_000_SUCCESS)
//
//        val exam = "exam"
//        val examContent = "exam content"
//        assert(
//            coreCACABAC.addResource(
//                resourceName = exam,
//                resourceContent = examContent.inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        /** not assigned user write resource */
//        run {
//            assert(coreCACABAC.assignUserToAttributes(alice, tall) == CODE_000_SUCCESS)
//            assert(aliceCoreCACABAC.writeResource(exam, exam.inputStream()) == CODE_006_RESOURCE_NOT_FOUND)
//        }
//
//        /** revoked user write resource */
//        run {
//            assert(coreCACABAC.assignUserToAttributes(alice, adult) == CODE_000_SUCCESS)
//            val downloadedResourceResult = aliceCoreCACABAC.readResource(exam)
//            assert(downloadedResourceResult.code == CODE_000_SUCCESS)
//            assert(String(downloadedResourceResult.stream!!.readAllBytes()) == examContent)
//
//            assert(coreCACABAC.revokeAttributesFromUser(alice, adult) == CODE_000_SUCCESS)
//            assert(aliceCoreCACABAC.writeResource(exam, exam.inputStream()) == CODE_006_RESOURCE_NOT_FOUND)
//        }
//    }
//
//
//
//    @Test
//    fun `get attribute of operational or deleted attribute works`() {
//        assert(coreCACABAC.addAttribute("operational") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("deleted") == CODE_000_SUCCESS)
//        assert(coreCACABAC.deleteAttributes("deleted") == CODE_000_SUCCESS)
//
//        /** get attribute of operational attribute */
//        run {
//            assert(coreCACABAC.getAttributes().attributes!!.filter { it.name == "operational" }.size == 1)
//        }
//
//        /** get attribute of deleted attribute */
//        run {
//            assert(coreCACABAC.getAttributes().attributes!!.first { it.status == TupleStatus.DELETED }.name == "deleted")
//        }
//    }
//
//    @Test
//    fun `get attribute of non-existing fails`() {
//
//        /** get attribute of non-existing attribute */
//        run {
//            assert(coreCACABAC.getAttributes().attributes!!.none { it.name == "not-existing" })
//        }
//    }
//
//    @Test
//    fun `get resource of operational or deleted resource works`() {
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("Short") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("Funny") == CODE_000_SUCCESS)
//
//        assert(
//            coreCACABAC.addResource(
//                resourceName = "operational",
//                resourceContent = "operational".inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        assert(
//            coreCACABAC.addResource(
//                resourceName = "deleted",
//                resourceContent = "deleted".inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//        assert(coreCACABAC.deleteResource("deleted") == CODE_000_SUCCESS)
//
//        /** get resource of operational resource */
//        run {
//            assert(coreCACABAC.getResources().resources!!.first {
//                it.status == TupleStatus.OPERATIONAL }
//                .name == "operational")
//        }
//
//        /** get resource of deleted resource */
//        run {
//            assert(coreCACABAC.getResources().resources!!.first {
//                it.status == TupleStatus.DELETED
//            }.name == "deleted")
//        }
//    }
//
//    @Test
//    fun `get resource of non-existing or deleted resource fails`() {
//
//        /** get resource of non-existing resource */
//        run {
//            assert(coreCACABAC.getResources().resources!!.none { it.name == "not-existing" })
//        }
//    }
//
//    @Test
//    fun `get existing assignment specifying any combination of username and attribute name works`() {
//        addAndInitUser(coreCACABAC, "alice")
//        assert(coreCACABAC.addAttribute("tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.assignUserToAttributes("alice", "tall") == CODE_000_SUCCESS)
//
//        /** get existing assignment specifying the username */
//        run {
//            assert(coreCACABAC.getAttributeAssignments(username = "alice").usersAttributes!!.filter { it.attributeName == "tall" }.size == 1)
//        }
//
//        /** get existing assignment specifying the attribute name */
//        run {
//            assert(coreCACABAC.getAttributeAssignments(attributeName = "tall").usersAttributes!!.filter { it.username == "alice" }.size == 1)
//        }
//
//        /** get existing assignment specifying both the username and the attribute name */
//        run {
//            assert(coreCACABAC.getAttributeAssignments(username = "alice", attributeName = "tall").usersAttributes!!.size == 1)
//        }
//    }
//
//    @Test
//    fun `get non-existing or deleted assignment fails`() {
//
//        /** get non-existing assignment */
//        run {
//            assert(coreCACABAC.getAttributeAssignments(username = "alice").usersAttributes!!.none { it.attributeName == "tall" })
//            assert(coreCACABAC.getAttributeAssignments(attributeName = "tall").usersAttributes!!.none { it.username == "alice" })
//            assert(coreCACABAC.getAttributeAssignments(username = "alice", attributeName = "tall").usersAttributes!!.isEmpty())
//        }
//
//        /** get deleted assignment */
//        run {
//            addAndInitUser(coreCACABAC, "alice")
//            assert(coreCACABAC.addAttribute("tall") == CODE_000_SUCCESS)
//            assert(coreCACABAC.assignUserToAttributes("alice", "tall") == CODE_000_SUCCESS)
//            assert(coreCACABAC.revokeAttributesFromUser("alice", "tall") == CODE_000_SUCCESS)
//
//            assert(coreCACABAC.getAttributeAssignments(username = "alice").usersAttributes!!.none { it.attributeName == "tall" })
//            assert(coreCACABAC.getAttributeAssignments(attributeName = "tall").usersAttributes!!.none { it.username == "alice" })
//            assert(coreCACABAC.getAttributeAssignments(username = "alice", attributeName = "tall").usersAttributes!!.isEmpty())
//        }
//    }
//
//    @Test
//    fun `get existing access structures specifying the resource name works`() {
//        assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//        assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//        assert(
//            coreCACABAC.addResource(
//                resourceName = "operational",
//                resourceContent = "operational".inputStream(),
//                enforcement = Enforcement.COMBINED,
//                operation = Operation.READ,
//                accessStructure = "Tall and From"
//            ) == CODE_000_SUCCESS)
//
//        /** get existing access structures specifying the resource name */
//        run {
//            assert(
//                coreCACABAC.getAccessStructures(
//                    resourceName = "operational"
//                ).accessStructuresPermissions!!.filter {
//                    it.resourceName == "operational"
//                }.size == 1
//            )
//        }
//    }
//
//    @Test
//    fun `get non-existing or deleted access structures fails`() {
//
//        /** get non-existing access structure */
//        run {
//            assert(coreCACABAC.getAccessStructures(
//                resourceName = "non-existing"
//            ).accessStructuresPermissions!!.isEmpty())
//        }
//
//        /** get deleted access structure */
//        run {
//            assert(coreCACABAC.addAttribute("Tall") == CODE_000_SUCCESS)
//            assert(coreCACABAC.addAttribute("From") == CODE_000_SUCCESS)
//            assert(
//                coreCACABAC.addResource(
//                    resourceName = "operational",
//                    resourceContent = "operational".inputStream(),
//                    enforcement = Enforcement.COMBINED,
//                    operation = Operation.READ,
//                    accessStructure = "Tall and From"
//                ) == CODE_000_SUCCESS)
//
//            assert(
//                coreCACABAC.assignAccessStructure(
//                    resourceName = "operational",
//                    accessStructure = "Tall or From",
//                    operation = Operation.READWRITE,
//                ) == CODE_000_SUCCESS)
//            assert(
//                coreCACABAC.revokeAccessStructure(
//                    resourceName = "operational",
//                    operation = Operation.READWRITE,
//                ) == CODE_000_SUCCESS)
//
//            val lastAS = coreCACABAC.getAccessStructures(
//                resourceName = "operational"
//            ).accessStructuresPermissions!!.filter {
//                it.resourceName == "operational"
//            }
//            assert(lastAS.size == 1)
//            lastAS.first().apply {
//                assert(resourceVersionNumber == 2)
//                assert(operation == Operation.READ)
//            }
//        }
//    }
//
//
//
//    override fun addAndInitUser(core: Core, username: String): CoreCACABAC {
//        return super.addAndInitUser(core, username) as CoreCACABAC
//    }
//}
