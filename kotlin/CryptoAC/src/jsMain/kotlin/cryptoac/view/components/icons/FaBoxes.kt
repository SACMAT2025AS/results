@file:JsModule("react-icons/fa")
@file:JsNonModule

package cryptoac.view.components.icons

import js.core.Object
import react.*

@JsName("FaBoxes")
external val faBoxes: ComponentClass<FaBoxesProps>

external interface FaBoxesProps : Props {
    var style: Object
}
