package io.github.usefulness.testing.screenshot.verification

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
internal object MetadataParser {

    fun parseMetadata(source: File): List<ScreenshotMetadata> {
        require(source.exists()) { "metadata.json does not exist at ${source.absolutePath}" }

        val rawInput = runCatching { Json.decodeFromStream<List<ScreenshotMetadata>>(source.inputStream()) }
            .getOrElse { cause ->
                throw IllegalStateException(
                    message = "Unable to parse metadata file, " +
                        "this commonly happens if you did not call `ScreenshotRunner.onDestroy()` from your instrumentation",
                    cause = cause,
                )
            }

        return rawInput.sortedWith(compareBy<ScreenshotMetadata> { it.group }.thenComparing(compareBy { it.name }))
    }

    fun readViewHierarchy(source: File) = Json.decodeFromStream<ScreenshotHierarchyDump>(source.inputStream())

    @Serializable
    data class ScreenshotMetadata(
        val name: String,
        val description: String? = null,
        val testClass: String,
        val testName: String,
        val tileWidth: Int,
        val tileHeight: Int,
        val viewHierarchy: String,
        val axIssues: String? = null,
        val error: String? = null,
        val group: String? = null,
        val extras: Map<String, String>? = null,
    )

    @Serializable
    data class ScreenshotHierarchyDump(
        val viewHierarchy: ViewNode,
        val version: Int,
        val axHierarchy: JsonObject?,
    ) {

        @Serializable
        data class ViewNode(
            @SerialName("class") val className: String = "android.view.View",
            val left: Int,
            val top: Int,
            val width: Int,
            val height: Int,
            val children: List<ViewNode> = emptyList(),
        )
    }
}
