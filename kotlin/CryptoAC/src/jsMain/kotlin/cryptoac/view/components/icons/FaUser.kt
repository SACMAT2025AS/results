@file:JsModule("react-icons/fa")
@file:JsNonModule

package cryptoac.view.components.icons

import js.core.Object
import react.*

@JsName("FaUser")
external val faUser: ComponentClass<FaUserProps>

external interface FaUserProps : Props {
    var style: Object
}
