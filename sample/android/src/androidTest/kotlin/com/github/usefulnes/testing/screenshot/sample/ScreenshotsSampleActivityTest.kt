package com.github.usefulnes.testing.screenshot.sample

import androidx.test.ext.junit.rules.activityScenarioRule
import com.facebook.testing.screenshot.Screenshot
import org.junit.Rule
import org.junit.Test
import sample.ScreenshotsSampleActivity

class ScreenshotsSampleActivityTest {

    @get:Rule
    val rule = activityScenarioRule<ScreenshotsSampleActivity>()

    @Test
    fun testScreenshotEntireActivity() {
        rule.scenario.onActivity { activity ->
            Screenshot.snapActivity(activity).record()
        }
    }

    @Test
    fun testScreenshotEntireActivityWithoutAccessibilityMetadata() {
        rule.scenario.onActivity { activity ->
            Screenshot.snapActivity(activity).setIncludeAccessibilityInfo(false).record()
        }
    }
}
