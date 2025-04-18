@file:JsModule("@material-ui/core")
@file:JsNonModule

package cryptoac.view.components.materialui

import js.core.Object
import react.*

@JsName("Typography")
external val typography: ComponentClass<TypographyProps>

external interface TypographyProps : Props {
    var style: Object
    var variant: String
    var id: String
    var component: String
    // TODO
}
