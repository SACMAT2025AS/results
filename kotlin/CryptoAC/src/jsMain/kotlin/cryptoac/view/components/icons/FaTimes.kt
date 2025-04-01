@file:JsModule("react-icons/fa")
@file:JsNonModule

package cryptoac.view.components.icons

import js.core.Object
import react.*

@JsName("FaTimes")
external val faTimes: ComponentClass<FaTimesProps>

external interface FaTimesProps : Props {
    var style: Object
}
