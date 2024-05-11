package io.github.usefulness.testing.screenshot

internal class ReportArtifactsManager(private val screenshotDirectories: ScreenshotDirectories) {

    fun recordFile(fileName: String, content: ByteArray) = screenshotDirectories.openOutputFile(fileName)
        .use { it.write(content) }
}
