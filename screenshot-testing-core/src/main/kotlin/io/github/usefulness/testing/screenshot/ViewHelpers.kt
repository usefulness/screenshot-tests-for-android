package io.github.usefulness.testing.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.TypedValue
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.ListView

/**
 * A collection of static utilities for measuring and pre-drawing a view, usually a pre-requirement
 * for taking a Screenshot.
 *
 *
 * This will mostly be used something like this: `
 * ViewHelpers.setupView(view)
 * .setExactHeightPx(1000)
 * .setExactWidthPx(100)
 * .layout();
` *
 */
class ViewHelpers private constructor(private val mView: View) {
    private var mWidthMeasureSpec: Int
    private var mHeightMeasureSpec: Int
    private var mGuessListViewHeight = false

    init {
        mWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        mHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
    }

    /**
     * Measure and layout the view after all the configuration is done.
     *
     * @returns an AfterLayout object that can be used to perform common operations after the layout
     * is done such as dispatchPreDraw()l.
     */
    fun layout(): AfterLayout {
        if (!mGuessListViewHeight) {
            layoutInternal()
        } else {
            layoutWithHeightDetection()
        }
        dispatchOnGlobalLayout(mView)
        dispatchPreDraw(mView)

        return AfterLayout()
    }

    private fun layoutInternal() {
        do {
            mView.measure(mWidthMeasureSpec, mHeightMeasureSpec)
            layoutView()
        } while (mView.isLayoutRequested)
    }

    private fun layoutWithHeightDetection() {
        val view = mView as ListView
        mHeightMeasureSpec = MeasureSpec.makeMeasureSpec(HEIGHT_LIMIT, MeasureSpec.EXACTLY)
        layoutInternal()

        check(view.count == view.childCount) { "the ListView is too big to be auto measured" }

        var bottom = 0

        if (view.count > 0) {
            bottom = view.getChildAt(view.count - 1).bottom
        }

        if (bottom == 0) {
            bottom = 1
        }

        mHeightMeasureSpec = MeasureSpec.makeMeasureSpec(bottom, MeasureSpec.EXACTLY)
        layoutInternal()
    }

    /**
     * Configure the height in pixel
     */
    fun setExactHeightPx(px: Int): ViewHelpers {
        mHeightMeasureSpec = MeasureSpec.makeMeasureSpec(px, MeasureSpec.EXACTLY)
        validateHeight()
        return this
    }

    fun guessListViewHeight(): ViewHelpers {
        require(mView is ListView) { "guessListViewHeight needs to be used with a ListView" }
        mGuessListViewHeight = true
        validateHeight()
        return this
    }

    private fun validateHeight() {
        check(!(mGuessListViewHeight && mHeightMeasureSpec != 0)) { "Can't call both setExactHeight && guessListViewHeight" }
    }

    /**
     * Configure the width in pixels
     */
    fun setExactWidthPx(px: Int): ViewHelpers {
        mWidthMeasureSpec = MeasureSpec.makeMeasureSpec(px, MeasureSpec.EXACTLY)
        return this
    }

    /**
     * Configure the height in dip
     */
    fun setExactWidthDp(dp: Int): ViewHelpers {
        setExactWidthPx(dpToPx(dp))
        return this
    }

    /**
     * Configure the width in dip
     */
    fun setExactHeightDp(dp: Int): ViewHelpers {
        setExactHeightPx(dpToPx(dp))
        return this
    }

    /**
     * Configure the height in pixels
     */
    fun setMaxHeightPx(px: Int): ViewHelpers {
        mHeightMeasureSpec = MeasureSpec.makeMeasureSpec(px, MeasureSpec.AT_MOST)
        return this
    }

    /**
     * Configure the height in dip
     */
    fun setMaxHeightDp(dp: Int): ViewHelpers {
        setMaxHeightPx(dpToPx(dp))
        return this
    }

    /**
     * Configure the with in pixels
     */
    fun setMaxWidthPx(px: Int): ViewHelpers {
        mWidthMeasureSpec = MeasureSpec.makeMeasureSpec(px, MeasureSpec.AT_MOST)
        return this
    }

    /**
     * Configure the width in dip
     */
    fun setMaxWidthDp(dp: Int): ViewHelpers {
        setMaxWidthPx(dpToPx(dp))
        return this
    }

    /**
     * Some views (e.g. SimpleVariableTextLayoutView) in FB4A rely on the predraw. Actually I don't
     * know why, ideally it shouldn't.
     *
     *
     * However if you find that text is not showing in your layout, try dispatching the pre draw
     * using this method. Note this method is only supported for views that are not attached to a
     * Window, and the behavior is slightly different than views attached to a window. (Views attached
     * to a window have a single ViewTreeObserver for all child views, whereas for unattached views,
     * each child has its own ViewTreeObserver.)
     */
    private fun dispatchPreDraw(view: View) {
        while (view.viewTreeObserver.dispatchOnPreDraw()) {
            Unit
        }

        if (view is ViewGroup) {
            val vg = view
            for (i in 0 until vg.childCount) {
                dispatchPreDraw(vg.getChildAt(i))
            }
        }
    }

    private fun dispatchOnGlobalLayout(view: View) {
        if (view is ViewGroup) {
            val vg = view
            for (i in 0 until vg.childCount) {
                dispatchOnGlobalLayout(vg.getChildAt(i))
            }
        }

        view.viewTreeObserver.dispatchOnGlobalLayout()
    }

    private fun layoutView() {
        mView.layout(0, 0, mView.measuredWidth, mView.measuredHeight)
    }

    private fun dpToPx(dp: Int): Int {
        val resources = mView.context.resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics,
        ).toInt()
    }

    inner class AfterLayout {

        fun draw(): Bitmap {
            val detacher = WindowAttachment.dispatchAttach(mView)
            try {
                val bmp = Bitmap.createBitmap(mView.width, mView.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                mView.draw(canvas)
                return bmp
            } finally {
                detacher.detach()
            }
        }
    }

    companion object {
        private const val HEIGHT_LIMIT = 100000

        @JvmStatic
        fun setupView(view: View): ViewHelpers = ViewHelpers(view)
    }
}
