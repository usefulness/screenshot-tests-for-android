package io.github.usefulness.testing.screenshot.verification

import com.dropbox.differ.Image
import com.dropbox.differ.SimpleImageComparator
import com.sksamuel.scrimage.Dimension
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.canvas.drawables.Rect
import com.sksamuel.scrimage.color.Colors
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.pixels.Pixel
import io.github.usefulness.testing.screenshot.ComparisonMethod
import io.github.usefulness.testing.screenshot.verification.MetadataParser.ScreenshotMetadata
import io.github.usefulness.testing.screenshot.verification.Recorder.VerificationResult.Mismatch
import java.awt.Color
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

    fun verify(comparisonMethod: ComparisonMethod): VerificationResult {
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

        val comparator: (ImmutableImage, ImmutableImage) -> Number = when (comparisonMethod) {
            is ComparisonMethod.DropboxDiffer -> { existing, incoming ->
                val comparator = SimpleImageComparator(
                    maxDistance = comparisonMethod.maxDistance,
                    hShift = comparisonMethod.hShift,
                    vShift = comparisonMethod.vShift,
                )
                val result = comparator.compare(
                    left = existing.dropbox(),
                    right = incoming.dropbox(),
                )

                result.pixelDifferences
            }

            is ComparisonMethod.RootMeanSquareErrorValue -> { existing, incoming ->
                existing.getRootMeetSquare(incoming)
            }
        }

        failureDirectory.deleteRecursively()
        val failures = mutableListOf<Mismatch.Item>()
        all.forEach { (key, input) ->
            val (existing, incoming) = input
            requireNotNull(existing)
            requireNotNull(incoming)

            val difference = comparator(existing, incoming)
            if (difference.toDouble() > 0) {
                failureDirectory.mkdirs()
                failures.add(Mismatch.Item(key = key, difference = difference))
                val inArgb = existing.copy(BufferedImage.TYPE_INT_ARGB)
                val outArgb = incoming.copy(BufferedImage.TYPE_INT_ARGB)
                val redDiff = createRedPixels(existing = inArgb, incoming = outArgb)
                val redBorder = createRedBorder(existing = inArgb, incoming = outArgb)
                existing.output(PngWriter.NoCompression, failureDirectory.resolve("${key}_expected.png"))
                incoming.output(PngWriter.NoCompression, failureDirectory.resolve("${key}_actual.png"))
                redDiff.output(PngWriter.NoCompression, failureDirectory.resolve("${key}_diff_red.png"))
                redBorder.output(PngWriter.NoCompression, failureDirectory.resolve("${key}_diff_border.png"))
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
                val difference: Number,
            )
        }
    }

    private fun ImmutableImage.getRootMeetSquare(other: ImmutableImage): Float {
        val oldScreenshotPixels = pixels()
        val newScreenshotPixels = other.pixels()

        if (oldScreenshotPixels.size != newScreenshotPixels.size) {
            return Float.MAX_VALUE
        }
        val diff = List(newScreenshotPixels.size) {
            val new = newScreenshotPixels[it]
            val old = oldScreenshotPixels[it]
            check(new.x == old.x)
            check(new.y == old.y)

            val redDiff = abs(new.red() - old.red())
            val greenDiff = abs(new.green() - old.green())
            val blueDiff = abs(new.green() - old.green())

            Pixel(new.x, new.y, redDiff, greenDiff, blueDiff, 0)
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

    private fun createRedPixels(existing: ImmutableImage, incoming: ImmutableImage) = incoming.map { new ->
        val old = runCatching { existing.pixel(new.x, new.y) }.getOrNull()

        if (new != old) {
            Color.RED
        } else {
            new.toColor().awt()
        }
    }

    private fun createRedBorder(existing: ImmutableImage, incoming: ImmutableImage): ImmutableImage {
        val oldScreenshotPixels = existing.pixels()
        val newScreenshotPixels = incoming.pixels()
        val differentPixels = List(newScreenshotPixels.size) {
            val new = newScreenshotPixels[it]
            val old = oldScreenshotPixels.getOrNull(it)
            val difference = if (old == null) {
                Int.MAX_VALUE
            } else {
                abs(new.argb - old.argb)
            }
            Pixel(new.x, new.y, difference)
        }
            .asSequence()
            .filter { it.argb > 0 }
        val (startX, startY) = differentPixels.minOf { it.x } to differentPixels.minOf { it.y }
        val (endX, endY) = differentPixels.maxOf { it.x } to differentPixels.maxOf { it.y }

        return incoming.toCanvas().draw(
            Rect(startX, startY, endX - startX, endY - startY) { g2 ->
                g2.setBasicStroke(5f)
                g2.color = Color.RED
            },
        ).image
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

private fun ImmutableImage.dropbox() = object : Image {
    override val height = this@dropbox.height
    override val width = this@dropbox.width

    override fun getPixel(x: Int, y: Int): com.dropbox.differ.Color {
        val pixel = pixel(x, y)

        return com.dropbox.differ.Color(
            r = pixel.red(),
            g = pixel.green(),
            b = pixel.blue(),
            a = pixel.alpha(),
        )
    }
}
