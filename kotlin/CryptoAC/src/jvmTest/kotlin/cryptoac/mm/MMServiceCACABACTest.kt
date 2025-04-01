//package cryptoac.mm
//
//import cryptoac.*
//import cryptoac.Constants.ADMIN
//import cryptoac.TestUtilities.Companion.assertUnlockAndLock
//import cryptoac.TestUtilities.Companion.createAttribute
//import cryptoac.TestUtilities.Companion.createResource
//import cryptoac.TestUtilities.Companion.createUserAttribute
//import cryptoac.crypto.AsymKeysType
//import cryptoac.crypto.EncryptedSymKey
//import cryptoac.tuple.*
//import org.junit.jupiter.api.*
//
//internal abstract class MMServiceCACABACTest : MMServiceTest() {
//
//    abstract override val mm: MMServiceCACABAC
//
//    @Test
//    open fun `set and get MPK works`() {
//        /** set and get MPK */
//        myRun {
//            assert(mm.setMPK("testMPK") == OutcomeCode.CODE_000_SUCCESS)
//            assert(mm.getMPK() == "testMPK")
//        }
//    }
//
//    @Test
//    open fun `set MPK twice fails`() {
//        myRun {
//            assert(mm.setMPK("testMPK") == OutcomeCode.CODE_000_SUCCESS)
//            assert(mm.setMPK("testMPK2") == OutcomeCode.CODE_072_MPK_ALREADY_INITIALIZED)
//        }
//    }
//
//    @Test
//    open fun `get MPK without setting it works`() {
//        assert(mm.getMPK() == null)
//    }
//
//    @Test
//    open fun `add attribute once or with same name as previously deleted attribute with restore option works`() {
//
//        /** add attribute once */
//        myRun {
//            addAttribute("tall")
//        }
//
//        /** add attribute with same name as previously deleted attribute with restore option */
//        myRun {
//            val adultAttribute = addAttribute("over18")
//            val newAdultAttribute = Attribute(
//                name = adultAttribute.name,
//                versionNumber = 2
//            )
//            assert(mm.deleteAttribute(attributeName = adultAttribute.name) == OutcomeCode.CODE_000_SUCCESS)
//            assert(mm.addAttribute(newAttribute = newAdultAttribute, restoreIfDeleted = true) == OutcomeCode.CODE_000_SUCCESS)
//            assert(mm.getAttributes(attributeName = adultAttribute.name).first().versionNumber == 2)
//        }
//    }
//
//    @Test
//    open fun `add attribute twice or with admin name or blank name or same name as previously deleted attribute with no restore option fails`() {
//        val adultAttribute = addAttribute("over18")
//
//        /** add attribute twice */
//        myRun {
//            assert(mm.addAttribute(newAttribute = adultAttribute, restoreIfDeleted = false) == OutcomeCode.CODE_065_ATTRIBUTE_ALREADY_EXISTS)
//        }
//
//        /** add attribute with admin name */
//        myRun {
//            assert(mm.addAttribute(createAttribute(attributeName = ADMIN), restoreIfDeleted = true) == OutcomeCode.CODE_065_ATTRIBUTE_ALREADY_EXISTS)
//        }
//
//        /** add attribute with blank name */
//        myRun {
//            assert(mm.addAttribute(createAttribute(attributeName = ""), restoreIfDeleted = true) == OutcomeCode.CODE_020_INVALID_PARAMETER)
//        }
//
//        /** add attribute with same name as previously deleted attribute with no restore option */
//        myRun {
//            assert(mm.deleteAttribute(attributeName = adultAttribute.name) == OutcomeCode.CODE_000_SUCCESS)
//            assert(mm.addAttribute(newAttribute = adultAttribute, restoreIfDeleted = false) == OutcomeCode.CODE_067_ATTRIBUTE_WAS_DELETED)
//        }
//    }
//
//    @Test
//    open fun `add resource once works`() {
//        /** add resource once */
//        myRun {
//            addResource("exam")
//        }
//    }
//
//    @Test
//    open fun `add resource twice or with blank name or same name as previously deleted resource fails`() {
//        val exam = addResource("exam")
//
//        /** add resource twice */
//        myRun {
//            assert(mm.addResource(exam) == OutcomeCode.CODE_003_RESOURCE_ALREADY_EXISTS)
//        }
//
//        /** add resource with blank name */
//        myRun {
//            assert(mm.addResource(createResource("")) == OutcomeCode.CODE_020_INVALID_PARAMETER)
//        }
//
//        /** add resource with same name as previously deleted resource */
//        myRun {
//            assert(mm.deleteResource(exam.name) == OutcomeCode.CODE_000_SUCCESS)
//            assert(mm.addResource(exam) == OutcomeCode.CODE_015_RESOURCE_WAS_DELETED)
//        }
//    }
//
//    @Test
//    open fun `add no, one or multiple user-attribute assignments work`() {
//        val aliceName = Parameters.aliceUser.name
//        addAndInitUser(Parameters.aliceUser)
//        val bobName = Parameters.bobUser.name
//        addAndInitUser(Parameters.bobUser)
//        val adultAttribute = addAttribute("over18")
//        val adultAttributeName = adultAttribute.name
//        val tallAttribute = addAttribute("tall")
//        val tallAttributeName = tallAttribute.name
//
//        /** add no user-attribute assignments */
//        myRun {
//            assert(mm.addUsersAttributes(HashSet()) == OutcomeCode.CODE_000_SUCCESS)
//        }
//
//        /** add one user-attribute assignment */
//        myRun {
//            addUserAttribute(aliceName, adultAttributeName)
//        }
//
//        /** add multiple user-attribute assignments */
//        myRun {
//            val bobAdultUserAttribute = createUserAttribute(
//                username = bobName,
//                attributeName = adultAttributeName,
//            )
//            val aliceTallUserAttribute = createUserAttribute(
//                username = aliceName,
//                attributeName = tallAttributeName,
//            )
//            assert(
//                mm.addUsersAttributes(hashSetOf(
//                    bobAdultUserAttribute,
//                    aliceTallUserAttribute
//                )) == OutcomeCode.CODE_000_SUCCESS)
//        }
//    }
//
//    @Test
//    open fun `add user-attribute assignment twice fails`() {
//        val aliceName = Parameters.aliceUser.name
//        addAndInitUser(Parameters.aliceUser)
//        val adultAttribute = addAttribute("over18")
//        val adultAttributeName = adultAttribute.name
//
//        /** add user-attribute assignment twice */
//        myRun {
//            val aliceAdultUserAttribute = addUserAttribute(aliceName, adultAttributeName)
//            assert(
//                mm.addUsersAttributes(hashSetOf(aliceAdultUserAttribute)) ==
//                        OutcomeCode.CODE_068_USER_ATTRIBUTE_ASSIGNMENT_ALREADY_EXISTS
//            )
//        }
//    }
//
//    @Test
//    open fun `add user-attribute assignment with non-existing or deleted user or attribute fails`() {
//        val aliceName = Parameters.aliceUser.name
//        addAndInitUser(Parameters.aliceUser)
//
//        val bobName = Parameters.bobUser.name
//        addAndInitUser(Parameters.bobUser)
//        assert(mm.deleteUser(bobName) == OutcomeCode.CODE_000_SUCCESS)
//
//        val userNonExisting = TestUtilities.createUser("non-existing-user")
//        val userNonExistingName = userNonExisting.name
//
//        val adultAttribute = addAttribute("over18")
//        val adultAttributeName = adultAttribute.name
//
//        val attributeNonExisting = TestUtilities.createAttribute("non-existing-attribute")
//        val attributeNonExistingName = attributeNonExisting.name
//        val tallAttribute = addAttribute("tall")
//        val deletedAttributeName = tallAttribute.name
//        assert(mm.deleteAttribute(deletedAttributeName) == OutcomeCode.CODE_000_SUCCESS)
//
//        /** add user-attribute assignment with non-existing user */
//        myRun {
//            val nonExistingUserAdultUserAttribute =
//                createUserAttribute(
//                    username = userNonExistingName,
//                    attributeName = adultAttributeName,
//                )
//            assert(
//                mm.addUsersAttributes(hashSetOf(nonExistingUserAdultUserAttribute)) ==
//                        OutcomeCode.CODE_004_USER_NOT_FOUND
//            )
//        }
//
//        /** add user-attribute assignment with deleted user */
//        myRun {
//            val deleteUserAdultUserAttribute =
//                createUserAttribute(
//                    username = bobName,
//                    attributeName = adultAttributeName,
//                )
//            assert(
//                mm.addUsersAttributes(hashSetOf(deleteUserAdultUserAttribute)) ==
//                        OutcomeCode.CODE_013_USER_WAS_DELETED
//            )
//        }
//
//        /** add user-attribute assignment with non-existing attribute */
//        myRun {
//            val aliceNonExistingAttributeUserAttribute =
//                createUserAttribute(
//                    username = aliceName,
//                    attributeName = attributeNonExistingName,
//                )
//            assert(
//                mm.addUsersAttributes(hashSetOf(aliceNonExistingAttributeUserAttribute)) ==
//                        OutcomeCode.CODE_066_ATTRIBUTE_NOT_FOUND
//            )
//        }
//
//        /** add user-attribute assignment with deleted attribute */
//        myRun {
//            val aliceDeletedAttributeUserAttribute =
//                createUserAttribute(
//                    username = aliceName,
//                    attributeName = deletedAttributeName,
//                )
//            assert(
//                mm.addUsersAttributes(hashSetOf(aliceDeletedAttributeUserAttribute)) ==
//                        OutcomeCode.CODE_067_ATTRIBUTE_WAS_DELETED
//            )
//        }
//    }
//
//    @Test
//    open fun `add no, one or multiple access structure-permission assignments work`() {
//        val exam = addResource("exam")
//        val text = addResource("text")
//        val page = addResource("page")
//
//        /** add no access structure-permission assignments */
//        myRun {
//            assert(mm.addAccessStructuresPermissions(HashSet()) == OutcomeCode.CODE_000_SUCCESS)
//        }
//
//        /** add one access structure-permission assignment */
//        myRun {
//            addAccessStructurePermission("From:Bob", exam)
//        }
//
//        /** add multiple access structure-permission assignments */
//        myRun {
//            val textAccessStructurePermission = TestUtilities.createAccessStructurePermission("Adult and Tall", text)
//            val examAccessStructurePermission = TestUtilities.createAccessStructurePermission("From:Admin", page)
//            assert(
//                mm.addAccessStructuresPermissions(hashSetOf(
//                    examAccessStructurePermission,
//                    textAccessStructurePermission
//                )) == OutcomeCode.CODE_000_SUCCESS)
//        }
//    }
//
//    @Test
//    open fun `add access structure-permission assignment twice fails`() {
//        val exam = addResource("exam")
//
//        /** add access structure-permission assignment twice */
//        myRun {
//            val examAccessStructurePermission = addAccessStructurePermission("Adult and Tall", exam)
//            assert(
//                mm.addAccessStructuresPermissions(hashSetOf(examAccessStructurePermission)) ==
//                        OutcomeCode.CODE_069_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_ALREADY_EXISTS
//            )
//        }
//    }
//
//    @Test
//    open fun `add access structure-permission assignment with non-existing or deleted resource fails`() {
//        val nonExistingResource = TestUtilities.createResource("non-existing")
//
//        val deletedResource = addResource("deletedResource")
//        assert(mm.deleteResource(deletedResource.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        /** add access structure-permission assignment with non-existing resource */
//        myRun {
//            val nonExistingResourceAccessStructurePermission =
//                TestUtilities.createAccessStructurePermission("Adult and Tall", nonExistingResource)
//            assert(
//                mm.addAccessStructuresPermissions(hashSetOf(nonExistingResourceAccessStructurePermission)) ==
//                        OutcomeCode.CODE_006_RESOURCE_NOT_FOUND
//            )
//        }
//
//        /** add access structure-permission assignment with deleted resource */
//        myRun {
//            val deletedResourceAccessStructurePermission =
//                TestUtilities.createAccessStructurePermission("Adult and Tall", deletedResource)
//            assert(
//                mm.addAccessStructuresPermissions(hashSetOf(deletedResourceAccessStructurePermission)) ==
//                        OutcomeCode.CODE_015_RESOURCE_WAS_DELETED
//            )
//        }
//    }
//
//    @Test
//    open fun `get all attributes works`() {
//        val tall = addAttribute("tall")
//        val adult = addAttribute("adult")
//        val short = addAttribute("short")
//
//        /** get all attributes */
//        myRun {
//            val allAttributes = mm.getAttributes()
//            /** there is also the admin */
//            assert(allAttributes.size == 4)
//            assert(allAttributes.filter { it.name == tall.name }.size == 1)
//            assert(allAttributes.filter { it.name == adult.name }.size == 1)
//            assert(allAttributes.filter { it.name == short.name }.size == 1)
//        }
//    }
//
//    @Test
//    open fun `get not-existing, operational and deleted attribute by name works`() {
//        val tall = addAttribute("tall")
//        val deleted = addAttribute("deleted")
//        assert(mm.deleteAttribute(deleted.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        /** get not-existing attribute by name */
//        myRun {
//            assert(mm.getAttributes(attributeName = "non-existing").isEmpty())
//        }
//
//        /** get operational attribute by name */
//        myRun {
//            val operationalAttributeByName = mm.getAttributes(attributeName = tall.name)
//            assert(operationalAttributeByName.size == 1)
//            assert(operationalAttributeByName.firstOrNull()!!.token == tall.token)
//        }
//
//        /** get deleted attribute by name */
//        myRun {
//            val deletedAttributeByName = mm.getAttributes(attributeName = deleted.name, status = TupleStatus.DELETED)
//            assert(deletedAttributeByName.size == 1)
//            assert(deletedAttributeByName.firstOrNull()!!.name == deleted.name)
//        }
//    }
//
//    @Test
//    open fun `get all resources works`() {
//        val exam = addResource("exam")
//        val document = addResource("document")
//        val excel = addResource("excel")
//
//        /** get all resources */
//        myRun {
//            val allResources = mm.getResources()
//            assert(allResources.size == 3)
//            assert(allResources.filter { it.name == exam.name }.size == 1)
//            assert(allResources.filter { it.name == document.name }.size == 1)
//            assert(allResources.filter { it.name == excel.name }.size == 1)
//        }
//    }
//
//    @Test
//    open fun `get not-existing, operational and deleted resource by name works`() {
//        val operational = addResource("operational")
//        val deleted = addResource("deleted")
//        assertUnlockAndLock(mm)
//        assert(mm.deleteResource(deleted.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        /** get not-existing resource by name */
//        myRun {
//            assert(mm.getResources(resourceName = "non-existing").isEmpty())
//        }
//
//        /** get operational resource by name */
//        myRun {
//            val operationalResourceByName = mm.getResources(resourceName = operational.name)
//            assert(operationalResourceByName.size == 1)
//            assert(operationalResourceByName.firstOrNull()!!.token == operational.token)
//        }
//
//        /** get deleted resource by name */
//        myRun {
//            val deletedResourceByName = mm.getResources(resourceName = deleted.name, status = TupleStatus.DELETED)
//            assert(deletedResourceByName.size == 1)
//            assert(deletedResourceByName.firstOrNull()!!.token == deleted.token)
//        }
//    }
//
//    @Test
//    open fun `get user-attribute assignments by user or attribute name works`() {
//        val aliceName = Parameters.aliceUser.name
//        addAndInitUser(Parameters.aliceUser)
//        val bobName = Parameters.bobUser.name
//        addAndInitUser(Parameters.bobUser)
//        val adultAttribute = addAttribute("over18")
//        val adultAttributeName = adultAttribute.name
//
//        addUserAttribute(aliceName, adultAttributeName)
//        addUserAttribute(bobName, adultAttributeName)
//
//        /** get user-attribute assignments by username */
//        myRun {
//            val adultUserAttributeByUserName = mm.getUsersAttributes(
//                username = aliceName
//            )
//            assert(adultUserAttributeByUserName.size == 1)
//            assert(adultUserAttributeByUserName.filter { it.attributeName == adultAttributeName }.size == 1)
//        }
//
//        /** get user-attribute assignments by attribute name */
//        myRun {
//            val adultsUserAttributeByAttributeName = mm.getUsersAttributes(attributeName = adultAttributeName)
//            assert(adultsUserAttributeByAttributeName.size == 2)
//            assert(adultsUserAttributeByAttributeName.filter { it.username == aliceName }.size == 1)
//            assert(adultsUserAttributeByAttributeName.filter { it.username == bobName }.size == 1)
//        }
//    }
//
//    @Test
//    open fun `get access structure-permission assignments of resource by name works`() {
//        val exam = addResource("exam")
//        val text = addResource("text")
//        addAccessStructurePermission("From:Bob", exam)
//        addAccessStructurePermission("Alice and Bob", text)
//
//        /** get access structure-permission assignments of resource by name */
//        myRun {
//            val examAccessStructurePermission = mm.getAccessStructuresPermissions(resourceName = exam.name)
//            assert(examAccessStructurePermission.size == 1)
//            assert(examAccessStructurePermission.first().accessStructure == "From:Bob")
//
//            val textAccessStructurePermission = mm.getAccessStructuresPermissions(resourceName = text.name)
//            assert(textAccessStructurePermission.size == 1)
//            assert(textAccessStructurePermission.first().accessStructure == "Alice and Bob")
//        }
//    }
//
//    @Test
//    open fun `get public key of non-existing, incomplete, operational and deleted users by name or token works`() {
//        val nonExistingUser = Parameters.aliceUser
//        val incompleteUser = Parameters.bobUser
//        val operationalUser = Parameters.carlUser
//        val deleteUser = Parameters.deniseUser
//        assert(mm.addUser(incompleteUser).code == OutcomeCode.CODE_000_SUCCESS)
//        addAndInitUser(operationalUser)
//        addAndInitUser(deleteUser)
//        assert(mm.deleteUser(deleteUser.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        /** get public key of non-existing users by name or token */
//        myRun {
//            assert(
//                mm.getUserPublicKey(
//                    name = nonExistingUser.name, asymKeyType = AsymKeysType.ENC
//                ) == null
//            )
//            assert(
//                mm.getUserPublicKey(
//                    token = nonExistingUser.token, asymKeyType = AsymKeysType.ENC
//                ) == null
//            )
//            assert(
//                mm.getUserPublicKey(
//                    name = nonExistingUser.name, asymKeyType = AsymKeysType.SIG
//                ) == null
//            )
//            assert(
//                mm.getUserPublicKey(
//                    token = nonExistingUser.token, asymKeyType = AsymKeysType.SIG
//                ) == null
//            )
//        }
//
//        /** get public key of incomplete users by name or token */
//        myRun {
//            assert(
//                mm.getUserPublicKey(
//                    name = incompleteUser.name, asymKeyType = AsymKeysType.ENC
//                ) == null
//            )
//            assert(
//                mm.getUserPublicKey(
//                    token = incompleteUser.token, asymKeyType = AsymKeysType.ENC
//                ) == null
//            )
//            assert(
//                mm.getUserPublicKey(
//                    name = incompleteUser.name, asymKeyType = AsymKeysType.SIG
//                ) == null
//            )
//            assert(
//                mm.getUserPublicKey(
//                    token = incompleteUser.token, asymKeyType = AsymKeysType.SIG
//                ) == null
//            )
//        }
//
//        /** get public key of operational users by name or token */
//        myRun {
//            val asymEncKeysBytesByName = mm.getUserPublicKey(
//                name = operationalUser.name, asymKeyType = AsymKeysType.ENC
//            )
//            assert(asymEncKeysBytesByName != null)
//            assert(asymEncKeysBytesByName.contentEquals(operationalUser.asymEncKeys!!.public.decodeBase64()))
//
//            val asymEncKeysBytesByToken = mm.getUserPublicKey(
//                token = operationalUser.token, asymKeyType = AsymKeysType.ENC
//            )
//            assert(asymEncKeysBytesByToken != null)
//            assert(asymEncKeysBytesByToken.contentEquals(operationalUser.asymEncKeys!!.public.decodeBase64()))
//
//            val asymSigKeysBytesByName = mm.getUserPublicKey(
//                name = operationalUser.name, asymKeyType = AsymKeysType.SIG
//            )
//            assert(asymSigKeysBytesByName != null)
//            assert(asymSigKeysBytesByName.contentEquals(operationalUser.asymSigKeys!!.public.decodeBase64()))
//
//            val asymSigKeysBytesByToken = mm.getUserPublicKey(
//                token = operationalUser.token, asymKeyType = AsymKeysType.SIG
//            )
//            assert(asymSigKeysBytesByToken != null)
//            assert(asymSigKeysBytesByToken.contentEquals(operationalUser.asymSigKeys!!.public.decodeBase64()))
//        }
//
//        /** get public key of deleted users by name or token */
//        myRun {
//            mm.getUserPublicKey(
//                name = deleteUser.name, asymKeyType = AsymKeysType.ENC
//            ).apply {
//                assert(this != null)
//                assert(this.contentEquals(deleteUser.asymEncKeys!!.public.decodeBase64()))
//            }
//            mm.getUserPublicKey(
//                token = deleteUser.token, asymKeyType = AsymKeysType.ENC
//            ).apply {
//                assert(this != null)
//                assert(this.contentEquals(deleteUser.asymEncKeys!!.public.decodeBase64()))
//            }
//            mm.getUserPublicKey(
//                name = deleteUser.name, asymKeyType = AsymKeysType.SIG
//            ).apply {
//                assert(this != null)
//                assert(this.contentEquals(deleteUser.asymSigKeys!!.public.decodeBase64()))
//            }
//            mm.getUserPublicKey(
//                token = deleteUser.token, asymKeyType = AsymKeysType.SIG
//            ).apply {
//                assert(this != null)
//                assert(this.contentEquals(deleteUser.asymSigKeys!!.public.decodeBase64()))
//            }
//        }
//    }
//
//    @Test
//    open fun `get version number of non-existing, incomplete, operational and deleted users, roles, and resources by name or token works`() {
//        val userNonExisting = User("userNonExisting")
//        val userIncomplete = addUser("userIncomplete", )
//        val userOperational = addAndInitUser("userOperational", )
//        val userDeleted = addUser("userDeleted")
//        assert(mm.deleteUser(userDeleted.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        val attributeNonExisting = Attribute("attributeNonExisting")
//        val attributeOperational = addAttribute("attributeOperational", 2)
//        val attributeDeleted = addAttribute("attributeDeleted")
//        assert(mm.deleteAttribute(attributeDeleted.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        val resourceNonExisting = Resource(
//            name = "resourceNonExisting",
//            enforcement = Enforcement.COMBINED
//        )
//        val resourceOperational = addResource("resourceOperational", 3)
//        val resourceDeleted = addResource("resourceDeleted")
//        assert(mm.deleteResource(resourceDeleted.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        /** get version number of non-existing users by name or token */
//        myRun {
//            assert(
//                mm.getVersionNumber(
//                    name = userNonExisting.name,
//                    elementType = ABACElementType.USER,
//                ) == null
//            )
//            assert(
//                mm.getVersionNumber(
//                    token = userNonExisting.token,
//                    elementType = ABACElementType.USER,
//                ) == null
//            )
//        }
//
//        /** get version number of incomplete users by name or token */
//        myRun {
//            assert(
//                mm.getVersionNumber(
//                    name = userIncomplete.name,
//                    elementType = ABACElementType.USER,
//                ) == null
//            )
//            assert(
//                mm.getVersionNumber(
//                    token = userIncomplete.token,
//                    elementType = ABACElementType.USER,
//                ) == null
//            )
//        }
//
//        /** get version number of operational users by name or token */
//        myRun {
//            val versionNumbersByName = mm.getVersionNumber(
//                name = userOperational.name,
//                elementType = ABACElementType.USER
//            )
//            assert(versionNumbersByName != null)
//            assert(versionNumbersByName == userOperational.versionNumber)
//
//            val versionNumbersByToken = mm.getVersionNumber(
//                token = userOperational.token,
//                elementType = ABACElementType.USER
//            )
//            assert(versionNumbersByToken != null)
//            assert(versionNumbersByToken == userOperational.versionNumber)
//        }
//
//        /** get version number of deleted users by name or token */
//        myRun {
//            assert(
//                mm.getVersionNumber(
//                    name = userDeleted.name,
//                    elementType = ABACElementType.USER,
//                ) == null
//            )
//            assert(
//                mm.getVersionNumber(
//                    token = userDeleted.token,
//                    elementType = ABACElementType.USER,
//                ) == null
//            )
//        }
//
//        /** get version number of non-existing attributes by name or token */
//        myRun {
//            assert(
//                mm.getVersionNumber(
//                    name = attributeNonExisting.name, elementType = ABACElementType.ATTRIBUTE,
//                ) == null
//            )
//            assert(
//                mm.getVersionNumber(
//                    token = attributeNonExisting.token, elementType = ABACElementType.ATTRIBUTE,
//                ) == null
//            )
//        }
//
//        /** get version number of operational attributes by name or token */
//        myRun {
//            val versionNumbersByName = mm.getVersionNumber(
//                name = attributeOperational.name, elementType = ABACElementType.ATTRIBUTE
//            )
//            assert(versionNumbersByName != null)
//            assert(versionNumbersByName == attributeOperational.versionNumber)
//
//            val versionNumbersByToken = mm.getVersionNumber(
//                token = attributeOperational.token, elementType = ABACElementType.ATTRIBUTE
//            )
//            assert(versionNumbersByToken != null)
//            assert(versionNumbersByToken == attributeOperational.versionNumber)
//        }
//
//        /** get version number of deleted attributes by name or token */
//        myRun {
//            assert(
//                mm.getVersionNumber(
//                    name = attributeDeleted.name, elementType = ABACElementType.ATTRIBUTE,
//                ) == null
//            )
//            assert(
//                mm.getVersionNumber(
//                    token = attributeDeleted.token, elementType = ABACElementType.ATTRIBUTE,
//                ) == null
//            )
//        }
//
//        /** get version number of non-existing resources by name or token */
//        myRun {
//            assert(
//                mm.getVersionNumber(
//                    name = resourceNonExisting.name, elementType = ABACElementType.RESOURCE,
//                ) == null
//            )
//            assert(
//                mm.getVersionNumber(
//                    token = resourceNonExisting.token, elementType = ABACElementType.RESOURCE,
//                ) == null
//            )
//        }
//
//        /** get version number of operational resources by name or token */
//        myRun {
//            val versionNumbersByName = mm.getVersionNumber(
//                name = resourceOperational.name, elementType = ABACElementType.RESOURCE
//            )
//            assert(versionNumbersByName != null)
//            assert(versionNumbersByName == resourceOperational.versionNumber)
//
//            val versionNumbersByToken = mm.getVersionNumber(
//                token = resourceOperational.token, elementType = ABACElementType.RESOURCE
//            )
//            assert(versionNumbersByToken != null)
//            assert(versionNumbersByToken == resourceOperational.versionNumber)
//        }
//
//        /** get version number of deleted resources by name or token */
//        myRun {
//            assert(
//                mm.getVersionNumber(
//                    name = resourceDeleted.name, elementType = ABACElementType.RESOURCE,
//                ) == null
//            )
//            assert(
//                mm.getVersionNumber(
//                    token = resourceDeleted.token, elementType = ABACElementType.RESOURCE,
//                ) == null
//            )
//        }
//    }
//
//    @Test
//    open fun `get token of non-existing, incomplete, operational and deleted users, attributes, and resources by name works`() {
//        val userNonExisting = User("userNonExisting")
//        val userIncomplete = addUser("userIncomplete", )
//        val userOperational = addAndInitUser("userOperational", )
//        val userDeleted = addUser("userDeleted")
//        assert(mm.deleteUser(userDeleted.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        val attributeNonExisting = Attribute("attributeNonExisting")
//        val attributeOperational = addAttribute("attributeOperational", 2)
//        val attributeDeleted = addAttribute("attributeDeleted")
//        assert(mm.deleteAttribute(attributeDeleted.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        val resourceNonExisting = Resource(
//            name = "resourceNonExisting",
//            enforcement = Enforcement.COMBINED
//        )
//        val resourceOperational = addResource("resourceOperational", 3)
//        val resourceDeleted = addResource("resourceDeleted")
//        assert(mm.deleteResource(resourceDeleted.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        /** get token of non-existing users by name */
//        myRun {
//            assert(
//                mm.getToken(
//                    name = userNonExisting.name,
//                    type = ABACElementType.USER,
//                ) == null
//            )
//        }
//        /** get token of incomplete users by name */
//        myRun {
//            val operationalUserToken = mm.getToken(
//                name = userIncomplete.name,
//                type = ABACElementType.USER
//            )
//            assert(operationalUserToken != null)
//            assert(operationalUserToken == userIncomplete.token)
//        }
//
//        /** get token of operational users by name */
//        myRun {
//            val operationalUserToken = mm.getToken(
//                name = userOperational.name,
//                type = ABACElementType.USER
//            )
//            assert(operationalUserToken != null)
//            assert(operationalUserToken == userOperational.token)
//        }
//
//        /** get token of deleted users by name */
//        myRun {
//            val deletedUserToken = mm.getToken(
//                name = userDeleted.name,
//                type = ABACElementType.USER,
//            )
//            assert(deletedUserToken != null)
//            assert(deletedUserToken == userDeleted.token)
//        }
//
//        /** get token of non-existing attributes by name */
//        myRun {
//            assert(
//                mm.getToken(
//                    name = attributeNonExisting.name, type = ABACElementType.ATTRIBUTE,
//                ) == null
//            )
//        }
//
//        /** get token of operational attributes by name */
//        myRun {
//            val operationalAttributeToken = mm.getToken(
//                name = attributeOperational.name, type = ABACElementType.ATTRIBUTE
//            )
//            assert(operationalAttributeToken != null)
//            assert(operationalAttributeToken == attributeOperational.token)
//        }
//
//        /** get token of deleted attributes by name */
//        myRun {
//            val deletedAttributeToken = mm.getToken(
//                name = attributeDeleted.name, type = ABACElementType.ATTRIBUTE,
//            )
//            assert(deletedAttributeToken != null)
//            assert(deletedAttributeToken == attributeDeleted.token)
//        }
//
//        /** get token of non-existing resources by name */
//        myRun {
//            assert(
//                mm.getToken(
//                    name = resourceNonExisting.name, type = ABACElementType.RESOURCE,
//                ) == null
//            )
//        }
//
//        /** get token of operational resources by name */
//        myRun {
//            val operationalResourceToken = mm.getToken(
//                name = resourceOperational.name, type = ABACElementType.RESOURCE
//            )
//            assert(operationalResourceToken != null)
//            assert(operationalResourceToken == resourceOperational.token)
//        }
//
//        /** get token of deleted resources by name */
//        myRun {
//            val deletedResourceToken = mm.getToken(
//                name = resourceDeleted.name, type = ABACElementType.RESOURCE
//            )
//            assert(deletedResourceToken != null)
//            assert(deletedResourceToken == resourceDeleted.token)
//        }
//    }
//
//    @Test
//    open fun `get status of non-existing, incomplete, operational and deleted users, attributes, and resources by name or token works`() {
//
//        /** get status of admin user by name or token */
//        myRun {
//            val statusByName = mm.getStatus(name = ADMIN, type = ABACElementType.USER)
//            assert(statusByName == TupleStatus.OPERATIONAL)
//            val statusByToken = mm.getStatus(token = ADMIN, type = ABACElementType.USER)
//            assert(statusByToken == TupleStatus.OPERATIONAL)
//        }
//
//        /** get status of non-existing user by name or token */
//        myRun {
//            val statusByName = mm.getStatus(name = "not-existing", type = ABACElementType.USER)
//            assert(statusByName == null)
//            val statusByToken = mm.getStatus(token = "not-existing", type = ABACElementType.USER)
//            assert(statusByToken == null)
//        }
//
//        /** get status of existing but incomplete user by name (not by token, as incomplete users still lack a token) */
//        myRun {
//            assert(mm.addUser(Parameters.aliceUser).code == OutcomeCode.CODE_000_SUCCESS)
//            val aliceName = Parameters.aliceUser.name
//            val statusByName = mm.getStatus(name = aliceName, type = ABACElementType.USER)
//            assert(statusByName == TupleStatus.INCOMPLETE)
//        }
//
//        /** get status of operational user by name or token */
//        myRun {
//            addAndInitUser(Parameters.bobUser)
//            val bobName = Parameters.bobUser.name
//            val bobToken = Parameters.bobUser.token
//            val statusByName = mm.getStatus(name = bobName, type = ABACElementType.USER)
//            assert(statusByName == TupleStatus.OPERATIONAL)
//            val statusByToken = mm.getStatus(token = bobToken, type = ABACElementType.USER)
//            assert(statusByToken == TupleStatus.OPERATIONAL)
//        }
//
//        /** get status of deleted user by name or token */
//        myRun {
//            addAndInitUser(Parameters.carlUser)
//            assert(mm.deleteUser(Parameters.carlUser.name) == OutcomeCode.CODE_000_SUCCESS)
//            val carlName = Parameters.carlUser.name
//            val carlToken = Parameters.carlUser.token
//            val statusByName = mm.getStatus(name = carlName, type = ABACElementType.USER)
//            assert(statusByName == TupleStatus.DELETED)
//            val statusByToken = mm.getStatus(token = carlToken, type = ABACElementType.USER)
//            assert(statusByToken == TupleStatus.DELETED)
//        }
//
//        /** get status of non-existing attribute by name or token */
//        myRun {
//            val statusByName = mm.getStatus(name = "not-existing", type = ABACElementType.ATTRIBUTE)
//            assert(statusByName == null)
//            val statusByToken = mm.getStatus(token = "not-existing", type = ABACElementType.ATTRIBUTE)
//            assert(statusByToken == null)
//        }
//
//        /** get status of operational attribute by name or token */
//        myRun {
//            val attributeOperational = addAttribute("attributeOperational")
//            val attributeName = attributeOperational.name
//            val attributeToken = attributeOperational.token
//            val statusByName = mm.getStatus(name = attributeName, type = ABACElementType.ATTRIBUTE)
//            assert(statusByName == TupleStatus.OPERATIONAL)
//            val statusByToken = mm.getStatus(token = attributeToken, type = ABACElementType.ATTRIBUTE)
//            assert(statusByToken == TupleStatus.OPERATIONAL)
//        }
//
//        /** get status of deleted attribute by name or token */
//        myRun {
//            val attributeDeleted = addAttribute("attributeDeleted")
//            assert(mm.deleteAttribute(attributeDeleted.name) == OutcomeCode.CODE_000_SUCCESS)
//            val attributeName = attributeDeleted.name
//            val attributeToken = attributeDeleted.token
//            val statusByName = mm.getStatus(name = attributeName, type = ABACElementType.ATTRIBUTE)
//            assert(statusByName == TupleStatus.DELETED)
//            val statusByToken = mm.getStatus(token = attributeToken, type = ABACElementType.ATTRIBUTE)
//            assert(statusByToken == TupleStatus.DELETED)
//        }
//
//        /** get status of non-existing resource by name or token */
//        myRun {
//            val statusByName = mm.getStatus(name = "not-existing", type = ABACElementType.RESOURCE)
//            assert(statusByName == null)
//            val statusByToken = mm.getStatus(token = "not-existing", type = ABACElementType.RESOURCE)
//            assert(statusByToken == null)
//        }
//
//        /** get status of operational resource by name or token */
//        myRun {
//            val resourceOperational = addResource("resourceOperational")
//            val resourceName = resourceOperational.name
//            val resourceToken = resourceOperational.token
//            val statusByName = mm.getStatus(name = resourceName, type = ABACElementType.RESOURCE)
//            assert(statusByName == TupleStatus.OPERATIONAL)
//            val statusByToken = mm.getStatus(token = resourceToken, type = ABACElementType.RESOURCE)
//            assert(statusByToken == TupleStatus.OPERATIONAL)
//        }
//
//        /** get status of deleted resource by name or token */
//        myRun {
//            val resourceDeleted = addResource("resourceDeleted")
//            assert(mm.deleteResource(resourceDeleted.name) == OutcomeCode.CODE_000_SUCCESS)
//            val resourceName = resourceDeleted.name
//            val resourceToken = resourceDeleted.token
//            val statusByName = mm.getStatus(name = resourceName, type = ABACElementType.RESOURCE)
//            assert(statusByName == TupleStatus.DELETED)
//            val statusByToken = mm.getStatus(token = resourceToken, type = ABACElementType.RESOURCE)
//            assert(statusByToken == TupleStatus.DELETED)
//        }
//    }
//
//    @Test
//    open fun `delete operational attributes by name works`() {
//        val operational = addAttribute("operational")
//
//        /** delete operational attributes */
//        myRun {
//            assert(mm.deleteAttribute(operational.name) == OutcomeCode.CODE_000_SUCCESS)
//            val deleteAttributes = mm.getAttributes(attributeName = operational.name, status = TupleStatus.DELETED)
//            assert(deleteAttributes.size == 1)
//            assert(deleteAttributes.firstOrNull()!!.name == operational.name)
//        }
//    }
//
//    @Test
//    open fun `delete non-existing and deleted attributes by name or blank name fails`() {
//        val nonExisting = Attribute("nonExisting")
//        val deleted = addAttribute("operational")
//        assert(mm.deleteAttribute(deleted.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        /** delete non-existing attributes */
//        myRun {
//            assert(mm.deleteAttribute(nonExisting.name) == OutcomeCode.CODE_066_ATTRIBUTE_NOT_FOUND)
//        }
//
//        /** delete deleted attributes */
//        myRun {
//            assert(mm.deleteAttribute(deleted.name) == OutcomeCode.CODE_067_ATTRIBUTE_WAS_DELETED)
//        }
//
//        /** delete attributes by blank name */
//        myRun {
//            assert(mm.deleteAttribute("") == OutcomeCode.CODE_020_INVALID_PARAMETER)
//        }
//    }
//
//    @Test
//    open fun `delete the admin attribute by name fails`() {
//        /** delete the admin attribute */
//        myRun {
//            assert(mm.deleteAttribute(ADMIN) == OutcomeCode.CODE_022_ADMIN_CANNOT_BE_MODIFIED)
//        }
//    }
//
//    @Test
//    open fun `delete operational resources by name works`() {
//        val operational = addResource("operational")
//
//        /** delete operational resources */
//        myRun {
//            assert(mm.deleteResource(operational.name) == OutcomeCode.CODE_000_SUCCESS)
//            val deleteResources = mm.getResources(resourceName = operational.name, status = TupleStatus.DELETED)
//            assert(deleteResources.size == 1)
//            assert(deleteResources.firstOrNull()!!.name == operational.name)
//        }
//    }
//
//    @Test
//    open fun `delete non-existing and deleted resources by name or blank name fails`() {
//        val nonExisting = Resource(
//            name = "nonExisting",
//            enforcement = Enforcement.COMBINED
//        )
//        val deleted = addResource("operational")
//        assert(mm.deleteResource(deleted.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        /** delete non-existing resources */
//        myRun {
//            assert(mm.deleteResource(nonExisting.name) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND)
//        }
//
//        /** delete deleted resources */
//        myRun {
//            assert(mm.deleteResource(deleted.name) == OutcomeCode.CODE_015_RESOURCE_WAS_DELETED)
//        }
//
//        /** delete resources by blank name */
//        myRun {
//            assert(mm.deleteResource("") == OutcomeCode.CODE_020_INVALID_PARAMETER)
//        }
//    }
//
//    @Test
//    open fun `delete existing user-attribute assignments by user or attribute name works`() {
//        val aliceName = Parameters.aliceUser.name
//        addAndInitUser(Parameters.aliceUser)
//        val bobName = Parameters.bobUser.name
//        addAndInitUser(Parameters.bobUser)
//        val adultAttribute = addAttribute("over18")
//        val adultAttributeName = adultAttribute.name
//
//        addUserAttribute(aliceName, adultAttributeName)
//        addUserAttribute(bobName, adultAttributeName)
//
//        /** delete existing user-attribute assignments by username */
//        myRun {
//            assert(mm.getUsersAttributes(attributeName = adultAttributeName).size == 2)
//            assert(mm.deleteUsersAttributes(username = aliceName) == OutcomeCode.CODE_000_SUCCESS)
//        }
//        assert(mm.getUsersAttributes(attributeName = adultAttributeName).size == 1)
//
//        /** delete existing user-attribute assignments by attribute name */
//        myRun {
//            assert(mm.deleteUsersAttributes(attributeName = adultAttributeName) == OutcomeCode.CODE_000_SUCCESS)
//        }
//        assert(mm.getUsersAttributes(attributeName = adultAttributeName).size == 0)
//    }
//
//    @Test
//    open fun `delete non-existing user-attribute assignments by user or attribute name fails`() {
//
//        /** delete non-existing user-attribute assignments by user or attribute name */
//        myRun {
//            assert(mm.deleteUsersAttributes(username = "non-existing") == OutcomeCode.CODE_004_USER_NOT_FOUND)
//            assert(mm.deleteUsersAttributes(attributeName = "non-existing") == OutcomeCode.CODE_066_ATTRIBUTE_NOT_FOUND)
//        }
//    }
//
//    @Test
//    open fun `delete existing access structure-permission assignments by resource name and permission works`() {
//        val exam = addResource("exam")
//        addAccessStructurePermission("From:Alice", exam, Operation.READ)
//        addAccessStructurePermission("From:Bob", exam, Operation.READWRITE)
//
//        /** delete existing access structure-permission assignments by resource name and permission */
//        myRun {
//            assert(mm.getAccessStructuresPermissions(resourceName = exam.name).size == 2)
//            assert(mm.deleteAccessStructuresPermissions(resourceName = exam.name, Operation.READWRITE) == OutcomeCode.CODE_000_SUCCESS)
//            assert(mm.getAccessStructuresPermissions(resourceName = exam.name).size == 1)
//            assert(mm.deleteAccessStructuresPermissions(resourceName = exam.name, Operation.READ) == OutcomeCode.CODE_000_SUCCESS)
//            assert(mm.getAccessStructuresPermissions(resourceName = exam.name).size == 0)
//        }
//    }
//
//    @Test
//    open fun `delete non-existing access structure-permission assignments by resource name fails`() {
//
//        /** delete non-existing access structure-permission assignments */
//        myRun {
//            assert(mm.deleteAccessStructuresPermissions(resourceName = "non-existing") == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND)
//        }
//    }
//
//    @Test
//    open fun `update symmetric encryption key version number and token for operational resource works`() {
//        val exam = addResource("exam", 1)
//        val examToken = exam.token
//
//        /** update symmetric encryption key version number and token for operational resource */
//        myRun {
//            assert(
//                mm.getVersionNumber(name = exam.name, elementType = ABACElementType.RESOURCE) == 1
//            )
//            assert(
//                mm.getToken(name = exam.name, type = ABACElementType.RESOURCE) == examToken
//            )
//
//            val newExamToken = Element.generateRandomToken()
//            assert(mm.updateResourceTokenAndVersionNumber(
//                resourceName = exam.name,
//                oldResourceToken = examToken,
//                newResourceToken = newExamToken,
//                newResourceVersionNumber = 2
//            ) == OutcomeCode.CODE_000_SUCCESS)
//
//            assert(
//                mm.getVersionNumber(name = exam.name, elementType = ABACElementType.RESOURCE) == 2
//            )
//            assert(
//                mm.getToken(name = exam.name, type = ABACElementType.RESOURCE) == newExamToken
//            )
//        }
//    }
//
//    @Test
//    open fun `update symmetric encryption key version number and token for non-existing or deleted resource fails`() {
//        val exam = addResource("exam", 1)
//        val examToken = exam.token
//        assert(mm.deleteResource(exam.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        /** update symmetric encryption key version number and token for non-existing resource */
//        myRun {
//            assert(mm.updateResourceTokenAndVersionNumber(
//                resourceName = "non-existing",
//                oldResourceToken = "non-existing-token",
//                newResourceToken = "newExamToken",
//                newResourceVersionNumber = 2
//            ) == OutcomeCode.CODE_006_RESOURCE_NOT_FOUND)
//        }
//
//        /** update symmetric encryption key version number and token for deleted resource */
//        myRun {
//            assert(mm.updateResourceTokenAndVersionNumber(
//                resourceName = exam.name,
//                oldResourceToken = examToken,
//                newResourceToken = "newExamToken",
//                newResourceVersionNumber = 2
//            ) == OutcomeCode.CODE_015_RESOURCE_WAS_DELETED)
//        }
//    }
//
//    @Test
//    open fun `update existing attribute token and version number works`() {
//        val tall = addAttribute("tall")
//
//        /** update existing attribute version number */
//        myRun {
//            val getBefore = mm.getAttributes(attributeName = tall.name)
//            assert(getBefore.filter { it.name == tall.name }.size == 1)
//            assert(getBefore.first { it.name == tall.name }.token == tall.token)
//            assert(getBefore.first { it.name == tall.name }.versionNumber == 1)
//            val newToken = Element.generateRandomToken()
//
//            assert(mm.updateAttributeTokenAndVersionNumber(
//                attributeName = tall.name,
//                oldAttributeToken = tall.token,
//                newAttributeToken = newToken,
//                newVersionNumber = 2
//            ) == OutcomeCode.CODE_000_SUCCESS)
//
//            val getAfter = mm.getAttributes(attributeName = tall.name)
//            assert(getAfter.filter { it.name == tall.name }.size == 1)
//            assert(getAfter.first { it.name == tall.name }.token == newToken)
//            assert(getAfter.first { it.name == tall.name }.versionNumber == 2)
//        }
//    }
//
//    @Test
//    open fun `update non-existing or deleted attribute version number fails`() {
//        val tall = addAttribute("tall")
//        assert(mm.deleteAttribute(tall.name) == OutcomeCode.CODE_000_SUCCESS)
//
//        /** update non-existing attribute version number */
//        myRun {
//            assert(mm.updateAttributeTokenAndVersionNumber(
//                attributeName = "non-existing",
//                oldAttributeToken = "non-existing",
//                newAttributeToken = "non-existing",
//                newVersionNumber = 2
//            ) == OutcomeCode.CODE_066_ATTRIBUTE_NOT_FOUND)
//
//        }
//        /** update deleted attribute version number */
//        myRun {
//            assert(mm.updateAttributeTokenAndVersionNumber(
//                attributeName = tall.name,
//                oldAttributeToken = tall.token,
//                newAttributeToken = tall.token,
//                newVersionNumber = 2
//            ) == OutcomeCode.CODE_067_ATTRIBUTE_WAS_DELETED)
//        }
//    }
//
//    @Test
//    open fun `update existing user ABE key works`() {
//        // TODO
//    }
//
//    @Test
//    open fun `update not-existing or incomplete or deleted user ABE key fails`() {
//        // TODO
//    }
//
//    @Test
//    open fun `update existing access structure-permission assignment works`() {
//        val exam = addResource("exam")
//        val examAccessStructurePermission = addAccessStructurePermission("From:Bob", exam)
//
//        /** update existing access structure-permission assignment */
//        myRun {
//            val getBefore = mm.getAccessStructuresPermissions(resourceName = exam.name)
//            assert(getBefore.filter { it.resourceName == exam.name }.size == 1)
//            assert(getBefore.firstOrNull { it.resourceName == exam.name }!!.accessStructure == "From:Bob")
//
//            val newResourceToken = Element.generateRandomToken()
//            val newEncryptedSymKey = EncryptedSymKey(
//                key = "newEncryptedSymKey".toByteArray()
//            )
//            val newAccessStructurePermission = AccessStructurePermission(
//                resourceName = exam.name,
//                resourceToken = newResourceToken,
//                accessStructure = "Prize < 10",
//                operation = Operation.READ,
//                resourceVersionNumber = exam.versionNumber + 1,
//                encryptedSymKey = newEncryptedSymKey,
//            )
//            val signature = Parameters.cryptoPKEObject.createSignature(
//                bytes = newAccessStructurePermission.getBytesForSignature(),
//                signingKey = Parameters.adminAsymSigKeys.private
//            )
//            newAccessStructurePermission.updateSignature(
//                newSignature = signature,
//                newSigner = ADMIN
//            )
//            /**
//             * This is needed to update the token in the
//             * resource table (the token is a foreign key
//             * in the access structure-permission assignments
//             * table)
//             */
//            assert(mm.updateResourceTokenAndVersionNumber(
//                resourceName = exam.name,
//                oldResourceToken = exam.token,
//                newResourceToken = newResourceToken,
//                newResourceVersionNumber = (exam.versionNumber + 1)
//            ) == OutcomeCode.CODE_000_SUCCESS)
//            assert(mm.updateAccessStructurePermission(newAccessStructurePermission) == OutcomeCode.CODE_000_SUCCESS)
//
//            val getAfter = mm.getAccessStructuresPermissions(resourceName = exam.name)
//            val after = getAfter.first { it.resourceName == exam.name }
//            assert(getAfter.filter { it.resourceName == exam.name }.size == 1)
//            assert(after.resourceToken == newResourceToken)
//            assert(after.accessStructure == "Prize < 10")
//            assert(after.encryptedSymKey!!.key.contentEquals(newEncryptedSymKey.key))
//            assert(after.resourceVersionNumber == exam.versionNumber + 1)
//        }
//    }
//
//    @Test
//    open fun `update non-existing access structure-permission assignment fails`() {
//        /** update non-existing access structure-permission assignment */
//        myRun {
//            val nonExistingAccessStructurePermission = AccessStructurePermission(
//                resourceName = "non-existing-resource",
//                resourceToken = "non-existing-token",
//                accessStructure = "Prize < 10",
//                operation = Operation.READWRITE,
//                resourceVersionNumber = 1,
//                encryptedSymKey = EncryptedSymKey(
//                    key = "non-existing-encryptingSymKey".toByteArray()
//                ),
//            )
//            val signature = Parameters.cryptoPKEObject.createSignature(
//                bytes = nonExistingAccessStructurePermission.getBytesForSignature(),
//                signingKey = Parameters.adminAsymSigKeys.private
//            )
//            nonExistingAccessStructurePermission.updateSignature(
//                newSignature = signature,
//                newSigner = ADMIN
//            )
//            assert(mm.updateAccessStructurePermission(nonExistingAccessStructurePermission) == OutcomeCode.CODE_071_ACCESS_STRUCTURE_PERMISSION_ASSIGNMENT_NOT_FOUND)
//        }
//    }
//
//    // TODO test:
//    //  - updateAttributeTokenAndVersionNumber
//    //  - updateUserABEKey
//    //  - getUserABEKey
//    // TODO test paginazione (limit e offset)
//
//
//
//    private fun addAttribute(
//        attributeName: String,
//        attributeVersionNumber: Int = 1,
//        restoreIfDeleted: Boolean = false
//    ): Attribute {
//        val newAttribute = TestUtilities.createAttribute(attributeName, attributeVersionNumber)
//        assert(mm.addAttribute(newAttribute, restoreIfDeleted) == OutcomeCode.CODE_000_SUCCESS)
//        assertUnlockAndLock(mm)
//        return newAttribute
//    }
//
//    private fun addResource(
//        resourceName: String,
//        symKeyVersionNumber: Int = 1
//    ): Resource {
//        val newResource = TestUtilities.createResource(resourceName, symKeyVersionNumber, enforcement = Enforcement.COMBINED)
//        assert(mm.addResource(newResource) == OutcomeCode.CODE_000_SUCCESS)
//        assertUnlockAndLock(mm)
//        return newResource
//    }
//
//    private fun addUserAttribute(
//        username: String,
//        attributeName: String,
//        attributeValue: String? = null,
//    ): UserAttribute {
//        val userAttribute = createUserAttribute(
//            username = username,
//            attributeName = attributeName,
//            attributeValue = attributeValue
//        )
//        assert(mm.addUsersAttributes(hashSetOf(userAttribute)) == OutcomeCode.CODE_000_SUCCESS)
//        assertUnlockAndLock(mm)
//        return userAttribute
//    }
//
//    private fun addAccessStructurePermission(
//        accessStructure: String,
//        resource: Resource,
//        operation: Operation = Operation.READ
//    ): AccessStructurePermission {
//        val accessStructurePermission = TestUtilities.createAccessStructurePermission(
//            accessStructure = accessStructure,
//            resource = resource,
//            operation = operation
//        )
//        assert(mm.addAccessStructuresPermissions(hashSetOf(accessStructurePermission)) == OutcomeCode.CODE_000_SUCCESS)
//        assertUnlockAndLock(mm)
//        return accessStructurePermission
//    }
//}
