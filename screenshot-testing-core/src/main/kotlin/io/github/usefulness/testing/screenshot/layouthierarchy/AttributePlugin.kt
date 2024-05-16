package io.github.usefulness.testing.screenshot.layouthierarchy

import android.graphics.Point
import org.json.JSONObject

/**
 * A plugin for a layout hierarchy that allows you to configure what attributes are added per view
 * type.
 */
interface AttributePlugin {
    /**
     * Determines whether this plugin operates on the given type
     */
    fun accept(obj: Any?): Boolean

    /**
     * Returns the namespace of the attributes this plugin inserts
     */
    fun namespace(): String

    /**
     * Puts all interesting attributes of the given object into the node
     */
    fun putAttributes(node: JSONObject, obj: Any, offset: Point)

    companion object {
        const val KEY_CLASS: String = "class"
        const val KEY_LEFT: String = "left"
        const val KEY_TOP: String = "top"
        const val KEY_WIDTH: String = "width"
        const val KEY_HEIGHT: String = "height"
    }
}
