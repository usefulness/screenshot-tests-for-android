package io.github.usefulness.testing.screenshot.layouthierarchy

import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import org.json.JSONArray
import org.json.JSONObject

object BaseViewHierarchyPlugin : HierarchyPlugin {

    override fun accept(obj: Any?) = obj is View

    override fun putHierarchy(
        dumper: LayoutHierarchyDumper,
        node: JSONObject,
        obj: Any,
        offset: Point,
    ) {
        if (obj !is ViewGroup) {
            return
        }

        val offsetLeft = LayoutHierarchyDumper.getViewLeft(obj)
        val offsetTop = LayoutHierarchyDumper.getViewTop(obj)
        offset.offset(offsetLeft, offsetTop)

        val children = JSONArray()
        obj.children.forEach { child ->
            children.put(dumper.dumpHierarchy(child, offset))
        }

        node.put(HierarchyPlugin.KEY_CHILDREN, children)
        offset.offset(-offsetLeft, -offsetTop)
    }
}
