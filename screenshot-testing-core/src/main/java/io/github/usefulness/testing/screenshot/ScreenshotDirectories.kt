package io.github.usefulness.testing.screenshot

import androidx.annotation.OptIn
import androidx.test.annotation.ExperimentalTestApi
import androidx.test.services.storage.TestStorage
import java.io.InputStream
import java.io.OutputStream

/**
 * Provides a directory for an Album to store its screenshots in.
 */
@OptIn(ExperimentalTestApi::class)
internal class ScreenshotDirectories {

    private val testStorage = TestStorage()

    fun openOutputFile(name: String): OutputStream = testStorage.openOutputFile(name)

    fun openInputFile(name: String): InputStream = testStorage.openInputFile(name)
}
