package io.github.usefulness.testing.screenshot.layouthierarchy

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.children

/**
 * This class provides utility methods for determining certain accessibility properties of [ ]s and [AccessibilityNodeInfo]s.
 * It is porting some of the checks from com.googlecode.eyesfree.utils.AccessibilityNodeInfoUtils, but has stripped many features which
 * are unnecessary here.
 */
object AccessibilityUtil {
    private const val NODE_INFO_CREATION_RETRY_COUNT = 3

    /**
     * Gets the role from a given [View]. If no role is defined it will return
     * AccessibilityRole.NONE, which has a value of null.
     *
     * @param view The View to check.
     * @return `AccessibilityRole` the defined role.
     */
    fun getRole(view: View) = createNodeInfoFromView(view)?.let { getRole(it) } ?: AccessibilityRole.NONE

    /**
     * Gets the role from a given [AccessibilityNodeInfo]. If no role is defined it will
     * return AccessibilityRole.NONE, which has a value of null.
     *
     * @param nodeInfo The node to check.
     * @return `AccessibilityRole` the defined role.
     */
    fun getRole(nodeInfo: AccessibilityNodeInfo): AccessibilityRole {
        val role = AccessibilityRole.fromValue(nodeInfo.className as String)
        if (role == AccessibilityRole.IMAGE_BUTTON || role == AccessibilityRole.IMAGE) {
            return if (nodeInfo.isClickable) AccessibilityRole.IMAGE_BUTTON else AccessibilityRole.IMAGE
        }

        if (role == AccessibilityRole.NONE) {
            val collection = nodeInfo.collectionInfo
            if (collection != null) {
                // RecyclerView will be classified as a list or grid.
                return if (collection.rowCount > 1 && collection.columnCount > 1) {
                    AccessibilityRole.GRID
                } else {
                    AccessibilityRole.LIST
                }
            }
        }

        return role
    }

    /**
     * Creates and returns an [AccessibilityNodeInfoCompat] from the the provided [View].
     * Note: This does not handle recycling of the [AccessibilityNodeInfoCompat].
     *
     * @param view       The [View] to create the [AccessibilityNodeInfoCompat] from.
     * @param retryCount The number of times to retry creating the AccessibilityNodeInfoCompat.
     * @return [AccessibilityNodeInfo]
     */
    private fun createNodeInfoFromView(view: View, retryCount: Int = NODE_INFO_CREATION_RETRY_COUNT): AccessibilityNodeInfo? {
        val nodeInfo = AccessibilityNodeInfoCompat.obtain().unwrap()

        // For some unknown reason, Android seems to occasionally throw a NPE from
        // onInitializeAccessibilityNodeInfo.
        try {
            view.onInitializeAccessibilityNodeInfo(nodeInfo)
        } catch (ignored: NullPointerException) {
            return null
        } catch (ignored: RuntimeException) {
            // For some unknown reason, Android seems to occasionally throw a IndexOutOfBoundsException
            // and also random RuntimeExceptions because the handler seems not to be initialized
            // from onInitializeAccessibilityNodeInfoInternal in ViewGroup.  This seems to be
            // nondeterministic, so lets retry if this happens.
            if (retryCount > 0) {
                return createNodeInfoFromView(view, retryCount - 1)
            }

            return null
        }

        return nodeInfo
    }

    /**
     * Returns whether the specified node has text or a content description.
     *
     * @param node The node to check.
     * @return `true` if the node has text.
     */
    fun hasText(node: AccessibilityNodeInfo) =
        node.collectionInfo == null && (!node.text.isNullOrEmpty() || !node.contentDescription.isNullOrEmpty())

    /**
     * Returns whether the supplied [View] and [AccessibilityNodeInfoCompat] would produce
     * spoken feedback if it were accessibility focused. NOTE: not all speaking nodes are focusable.
     *
     * @param view The [View] to evaluate
     * @param node The [AccessibilityNodeInfoCompat] to evaluate
     * @return `true` if it meets the criterion for producing spoken feedback
     */
    @JvmStatic
    fun isSpeakingNode(node: AccessibilityNodeInfo, view: View): Boolean {
        val important = view.importantForAccessibility
        if (important == View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS ||
            (important == View.IMPORTANT_FOR_ACCESSIBILITY_NO && node.childCount <= 0)
        ) {
            return false
        }

        return node.isCheckable || hasText(node) || hasNonActionableSpeakingDescendants(view)
    }

