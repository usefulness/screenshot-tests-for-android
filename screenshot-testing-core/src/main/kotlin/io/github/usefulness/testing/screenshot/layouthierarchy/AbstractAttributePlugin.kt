package io.github.usefulness.testing.screenshot.layouthierarchy

import org.json.JSONObject

abstract class AbstractAttributePlugin : AttributePlugin {
    protected fun prefix(name: String): String {
        val prefix = namespace()

        return if (prefix.isEmpty()) {
            name
        } else {
            "$prefix:$name"
        }
    }

    protected fun put(node: JSONObject, key: String, value: String) {
        node.put(prefix(key), value)
    }

    protected fun putPlain(node: JSONObject, key: String, value: String) {
        node.put(key, value)
    }

    protected fun putRequired(node: JSONObject, name: String?, left: Int, top: Int, width: Int, height: Int) {
        node.put(AttributePlugin.KEY_CLASS, name)
        node.put(AttributePlugin.KEY_LEFT, left)
        node.put(AttributePlugin.KEY_TOP, top)
        node.put(AttributePlugin.KEY_WIDTH, width)
        node.put(AttributePlugin.KEY_HEIGHT, height)
    }
}
