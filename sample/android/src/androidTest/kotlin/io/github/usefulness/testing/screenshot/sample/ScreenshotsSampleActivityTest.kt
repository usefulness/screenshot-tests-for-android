package io.github.usefulness.testing.screenshot.sample

import android.graphics.Color
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.updatePaddingRelative
import androidx.test.ext.junit.rules.activityScenarioRule
import io.github.usefulness.testing.screenshot.Screenshot
import io.github.usefulness.testing.screenshot.ViewHelpers
import org.junit.Rule
import org.junit.Test
import sample.ScreenshotsSampleActivity
import kotlin.math.roundToInt

class ScreenshotsSampleActivityTest {
    @get:Rule
    val rule = activityScenarioRule<ScreenshotsSampleActivity>()

    @Test
    fun testScreenshotEntireActivity() {
        rule.scenario.onActivity { activity ->
            Screenshot.snap(activity).record()
        }
    }

    @Test
    fun testScreenshotEntireActivityWithoutAccessibilityMetadata() {
        rule.scenario.onActivity { activity ->
            Screenshot.snap(activity).setIncludeAccessibilityInfo(false).record()
        }
    }

    @Test
    fun captureImage() {
        rule.scenario.onActivity { activity ->
            val text =
                TextView(activity).apply {
                    text = "foobar"
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setBackgroundColor(Color.GRAY)
                }
            val container =
                FrameLayout(activity).apply {
                    val padding = (activity.resources.displayMetrics.density * 16).roundToInt()
                    updatePaddingRelative(start = padding, end = padding, top = padding, bottom = padding)
                    addView(text)
                }
            ViewHelpers.setupView(container).layout()
            Screenshot.snap(container).record()
        }
    }
}
