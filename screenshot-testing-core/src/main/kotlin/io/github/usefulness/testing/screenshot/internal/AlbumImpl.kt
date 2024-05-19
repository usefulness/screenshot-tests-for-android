package io.github.usefulness.testing.screenshot.internal

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

internal class AlbumImpl(screenshotDirectories: ScreenshotDirectories) : Album {
    private val metadataRecorder = MetadataRecorder(screenshotDirectories)
    private val reportArtifactsManager = ReportArtifactsManager(screenshotDirectories)

    override fun flush() {
        metadataRecorder.flush()
    }

    override fun writeBitmap(name: String, tilei: Int, tilej: Int, bitmap: Bitmap): String {
        val tileName = generateTileName(name, tilei, tilej)
        val filename = getScreenshotFilenameInternal(tileName)
        val tileBytes = ByteArrayOutputStream().use { os ->
            bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, os)
            os.toByteArray()
        }
        reportArtifactsManager.recordFile(filename, tileBytes)
        return tileName
    }

    override fun writeAxIssuesFile(name: String, data: String) {
        writeMetadataFile(getAxIssuesFilename(name), data)
    }

    override fun writeViewHierarchyFile(name: String, data: String) {
        writeMetadataFile(getViewHierarchyFilename(name), data)
    }

    private fun writeMetadataFile(name: String, data: String) {
        val out = data.toByteArray()
        reportArtifactsManager.recordFile(name, out)
    }

    override fun addRecord(recordBuilder: RecordBuilderImpl) {
        recordBuilder.checkState()
        if (metadataRecorder.snapshot().any { it.name == recordBuilder.name }) {
            if (recordBuilder.hasExplicitName()) {
                error("Can't create multiple screenshots with the same name: ${recordBuilder.name}")
            }

            error("Can't create multiple screenshots from the same test, or use .setName() to name each screenshot differently")
        }

        val tiling = recordBuilder.tiling

        metadataRecorder.addNew(
            screenshot = MetadataRecorder.ScreenshotMetadata(
                description = recordBuilder.description,
                name = recordBuilder.name,
                testClass = recordBuilder.testClass,
                testName = recordBuilder.testName,
                tileWidth = tiling.width,
                tileHeight = tiling.height,
                viewHierarchy = getViewHierarchyFilename(recordBuilder.name),
                axIssues = getAxIssuesFilename(recordBuilder.name),
                extras = recordBuilder.extras,
                error = recordBuilder.error,
                group = recordBuilder.group,
            ),
        )
    }

    /**
     * For a given screenshot, and a tile position, generates a name where we store the screenshot in
     * the album.
     *
     *
     * For backward compatibility with existing screenshot scripts, for the tile (0, 0) we use the
     * name directly.
     */
    private fun generateTileName(name: String, i: Int, j: Int): String {
        if (i == 0 && j == 0) {
            return name
        }

        return "${name}_${i}_$j"
    }

    companion object {
        private const val COMPRESSION_QUALITY = 90

        /**
         * Creates a "local" album that stores all the images on device.
         */
        @JvmStatic
        fun create(): AlbumImpl = AlbumImpl(ScreenshotDirectories())

        /**
         * Same as the public getScreenshotFile() except it returns the File even if the screenshot
         * doesn't exist.
         */
        private fun getScreenshotFilenameInternal(name: String?): String = "$name.png"

        private fun getViewHierarchyFilename(name: String?): String = name + "_dump.json"

        private fun getAxIssuesFilename(name: String?): String = name + "_issues.json"
    }
}
