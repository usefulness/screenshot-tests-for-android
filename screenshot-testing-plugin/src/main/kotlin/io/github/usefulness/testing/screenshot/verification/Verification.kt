package io.github.usefulness.testing.screenshot.verification

import java.io.File

internal class ScreenshotsEngine(
    emulatorSpecificFolder: File,
    reportDirectory: File,
    private val referenceDirectory: File,
) {

    private val htmlReportBuilder = HtmlReportBuilder(
        emulatorSpecificFolder = emulatorSpecificFolder,
        reportDirectory = reportDirectory,
    )
    private val recorder = Recorder(
        emulatorSpecificFolder = emulatorSpecificFolder,
        referenceDirectory = referenceDirectory,
    )

    init {
        require(emulatorSpecificFolder.exists()) {
            "Expected emulatorSpecificFolder=${emulatorSpecificFolder.absolutePath} to exist"
        }
    }

    internal fun record() {
        htmlReportBuilder.generate()
        recorder.record()
    }

    internal fun verify(tolerance: Float) {
        htmlReportBuilder.generate()
        recorder.verify(tolerance = tolerance)
    }
}

fun main() {
    val engine = ScreenshotsEngine(
        emulatorSpecificFolder = File(
            "/Users/mateuszkwiecinski/sourcy/screenshot-tests-for-android/sample/android/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/Pixel_6_Pro_API_33(AVD) - 13",
        ),
        reportDirectory = File(
            "/Users/mateuszkwiecinski/sourcy/screenshot-tests-for-android/sample/android/build/reports/screenshotsDebugAndroidTest",
        ),
        referenceDirectory = File("/Users/mateuszkwiecinski/sourcy/screenshot-tests-for-android/sample/android/screenshots"),
    )

    println(engine.verify(tolerance = 0f))
}
