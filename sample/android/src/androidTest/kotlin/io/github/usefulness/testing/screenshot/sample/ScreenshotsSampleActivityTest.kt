package io.github.usefulness.testing.screenshot.sample

import androidx.test.ext.junit.rules.activityScenarioRule
import io.github.usefulness.testing.screenshot.Screenshot
import org.junit.Rule
import org.junit.Test
import sample.ScreenshotsSampleActivity

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
}
