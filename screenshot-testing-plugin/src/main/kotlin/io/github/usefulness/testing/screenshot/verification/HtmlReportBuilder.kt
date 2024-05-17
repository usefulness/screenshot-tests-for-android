package io.github.usefulness.testing.screenshot.verification

import io.github.usefulness.testing.screenshot.verification.MetadataParser.ScreenshotHierarchyDump.ViewNode
import io.github.usefulness.testing.screenshot.verification.MetadataParser.ScreenshotMetadata
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

internal class HtmlReportBuilder(
    private val emulatorSpecificFolder: File,
    private val reportDirectory: File,
) {

    fun generate(): Output {
        val metadata = MetadataParser.parseMetadata(emulatorSpecificFolder.resolve("metadata.json"))
        reportDirectory.deleteRecursively()
        reportDirectory.mkdirs()

        copyStaticAssets()
        emulatorSpecificFolder.copyRecursively(target = reportDirectory)

        val index = reportDirectory.resolve("index.html")
        index.writeText(buildHtml(metadata))

        return Output(
            numberOfScreenshots = metadata.size,
            reportEntrypoint = index,
        )
    }

    private fun buildHtml(screenshots: List<ScreenshotMetadata>) = buildString {
        var alternate = false
        appendLine("<!DOCTYPE html>")
        appendLine("<html>")
        appendLine("<head>")
        appendLine("<title>Screenshot Test Results</title>")
        appendLine("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js\"></script>")
        appendLine("<script src=\"https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.3/jquery-ui.min.js\"></script>")
        appendLine("<script src=\"default.js\"></script>")
        appendLine(
            "<link rel=\"stylesheet\" href=\"https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.3/themes/smoothness/jquery-ui.css\" />",
        )
        appendLine("<link rel=\"stylesheet\" href=\"default.css\"></head>")
        appendLine("<body>")
        screenshots.forEachIndexed { index, screenshot ->
            alternate = !alternate
            val name = screenshot.name.trim()
            val (filePackage, fileName) = if (name.contains(".")) {
                name.substringBeforeLast(".") to name.substringAfterLast(".")
            } else {
                "" to name
            }

            appendLine("<div class=\"screenshot ${if (alternate) "alternate" else ""}\">")
            appendLine("<div class=\"screenshot_name\">")
            appendLine("<span class=\"demphasize\">$filePackage.</span>$fileName")
            appendLine("</div>")
            screenshot.group?.let { group ->
                appendLine("<div class=\"screenshot_group\">$group</div>")
            }
            screenshot.extras?.takeIf { it.isNotEmpty() }?.let { extras ->
                val extraHtml = extras.entries.joinToString { (key, value) -> "*****$key*****\n\n$value\n\n\n" }.trim()
                appendLine("<button class=\"extra\" data=\"$extraHtml\">Extra info</button>")
            }
            screenshot.description?.let { description ->
                appendLine("<div class=\"screenshot_description\">$description</div>")
            }

            if (screenshot.error == null) {
                val viewHierarchyDump = MetadataParser.readViewHierarchy(reportDirectory.resolve(screenshot.viewHierarchy))
                appendLine("<div class=\"flex-wrapper\">")
                appendImage(screenshot = screenshot, node = viewHierarchyDump.viewHierarchy, parentId = index)
                appendLine("<div class=\"command-wrapper\">")
                appendCommands()
                appendViewHierarchy(hierarchy = viewHierarchyDump.viewHierarchy, parentId = index)
                viewHierarchyDump.axHierarchy?.let { hierarchy ->
                    appendAccessibilityHierarchy(hierarchy = hierarchy, parentId = index)
                }
                appendLine("</div>")
                appendLine("</div>")
            } else {
                appendLine("<div class=\"screenshot_error\">${screenshot.error}</div>")
            }

            appendLine("</div>")
            appendLine("<div class=\"clearfix\"></div>")
            appendLine("<hr/>")
        }
        appendLine("</body></html>")
    }

    private fun StringBuilder.appendViewHierarchy(hierarchy: ViewNode, parentId: Int) {
        appendLine("<h3>View Hierarchy</h3>")
        appendLine("<div class=\"view-hierarchy\">")
        appendViewHierarchyTreeNode(hierarchy, parentId)
        appendLine("</div>")
    }

    private fun StringBuilder.appendAccessibilityHierarchy(hierarchy: JsonObject, parentId: Int) {
        if (hierarchy.isEmpty()) {
            return
        }
        appendLine("<h3>Accessibility Hierarchy</h3>")
        appendLine("<div class=\"view-hierarchy\">")
        appendAccessibilityHierarchyTreeNode(hierarchy, parentId)
        appendLine("</div>")
    }

    private fun StringBuilder.appendViewHierarchyTreeNode(node: ViewNode, parentId: Int) {
        appendLine("<details target=\"#$parentId-${getViewHierarchyOverlayNodeId(node)}\">")
        appendLine("<summary>${node.className}</summary>")
        appendLine("<ul>")
        listOf(
            "width" to node.width,
            "height" to node.height,
            "left" to node.left,
            "top" to node.top,
        ).forEach { (key, value) ->
            appendLine("<li><strong>$key:</strong> $value</li>")
        }
        appendLine("</ul>")
        node.children.forEach { child ->
            appendViewHierarchyTreeNode(child, parentId)
        }
        appendLine("</details>")
    }

    private fun StringBuilder.appendAccessibilityHierarchyTreeNode(node: JsonObject, parentId: Int) {
        appendLine("<details>")
        appendLine("<summary>${node.getValue("class").jsonPrimitive.content}</summary>")
        appendLine("<ul>")
        node.filterKeys { it !in listOf("class", "children") }.forEach { (key, value) ->
            appendLine("<li><strong>$key:</strong> $value</li>")
        }
        appendLine("</ul>")
        node["children"]?.takeIf { it != JsonNull }?.jsonArray?.forEach { child ->
            appendAccessibilityHierarchyTreeNode(child.jsonObject, parentId)
        }
        appendLine("</details>")
    }

    private fun StringBuilder.appendCommands() {
        appendLine("<button class=\"toggle_dark\">Toggle Dark Background</button>")
        appendLine("<button class=\"toggle_hierarchy\">Toggle View Hierarchy Overlay</button>")
    }

    private fun StringBuilder.appendImage(screenshot: ScreenshotMetadata, node: ViewNode, parentId: Int) {
        appendLine("<div class=\"img-block\">")
        appendLine("<div class=\"img-wrapper\">")
        appendLine("<table>")
        repeat(screenshot.tileHeight) { y ->
            appendLine("<tr>")
            repeat(screenshot.tileWidth) { x ->
                appendLine("<td>")
                val imageFile = screenshot.getTileName(x = x, y = y)
                if (imageFile.exists()) {
                    appendLine("<img src=\"${imageFile.toRelativeString(reportDirectory)}\" />")
                }
            }
        }
        appendLine("</td>")
        appendLine("</tr>")
        appendLine("</table>")
        appendLine("<div class=\"hierarchy-overlay\">")
        appendViewHierarchyNodes(node = node, parentId = parentId)
        appendLine("</div></div></div>")
    }

    private fun StringBuilder.appendViewHierarchyNodes(node: ViewNode, parentId: Int) {
        appendViewHierarchyNode(node, parentId = parentId)
    }

    private fun StringBuilder.appendViewHierarchyNode(node: ViewNode, parentId: Int) {
        val left = node.left
        val top = node.top
        val width = node.width - 4
        val height = node.height - 4
        val id = getViewHierarchyOverlayNodeId(node)
        appendLine(
            """
            <div
              class="hierarchy-node"
              style="left:${left}px;top:${top}px;width:${width}px;height:${height}px;"
              id="$parentId-$id"></div>
            """.trimIndent(),
        )
        node.children.forEach {
            appendViewHierarchyNode(it, parentId)
        }
    }

    private fun getViewHierarchyOverlayNodeId(node: ViewNode): String {
        val className = node.className.replace(".", "-")

        return "node-$className-${node.left}-${node.top}-${node.width}-${node.height}"
    }

    private fun ScreenshotMetadata.getTileName(x: Int, y: Int): File {
        val name = if (x == 0 && y == 0) {
            "$name.png"
        } else {
            "${name}_${x}_$y.png"
        }
        return reportDirectory.resolve(name)
    }

    /**
     * Copy static assets required for rendering the HTML
     */
    private fun copyStaticAssets() {
        fun copyAsset(path: String) {
            loadResource("/static_assets/$path").copyTo(reportDirectory.resolve(path).outputStream())
        }
        copyAsset("default.css")
        copyAsset("default.js")
        copyAsset("background.png")
        copyAsset("background_dark.png")
    }

    data class Output(
        val numberOfScreenshots: Int,
        val reportEntrypoint: File,
    )
}
