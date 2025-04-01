package cryptoac.ac.dynsec

/**
 * The possible ACL types that the
 * DynSec plugin of Mosquitto supports
 */
enum class DynSecAclType {
    publishClientSend,
    publishClientReceive,
    subscribePattern,
    unsubscribePattern,
    //subscribeLiteral,
    //unsubscribeLiteral,
}