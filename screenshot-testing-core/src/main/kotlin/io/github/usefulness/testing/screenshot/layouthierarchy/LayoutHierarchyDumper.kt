package io.github.usefulness.testing.screenshot.layouthierarchy

import android.graphics.Point
import android.view.View
import org.json.JSONObject

class LayoutHierarchyDumper internal constructor(
    private val hierarchyPlugins: List<HierarchyPlugin>,
    private val attributePlugins: List<AttributePlugin>,
) {

    internal fun dumpHierarchy(view: View): JSONObject {
        val offset = Point(-getViewLeft(view), -getViewTop(view))
        return dumpHierarchy(view, offset)
    }

    internal fun dumpHierarchy(obj: Any, offset: Point): JSONObject {
        val node = dumpAttributes(obj, offset)
        for (plugin in hierarchyPlugins) {
            if (plugin.accept(obj)) {
                // First hierarchy wins
                plugin.putHierarchy(this, node, obj, offset)
                return node
            }
        }

        error("No available plugins for type " + obj.javaClass.canonicalName)
    }

    private fun dumpAttributes(obj: Any, offset: Point): JSONObject {
        val node = JSONObject()
        for (plugin in attributePlugins) {
            if (plugin.accept(obj)) {
                plugin.putAttributes(node, obj, offset)
            }
        }
        return node
    }

    companion object {
        private val sGlobalAttributePlugins: MutableList<AttributePlugin> = ArrayList()
        private val sGlobalHierarchyPlugins: MutableList<HierarchyPlugin> = ArrayList()

        @JvmStatic
        fun addGlobalAttributePlugin(plugin: AttributePlugin) {
            sGlobalAttributePlugins.add(plugin)
        }

        @JvmStatic
        fun removeGlobalAttributePlugin(plugin: AttributePlugin) {
            sGlobalAttributePlugins.remove(plugin)
        }

        @JvmStatic
        fun addGlobalHierarchyPlugin(plugin: HierarchyPlugin) {
            sGlobalHierarchyPlugins.add(plugin)
        }

        @JvmStatic
        fun removeGlobalHierarchyPlugin(plugin: HierarchyPlugin) {
            sGlobalHierarchyPlugins.remove(plugin)
        }

        internal fun create() = createWith(emptyList(), emptyList())

        internal fun createWith(hierarchyPlugins: List<HierarchyPlugin>, attributePlugins: List<AttributePlugin>): LayoutHierarchyDumper {
            val allHierarchyPlugins = buildList(capacity = hierarchyPlugins.size + sGlobalHierarchyPlugins.size + 1) {
                addAll(hierarchyPlugins)
                addAll(sGlobalHierarchyPlugins)
                add(BaseViewHierarchyPlugin)
            }

            val allAttributePlugins = buildList(capacity = attributePlugins.size + sGlobalAttributePlugins.size + 1) {
                add(BaseViewAttributePlugin)
                addAll(attributePlugins)
                addAll(sGlobalAttributePlugins)
            }

            return LayoutHierarchyDumper(allHierarchyPlugins, allAttributePlugins)
        }

        @JvmStatic
        fun getViewLeft(view: View): Int = view.left + view.translationX.toInt()

        @JvmStatic
        fun getViewTop(view: View): Int = view.top + view.translationY.toInt()
    }
}
