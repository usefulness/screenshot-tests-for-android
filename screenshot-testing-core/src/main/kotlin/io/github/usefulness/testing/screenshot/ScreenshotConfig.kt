package io.github.usefulness.testing.screenshot

import androidx.annotation.Px
import io.github.usefulness.testing.screenshot.internal.Poko

@Poko
class ScreenshotConfig(
    @Px val tileSize: Int = 1536,
    /**
     * Set the maximum number of pixels this screenshot should produce. Producing any number higher
     * will throw an exception.
     *
     * Maximum number of pixels this screenshot should produce. Specify zero or a negative number for no limit.
     */
    @Px val maxPixels: Long = 10000000L,
)
