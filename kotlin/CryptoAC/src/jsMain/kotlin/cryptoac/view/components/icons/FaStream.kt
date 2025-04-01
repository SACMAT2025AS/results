@file:JsModule("react-icons/fa")
@file:JsNonModule

package cryptoac.view.components.icons

import js.core.Object
import react.*

@JsName("FaStream")
external val faStream: ComponentClass<FaStreamProps>

external interface FaStreamProps : Props {
    var style: Object
}
