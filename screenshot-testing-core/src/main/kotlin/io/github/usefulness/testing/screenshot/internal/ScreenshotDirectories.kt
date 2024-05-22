package io.github.usefulness.testing.screenshot.internal

import androidx.annotation.OptIn
import androidx.test.annotation.ExperimentalTestApi
import androidx.test.platform.io.PlatformTestStorageRegistry
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalTestApi::class)
internal object ScreenshotDirectories {

    fun openOutputFile(name: String): OutputStream = PlatformTestStorageRegistry.getInstance().openOutputFile(name)

    fun openInputFile(name: String): InputStream = PlatformTestStorageRegistry.getInstance().openInputFile(name)
}
