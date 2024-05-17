package io.github.usefulness.testing.screenshot.tasks

import io.github.usefulness.testing.screenshot.ScreenshotsPlugin
import io.github.usefulness.testing.screenshot.verification.HtmlReportBuilder
import io.github.usefulness.testing.screenshot.verification.MetadataParser
import io.github.usefulness.testing.screenshot.verification.Recorder
import io.github.usefulness.testing.screenshot.verification.Recorder.VerificationResult
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class PullScreenshotsTask @Inject constructor(
    objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout,
) : ScreenshotTask(objectFactory = objectFactory) {

    internal companion object {
        internal fun ProjectLayout.getReportDir(variantName: String): File =
            buildDirectory.file("reports/screenshots${variantName.replaceFirstChar(Char::titlecase)}").get().asFile
    }

    @Input
    protected var verify = false

    @Input
    protected var record = false

    init {
        description = "Pull screenshots from your device"
        group = ScreenshotsPlugin.GROUP
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun pullScreenshots() {
        val outputDir = projectLayout.getReportDir(variantName.get())

        assert(if (verify) outputDir.exists() else !outputDir.exists())

        val testOutput = connectedTestOutput.asFile.get()
        val emulatorSpecificFolder = testOutput.listFiles()?.singleOrNull()
            ?: error("Expected single folder under path=$testOutput, got=${testOutput.list()?.joinToString(separator = "\n")}")

        val metadata = MetadataParser.parseMetadata(emulatorSpecificFolder.resolve("metadata.json"))

        val htmlReportBuilder = HtmlReportBuilder(
            emulatorSpecificFolder = emulatorSpecificFolder,
            metadata = metadata,
            reportDirectory = outputDir,
        )
        val htmlOutput = htmlReportBuilder.generate()

        logger.quiet("Found ${htmlOutput.numberOfScreenshots} screenshots")

        val recorder = Recorder(
            emulatorSpecificFolder = emulatorSpecificFolder,
            metadata = metadata,
            referenceDirectory = referenceDirectory.asFile.get(),
            failureDirectory = failureDirectory.asFile.get(),
        )
        if (verify) {
            when (val result = recorder.verify(tolerance = tolerance.get())) {
                is VerificationResult.Mismatch -> {
                    result.items.forEach { item ->
                        logger.warn("Image ${item.key} has changed. RMS=${item.differenceRms}")
                    }

                    val message = when (result.items.size) {
                        1 -> "One screenshot has changed"
                        else -> "${result.items.size} screenshots have changed"
                    }
                    error("Verification failed - $message. tolerance=${tolerance.get()}\n" +
                        "Open ${failureDirectory.asFile.get()} to review the diff")
                }

                VerificationResult.Success -> Unit
            }
        } else if (record) {
            recorder.record()
        } else {
            error("Unsupported mode")
        }

        logger.quiet("Open the following url in a browser to view the results: ")
        logger.quiet("  ${htmlOutput.reportEntrypoint}")
    }
}