    /**
     * Determines if the supplied [View] and [AccessibilityNodeInfoCompat] has any
     * children which are not independently accessibility focusable and also have a spoken
     * description.
     *
     *
     * NOTE: Accessibility services will include these children's descriptions in the closest
     * focusable ancestor.
     *
     * @param view The [View] to evaluate
     * @return `true` if it has any non-actionable speaking descendants within its subtree
     */
    fun hasNonActionableSpeakingDescendants(view: View): Boolean {
        val viewGroup = view as? ViewGroup ?: return false

        return viewGroup.children.any { childView ->
            val childNode = createNodeInfoFromView(childView)
            when {
                childNode == null -> false
                !childNode.isVisibleToUser -> false
                isAccessibilityFocusable(childNode, childView) -> false
                isSpeakingNode(childNode, childView) -> true
                else -> false
            }
        }
    }

    /**
     * Determines if the provided [View] and [AccessibilityNodeInfoCompat] meet the
     * criteria for gaining accessibility focus.
     *
     *
     * Note: this is evaluating general focusability by accessibility services, and does not mean
     * this view will be guaranteed to be focused by specific services such as Talkback. For Talkback
     * focusability, see [.isTalkbackFocusable]
     *
     * @param view The [View] to evaluate
     * @param node The [AccessibilityNodeInfoCompat] to evaluate
     * @return `true` if it is possible to gain accessibility focus
     */
    fun isAccessibilityFocusable(node: AccessibilityNodeInfo, view: View): Boolean {
        // Never focus invisible nodes.
        if (!node.isVisibleToUser) {
            return false
        }

        // Always focus "actionable" nodes.
        if (isActionableForAccessibility(node)) {
            return true
        }

        // only focus top-level list items with non-actionable speaking children.
        return isTopLevelScrollItem(node, view) && isSpeakingNode(node, view)
    }

