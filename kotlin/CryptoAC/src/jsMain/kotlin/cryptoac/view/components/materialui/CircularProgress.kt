@file:JsModule("@material-ui/core")
@file:JsNonModule

package cryptoac.view.components.materialui

import react.*

@JsName("CircularProgress")
external val circularProgress: ComponentClass<CircularProgressProps>

external interface CircularProgressProps : Props {
    /**
     * The value of the progress indicator for the
     * determinate variant. Value between 0 and 100
     */
    var value: Int
}
