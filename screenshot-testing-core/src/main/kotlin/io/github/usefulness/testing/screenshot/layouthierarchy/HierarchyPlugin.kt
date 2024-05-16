package io.github.usefulness.testing.screenshot.layouthierarchy

import android.graphics.Point
import org.json.JSONObject

/**
 * A plugin for a layout hierarchy that allows you to configure how certain hierarchies are created.
 * If you have a custom view group, for example, that you want to display differently than normal,
 * then you would create a plugin for it.
 */
interface HierarchyPlugin {
    /**
     * Determines whether this plugin operates on the given type
     */
    fun accept(obj: Any?): Boolean

    /**
     * Constructs the hierarchy of the given type into a [org.json.JSONObject]
     */
    fun putHierarchy(dumper: LayoutHierarchyDumper, node: JSONObject, obj: Any, offset: Point)

    companion object {
        const val KEY_CHILDREN: String = "children"
    }
}
