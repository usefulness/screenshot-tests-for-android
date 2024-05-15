package io.github.usefulness.testing.screenshot.internal

import android.annotation.SuppressLint
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

internal class AlbumImpl(screenshotDirectories: ScreenshotDirectories) : Album {
    private val mAllNames: MutableSet<String> = HashSet()
    private val mMetadataRecorder = MetadataRecorder(screenshotDirectories)
    private val mReportArtifactsManager = ReportArtifactsManager(screenshotDirectories)

    override fun flush() {
        mMetadataRecorder.flush()
    }

    override fun writeBitmap(name: String, tilei: Int, tilej: Int, bitmap: Bitmap): String {
        val tileName = generateTileName(name, tilei, tilej)
        val filename = getScreenshotFilenameInternal(tileName)
        val os = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, os)
        mReportArtifactsManager.recordFile(filename, os.toByteArray())
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
        mReportArtifactsManager.recordFile(name, out)
    }

    /**
     * Add the given record to the album. This is called by RecordBuilderImpl#record() and so is an
     * internal detail.
     */
    @SuppressLint("SetWorldReadable")
    override fun addRecord(recordBuilder: RecordBuilderImpl) {
        recordBuilder.checkState()
        if (mAllNames.contains(recordBuilder.name)) {
            if (recordBuilder.hasExplicitName()) {
                throw AssertionError(
                    "Can't create multiple screenshots with the same name: " + recordBuilder.name,
                )
            }

            throw AssertionError(
                "Can't create multiple screenshots from the same test, or " +
                    "use .setName() to name each screenshot differently",
            )
        }

        val tiling = recordBuilder.tiling

        val screenshotNode =
            mMetadataRecorder
                .addNewScreenshot()
                .withDescription(recordBuilder.description)
                .withName(recordBuilder.name)
                .withTestClass(recordBuilder.testClass)
                .withTestName(recordBuilder.testName)
                .withTileWidth(tiling.width)
                .withTileHeight(tiling.height)
                .withViewHierarchy(getViewHierarchyFilename(recordBuilder.name))
                .withAxIssues(getAxIssuesFilename(recordBuilder.name))
                .withExtras(recordBuilder.extras)

        recordBuilder.error?.let { screenshotNode.withError(it) }
        recordBuilder.group?.let { screenshotNode.withGroup(it) }

        mAllNames.add(recordBuilder.name)

        screenshotNode.save()
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
