package io.github.usefulness.testing.screenshot

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.usefulness.testing.screenshot.Screenshot.snap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is not really a test, this test is just a "fixture" for all the tests for the scripts
 * related to running tests and getting screenshots.
 */
@RunWith(AndroidJUnit4::class)
class ScriptsFixtureTest {
    private lateinit var mTextView: TextView

    @get:Rule
    val rule = activityScenarioRule<MyActivity>()

    @Before
    fun setUp() {
        rule.scenario.onActivity { activity ->
            mTextView = TextView(activity)
            mTextView.text = "foobar"

            // Unfortunately TextView needs a LayoutParams for onDraw
            mTextView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        measureAndLayout()
    }

    @Test
    fun testGetTextViewScreenshot() {
        snap(mTextView).record()
    }

    @Test
    fun testSecondScreenshot() {
        mTextView.text = "foobar3"
        measureAndLayout()
        snap(mTextView).record()
    }

    private fun measureAndLayout() {
        var exception: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            try {
                mTextView.measure(
                    View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY),
                )
                mTextView.layout(0, 0, mTextView.measuredWidth, mTextView.measuredHeight)
            } catch (throwable: Throwable) {
                exception = throwable
            }
        }
        exception?.let { throw it }
    }

    companion object {
        private const val HEIGHT = 100
        private const val WIDTH = 200
    }
}
