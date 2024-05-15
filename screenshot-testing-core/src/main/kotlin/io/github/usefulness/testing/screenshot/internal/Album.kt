package io.github.usefulness.testing.screenshot.internal

import android.graphics.Bitmap

/**
 * Stores metadata about an album of screenshots during an instrumentation test run.
 */
internal interface Album {

    /**
     * Writes the bitmap corresponding to the screenshot with the name `name` in the `(tilei, tilej)` position.
     */
    fun writeBitmap(name: String, tilei: Int, tilej: Int, bitmap: Bitmap): String

    /**
     * Call after all the screenshots are done.
     */
    fun flush()

    /**
     * Opens a stream to dump the view hierarchy into. This should be called before addRecord() is
     * called for the given name.
     *
     *
     * It is the callers responsibility to call `close()` on the returned stream.
     */
    fun writeViewHierarchyFile(name: String, data: String)

    /**
     * Opens a stream to dump the accessibility issues into. This should be called before addRecord()
     * is called for the given name.
     *
     *
     * It is the callers responsibility to call `close()` on the returned stream.
     */
    fun writeAxIssuesFile(name: String, data: String)

    /**
     * This is called after every record is finally set up.
     */
    fun addRecord(recordBuilder: RecordBuilderImpl)
}
