package io.github.usefulness.testing.screenshot.verification

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.color.Colors
import com.sksamuel.scrimage.composite.DifferenceComposite
import com.sksamuel.scrimage.composite.RedComposite
import com.sksamuel.scrimage.nio.PngWriter
import io.github.usefulness.testing.screenshot.verification.MetadataParser.ScreenshotMetadata
import io.github.usefulness.testing.screenshot.verification.Recorder.VerificationResult.Mismatch
import java.awt.image.BufferedImage
import java.io.File

internal class Recorder(
    private val emulatorSpecificFolder: File,
    private val metadata: List<ScreenshotMetadata>,
    private val referenceDirectory: File,
    private val failureDirectory: File,
) {

    private val tileSize = 512

    fun record() {
        loadRecordedImages().forEach { (name, image) ->
            image.output(PngWriter.NoCompression, referenceDirectory.resolve("$name.png"))
        }
    }

    fun verify(tolerance: Float): VerificationResult {
        val reference = loadReferenceImages()
        val recorded = loadRecordedImages()
        val all = reference.mapValues { (key, existing) -> existing to recorded[key] }

        val missingImages = all.filterValues { (_, incoming) -> incoming == null }
        if (missingImages.isNotEmpty()) {
            error("Missing reference image(s) for: ${missingImages.keys.joinToString(prefix = "\n", separator = "\n")}")
        }

        val failures = mutableListOf<Mismatch.Item>()
        all.forEach { (key, input) ->
            val (existing, incoming) = input
            requireNotNull(incoming)
            if (!existing.isSameAs(incoming)) {
                failures.add(Mismatch.Item(key = key))
                val inArgb = existing.copy(BufferedImage.TYPE_INT_ARGB)
                val outArgb = incoming.copy(BufferedImage.TYPE_INT_ARGB)
                val redDiff = inArgb.composite(RedComposite(1.0), outArgb)
                val diffDiff = inArgb.composite(DifferenceComposite(0.8), outArgb)
                failureDirectory.mkdirs()
                existing.output(PngWriter.MaxCompression, failureDirectory.resolve("${key}_expected.png"))
                incoming.output(PngWriter.MaxCompression, failureDirectory.resolve("${key}_actual.png"))
                redDiff.output(PngWriter.MaxCompression, failureDirectory.resolve("${key}_diff_red.png"))
                diffDiff.output(PngWriter.MaxCompression, failureDirectory.resolve("${key}_diff_diff.png"))
            }
        }

        return failures.takeIf { it.isNotEmpty() }?.let(VerificationResult::Mismatch) ?: VerificationResult.Success
    }

    sealed class VerificationResult {

        object Success : VerificationResult()

        data class Mismatch(val items: List<Item>) : VerificationResult() {

            data class Item(val key: String)
        }
    }

    private fun ImmutableImage.isSameAs(other: ImmutableImage): Boolean = arePixelsTheSame(other)

    private fun ImmutableImage.arePixelsTheSame(other: ImmutableImage): Boolean {
        val oldScreenshotPixels = pixels()
        val newScreenshotPixels = other.pixels()

        if (oldScreenshotPixels.size != newScreenshotPixels.size) {
            return false
        }

        oldScreenshotPixels.forEachIndexed { index, pixel ->
            if (pixel != newScreenshotPixels[index]) {
                return false
            }
        }

        return true
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
        .associate { it.nameWithoutExtension to ImmutableImage.loader().fromFile(it) }
}
