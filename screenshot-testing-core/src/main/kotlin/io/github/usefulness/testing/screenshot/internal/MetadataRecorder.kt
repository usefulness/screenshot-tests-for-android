package io.github.usefulness.testing.screenshot.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.FileNotFoundException

@OptIn(ExperimentalSerializationApi::class)
internal class MetadataRecorder(private val screenshotDirectories: ScreenshotDirectories) {
    private val metadataFileName = "metadata.json"
    private val metadata by lazy {
        try {
            screenshotDirectories.openInputFile(metadataFileName).use { metadataFile ->
                Json.decodeFromStream<List<ScreenshotMetadata>>(metadataFile).toMutableList()
            }
        } catch (ignored: FileNotFoundException) {
            mutableListOf()
        }
    }

    fun flush() {
        writeMetadata()
    }

    fun addNewScreenshot() = ScreenshotMetadataRecorder()

    internal inner class ScreenshotMetadataRecorder {
        private val mCurrentScreenshotMetadata = ScreenshotMetadata()

        fun save() {
            check(!metadata.contains(mCurrentScreenshotMetadata)) { "metadata was already saved" }
            metadata.add(mCurrentScreenshotMetadata)
        }

        fun withDescription(description: String?): ScreenshotMetadataRecorder {
            mCurrentScreenshotMetadata.description = description
            return this
        }

        fun withName(name: String): ScreenshotMetadataRecorder {
            mCurrentScreenshotMetadata.name = name
            return this
        }

        fun withTestClass(testClass: String?): ScreenshotMetadataRecorder {
            mCurrentScreenshotMetadata.testClass = testClass
            return this
        }

        fun withTestName(testName: String?): ScreenshotMetadataRecorder {
            mCurrentScreenshotMetadata.testName = testName
            return this
        }

        fun withTileWidth(width: Int): ScreenshotMetadataRecorder {
            mCurrentScreenshotMetadata.tileWidth = width
            return this
        }

        fun withTileHeight(height: Int): ScreenshotMetadataRecorder {
            mCurrentScreenshotMetadata.tileHeight = height
            return this
        }

        fun withViewHierarchy(viewHierarchyFilename: String): ScreenshotMetadataRecorder {
            mCurrentScreenshotMetadata.viewHierarchy = viewHierarchyFilename
            return this
        }

        fun withAxIssues(axIssuesFilename: String): ScreenshotMetadataRecorder {
            mCurrentScreenshotMetadata.axIssues = axIssuesFilename
            return this
        }

        fun withExtras(extras: Map<String, String>): ScreenshotMetadataRecorder {
            mCurrentScreenshotMetadata.extras = HashMap(extras)
            return this
        }

        fun withError(error: String): ScreenshotMetadataRecorder {
            mCurrentScreenshotMetadata.error = error
            return this
        }

        fun withGroup(group: String): ScreenshotMetadataRecorder {
            mCurrentScreenshotMetadata.group = group
            return this
        }
    }

    private fun writeMetadata() {
        screenshotDirectories.openOutputFile(metadataFileName).use { output ->
            Json.encodeToStream<List<ScreenshotMetadata>>(value = metadata, stream = output)
        }
    }

    @Serializable
    private class ScreenshotMetadata {
        var description: String? = null
        var name: String? = null
        var testClass: String? = null
        var testName: String? = null
        var tileWidth: Int = 0
        var tileHeight: Int = 0
        var viewHierarchy: String? = null
        var axIssues: String? = null
        var error: String? = null
        var group: String? = null
        var extras: Map<String, String>? = null
    }
}
