@file:JsModule("@material-ui/core")
@file:JsNonModule

package cryptoac.view.components.materialui

import js.core.Object
import org.w3c.dom.events.Event
import react.*

@JsName("IconButton")
external val iconButton: ComponentClass<IconButtonProps>

external interface IconButtonProps : Props {
    var children: ReactElement<Props>
    var color: String
    var label: String
    var component: String
    var style: Object
    var size: String
    var onClick: (Event) -> Unit
}
