@file:JsModule("@material-ui/core")
@file:JsNonModule

package cryptoac.view.components.materialui

import react.*

@JsName("MenuItem")
external val menuItem: ComponentClass<MenuItemProps>

external interface MenuItemProps : Props {
    var value: String
}
