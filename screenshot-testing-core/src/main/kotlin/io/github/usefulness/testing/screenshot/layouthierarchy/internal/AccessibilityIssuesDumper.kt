package io.github.usefulness.testing.screenshot.layouthierarchy.internal

import android.view.ViewGroup
import io.github.usefulness.testing.screenshot.layouthierarchy.internal.AccessibilityUtil.AXTreeNode
import io.github.usefulness.testing.screenshot.layouthierarchy.internal.AccessibilityUtil.isSpeakingNode
import io.github.usefulness.testing.screenshot.layouthierarchy.internal.AccessibilityUtil.isTalkbackFocusable
import org.json.JSONArray
import org.json.JSONObject

internal object AccessibilityIssuesDumper {

    fun dumpIssues(axTree: AXTreeNode): JSONArray {
        val root = JSONArray()

        findTalkbackFocusableElementsWithoutSpokenFeedback(axTree)?.let { root.put(it) }

        return root
    }

    private fun findTalkbackFocusableElementsWithoutSpokenFeedback(axTree: AXTreeNode): JSONObject? {
        val evaluation = JSONObject()
        evaluation.put("id", "talkback_focusable_element_without_spoken_feedback")
        evaluation.put("name", "Focusable Element Without Spoken Feedback")
        evaluation.put("description", "The element is focusable by screen readers such as Talkback, but has no text to announce.")

        val elements = JSONArray()
        for (axTreeNode in axTree.allNodes) {
            val view = axTreeNode.view
            val nodeInfo = axTreeNode.nodeInfo
            if (isTalkbackFocusable(view) && !isSpeakingNode(nodeInfo, view)) {
                val element = JSONObject().apply {
                    put("name", view.javaClass.simpleName)
                    put("class", view.javaClass.name)
                    val elementPos = JSONObject().apply {
                        put("left", view.left)
                        put("top", view.top)
                        put("width", view.width)
                        put("height", view.height)
                    }
                    put("position", elementPos)
                    val suggestions = JSONArray()
                    suggestions.put("Add a contentDescription to the element.")
                    if (view is ViewGroup) {
                        suggestions.put("Add a contentDescription or visible text to a child element.")
                    }
                    put("suggestions", suggestions)
                }
                elements.put(element)
            }
        }

        if (elements.length() > 0) {
            evaluation.put("elements", elements)
            return evaluation
        }

        return null
    }
}
