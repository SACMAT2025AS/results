package cryptoac.core

import cryptoac.ac.ACServiceParameters
import cryptoac.tuple.User
import cryptoac.crypto.CryptoType
import cryptoac.dm.DMServiceParameters
import cryptoac.mm.MMServiceParameters
import cryptoac.rm.RMServiceParameters
import kotlinx.serialization.Serializable

/** Class defining the parameters required to configure a RB-CAC core object */
@Serializable
open class CoreParametersRBAC(
    override var user: User,
    override val coreType: CoreType,
    override val cryptoType: CryptoType,
    override val versionNumber: Int = 1,
    override val rmServiceParameters: RMServiceParameters?,
    override val mmServiceParameters: MMServiceParameters,
    override val dmServiceParameters: DMServiceParameters?,
    override val acServiceParameters: ACServiceParameters?
) : CoreParameters()