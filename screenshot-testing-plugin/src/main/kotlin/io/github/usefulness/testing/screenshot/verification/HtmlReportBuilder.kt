package io.github.usefulness.testing.screenshot.verification

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
        index.writeText("TODO")

        return Output(
            numberOfScreenshots = 100,
            reportEntrypoint = index,
        )
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

/**
 * def generate_html(output_dir):
 *     with open(join(output_dir, "metadata.json")) as m:
 *         screenshots = json.load(m)
 *     alternate = False
 *     index_html = abspath(join(output_dir, "index.html"))
 *     with codecs.open(index_html, mode="w", encoding="utf-8") as html:
 *         html.write("<!DOCTYPE html>")
 *         html.write("<html>")
 *         html.write("<head>")
 *         html.write("<title>Screenshot Test Results</title>")
 *         html.write(
 *             '<script src="https://ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js"></script>'
 *         )
 *         html.write(
 *             '<script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.3/jquery-ui.min.js"></script>'
 *         )
 *         html.write('<script src="default.js"></script>')
 *         html.write(
 *             '<link rel="stylesheet" href="https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.3/themes/smoothness/jquery-ui.css" />'
 *         )
 *         html.write('<link rel="stylesheet" href="default.css"></head>')
 *         html.write("<body>")
 *
 *         screenshot_num = 0
 *         for screenshot in sort_screenshots(screenshots):
 *             screenshot_num += 1
 *             alternate = not alternate
 *             canonical_name = screenshot["name"]
 *             package = ""
 *             name = canonical_name
 *             if "." in canonical_name:
 *                 last_seperator = canonical_name.rindex(".") + 1
 *                 package = canonical_name[:last_seperator]
 *                 name = canonical_name[last_seperator:]
 *
 *             html.write(
 *                 '<div class="screenshot %s">' % ("alternate" if alternate else "")
 *             )
 *             html.write('<div class="screenshot_name">')
 *             html.write('<span class="demphasize">%s</span>%s' % (package, name))
 *             html.write("</div>")
 *
 *             group = screenshot.get("group")
 *             if group:
 *                 html.write('<div class="screenshot_group">%s</div>' % group)
 *
 *             extras = screenshot.get("extras")
 *             if extras is not None:
 *                 str = ""
 *                 for key, value in extras:
 *                     if key is not None:
 *                         str = str + "*****" + key + "*****\n\n" + value + "\n\n\n"
 *                 if str != "":
 *                     extra_html = (
 *                             '<button class="extra" data="%s">Extra info</button>' % str
 *                     )
 *                     html.write(extra_html.encode("utf-8").strip())
 *
 *             description = screenshot.get("description")
 *             if description is not None:
 *                 html.write('<div class="screenshot_description">%s</div>' % description)
 *
 *             error = screenshot.get("error")
 *             if error is not None:
 *                 html.write('<div class="screenshot_error">%s</div>' % error)
 *             else:
 *                 hierarchy_data = get_view_hierarchy(output_dir, screenshot)
 *                 if hierarchy_data and KEY_VIEW_HIERARCHY in hierarchy_data:
 *                     hierarchy = hierarchy_data[KEY_VIEW_HIERARCHY]
 *                     ax_hierarchy = hierarchy_data[KEY_AX_HIERARCHY]
 *                 else:
 *                     hierarchy = hierarchy_data
 *                     ax_hierarchy = None
 *
 *                 html.write('<div class="flex-wrapper">')
 *                 write_image(
 *                     hierarchy,
 *                     output_dir,
 *                     html,
 *                     screenshot,
 *                     screenshot_num,
 *                 )
 *                 html.write('<div class="command-wrapper">')
 *                 write_commands(html)
 *                 write_view_hierarchy(hierarchy, html, screenshot_num)
 *                 write_ax_hierarchy(ax_hierarchy, html, screenshot_num)
 *                 html.write("</div>")
 *                 html.write("</div>")
 *
 *             html.write("</div>")
 *             html.write('<div class="clearfix"></div>')
 *             html.write("<hr/>")
 *
 *         html.write("</body></html>")
 *         return index_html
 *
 */
