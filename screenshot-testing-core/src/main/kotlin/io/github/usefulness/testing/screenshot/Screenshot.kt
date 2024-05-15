package io.github.usefulness.testing.screenshot

import android.app.Activity
import android.view.View
import com.facebook.testing.screenshot.ScreenshotImpl

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

    /**
     * Take a snapshot of an already measured and layout-ed view. See adb-logcat for how to pull the
     * screenshot.
     *
     *
     * This method is thread safe.
     */
    @JvmStatic
    fun snap(measuredView: View): RecordBuilder = ScreenshotImpl.getInstance().snap(measuredView)

    /**
     * Take a snapshot of the activity and store it with the the testName. See the adb-logcat for how
     * to pull the screenshot.
     *
     *
     * This method is thread safe.
     */
    @JvmStatic
    fun snap(activity: Activity): RecordBuilder = ScreenshotImpl.getInstance().snapActivity(activity)

    val maxPixels = ScreenshotImpl.getMaxPixels()
}
