package io.github.usefulness.testing.screenshot.internal

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import java.io.FileNotFoundException
import java.io.InputStreamReader

internal class MetadataRecorder(private val screenshotDirectories: ScreenshotDirectories) {
    private val metadataFileName = "metadata.json"
    private val metadata by lazy {
        try {
            screenshotDirectories.openInputFile(metadataFileName).use { metadataFile ->
                val gson = Gson()
                val jsonReader = JsonReader(InputStreamReader(metadataFile))
                gson.fromJson<List<ScreenshotMetadata>>(
                    jsonReader,
                    object : TypeToken<List<ScreenshotMetadata?>?>() {
                    }.type,
                )
                    .orEmpty()
                    .toMutableList()
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
        val gson = Gson()
        val json = gson.toJson(metadata)
        screenshotDirectories.openOutputFile(metadataFileName).use { output ->
            output.write(json.toByteArray())
        }
    }

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
