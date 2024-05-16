package io.github.usefulness.testing.screenshot.layouthierarchy

import android.graphics.Point
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.usefulness.testing.screenshot.ViewHelpers.Companion.setupView
import io.github.usefulness.testing.screenshot.tests.test.R
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.ArrayDeque
import java.util.Queue

@RunWith(AndroidJUnit4::class)
class LayoutHierarchyDumperTest {

    private lateinit var testedView: ViewGroup

    private val mAttributePlugin: AttributePlugin = object : AbstractAttributePlugin() {
        override fun accept(obj: Any?) = true

        override fun namespace(): String = "foo"

        override fun putAttributes(node: JSONObject, obj: Any, offset: Point) {
            put(node, "foo", "bar")
        }
    }

    private val mTextAttributePlugin: AttributePlugin = object : AbstractAttributePlugin() {
        override fun accept(obj: Any?): Boolean = obj is TextView

        override fun namespace() = "Text"

        override fun putAttributes(node: JSONObject, obj: Any, offset: Point) {
            put(node, "text", (obj as TextView).text.toString())
        }
    }

    /**
     * Utility class to make inspecting a serialized hierarchy easier
     */
    internal class ParsedViewDetail(
        val name: String,
        val absoluteRect: Rect,
    ) {
        val children: MutableList<ParsedViewDetail> = ArrayList()

        fun childAt(index: Int) = children[index]

        companion object {
            fun convert(view: View) = convert(LayoutHierarchyDumper.create().dumpHierarchy(view))

            private fun convert(node: JSONObject): ParsedViewDetail {
                val left = node.getInt(AttributePlugin.KEY_LEFT)
                val top = node.getInt(AttributePlugin.KEY_TOP)
                val detail = ParsedViewDetail(
                    node.getString(AttributePlugin.KEY_CLASS),
                    Rect(
                        left,
                        top,
                        left + node.getInt(AttributePlugin.KEY_WIDTH),
                        top + node.getInt(AttributePlugin.KEY_HEIGHT),
                    ),
                )

                val children = node.optJSONArray(HierarchyPlugin.KEY_CHILDREN) ?: return detail

                for (i in 0 until children.length()) {
                    detail.children.add(convert(children.getJSONObject(i)))
                }

                return detail
            }
        }
    }

    @Before
    fun setUp() {
        testedView = LayoutInflater.from(ApplicationProvider.getApplicationContext())
            .inflate(R.layout.testing_for_view_hierarchy, null, false) as ViewGroup
    }

    @Test
    fun testClassNames() {
        val node = ParsedViewDetail.convert(testedView)
        Assert.assertEquals("android.widget.LinearLayout", node.name)
        Assert.assertEquals("android.widget.TextView", node.childAt(2).childAt(0).name)
    }

    @Test
    fun testBasicCoordinateCheck() {
        setupView(testedView).setExactHeightPx(1000).setExactWidthPx(20000).layout()
        val node = ParsedViewDetail.convert(testedView)
        Assert.assertEquals(0, node.absoluteRect.top.toLong())
        Assert.assertEquals(0, node.absoluteRect.left.toLong())
        Assert.assertEquals(
            node.childAt(0).absoluteRect.bottom.toLong(),
            node.childAt(1).absoluteRect.top.toLong(),
        )
        Assert.assertTrue(node.childAt(0).absoluteRect.bottom != 0)
    }

    @Test
    fun testNestedAbsoluteCoordinates() {
        setupView(testedView).setExactHeightPx(1000).setExactWidthPx(20000).layout()
        val node = ParsedViewDetail.convert(testedView)

        val textViewHeight = testedView.getChildAt(0).height

        Assert.assertEquals(
            (3 * textViewHeight).toLong(),
            node.childAt(2).childAt(1).absoluteRect.top.toLong(),
        )
    }

    @Test
    fun testDumpHierarchyOnNestedNode() {
        setupView(testedView).setExactHeightPx(1000).setExactWidthPx(20000).layout()
        val node = ParsedViewDetail.convert(testedView.getChildAt(2))

        Assert.assertEquals(0, node.absoluteRect.top.toLong())
        Assert.assertEquals(0, node.absoluteRect.left.toLong())
        val textViewHeight = testedView.getChildAt(0).height

        Assert.assertEquals(textViewHeight.toLong(), node.childAt(1).absoluteRect.top.toLong())
    }

    @Test
    fun testPluginDumps() {
        setupView(testedView).setExactHeightPx(1000).setExactWidthPx(20000).layout()

        val dumper =
            LayoutHierarchyDumper.createWith(
                emptyList(),
                listOf(mAttributePlugin),
            )
        val root = dumper.dumpHierarchy(testedView)

        Assert.assertEquals("bar", root.getString("foo:foo"))
    }

    @Test
    fun testPluginDumpsRecursively() {
        setupView(testedView).setExactHeightPx(1000).setExactWidthPx(20000).layout()

        val dumper =
            LayoutHierarchyDumper.createWith(
                emptyList(),
                listOf(mTextAttributePlugin),
            )

        val node = dumper.dumpHierarchy(testedView)

        val allText: MutableList<String> = ArrayList()
        val toCheck: Queue<JSONObject> = ArrayDeque()
        toCheck.offer(node)
        while (!toCheck.isEmpty()) {
            val item = toCheck.poll().let(::checkNotNull)
            val maybeText = item.optString("Text:text")
            if (maybeText.isNotEmpty()) {
                allText.add(maybeText)
            }
            val children = item.optJSONArray(HierarchyPlugin.KEY_CHILDREN) ?: continue
            for (i in 0 until children.length()) {
                toCheck.offer(children.getJSONObject(i))
            }
        }

        val expected: MutableList<String> = ArrayList()
        expected.add("foobar")
        expected.add("foobar2")
        expected.add("foobar3")
        expected.add("foobar4")

        Assert.assertEquals(expected, allText)
    }
}
