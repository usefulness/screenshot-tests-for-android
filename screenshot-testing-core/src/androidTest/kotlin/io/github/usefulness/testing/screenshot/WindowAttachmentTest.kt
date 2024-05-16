package io.github.usefulness.testing.screenshot

import android.R
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.usefulness.testing.screenshot.WindowAttachment.dispatchAttach
import io.github.usefulness.testing.screenshot.WindowAttachment.setAttachInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WindowAttachmentTest {
    private var mAttachedCalled = 0
    private var mDetachedCalled = 0

    @get:Rule
    val rule = activityScenarioRule<MyActivity>()

    @Test
    fun testCalled() {
        rule.scenario.onActivity { activity ->
            val view = MyView(activity)
            val detacher = dispatchAttach(view)
            assertEquals(1, mAttachedCalled.toLong())
            assertEquals(0, mDetachedCalled.toLong())

            detacher.detach()
            assertEquals(1, mDetachedCalled.toLong())
        }
    }

    @Test
    fun testCalledForViewGroup() {
        rule.scenario.onActivity { activity ->
            val view = Parent(activity)
            val detacher = dispatchAttach(view)
            assertEquals(1, mAttachedCalled.toLong())
            assertEquals(0, mDetachedCalled.toLong())

            detacher.detach()
            assertEquals(1, mDetachedCalled.toLong())
        }
    }

    @Test
    fun testForNested() {
        rule.scenario.onActivity { activity ->
            val view = Parent(activity)
            val child = MyView(activity)
            view.addView(child)

            val detacher = dispatchAttach(view)
            assertEquals(2, mAttachedCalled.toLong())
            assertEquals(0, mDetachedCalled.toLong())

            detacher.detach()
            assertEquals(2, mDetachedCalled.toLong())
        }
    }

    @Test
    fun testAReallyAttachedViewIsntAttachedAgain() {
        lateinit var view: View
        rule.scenario.onActivity { activity ->
            view = MyView(activity)
            activity.setContentView(view)
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // Call some express method to make sure we're ready:
        Espresso.onView(ViewMatchers.withId(R.id.content)).perform(ViewActions.click())
        Espresso.onIdle()

        mAttachedCalled = 0
        mDetachedCalled = 0

        val detacher = dispatchAttach(view)
        detacher.detach()

        assertEquals(0, mAttachedCalled.toLong())
        assertEquals(0, mDetachedCalled.toLong())
    }

    @Test
    fun testSetAttachInfo() {
        rule.scenario.onActivity { activity ->
            val view = MyView(activity)
            setAttachInfo(view)

            assertNotNull(view.windowToken)
        }
    }

    inner class MyView(context: Context) : View(context) {

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            mAttachedCalled++
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            mDetachedCalled++
        }
    }

    inner class Parent(context: Context) : LinearLayout(context) {

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            mAttachedCalled++
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            mDetachedCalled++
        }
    }
}
