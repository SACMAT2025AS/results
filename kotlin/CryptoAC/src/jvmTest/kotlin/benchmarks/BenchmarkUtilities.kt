//package benchmarks
//
//import benchmarks.Parameters.adminUser
//import benchmarks.Parameters.cryptoPKEObject
//import benchmarks.Parameters.cryptoSKEObject
//import benchmarks.Parameters.dmServiceCryptoACParameters
//import benchmarks.Parameters.mmServiceRBACMySQLParameters
//import benchmarks.Parameters.mmServiceABACMySQLParameters
//import benchmarks.Parameters.mmServiceRBACRedisParameters
//import benchmarks.Parameters.opaBaseAPI
//import benchmarks.Parameters.xacmlAuthzForceBaseAPI
//import cryptoac.*
//import cryptoac.Constants.ADMIN
//import cryptoac.OutcomeCode.*
//import cryptoac.ac.ACFactory
//import cryptoac.ac.dynsec.ACServiceRBACDynSec
//import cryptoac.ac.xacmlauthzforce.ACServiceRBACXACMLAuthzForce
//import cryptoac.ac.xacmlauthzforce.XACMLPolicySet
//import cryptoac.ac.xacmlauthzforce.XACMLPolicySet.Companion.getRoleNameFromPolicySetID
//import cryptoac.core.*
//import cryptoac.core.CoreType.*
//import cryptoac.core.mqtt.CoreCACRBACMQTT
//import cryptoac.tuple.*
//import cryptoac.crypto.AsymKeys
//import cryptoac.crypto.AsymKeysType
//import cryptoac.crypto.PrivateKeyCryptoAC
//import cryptoac.dm.DMFactory.Companion.getDM
//import cryptoac.dm.cryptoac.DMServiceCryptoAC
//import cryptoac.dm.mqtt.DMServiceMQTT
//import cryptoac.mm.MMFactory.Companion.getMM
//import cryptoac.mm.MMServiceCACABAC
//import cryptoac.mm.mysql.*
//import cryptoac.mm.mysql.MMServiceCACABACMySQL.Companion.accessStructuresPermissionsTable
//import cryptoac.mm.mysql.MMServiceCACABACMySQL.Companion.usersAttributesTable
//import cryptoac.mm.mysql.MMServiceCACABACMySQL.Companion.attributesTable
//import cryptoac.mm.mysql.MMServiceCACABACMySQL.Companion.deletedAttributesTable
//import cryptoac.mm.mysql.MMServiceCACABACMySQL.Companion.masterPublicKeyTable
//import cryptoac.mm.mysql.MMServiceCACRBACMySQL.Companion.deletedResourcesTable
//import cryptoac.mm.mysql.MMServiceCACRBACMySQL.Companion.deletedRolesTable
//import cryptoac.mm.mysql.MMServiceCACRBACMySQL.Companion.deletedUsersTable
//import cryptoac.mm.mysql.MMServiceCACRBACMySQL.Companion.rolesPermissionsTable
//import cryptoac.mm.mysql.MMServiceCACRBACMySQL.Companion.resourceNameColumn
//import cryptoac.mm.mysql.MMServiceCACRBACMySQL.Companion.resourcesTable
//import cryptoac.mm.mysql.MMServiceCACRBACMySQL.Companion.usersRolesTable
//import cryptoac.mm.mysql.MMServiceCACRBACMySQL.Companion.rolesTable
//import cryptoac.mm.mysql.MMServiceCACRBACMySQL.Companion.usernameColumn
//import cryptoac.mm.mysql.MMServiceCACRBACMySQL.Companion.usersTable
//import cryptoac.mm.redis.MMServiceCACRBACRedis
//import io.ktor.client.*
//import io.ktor.client.engine.cio.*
//import io.ktor.client.plugins.contentnegotiation.*
//import io.ktor.client.plugins.cookies.*
//import io.ktor.client.request.*
//import io.ktor.http.*
//import io.ktor.serialization.kotlinx.json.*
//import io.ktor.serialization.kotlinx.xml.*
//import kotlinx.coroutines.runBlocking
//import kotlinx.serialization.encodeToString
//import mu.KotlinLogging
//import nl.adaptivity.xmlutil.serialization.XML
//import redis.clients.jedis.JedisPool
//import redis.clients.jedis.JedisPoolConfig
//import java.io.*
//import java.security.cert.X509Certificate
//import java.util.*
//import javax.net.ssl.X509TrustManager
//import kotlin.collections.HashSet
//
//private val logger = KotlinLogging.logger {}
//
//class TestUtilities {
//    companion object {
//
//        fun generateRandomStrings(size: Int, number: Int): List<String> {
//            val names = java.util.HashSet<String>()
//            val bytes = ByteArray(size)
//            while (names.size < number) {
//                Random().nextBytes(bytes)
//                names.add(bytes.encodeBase64())
//            }
//            return names.toList()
//        }
//
//        fun generateRandomStreams(sizeInBytes: Int, number: Int): List<InputStream> {
//            val streams = java.util.HashSet<InputStream>()
//            val bytes = ByteArray(sizeInBytes)
//            while (streams.size < number) {
//                Random().nextBytes(bytes)
//                streams.add(bytes.inputStream())
//            }
//            return streams.toList()
//        }
//
//
//        class RandomNamesState {
//            private var names = generateRandomStrings(size = 50, number = 100 + 1000)
//            private var iterator = names.iterator()
//            fun getNextName(): String = iterator.next()
//            fun reset() { iterator = names.iterator() }
//        }
//
//        class RandomStreamState {
//            private var streams = generateRandomStreams(sizeInBytes = 1024, number = 5 + 20)
//            private var iterator = streams.iterator()
//            fun getNextStream(): InputStream = iterator.next()
//            fun reset() { iterator = streams.iterator() }
//        }
//
//        fun addAndInitUser(core: Core, username: String): OutcomeCode {
//            val userCore = addUser(core, username)
//            return userCore.initUser()
//        }
//
//
//        val coreCACRBACMQTTNoACNoTLS: CoreCACRBACMQTT = CoreFactory.getCore(
//            Parameters.adminCoreCACRBACMQTTParametersNoMosquittoNoTLS
//        ) as CoreCACRBACMQTT
//        val coreCACRBACMQTTNoACWithTLS: CoreCACRBACMQTT = CoreFactory.getCore(
//            Parameters.adminCoreCACRBACMQTTParametersNoMosquittoWithTLS
//        ) as CoreCACRBACMQTT
//
//        val dir = File("../docs/source/gettingstarted/installation/")
//
//        fun deleteLocalCryptoACUsersProfiles() {
//            logger.warn { "Resetting Local CryptoAC" }
//            File(USERS_PROFILE_DIRECTORY_PATH).deleteRecursively()
//        }
//
//        fun resetDMServiceRBACCryptoAC() {
//            logger.warn { "Resetting DM Service RBAC CryptoAC" }
//
//            val mm = MMServiceCACRBACMySQL(mmServiceRBACMySQLParameters)
//            assert(mm.lock() == CODE_000_SUCCESS)
//            val resources = HashSet<String>()
//            mm.connection!!.prepareStatement(
//                "SELECT $resourceNameColumn FROM $resourcesTable"
//            ).use {
//                val resourceNames = it.executeQuery()
//                while (resourceNames.next()) {
//                    resources.add(resourceNames.getString(1))
//                }
//            }
//            assert(mm.unlock() == CODE_000_SUCCESS)
//
//            val dm = DMServiceCryptoAC(dmServiceCryptoACParameters)
//            assert(dm.lock() == CODE_000_SUCCESS)
//            resources.forEach {
//                logger.warn { "Deleting resource $it from the DM" }
//                assert(dm.deleteResource(it) == CODE_000_SUCCESS)
//            }
//            assert(dm.unlock() == CODE_000_SUCCESS)
//        }
//
//        fun resetDMServiceABACCryptoAC() {
////            logger.warn { "Resetting DM Service ABAC CryptoAC" }
////
////            val mm = MMServiceCACABACMySQL(mmServiceMySQLParameters)
////            assert(mm.lock() == OutcomeCode.CODE_000_SUCCESS)
////            val resources = HashSet<String>()
////            mm.connection!!.prepareStatement(
////                "SELECT $resourceNameColumn FROM $resourcesTable"
////            ).use {
////                val resourceNames = it.executeQuery()
////                while (resourceNames.next()) {
////                    resources.add(resourceNames.getString(1))
////                }
////            }
////            assert(mm.unlock() == OutcomeCode.CODE_000_SUCCESS)
////
////            val dm = DMServiceABACCryptoAC(dmServiceCryptoACParameters)
////            assert(dm.lock() == OutcomeCode.CODE_000_SUCCESS)
////            resources.forEach {
////                logger.warn { "Deleting resource $it from the DM" }
////                assert(dm.deleteResource(it) == OutcomeCode.CODE_000_SUCCESS)
////            }
////            assert(dm.unlock() == OutcomeCode.CODE_000_SUCCESS)
//        }
//
//        fun resetDMServiceRBACMQTT(
//            dm: DMServiceMQTT? = getDM(
//                dmParameters = Parameters.dmServiceMQTTWithACParameters
//            ) as DMServiceMQTT,
//        ) {
//            logger.warn { "Resetting DM Service RBAC MQTT" }
//            dm as DMServiceMQTT
//
//            val mm = MMServiceCACRBACRedis(mmServiceRBACRedisParameters)
//            assert(mm.lock() == CODE_000_SUCCESS)
//            val resources = mm.getResources().filter { it.name != ADMIN }.map { it.name }
//            assert(mm.unlock() == CODE_000_SUCCESS)
//
//            assert(dm.lock() == CODE_000_SUCCESS)
//            resources.forEach {
//                logger.warn { "Deleting resource $it from the DM" }
//                assert(dm.deleteResource(it) == CODE_000_SUCCESS)
//            }
//            assert(dm.unlock() == CODE_000_SUCCESS)
//        }
//
//
//        fun resetDMServiceABACMQTT(
//            dm: DMServiceMQTT? = getDM(
//                dmParameters = Parameters.dmServiceMQTTNoACParameters
//            ) as DMServiceMQTT,
//            mm: MMServiceCACABAC = getMM(
//                mmParameters = mmServiceABACMySQLParameters
//            ) as MMServiceCACABAC
//        ) {
//            logger.warn { "Resetting DM Service ABAC MQTT" }
//            dm as DMServiceMQTT
//
//            assert(mm.lock() == CODE_000_SUCCESS)
//            val resources = mm.getResources().filter { it.name != ADMIN }.map { it.name }
//            assert(mm.unlock() == CODE_000_SUCCESS)
//
//            assert(dm.lock() == CODE_000_SUCCESS)
//            resources.forEach {
//                logger.warn { "Deleting resource $it from the DM" }
//                assert(dm.deleteResource(it) == CODE_000_SUCCESS)
//            }
//            assert(dm.unlock() == CODE_000_SUCCESS)
//        }
//
//        fun resetACServiceRBACOPA() {
//            logger.warn { "Resetting OPA" }
//
//            runBlocking {
//
//                HttpClient(CIO) {
//                    // TODO configure this, as for now the client accepts all certificates
//                    engine {
//                        https {
//                            trustManager = object: X509TrustManager {
//                                override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) { }
//                                override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) { }
//                                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
//                            }
//                        }
//                    }
//                    install(ContentNegotiation) {
//                        json(json = myJson)
//                    }
//                    expectSuccess = false
//                    /** To accept all cookies */
//                    install(HttpCookies)
//                }.use {
//                    assert(it.delete {
//                        url("${API.HTTPS}${opaBaseAPI}${API.OPA_RBAC_POLICY}")
//                    }.status == HttpStatusCode.OK)
//                    assert(
//                        it.delete {
//                            url("${API.HTTPS}${opaBaseAPI}${API.OPA_RBAC_DATA}")
//                        }.status ==
//                                HttpStatusCode.NoContent
//                    )
//                }
//            }
//        }
//
//        fun resetACServiceRBACXACMLAuthzForce() {
//            logger.warn { "Resetting AuthzForce XACML Server" }
//
//            val ac = ACServiceRBACXACMLAuthzForce(Parameters.acServiceRBACXACMLAuthzForceParameters)
//            assert(ac.lock() == CODE_000_SUCCESS)
//            assert(ac.getDomainCryptoACID().code == CODE_000_SUCCESS)
//
//            val policySetRootCode = ac.getRootPolicySet()
//            assert(policySetRootCode.code == CODE_000_SUCCESS)
//            val policySetRoot = policySetRootCode.xacmlPolicySetRoot!!
//
//            val policySetsCode = ac.getPolicySetsInDomain()
//            assert(policySetsCode.code == CODE_000_SUCCESS)
//
//            policySetsCode.policySetsLinks!!.forEach {
//                if (it.href != XACMLPolicySet.rootDefaultPolicySetId && it.href != XACMLPolicySet.rootPolicySetIdCryptoAC) {
//                    policySetRoot.removeRPSandREPS(
//                        roleName = getRoleNameFromPolicySetID(it.href)
//                    )
//                }
//            }
//            policySetRoot.updatePolicySetVersionNumber()
//            assert(ac.addOrUpdatePolicySet(
//                policySet = XML{ }.encodeToString(policySetRoot)
//            ) == CODE_000_SUCCESS)
//
//
//            policySetsCode.policySetsLinks.forEach {
//                if (it.href != XACMLPolicySet.rootDefaultPolicySetId && it.href != XACMLPolicySet.rootPolicySetIdCryptoAC) {
//                    ac.deletePolicySetByID(it.href)
//                }
//            }
//
//            runBlocking {
//                HttpClient(CIO) {
//                    // TODO configure this, as for now the client accepts all certificates
//                    engine {
//                        https {
//                            trustManager = object : X509TrustManager {
//                                override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
//                                override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
//                                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
//                            }
//                        }
//                    }
//                    install(ContentNegotiation) {
//                        xml(format = XML { })
//                    }
//                    expectSuccess = false
//                    /** To accept all cookies */
//                    install(HttpCookies)
//                }.use {
//                    val xacmlDomainsURL = "${API.HTTPS}${xacmlAuthzForceBaseAPI}${API.XACML_AUTHZFORCE}${ac.getDomainCryptoACID().string}"
//                    val xacmlResponse = it.delete {
//                        header("Accept", "application/xml")
//                        header("Content-Type", "application/xml;charset=UTF-8")
//                        url(xacmlDomainsURL)
//                    }
//                    assert(xacmlResponse.status == HttpStatusCode.OK)
//                }
//            }
//
//            assert(ac.lock() == CODE_000_SUCCESS)
//        }
//
//        fun resetMMServiceRBACMySQL() {
//            logger.warn { "Resetting MM Service RBAC MySQL" }
//
//            val mm = MMServiceCACRBACMySQL(mmServiceRBACMySQLParameters)
//            assert(mm.lock() == CODE_000_SUCCESS)
//
//            val users = HashSet<String>()
//            mm.connection!!.prepareStatement(
//                "SELECT $usernameColumn FROM $usersTable"
//            ).use {
//                val usernames = it.executeQuery()
//                while (usernames.next()) {
//                    users.add(usernames.getString(1))
//                }
//                users.remove(ADMIN)
//            }
//
//            val deleteFrom = "DELETE FROM"
//            mm.connection!!.prepareStatement("$deleteFrom $tuplesRolePermissionTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $tuplesUserRoleTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $usersTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $deletedUsersTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $rolesTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $deletedRolesTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $resourcesTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $deletedResourcesTable")
//                .use { it.executeUpdate() }
//
//            if (users.isNotEmpty()) {
//                val u = StringBuilder("DROP USER IF EXISTS ")
//                users.forEach { u.append("'").append(it).append("', ") }
//                u.delete(u.length - 2, u.length)
//                mm.connection!!.prepareStatement(u.toString()).use { it.executeUpdate() }
//            }
//
//            assert(mm.unlock() == CODE_000_SUCCESS)
//        }
//
//        fun resetMMServiceABACMySQL() {
//            logger.warn { "Resetting MM Service ABAC MySQL" }
//
//            val mm = MMServiceCACABACMySQL(mmServiceABACMySQLParameters)
//            assert(mm.lock() == CODE_000_SUCCESS)
//
//            val users = HashSet<String>()
//            mm.connection!!.prepareStatement(
//                "SELECT $usernameColumn FROM $usersTable"
//            ).use {
//                val usernames = it.executeQuery()
//                while (usernames.next()) {
//                    users.add(usernames.getString(1))
//                }
//                users.remove(ADMIN)
//            }
//
//            val deleteFrom = "DELETE FROM"
//            mm.connection!!.prepareStatement("$deleteFrom $masterPublicKeyTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $tuplesAccessStructurePermissionTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $tuplesUserAttributeTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $usersTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $deletedUsersTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $attributesTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $deletedAttributesTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $resourcesTable")
//                .use { it.executeUpdate() }
//            mm.connection!!.prepareStatement("$deleteFrom $deletedResourcesTable")
//                .use { it.executeUpdate() }
//
//            if (users.isNotEmpty()) {
//                val u = StringBuilder("DROP USER IF EXISTS ")
//                users.forEach { u.append("'").append(it).append("', ") }
//                u.delete(u.length - 2, u.length)
//                mm.connection!!.prepareStatement(u.toString()).use { it.executeUpdate() }
//            }
//
//            assert(mm.unlock() == CODE_000_SUCCESS)
//        }
//
//        fun resetMMServiceRBACRedis() {
//            logger.warn { "Resetting MM Service RBAC Redis" }
//            val pool = JedisPool(JedisPoolConfig(), mmServiceRBACRedisParameters.url, mmServiceRBACRedisParameters.port)
//            pool.resource.use { jedis ->
//                jedis.auth("default", mmServiceRBACRedisParameters.password)
//                jedis.keys("*").forEach {
//                    jedis.del(it)
//                }
//            }
//        }
//
//        fun addUser(core: Core, username: String): Core {
//            val addUserResult = core.addUser(username)
//            assert(addUserResult.code == CODE_000_SUCCESS)
//            val userParameters = addUserResult.parameters!!
//            val asymEncKeys = cryptoPKEObject.generateAsymEncKeys(
//                keyID = "${username}PKE" // TODO check this ID is fine
//            )
//            val asymSigKeys = cryptoPKEObject.generateAsymSigKeys(
//                keyID = "${username}PKSIG" // TODO check this ID is fine
//            )
//            val user = User(
//                name = username,
//                isAdmin = false,
//                asymEncKeys = AsymKeys(
//                    public = asymEncKeys.public.encoded.encodeBase64(),
//                    private = asymEncKeys.private.encoded.encodeBase64(),
//                    keysType = AsymKeysType.ENC
//                ),
//                asymSigKeys = AsymKeys(
//                    public = asymSigKeys.public.encoded.encodeBase64(),
//                    private = asymSigKeys.private.encoded.encodeBase64(),
//                    keysType = AsymKeysType.SIG
//                )
//            )
//            val userCore = when (core.coreParameters.coreType) {
//                RBAC_FILES -> CoreFactory.getCore(
//                    coreParameters = CoreParametersRBAC(
//                        user = user,
//                        coreType = RBAC_FILES,
//                        cryptoType = Parameters.cryptoType,
//                        mmServiceParameters = userParameters.mmServiceParameters,
//                        rmServiceParameters = userParameters.rmServiceParameters,
//                        dmServiceParameters = userParameters.dmServiceParameters,
//                        acServiceParameters = userParameters.acServiceParameters,
//                    ),
//                )
//                RBAC_MQTT -> CoreFactory.getCore(
//                    coreParameters = CoreParametersRBAC(
//                        user = user,
//                        coreType = RBAC_MQTT,
//                        cryptoType = Parameters.cryptoType,
//                        mmServiceParameters = userParameters.mmServiceParameters,
//                        dmServiceParameters = userParameters.dmServiceParameters,
//                        rmServiceParameters = null,
//                        acServiceParameters = userParameters.acServiceParameters,
//                    ),
//                )
//                ABAC_FILES -> CoreFactory.getCore(
//                    coreParameters = CoreParametersABAC(
//                        user = user,
//                        coreType = ABAC_FILES,
//                        cryptoType = Parameters.cryptoType,
//                        cryptoABEType = Parameters.cryptoABEType,
//                        rmServiceParameters = userParameters.rmServiceParameters,
//                        mmServiceParameters = userParameters.mmServiceParameters,
//                        dmServiceParameters = userParameters.dmServiceParameters,
//                        acServiceParameters = userParameters.acServiceParameters,
//                        abePublicParameters = Parameters.abePublicParameters
//                    ),
//                )
//                ABAC_MQTT -> CoreFactory.getCore(
//                    coreParameters = CoreParametersABAC(
//                        user = user,
//                        coreType = ABAC_MQTT,
//                        cryptoType = Parameters.cryptoType,
//                        cryptoABEType = Parameters.cryptoABEType,
//                        rmServiceParameters = null,
//                        mmServiceParameters = userParameters.mmServiceParameters,
//                        dmServiceParameters = userParameters.dmServiceParameters,
//                        acServiceParameters = userParameters.acServiceParameters,
//                        abePublicParameters = Parameters.abePublicParameters
//                    ),
//                )
//            }
//            return userCore
//        }
//
//        fun getKtorClientJetty(): HttpClient {
//            return HttpClient(io.ktor.client.engine.jetty.Jetty) {
//                expectSuccess = false
//                install(HttpCookies) /** To accept all cookies */
//                install(ContentNegotiation) {
//                    json(json = myJson)
//                }
//                engine {
//                    sslContextFactory.isTrustAll = true
//                }
//            }
//        }
//
//        fun createUser(
//            username: String,
//            status: ElementStatus = ElementStatus.INCOMPLETE,
//            isAdmin: Boolean = false
//        ): User {
//            val asymEncKeys = cryptoPKEObject.generateAsymEncKeys(
//                keyID = "${username}PKE" // TODO check this ID is fine
//            )
//            val asymSigKeys = cryptoPKEObject.generateAsymSigKeys(
//                keyID = "${username}PKSIG" // TODO check this ID is fine
//            )
//            return User(
//                name = username,
//                status = status,
//                isAdmin = isAdmin,
//                asymEncKeys = AsymKeys(
//                    private = asymEncKeys.private.encoded.encodeBase64(),
//                    public = asymEncKeys.public.encoded.encodeBase64(),
//                    AsymKeysType.ENC
//                ),
//                asymSigKeys = AsymKeys(
//                    private = asymSigKeys.private.encoded.encodeBase64(),
//                    public = asymSigKeys.public.encoded.encodeBase64(),
//                    AsymKeysType.SIG
//                ),
//            )
//        }
//
//        fun createRole(roleName: String, roleVersionNumber: Int = 1): Role {
//            val asymEncKeys = cryptoPKEObject.generateAsymEncKeys(
//                keyID = "${roleName}PKE" // TODO check this ID is fine
//            )
//            val asymSigKeys = cryptoPKEObject.generateAsymSigKeys(
//                keyID = "${roleName}PKE" // TODO check this ID is fine
//            )
//            return Role(
//                name = roleName,
//                asymEncKeys = AsymKeys(
//                    private = asymEncKeys.private.encoded.encodeBase64(),
//                    public = asymEncKeys.public.encoded.encodeBase64(),
//                    AsymKeysType.ENC
//                ),
//                asymSigKeys = AsymKeys(
//                    private = asymSigKeys.private.encoded.encodeBase64(),
//                    public = asymSigKeys.public.encoded.encodeBase64(),
//                    AsymKeysType.SIG
//                ),
//                versionNumber = roleVersionNumber,
//            )
//        }
//
//        fun createAttribute(
//            attributeName: String,
//            attributeVersionNumber: Int = 1,
//            status: ElementStatus = ElementStatus.OPERATIONAL,
//        ): Attribute {
//            return Attribute(
//                name = attributeName,
//                versionNumber = attributeVersionNumber,
//                status = status
//            )
//        }
//
//        fun createResource(
//            resourceName: String,
//            symKeyVersionNumber: Int = 1,
//            enforcement: Enforcement = Enforcement.COMBINED,
//        ): Resource {
//            return Resource(
//                name = resourceName,
//                symKeyVersionNumber = symKeyVersionNumber,
//                enforcement = enforcement
//            )
//        }
//
//        fun createTupleUserRole(username: String, role: Role): UserRole {
//            val tupleUserRole = UserRole(
//                username = username, roleName = role.name,
//                encryptedAsymEncKeys = cryptoPKEObject.encryptAsymKeys(
//                    Parameters.adminAsymEncKeys.public,
//                    cryptoPKEObject.recreateAsymKeyPair(
//                        asymPublicKeyBytes = role.asymEncKeys!!.public.decodeBase64(),
//                        asymPrivateKeyBytes = role.asymEncKeys.private.decodeBase64(),
//                        AsymKeysType.ENC
//                    ),
//                    AsymKeysType.ENC
//                ),
//                encryptedAsymSigKeys = cryptoPKEObject.encryptAsymKeys(
//                    Parameters.adminAsymEncKeys.public,
//                    cryptoPKEObject.recreateAsymKeyPair(
//                        asymPublicKeyBytes = role.asymSigKeys!!.public.decodeBase64(),
//                        asymPrivateKeyBytes = role.asymSigKeys.private.decodeBase64(),
//                        AsymKeysType.SIG
//                    ),
//                    AsymKeysType.SIG
//                ),
//            )
//            val signature = cryptoPKEObject.createSignature(
//                bytes = tupleUserRole.getBytesForSignature(),
//                signingKey = Parameters.adminAsymSigKeys.private
//            )
//            tupleUserRole.updateSignature(
//                newSignature = signature,
//                newSigner = ADMIN
//            )
//            return tupleUserRole
//        }
//
//        fun createRolePermission(
//            role: Role,
//            resource: Resource,
//            operation: Operation = Operation.READ,
//            signingKey: PrivateKeyCryptoAC = Parameters.adminAsymSigKeys.private
//        ): RolePermission {
//            val encryptedSymKey = cryptoPKEObject.encryptSymKey(
//                cryptoPKEObject.recreateAsymPublicKey(
//                    asymPublicKeyBytes = role.asymEncKeys!!.public.decodeBase64(),
//                    type = AsymKeysType.ENC
//                ),
//                cryptoSKEObject.generateSymKey()
//            )
//            val rolePermission = RolePermission(
//                roleName = role.name,
//                resourceName = resource.name,
//                roleToken = role.token,
//                resourceToken = resource.token,
//                permission = operation,
//                roleVersionNumber = role.versionNumber,
//                symKeyVersionNumber = resource.symEncKeyVersionNumber,
//                encryptingSymKey = encryptedSymKey,
//                decryptingSymKey = encryptedSymKey
//            )
//            val signature =
//                cryptoPKEObject.createSignature(
//                    bytes = rolePermission.getBytesForSignature(),
//                    signingKey = signingKey
//                )
//            rolePermission.updateSignature(
//                newSignature = signature,
//                newSigner = ADMIN
//            )
//            return rolePermission
//        }
//
//        fun createUserAttribute(
//            username: String,
//            attributeName: String,
//            attributeValue: String? = null,
//            signingKey: PrivateKeyCryptoAC = Parameters.adminAsymSigKeys.private
//        ): UserAttribute {
//            val tupleUserAttribute = UserAttribute(
//                username = username,
//                attributeName = attributeName,
//                attributeValue = attributeValue,
//            )
//            val signature =
//                cryptoPKEObject.createSignature(
//                    bytes = tupleUserAttribute.getBytesForSignature(),
//                    signingKey = signingKey
//                )
//            tupleUserAttribute.updateSignature(
//                newSignature = signature,
//                newSigner = ADMIN
//            )
//            return tupleUserAttribute
//        }
//
//        fun createAccessStructurePermission(
//            accessStructure: String,
//            resource: Resource,
//            operation: Operation = Operation.READ,
//            signingKey: PrivateKeyCryptoAC = Parameters.adminAsymSigKeys.private
//        ): AccessStructurePermission {
//            val encryptedSymKey = cryptoPKEObject.encryptSymKey(
//                cryptoPKEObject.recreateAsymPublicKey(
//                    asymPublicKeyBytes = adminUser.asymEncKeys!!.public.decodeBase64(),
//                    type = AsymKeysType.ENC
//                ),
//                cryptoSKEObject.generateSymKey()
//            )
//            val accessStructurePermission = AccessStructurePermission(
//                resourceName = resource.name,
//                resourceToken = resource.token,
//                accessStructure = accessStructure,
//                permission = operation,
//                symKeyVersionNumber = resource.symEncKeyVersionNumber,
//                encryptingSymKey = encryptedSymKey,
//                decryptingSymKey = encryptedSymKey
//            )
//            val signature =
//                cryptoPKEObject.createSignature(
//                    bytes = accessStructurePermission.getBytesForSignature(),
//                    signingKey = signingKey
//                )
//            accessStructurePermission.updateSignature(
//                newSignature = signature,
//                newSigner = ADMIN
//            )
//            return accessStructurePermission
//        }
//
//        fun assertRollbackAndLock(service: Service) {
//            assertRollback(service)
//            assertLock(service)
//        }
//
//        fun assertUnLockAndLock(
//            service: Service,
//            lockCode: OutcomeCode = CODE_000_SUCCESS,
//            unlockCode: OutcomeCode = CODE_000_SUCCESS
//        ) {
//            assertUnlock(service, unlockCode)
//            assertLock(service, lockCode)
//        }
//
//        fun assertLock(
//            service: Service,
//            lockCode: OutcomeCode = CODE_000_SUCCESS
//        ) {
//            assert(service.lock() == lockCode)
//        }
//
//        fun assertUnlock(
//            service: Service,
//            unlockCode: OutcomeCode = CODE_000_SUCCESS
//        ) {
//            assert(service.unlock() == unlockCode)
//        }
//
//        fun assertRollback(service: Service) {
//            assert(service.rollback() == CODE_000_SUCCESS)
//        }
//
//        fun assertSuccessOrThrow(code: OutcomeCode) {
//            if (code != CODE_000_SUCCESS) {
//                throw IllegalStateException("assertSuccessOrThrow: $code")
//            }
//        }
//
//        fun assertTrueOrThrow(value: Boolean) {
//            if (!value) {
//                throw IllegalStateException("assertTrueOrThrow: false")
//            }
//        }
//    }
//}
//
///**
// * Execute the given string as a command for a ProcessBuilder,
// * setting the [workingDir] and waiting until the process sends
// * as output all strings contained in [endStrings]. For instance,
// * this is useful when you need to launch a command that takes some
// * time to execute, and you want to wait until its completion
// */
//fun String.runCommand(workingDir: File, endStrings: HashSet<String>): Process {
//    val arguments = this.split(Regex(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")).map {
//        it.replace("\"", "")
//    }.toTypedArray()
//    val process = ProcessBuilder(*arguments)
//        .directory(workingDir)
//        .start()
//
//    var output = ""
//    var error = ""
//    val inputStream = BufferedReader(InputStreamReader(process.inputStream))
//    val errorStream = BufferedReader(InputStreamReader(process.errorStream))
//    while (
//        endStrings.isNotEmpty() &&
//        (inputStream.readLine()?.also { output = it } != null
//                || errorStream.readLine()?.also { error = it } != null
//                )
//    ) {
//        if (error != "") {
//            println("runCommand [error]: $error")
//            error = ""
//        }
//        if (output != "") {
//            println("runCommand [output]: $output")
//            endStrings.removeIf { output.contains(it) }
//            output = ""
//        }
//    }
//    inputStream.close()
//    return process
//}
