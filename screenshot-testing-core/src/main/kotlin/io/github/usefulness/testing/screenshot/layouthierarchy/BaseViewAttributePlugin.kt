package io.github.usefulness.testing.screenshot.layouthierarchy

import android.graphics.Point
import android.view.View
import org.json.JSONObject

/**
 * Dumps basic information that applies to all [View]s, like position and class
 */
object BaseViewAttributePlugin : AbstractAttributePlugin() {
    override fun accept(obj: Any?) = obj is View

    override fun namespace() = ""

    override fun putAttributes(node: JSONObject, obj: Any, offset: Point) {
        val view = obj as View
        putRequired(
            node,
            view.javaClass.canonicalName,
            offset.x + LayoutHierarchyDumper.getViewLeft(view),
            offset.y + LayoutHierarchyDumper.getViewTop(view),
            view.width,
            view.height,
        )
    }
}
