package io.github.usefulness.testing.screenshot.verification

import com.sksamuel.scrimage.Dimension
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.color.Colors
import com.sksamuel.scrimage.composite.DifferenceComposite
import com.sksamuel.scrimage.composite.RedComposite
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.pixels.Pixel
import io.github.usefulness.testing.screenshot.verification.MetadataParser.ScreenshotMetadata
import io.github.usefulness.testing.screenshot.verification.Recorder.VerificationResult.Mismatch
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

internal class Recorder(
    private val emulatorSpecificFolder: File,
    private val metadata: List<ScreenshotMetadata>,
    private val referenceDirectory: File,
    private val failureDirectory: File,
) {

    fun record() {
        loadRecordedImages().forEach { (name, image) ->
            image.output(PngWriter.NoCompression, referenceDirectory.resolve("$name.png"))
        }
    }

    fun verify(tolerance: Float): VerificationResult {
        val reference = loadReferenceImages()
        val recorded = loadRecordedImages()

        // Ignore files in referenceDirectory, turns out to be useful storing screenshots for multiple build variants in a single directory
        val all = recorded.keys.associateWith { key -> reference[key] to recorded[key] }

        val missingImages = all.filterValues { (reference, incoming) -> reference == null || incoming == null }
        if (missingImages.isNotEmpty()) {
            error(
                "Missing image(s) for: ${missingImages.keys.joinToString(prefix = "\n", separator = "\n")}\n" +
                    "Did you forget to call `record`?",
            )
        }

        failureDirectory.deleteRecursively()
        val failures = mutableListOf<Mismatch.Item>()
        all.forEach { (key, input) ->
            val (existing, incoming) = input
            requireNotNull(existing)
            requireNotNull(incoming)

            val diffRms = existing.getRootMeetSquare(incoming)
            if (diffRms > tolerance) {
                failureDirectory.mkdirs()
                failures.add(Mismatch.Item(key = key, differenceRms = diffRms))
                val inArgb = existing.copy(BufferedImage.TYPE_INT_ARGB)
                val outArgb = incoming.copy(BufferedImage.TYPE_INT_ARGB)
                val redDiff = inArgb.composite(RedComposite(1.0), outArgb)
                val diffDiff = inArgb.composite(DifferenceComposite(0.9), outArgb)
                existing.output(PngWriter.MaxCompression, failureDirectory.resolve("${key}_expected.png"))
                incoming.output(PngWriter.MaxCompression, failureDirectory.resolve("${key}_actual.png"))
                redDiff.output(PngWriter.MaxCompression, failureDirectory.resolve("${key}_diff_red.png"))
                diffDiff.output(PngWriter.MaxCompression, failureDirectory.resolve("${key}_diff_diff.png"))
            }
        }

        return when {
            failures.isNotEmpty() -> Mismatch(failures)
            recorded.isEmpty() -> VerificationResult.NoImages
            else -> VerificationResult.Success
        }
    }

    sealed class VerificationResult {

        object Success : VerificationResult()

        object NoImages : VerificationResult()

        data class Mismatch(val items: List<Item>) : VerificationResult() {

            data class Item(
                val key: String,
                val differenceRms: Float,
            )
        }
    }

    private fun ImmutableImage.getRootMeetSquare(other: ImmutableImage): Float {
        val oldScreenshotPixels = pixels()
        val newScreenshotPixels = other.pixels()

        if (oldScreenshotPixels.size != newScreenshotPixels.size) {
            return Float.NaN
        }

        val diff = List(newScreenshotPixels.size) {
            val new = newScreenshotPixels[it]
            val old = oldScreenshotPixels[it]
            check(new.x == old.x)
            check(new.y == old.y)
            Pixel(new.x, new.y, abs(new.argb - old.argb))
        }
        val histogram = diff.histogram()

        return calculatedRootMeanSquare(histogram, dimensions())
    }

    private fun List<Pixel>.histogram(): List<Int> {
        val red = List(256) { it to 0 }.toMap().toMutableMap()
        val green = List(256) { it to 0 }.toMap().toMutableMap()
        val blue = List(256) { it to 0 }.toMap().toMutableMap()

        forEach { pixel ->
            red[pixel.red()] = red.getValue(pixel.red()) + 1
            green[pixel.green()] = green.getValue(pixel.green()) + 1
            blue[pixel.blue()] = blue.getValue(pixel.blue()) + 1
        }

        return red.values + green.values + blue.values
    }

    private fun calculatedRootMeanSquare(histogram: List<Int>, imageDimensions: Dimension): Float {
        val sumOfSquares = histogram
            .mapIndexed { index, value -> value * (index % 256) * (index % 256) }
            .sum()

        return sqrt(sumOfSquares.toFloat() / (imageDimensions.x * imageDimensions.y))
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
        var xPosition = 0
        tiles.forEach { columns ->
            var yPosition = 0
            columns.forEach { part ->
                composedImage = composedImage.overlay(part, xPosition, yPosition)
                yPosition += part.height
            }
            xPosition += columns.first().width
        }

        screenshot.name to composedImage
    }

    private fun loadReferenceImages() = referenceDirectory.listFiles().orEmpty()
        .filter { it.extension == "png" } // ignore `.DS_Store` file
        .associate { it.nameWithoutExtension to ImmutableImage.loader().fromFile(it) }
}
