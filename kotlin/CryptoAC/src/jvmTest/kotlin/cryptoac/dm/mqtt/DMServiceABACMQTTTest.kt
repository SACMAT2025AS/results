//package cryptoac.dm.mqtt
//
//import cryptoac.*
//import cryptoac.core.mqtt.CryptoACMQTTClient
//import cryptoac.dm.*
//import cryptoac.dm.DMServiceABACTest
//import cryptoac.tuple.User
//import mu.KotlinLogging
//import org.junit.jupiter.api.*
//
//private val logger = KotlinLogging.logger {}
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//internal abstract class DMServiceABACMQTTTest : DMServiceABACTest() {
//
//    // TODO "internal abstract class" -> "internal class" e implementa
//    override var dm = DMFactory.getDM(
//        Parameters.dmServiceMQTTNoACParameters
////    ) as DMServiceMQTT
//    override val dmABAC: DMServiceABAC = dm
//    override val service: Service = dm
//
//    private var client: CryptoACMQTTClient? = null
//
//    private var processDocker: Process? = null
//
//
//    /** In this implementation, set the callback of the MQTT client */
//    override fun addUser(user: User): Service {
//        return super.addUser(user).apply {
//            (this as DMServiceMQTT).client.setCallback(dm)
//        }
//    }
//}
