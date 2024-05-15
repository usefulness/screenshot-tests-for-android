package io.github.usefulness.testing.screenshot

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.usefulness.testing.screenshot.ViewHelpers.Companion.setupView
import io.github.usefulness.testing.screenshot.tests.test.R
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewHelpersTest {
    private lateinit var mTextView: TextView
    private lateinit var targetContext: Context

    @Before
    fun setUp() {
        targetContext = ApplicationProvider.getApplicationContext()
        mTextView = TextView(targetContext)
        mTextView.text = "foobar"
    }

    @Test
    fun testPreconditions() {
        assertThat(mTextView.measuredHeight).isEqualTo(0)
    }

    @Test
    fun testMeasureWithoutHeight() {
        setupView(mTextView).setExactWidthDp(100).layout()

        assertThat(mTextView.measuredHeight).isGreaterThan(0)
    }

    @Test
    fun testMeasureWithoutHeightPx() {
        setupView(mTextView).setExactWidthPx(100).layout()

        assertThat(mTextView.measuredHeight).isGreaterThan(0)
    }

    @Test
    fun testMeasureForOnlyWidth() {
        setupView(mTextView).setExactHeightPx(100).layout()

        assertThat(mTextView.measuredHeight).isEqualTo(100)
        assertThat(mTextView.measuredWidth).isGreaterThan(0)
    }

    @Test
    fun testBothWrapContent() {
        setupView(mTextView).layout()

        assertThat(mTextView.measuredHeight).isGreaterThan(0)
        assertThat(mTextView.measuredWidth).isGreaterThan(0)
    }

    @Test
    fun testHeightAndWidthCorrectlyPropagated() {
        setupView(mTextView).setExactHeightDp(100).setExactWidthDp(1000).layout()

        assertThat(mTextView.measuredWidth)
            .isGreaterThan(mTextView.measuredHeight)
    }

    @Test
    fun testListViewHeight() {
        val view = ListView(targetContext)
        view.dividerHeight = 0
        val adapter = ArrayAdapter<String>(targetContext, R.layout.testing_simple_textview)
        view.adapter = adapter

        List(20) { adapter.add("foo $it") }

        setupView(view).guessListViewHeight().setExactWidthDp(200).layout()

        assertThat(view.measuredHeight).isGreaterThan(10)

        val oneHeight = view.getChildAt(0).measuredHeight
        assertThat(view.measuredHeight).isEqualTo(oneHeight * 20)
    }

    @Test
    fun testMaxHeightLessThanHeight() {
        setupView(mTextView).setMaxHeightPx(100).layout()
        assertThat(mTextView.measuredHeight).isLessThan(100)
    }

    @Test
    fun testMaxHeightUsesFullHeight() {
        setupView(mTextView).setMaxHeightPx(1).layout()
        assertThat(mTextView.measuredHeight).isEqualTo(1)
    }
}
