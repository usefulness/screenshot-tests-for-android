package io.github.usefulness.testing.screenshot.verification

import java.io.File

@Suppress("UNUSED_PARAMETER", "unused")
internal class Recorder(
    private val emulatorSpecificFolder: File,
    private val referenceDirectory: File,
) {

    fun record() = Unit

    fun verify(tolerance: Float) = Unit
}
