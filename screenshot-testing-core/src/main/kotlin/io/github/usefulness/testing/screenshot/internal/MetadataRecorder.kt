package io.github.usefulness.testing.screenshot.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.FileNotFoundException

@OptIn(ExperimentalSerializationApi::class)
internal object MetadataRecorder {
    private const val metadataFileName = "metadata.json"
    private val metadata by lazy {
        try {
            ScreenshotDirectories.openInputFile(metadataFileName).use { metadataFile ->
                Json.decodeFromStream<List<ScreenshotMetadata>>(metadataFile).toMutableList()
            }
        } catch (ignored: FileNotFoundException) {
            mutableListOf()
        }
    }

    fun snapshot() = metadata.asSequence()

    fun flush() {
        ScreenshotDirectories.openOutputFile(metadataFileName).use { output ->
            Json.encodeToStream<List<ScreenshotMetadata>>(value = metadata, stream = output)
        }
    }

    fun addNew(screenshot: ScreenshotMetadata) {
        metadata.add(screenshot)
    }

    @Serializable
    internal data class ScreenshotMetadata(
        val description: String?,
        val name: String,
        val testClass: String?,
        val testName: String?,
        val tileWidth: Int,
        val tileHeight: Int,
        val viewHierarchy: String,
        val axIssues: String,
        val error: String?,
        val group: String?,
        val extras: Map<String, String>?,
    )
}
