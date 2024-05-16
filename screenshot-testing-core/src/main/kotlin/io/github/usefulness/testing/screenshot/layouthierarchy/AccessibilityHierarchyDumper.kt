package io.github.usefulness.testing.screenshot.layouthierarchy

import android.graphics.Rect
import android.os.Build
import android.view.View
import io.github.usefulness.testing.screenshot.layouthierarchy.AccessibilityUtil.AXTreeNode
import io.github.usefulness.testing.screenshot.layouthierarchy.AccessibilityUtil.generateAccessibilityTree
import org.json.JSONArray
import org.json.JSONObject

/**
 * Dumps information about the accessibility hierarchy into a JSON object
 */
object AccessibilityHierarchyDumper {

    fun dumpHierarchy(axTree: AXTreeNode?): JSONObject {
        val root = JSONObject()

        val view = axTree?.view ?: return root

        val nodeInfo = axTree.nodeInfo

        root.put("class", view.javaClass.name)

        if (nodeInfo.actionList.size == 0) {
            root.put("actionList", JSONObject.NULL)
        } else {
            val actionList = JSONArray()
            for (action in nodeInfo.actionList) {
                actionList.put(action.id)
            }
            root.put("actionList", actionList)
        }

        val tempRect = Rect()

        nodeInfo.getBoundsInScreen(tempRect)
        val screenBoundsObj = JSONObject().apply {
            put("left", tempRect.left)
            put("right", tempRect.right)
            put("top", tempRect.top)
            put("bottom", tempRect.bottom)
        }
        root.put("boundsInScreen", screenBoundsObj)

        root.put("canOpenPopup", nodeInfo.canOpenPopup())
        root.put("childCount", nodeInfo.childCount)
        root.put("className", nodeInfo.className)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            root.put("isHeading", nodeInfo.isHeading)
        }

        if (nodeInfo.collectionInfo == null) {
            root.put("collectionInfo", JSONObject.NULL)
        } else {
            val collectionInfo = nodeInfo.collectionInfo
            val collectionInfoObj = JSONObject().apply {
                put("columnCount", collectionInfo.columnCount)
                put("rowCount", collectionInfo.rowCount)
                put("selectionMode", collectionInfo.selectionMode)
                put("isHierarchical", collectionInfo.isHierarchical)
            }
            root.put("collectionInfo", collectionInfoObj)
        }

        if (nodeInfo.collectionItemInfo == null) {
            root.put("collectionItemInfo", JSONObject.NULL)
        } else {
            val collectionItemInfoObj = JSONObject().apply {
                val itemInfo = nodeInfo.collectionItemInfo
                put("columnIndex", itemInfo.columnIndex)
                put("columnSpan", itemInfo.columnSpan)
                put("rowIndex", itemInfo.rowIndex)
                put("rowSpan", itemInfo.rowSpan)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    put("isHeading", itemInfo.isHeading)
                }
                put("isSelected", itemInfo.isSelected)
            }
            root.put("collectionItemInfo", collectionItemInfoObj)
        }

        root.put("contentDescription", jsonNullOr(nodeInfo.contentDescription))
        root.put("error", jsonNullOr(nodeInfo.error))

        if (nodeInfo.extras == null) {
            root.put("extras", JSONObject.NULL)
        } else {
            val extras = nodeInfo.extras
            root.put("extras", extras.toString())
        }

        root.put("inputType", nodeInfo.inputType)
        root.put("isCheckable", nodeInfo.isCheckable)
        root.put("isChecked", nodeInfo.isChecked)
        root.put("isClickable", nodeInfo.isClickable)
        root.put("isContentInvalid", nodeInfo.isContentInvalid)
        root.put("isDismissable", nodeInfo.isDismissable)
        root.put("isEditable", nodeInfo.isEditable)
        root.put("isEnabled", nodeInfo.isEnabled)
        root.put("isFocusable", nodeInfo.isFocusable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            root.put("isImportantForAccessibility", nodeInfo.isImportantForAccessibility)
        }
        root.put("isLongClickable", nodeInfo.isLongClickable)
        root.put("isMultiLine", nodeInfo.isMultiLine)
        root.put("isPassword", nodeInfo.isPassword)
        root.put("isScrollable", nodeInfo.isScrollable)
        root.put("isSelected", nodeInfo.isSelected)
        root.put("isVisibleToUser", nodeInfo.isVisibleToUser)
        root.put("liveRegion", nodeInfo.liveRegion)
        root.put("maxTextLength", nodeInfo.maxTextLength)
        root.put("movementGranularities", nodeInfo.movementGranularities)

        if (nodeInfo.rangeInfo == null) {
            root.put("rangeInfo", JSONObject.NULL)
        } else {
            val rangeInfoObj = JSONObject()
            val rangeInfo = nodeInfo.rangeInfo
            rangeInfoObj.put("current", rangeInfo.current.toDouble())
            rangeInfoObj.put("max", rangeInfo.max.toDouble())
            rangeInfoObj.put("min", rangeInfo.min.toDouble())
            rangeInfoObj.put("type", rangeInfo.type)
            root.put("rangeInfo", rangeInfoObj)
        }

        root.put("text", jsonNullOr(nodeInfo.text))

        if (axTree.childCount > 0) {
            val children = JSONArray()
            for (child in axTree.children) {
                val childSerialization = dumpHierarchy(child)
                children.put(childSerialization)
            }
            root.put("children", children)
        } else {
            root.put("children", JSONObject.NULL)
        }

        return root
    }

    fun dumpHierarchy(view: View) = dumpHierarchy(generateAccessibilityTree(view))

    private fun jsonNullOr(obj: Any?): Any = obj ?: JSONObject.NULL
}
