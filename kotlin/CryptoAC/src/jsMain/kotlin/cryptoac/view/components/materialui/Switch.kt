@file:JsModule("@material-ui/core")
@file:JsNonModule

package cryptoac.view.components.materialui

import org.w3c.dom.events.Event
import react.*

@JsName("Switch")
external val switch: ComponentClass<SwitchProps>

external interface SwitchProps : Props {
    var checked: Boolean
    var color: String
    var size: String
    var onChange: (Event) -> Unit
}
