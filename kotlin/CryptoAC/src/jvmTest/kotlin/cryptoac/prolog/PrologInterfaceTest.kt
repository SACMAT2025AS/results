package prolog

import cryptoac.OutcomeCode
import cryptoac.core.CoreCACRBACTuples
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import cryptoac.Parameters
import cryptoac.core.CoreFactory
import cryptoac.inputStream
import cryptoac.tuple.Enforcement
import cryptoac.tuple.Operation
import org.junit.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PrologInterfaceTest {

    @Test
    fun prolog() {
        val cac: CoreCACRBACTuples =
            CoreFactory.getCore(Parameters.adminCoreCACRBACCryptoACParametersNoOPA) as CoreCACRBACTuples

        val file = File("prolog/instructions.txt")
        val input = BufferedReader(FileReader(file))

        val users = HashMap<String, CoreCACRBACTuples>()

        while(input.ready()) {
            val line = input.readLine()

            if(!line.startsWith("[CryptoAC]")) {
                continue;
            }

            val toks = line.split(" ")

            print("Executing $line...")

            val start = System.currentTimeMillis()
            val code = when(toks[1]) {
                // init system
                "initAdmC" -> cac.configureServices()

                // users
                "addUserC" -> {
                    val params = cac.addUser(toks[2])
                    if(params.code != OutcomeCode.CODE_000_SUCCESS) {
                        params.code
                    }
                    val core = CoreFactory.getCore(params.coreParameters!!) as CoreCACRBACTuples
                    users[toks[2]] = core
                    params.code
                }
                "initUserC" -> users[toks[2]]!!.initUser()
                "deleteUserC" -> cac.deleteUser(toks[2])

                // roles
                "addRoleC" -> cac.addRole(toks[2])
                "deleteRoleC" -> cac.deleteUser(toks[2])

                // resource
                "addResourceC" -> cac.addResource(toks[2], "default".inputStream(), Enforcement.COMBINED)
                "deleteResourceC" -> cac.deleteResource(toks[2])
                "prepareReencryptionC" -> cac.rotateResourceKey(toks[2])
                "reencryptResourceC" -> cac.eagerReencryption(toks[2])

                // user-roles
                "assignUserToRoleC" -> cac.assignUserToRole(toks[2], toks[3])
                "revokeUserFromRoleC" -> cac.revokeUserFromRole(toks[2], toks[3])
                "rotateRoleKeyUserRoleC" -> cac.rotateRoleKeyUserRoles(toks[2])
                "rotateRoleKeyPermissionsC" -> cac.rotateRoleKeyPermissions(toks[2])

                // role-permissions
                "assignPermissionToRoleC" -> cac.assignPermissionToRole(toks[2], toks[4], Operation.fromString(toks[3])!!)
                "revokePermissionFromRoleC" -> cac.revokePermissionFromRole(toks[3], toks[4], Operation.fromString(toks[3])!!)

                // IO
                "readResourceC" -> {
                    val result = users[toks[2]]!!.readResource(toks[3])
                    result.code
                }
                "writeResourceC" -> users[toks[2]]!!.writeResource(toks[3], "content".inputStream())

                // other -> error
                else -> OutcomeCode.CODE_049_UNEXPECTED
            }
            val end = System.currentTimeMillis()

            println("$code in ${end - start}")

            assertEquals(OutcomeCode.CODE_000_SUCCESS, code)
        }

        assert(1 == 1)
    }

}

