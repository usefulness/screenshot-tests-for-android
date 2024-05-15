package io.github.usefulness.testing.screenshot.internal

/**
 * A 2D layout of image tiles. We represent images as strings which can be looked up in an `AlbumImpl`
 */
internal class Tiling(
    val width: Int,
    val height: Int,
) {

    private val mContents = Array(width) { arrayOfNulls<String>(height) }

    fun getAt(x: Int, y: Int): String? = mContents[x][y]

    fun setAt(x: Int, y: Int, name: String) {
        mContents[x][y] = name
    }
}
