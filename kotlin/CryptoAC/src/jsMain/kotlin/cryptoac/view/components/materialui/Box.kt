@file:JsModule("@material-ui/core")
@file:JsNonModule

package cryptoac.view.components.materialui

import js.core.Object
import react.*

@JsName("Box")
external val box: ComponentClass<BoxProps>

external interface BoxProps : Props {
    var sx: Object
}
