package io.github.usefulness.testing.screenshot

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

@Suppress("unused")
class CustomScreenshotTestRunner : AndroidJUnitRunner() {
    override fun onCreate(args: Bundle) {
        ScreenshotRunner.onCreate(this, args)
        super.onCreate(args)
    }

    override fun finish(resultCode: Int, results: Bundle) {
        ScreenshotRunner.onDestroy()
        super.finish(resultCode, results)
    }
}
