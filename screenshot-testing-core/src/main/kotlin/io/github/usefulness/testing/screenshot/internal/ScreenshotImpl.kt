package io.github.usefulness.testing.screenshot.internal

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Handler
import android.os.Looper
import android.view.View
import io.github.usefulness.testing.screenshot.TestNameDetector.getTestMethodInfo
import io.github.usefulness.testing.screenshot.WindowAttachment.dispatchAttach
import io.github.usefulness.testing.screenshot.layouthierarchy.internal.AccessibilityHierarchyDumper.dumpHierarchy
import io.github.usefulness.testing.screenshot.layouthierarchy.internal.AccessibilityIssuesDumper.dumpIssues
import io.github.usefulness.testing.screenshot.layouthierarchy.internal.AccessibilityUtil.generateAccessibilityTree
import io.github.usefulness.testing.screenshot.layouthierarchy.LayoutHierarchyDumper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import kotlin.math.min

internal object ScreenshotImpl {
    private val mAlbum: Album = AlbumImpl.create()

    private const val TILE_SIZE = 512

    private val mBitmap by lazy {
        Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
    }

    /**
     * Snaps a screenshot of the activity using the testName as the name.
     */
    fun snapActivity(activity: Activity): RecordBuilderImpl {
        if (!isUiThread) {
            val testMethodInfo = getTestMethodInfo()
            return runCallableOnUiThread { snapActivity(activity) }
                .setTestClass(testMethodInfo?.className ?: "unknown")
                .setTestName(testMethodInfo?.methodName ?: "unknown")
        }

        return snap(activity.window.decorView)
    }

    /**
     * Snaps a screenshot of the view (which should already be measured and laid out) using testName
     * as the name.
     */
    fun snap(measuredView: View): RecordBuilderImpl {
        val testMethodInfo = getTestMethodInfo()

        return RecordBuilderImpl(this)
            .setView(measuredView)
            .setTestClass(testMethodInfo?.className ?: "unknown")
            .setTestName(testMethodInfo?.methodName ?: "unknown")
    }

    fun flush() {
        mAlbum.flush()
    }

    private fun storeBitmap(recordBuilder: RecordBuilderImpl) {
        if (recordBuilder.tiling.getAt(0, 0) != null || recordBuilder.error != null) {
            return
        }

        if (!isUiThread) {
            runCallableOnUiThread { storeBitmap(recordBuilder) }
            return
        }

        val measuredView = recordBuilder.view
        if (measuredView == null || measuredView.measuredHeight == 0 || measuredView.measuredWidth == 0) {
            error("Can't take a screenshot, since this view is not measured")
        }

        val detacher = dispatchAttach(measuredView)
        try {
            val width = measuredView.width
            val height = measuredView.height

            assertNotTooLarge(width, height, recordBuilder)

            val maxi = (width + TILE_SIZE - 1) / TILE_SIZE
            val maxj = (height + TILE_SIZE - 1) / TILE_SIZE
            recordBuilder.setTiling(Tiling(maxi, maxj))

            for (i in 0 until maxi) {
                for (j in 0 until maxj) {
                    drawTile(measuredView, i, j, recordBuilder)
                }
            }
        } finally {
            detacher.detach()
        }
    }

    private fun drawTile(measuredView: View, i: Int, j: Int, recordBuilder: RecordBuilderImpl) {
        val width = measuredView.width
        val height = measuredView.height
        val left = i * TILE_SIZE
        val top = j * TILE_SIZE
        val right = min((left + TILE_SIZE).toDouble(), width.toDouble()).toInt()
        val bottom = min((top + TILE_SIZE).toDouble(), height.toDouble()).toInt()

        mBitmap.reconfigure(right - left, bottom - top, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mBitmap)
        clearCanvas(canvas)

        drawClippedView(measuredView, left, top, canvas)
        val tempName = mAlbum.writeBitmap(recordBuilder.name, i, j, mBitmap)
        recordBuilder.tiling.setAt(left / TILE_SIZE, top / TILE_SIZE, tempName)
    }

    private fun clearCanvas(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC)
    }

    /**
     * Draw a part of the view, in particular it returns a bitmap of dimensions `
     * (right-left)*(bottom-top)`, with the rendering of the view starting from position (`
     * left`, `top`).
     *
     *
     * For well behaved views, calling this repeatedly shouldn't change the rendering, so it should
     * it okay to render each tile one by one and combine it later.
     */
    private fun drawClippedView(view: View, left: Int, top: Int, canvas: Canvas) {
        canvas.translate(-left.toFloat(), -top.toFloat())
        view.draw(canvas)
        canvas.translate(left.toFloat(), top.toFloat())
    }

    /**
     * Records the RecordBuilderImpl, and verifies if required
     */
    fun record(recordBuilder: RecordBuilderImpl) {
        storeBitmap(recordBuilder)
        val dump = JSONObject()
        val view = checkNotNull(recordBuilder.view)
        val viewDump = LayoutHierarchyDumper.create().dumpHierarchy(view)
        dump.put("viewHierarchy", viewDump)
        dump.put("version", METADATA_VERSION)

        val axTree = if (recordBuilder.includeAccessibilityInfo) {
            generateAccessibilityTree(view)
        } else {
            null
        }
        dump.put("axHierarchy", dumpHierarchy(axTree))
        mAlbum.writeViewHierarchyFile(recordBuilder.name, dump.toString(2))

        if (axTree != null) {
            val issues = JSONObject()
            issues.put("axIssues", dumpIssues(axTree))
            mAlbum.writeAxIssuesFile(recordBuilder.name, issues.toString(2))
        }

        mAlbum.addRecord(recordBuilder)
    }

    fun getBitmap(recordBuilder: RecordBuilderImpl): Bitmap {
        require(recordBuilder.tiling.getAt(0, 0) == null) { "can't call getBitmap() after record()" }

        val view = checkNotNull(recordBuilder.view)
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        val detacher = dispatchAttach(view)
        try {
            drawClippedView(view, 0, 0, Canvas(bmp))
        } finally {
            detacher.detach()
        }

        return bmp
    }

    private val isUiThread: Boolean
        get() = Looper.getMainLooper().thread === Thread.currentThread()

    private fun <T> runCallableOnUiThread(callable: () -> T): T {
        val completableDeferred = CompletableDeferred<T>()
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            completableDeferred.completeWith(runCatching(callable))
        }

        return runBlocking { completableDeferred.await() }
    }

    /**
     * The version of the metadata file generated. This should be bumped whenever the structure of the
     * metadata file changes in such a way that would cause a comparison between old and new files to
     * be invalid or not useful.
     */
    private const val METADATA_VERSION = 1

    private fun assertNotTooLarge(width: Int, height: Int, recordBuilder: RecordBuilderImpl) {
        val maxPixels = recordBuilder.maxPixels
        if (maxPixels <= 0) {
            return
        }
        check((width.toLong()) * height <= maxPixels) { "View too large: ($width, $height)" }
    }

    const val MAX_PIXELS = RecordBuilderImpl.DEFAULT_MAX_PIXELS
}
