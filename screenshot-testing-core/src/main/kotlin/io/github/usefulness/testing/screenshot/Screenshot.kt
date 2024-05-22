package io.github.usefulness.testing.screenshot

import android.app.Activity
import android.view.View
import io.github.usefulness.testing.screenshot.internal.ScreenshotImpl

/**
 * A testing tool for taking a screenshot during an Activity instrumentation test. This is really
 * useful while manually investigating how the rendering looks like after setting up some complex
 * set of conditions in the test. (Which might be hard to manually recreate)
 *
 *
 * Eventually we can use this to catch rendering changes, with very little work added to the
 * instrumentation test.
 */
object Screenshot {

    var defaultConfig = ScreenshotConfig()

    /**
     * Take a snapshot of an already measured and layout-ed view.
     *
     *
     * This method is thread safe.
     */
    @JvmStatic
    @JvmOverloads
    fun snap(measuredView: View, config: ScreenshotConfig = defaultConfig): RecordBuilder =
        ScreenshotImpl(config = config).snap(measuredView)

    /**
     * Take a snapshot of the activity and store it with the the testName.
     *
     *
     * This method is thread safe.
     */
    @JvmStatic
    @JvmOverloads
    fun snap(activity: Activity, config: ScreenshotConfig = defaultConfig): RecordBuilder =
        ScreenshotImpl(config = config).snapActivity(activity)

    @JvmField
    @Deprecated("Scheduled for removal in next major release")
    val MAX_PIXELS = ScreenshotConfig().maxPixels
}
