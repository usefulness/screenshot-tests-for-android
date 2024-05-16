/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.usefulness.testing.screenshot.layouthierarchy

import android.graphics.Point
import android.view.View
import org.json.JSONObject

/**
 * Dumps information about a layout hierarchy into a JSON object
 */
class LayoutHierarchyDumper internal constructor(
    private val hierarchyPlugins: List<HierarchyPlugin>,
    private val attributePlugins: List<AttributePlugin>,
) {

    fun dumpAttributes(obj: Any, offset: Point): JSONObject {
        val node = JSONObject()
        for (plugin in attributePlugins) {
            if (plugin.accept(obj)) {
                plugin.putAttributes(node, obj, offset)
            }
        }
        return node
    }

    fun dumpHierarchy(view: View): JSONObject {
        val offset = Point(-getViewLeft(view), -getViewTop(view))
        return dumpHierarchy(view, offset)
    }

    fun dumpHierarchy(obj: Any, offset: Point): JSONObject {
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

    companion object {
        private val sGlobalAttributePlugins: MutableList<AttributePlugin> = ArrayList()
        private val sGlobalHierarchyPlugins: MutableList<HierarchyPlugin> = ArrayList()

        fun addGlobalAttributePlugin(plugin: AttributePlugin) {
            sGlobalAttributePlugins.add(plugin)
        }

        fun removeGlobalAttributePlugin(plugin: AttributePlugin) {
            sGlobalAttributePlugins.remove(plugin)
        }

        fun addGlobalHierarchyPlugin(plugin: HierarchyPlugin) {
            sGlobalHierarchyPlugins.add(plugin)
        }

        fun removeGlobalHierarchyPlugin(plugin: HierarchyPlugin) {
            sGlobalHierarchyPlugins.remove(plugin)
        }

        fun create() = createWith(emptyList(), emptyList())

        fun createWith(
            hierarchyPlugins: List<HierarchyPlugin>,
            attributePlugins: List<AttributePlugin>,
        ): LayoutHierarchyDumper {
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

            return createWithOnly(allHierarchyPlugins, allAttributePlugins)
        }

        fun createWithOnly(
            hierarchyPlugins: List<HierarchyPlugin>,
            attributePlugins: List<AttributePlugin>,
        ): LayoutHierarchyDumper = LayoutHierarchyDumper(hierarchyPlugins, attributePlugins)

        fun getViewLeft(view: View): Int {
            return view.left + view.translationX.toInt()
        }

        fun getViewTop(view: View): Int {
            return view.top + view.translationY.toInt()
        }
    }
}