    /**
     * Determines whether the provided [View] and [AccessibilityNodeInfo] is a
     * top-level item in a scrollable container.
     *
     * @param view The [View] to evaluate
     * @param node The [AccessibilityNodeInfo] to evaluate
     * @return `true` if it is a top-level item in a scrollable container.
     */
    fun isTopLevelScrollItem(node: AccessibilityNodeInfo, view: View): Boolean {
        val parent = view.parentForAccessibility as? View ?: return false

        if (node.isScrollable) {
            return true
        }

        val actionList: List<*> = node.actionList
        if (actionList.contains(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ||
            actionList.contains(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        ) {
            return true
        }

        // Top-level items in a scrolling pager are actually two levels down since the first
        // level items in pagers are the pages themselves.
        val grandparent = parent.parentForAccessibility as? View
        if (grandparent != null && getRole(grandparent) == AccessibilityRole.PAGER) {
            return true
        }

        val parentRole = getRole(parent)
        return parentRole == AccessibilityRole.LIST ||
            parentRole == AccessibilityRole.GRID ||
            parentRole == AccessibilityRole.SCROLL_VIEW ||
            parentRole == AccessibilityRole.HORIZONTAL_SCROLL_VIEW
    }

    /**
     * Returns whether a node is actionable. That is, the node supports one of [AccessibilityNodeInfo.isClickable],
     * [AccessibilityNodeInfo.isFocusable], or [AccessibilityNodeInfo.isLongClickable].
     *
     * @param node The [AccessibilityNodeInfo] to evaluate
     * @return `true` if node is actionable.
     */
    fun isActionableForAccessibility(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable || node.isLongClickable || node.isFocusable) {
            return true
        }
        val actionsList = listOf(
            AccessibilityNodeInfo.ACTION_CLICK,
            AccessibilityNodeInfo.ACTION_LONG_CLICK,
            AccessibilityNodeInfo.ACTION_FOCUS,
        )
        return node.actionList.any { it.id in actionsList }
    }

    /**
     * Determines if any of the provided [View]'s and [AccessibilityNodeInfo]'s
     * ancestors can receive accessibility focus
     *
     * @param view The [View] to evaluate
     * @return `true` if an ancestor of may receive accessibility focus
     */
    fun hasFocusableAncestor(view: View): Boolean {
        val parentView = view.parentForAccessibility as? View ?: return false

        val parentNode = createNodeInfoFromView(parentView) ?: return false

        if (areBoundsIdenticalToWindow(parentNode, parentView) && parentNode.childCount > 0) {
            return false
        }

        if (isAccessibilityFocusable(parentNode, parentView)) {
            return true
        }

        if (hasFocusableAncestor(parentView)) {
            return true
        }
        return false
    }

    /**
     * Returns whether a AccessibilityNodeInfo has the same size and position as its containing
     * Window.
     *
     * @param node The [AccessibilityNodeInfo] to evaluate
     * @return `true` if node has equal bounds to its containing Window
     */
    fun areBoundsIdenticalToWindow(node: AccessibilityNodeInfo, view: View): Boolean {
        var window: Window? = null
        var context = view.context
        while (context is ContextWrapper) {
            if (context is Activity) {
                window = context.window
            }
            context = context.baseContext
        }

        if (window == null) {
            return false
        }

        val windowParams = window.attributes
        val windowBounds =
            Rect(
                windowParams.x,
                windowParams.y,
                windowParams.x + windowParams.width,
                windowParams.y + windowParams.height,
            )

        val nodeBounds = Rect()
        node.getBoundsInScreen(nodeBounds)

        return windowBounds == nodeBounds
    }

    /**
     * Returns whether a View has any children that are visible.
     *
     * @param view The [View] to evaluate
     * @return `true` if node has any visible children
     */
    fun hasVisibleChildren(view: View): Boolean {
        val viewGroup = view as? ViewGroup ?: return false
        val childCount = viewGroup.childCount
        for (i in 0 until childCount) {
            val childNodeInfo = createNodeInfoFromView(viewGroup.getChildAt(i))
            if (childNodeInfo != null) {
                if (childNodeInfo.isVisibleToUser) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Returns whether a given [View] will be focusable by Google's TalkBack screen reader.
     *
     * @param view The [View] to evaluate.
     * @return `boolean` if the view will be ignored by TalkBack.
     */
    @JvmStatic
    fun isTalkbackFocusable(view: View): Boolean {
        val important = view.importantForAccessibility
        if (important == View.IMPORTANT_FOR_ACCESSIBILITY_NO ||
            important == View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        ) {
            return false
        }

        // Go all the way up the tree to make sure no parent has hidden its descendants
        var parent = view.parent
        while (parent is View) {
            if (parent.importantForAccessibility == View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) {
                return false
            }
            parent = parent.getParent()
        }

        // Trying to evaluate the focusability of certain element types (mainly list views) can cause
        // problems when trying to determine the offset of the elements Rect relative to its parent in
        // ViewGroup.offsetRectBetweenParentAndChild. If this happens, simply return false, as this view
        // will not be focusable.
        val node = try {
            createNodeInfoFromView(view) ?: return false
        } catch (ignored: IllegalArgumentException) {
            return false
        }

        // Non-leaf nodes identical in size to their Window should not be focusable.
        if (areBoundsIdenticalToWindow(node, view) && node.childCount > 0) {
            return false
        }

        if (!node.isVisibleToUser) {
            return false
        }

        if (isAccessibilityFocusable(node, view)) {
            if (!hasVisibleChildren(view)) {
                // Leaves that are accessibility focusable are never ignored, even if they don't have a
                // speakable description
                return true
            } else if (isSpeakingNode(node, view)) {
                // Node is focusable and has something to speak
                return true
            }

            // Node is focusable and has nothing to speak
            return false
        }

        // if view is not accessibility focusable, it needs to have text and no focusable ancestors.
        if (!hasText(node)) {
            return false
        }

        if (!hasFocusableAncestor(view)) {
            return true
        }

        return false
    }

    @JvmStatic
    fun generateAccessibilityTree(view: View): AXTreeNode {
        val axTree = AXTreeNode(view)

        if (view is ViewGroup) {
            val viewGroup = view
            for (i in 0 until viewGroup.childCount) {
                val descendantTree = generateAccessibilityTree(viewGroup.getChildAt(i))
                axTree.addChild(descendantTree)
            }
        }

        return axTree
    }

    /**
     * These roles are defined by Google's TalkBack screen reader, and this list should be kept up to
     * date with their implementation. Details can be seen in their source code here:
     *
     *
     * https://github.com/google/talkback/blob/master/utils/src/main/java/Role.java
     */
    enum class AccessibilityRole(val value: String?) {
        NONE(null),
        BUTTON("android.widget.Button"),
        CHECK_BOX("android.widget.CompoundButton"),
        DROP_DOWN_LIST("android.widget.Spinner"),
        EDIT_TEXT("android.widget.EditText"),
        GRID("android.widget.GridView"),
        IMAGE("android.widget.ImageView"),
        IMAGE_BUTTON("android.widget.ImageView"),
        LIST("android.widget.AbsListView"),
        PAGER("androidx.viewpager.widget.ViewPager"),
        RADIO_BUTTON("android.widget.RadioButton"),
        SEEK_CONTROL("android.widget.SeekBar"),
        SWITCH("android.widget.Switch"),
        TAB_BAR("android.widget.TabWidget"),
        TOGGLE_BUTTON("android.widget.ToggleButton"),
        VIEW_GROUP("android.view.ViewGroup"),
        WEB_VIEW("android.webkit.WebView"),
        CHECKED_TEXT_VIEW("android.widget.CheckedTextView"),
        PROGRESS_BAR("android.widget.ProgressBar"),
        ACTION_BAR_TAB("android.app.ActionBar\$Tab"),
        DRAWER_LAYOUT("androidx.drawerlayout.widget.DrawerLayout"),
        SLIDING_DRAWER("android.widget.SlidingDrawer"),
        ICON_MENU("com.android.internal.view.menu.IconMenuView"),
        TOAST("android.widget.Toast\$TN"),
        DATE_PICKER_DIALOG("android.app.DatePickerDialog"),
        TIME_PICKER_DIALOG("android.app.TimePickerDialog"),
        DATE_PICKER("android.widget.DatePicker"),
        TIME_PICKER("android.widget.TimePicker"),
        NUMBER_PICKER("android.widget.NumberPicker"),
        SCROLL_VIEW("android.widget.ScrollView"),
        HORIZONTAL_SCROLL_VIEW("android.widget.HorizontalScrollView"),
        KEYBOARD_KEY("android.inputmethodservice.Keyboard\$Key"),
        ;

        companion object {
            fun fromValue(value: String): AccessibilityRole {
                for (role in entries) {
                    if (role.value != null && role.value == value) {
                        return role
                    }
                }
                return NONE
            }
        }
    }

    class AXTreeNode(val view: View) {
        val nodeInfo = checkNotNull(createNodeInfoFromView(view))
        private val mChildren: MutableList<AXTreeNode> = ArrayList()

        val children: List<AXTreeNode>
            get() = mChildren

        val childCount: Int
            get() = mChildren.size

        fun addChild(child: AXTreeNode) {
            mChildren.add(child)
        }

        val allNodes: List<AXTreeNode>
            get() = mutableListOf<AXTreeNode>().apply(::addAllNodes)

        fun addAllNodes(nodes: MutableList<AXTreeNode>) {
            nodes.add(this)
            for (child in mChildren) {
                child.addAllNodes(nodes)
            }
        }

        override fun toString(): String {
            val sb = StringBuilder()
            toStringInner(sb, "")
            return sb.toString()
        }

        private fun toStringInner(sb: StringBuilder, indent: String) {
            sb.append(view.javaClass.simpleName)
            val nextIndent = "$indent  "
            for (child in mChildren) {
                sb.append('\n')
                sb.append(indent)
                sb.append("-> ")
                child.toStringInner(sb, nextIndent)
            }
        }
    }
}
