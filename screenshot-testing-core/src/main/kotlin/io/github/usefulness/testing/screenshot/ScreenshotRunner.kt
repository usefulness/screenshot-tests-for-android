package io.github.usefulness.testing.screenshot

import android.app.Instrumentation
import android.os.Bundle
import com.facebook.testing.screenshot.ScreenshotImpl

/**
 * The ScreenshotRunner needs to be called from the top level Instrumentation test runner before and
 * after all the tests run.
 */

object ScreenshotRunner {
    /**
     * Call this exactly once in your process before any screenshots are generated.
     *
     * Typically this will be in `AndroidJUnitRunner#onCreate()`
     */
    @Suppress("UNUSED_PARAMETER")
    fun onCreate(instrumentation: Instrumentation?, arguments: Bundle?) = Unit

    /**
     * Call this exactly once after all your tests have run.
     *
     * Typically this can be in `AndroidJUnitRunner#finish()`
     */
    fun onDestroy() {
        if (ScreenshotImpl.hasBeenCreated()) {
            ScreenshotImpl.getInstance().flush()
        }
    }
}
