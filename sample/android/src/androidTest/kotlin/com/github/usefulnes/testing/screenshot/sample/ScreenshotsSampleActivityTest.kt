package com.github.usefulnes.testing.screenshot.sample

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import com.facebook.testing.screenshot.Screenshot
import com.github.usefulness.testing.screenshot.sample.R
import org.hamcrest.core.AllOf.allOf
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
