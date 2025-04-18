@file:JsModule("react-icons/fa")
@file:JsNonModule

package cryptoac.view.components.icons

import js.core.Object
import react.*

@JsName("FaMedal")
external val faMedal: ComponentClass<FaMedalProps>

external interface FaMedalProps : Props {
    var style: Object
}
