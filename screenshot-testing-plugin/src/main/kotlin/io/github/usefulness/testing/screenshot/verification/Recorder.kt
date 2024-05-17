package io.github.usefulness.testing.screenshot.verification

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.color.Colors
import com.sksamuel.scrimage.nio.PngWriter
import io.github.usefulness.testing.screenshot.verification.MetadataParser.ScreenshotMetadata
import java.io.File

internal class Recorder(
    private val emulatorSpecificFolder: File,
    private val metadata: List<ScreenshotMetadata>,
    private val referenceDirectory: File,
) {

    private val tileSize = 512

    fun record() {
        loadRecordedImages().forEach { (name, image) ->
            image.output(PngWriter.NoCompression, referenceDirectory.resolve("$name.png"))
        }
    }

    fun verify(tolerance: Float) {
        val reference = loadReferenceImages()
        val recorded = loadRecordedImages()
        error("tolerance=$tolerance")
    }

    private fun loadRecordedImages() = metadata.associate { screenshot ->
        val tiles = List(screenshot.tileWidth) { x ->
            List(screenshot.tileHeight) { y ->
                val tileFile = emulatorSpecificFolder.resolve(screenshot.getTileName(x = x, y = y))
                ImmutableImage.loader().fromFile(tileFile)
            }
        }
        var composedImage = ImmutableImage.filled(
            tiles.sumOf { it.first().width },
            tiles.first().sumOf { it.height },
            Colors.Transparent.awt(),
        )
        tiles.forEachIndexed { x, columns ->
            columns.forEachIndexed { y, part ->
                val xPosition = x * tileSize
                val yPosition = y * tileSize
                composedImage = composedImage.overlay(part, xPosition, yPosition)
            }
        }
        screenshot.name to composedImage
    }

    private fun loadReferenceImages() = referenceDirectory.listFiles().orEmpty()
        .associate { it.name to ImmutableImage.loader().fromFile(it) }
}

fun main() {
    val source = File(
        "/Users/mateuszkwiecinski/sourcy/screenshot-tests-for-android/sample/android/build/outputs/" +
            "connected_android_test_additional_output/debugAndroidTest/connected/Pixel_6_Pro_API_33(AVD) - 13",
    )
    val recorder = Recorder(
        emulatorSpecificFolder = source,
        metadata = MetadataParser.parseMetadata(source.resolve("metadata.json")),
        referenceDirectory = File("/Users/mateuszkwiecinski/sourcy/screenshot-tests-for-android/sample/android/screenshots"),
    )
    recorder.verify(tolerance = 0f)
}
