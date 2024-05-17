package io.github.usefulness.testing.screenshot.verification

import io.github.usefulness.testing.screenshot.verification.MetadataParser.ScreenshotMetadata

internal fun ScreenshotMetadata.getTileName(x: Int, y: Int): String = if (x == 0 && y == 0) {
    "$name.png"
} else {
    "${name}_${x}_$y.png"
}
