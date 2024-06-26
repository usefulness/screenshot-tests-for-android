package io.github.usefulness.testing.screenshot.tasks

import io.github.usefulness.testing.screenshot.ComparisonMethod
import io.github.usefulness.testing.screenshot.verification.HtmlReportBuilder
import io.github.usefulness.testing.screenshot.verification.MetadataParser
import io.github.usefulness.testing.screenshot.verification.Recorder
import io.github.usefulness.testing.screenshot.verification.Recorder.VerificationResult
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import java.io.File

public abstract class RunScreenshotTestsTask internal constructor(
    objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout,
) : DefaultTask() {

    @get:Internal
    internal val variantName: Property<String> = objectFactory.property(String::class.java)

    @get:Internal
    internal val comparisonMethod: Property<ComparisonMethod> = objectFactory.property(ComparisonMethod::class.java)

    @get:InputDirectory
    public val connectedTestOutput: DirectoryProperty = objectFactory.directoryProperty()

    @get:Internal
    internal open val referenceDirectory: DirectoryProperty = objectFactory.directoryProperty()

    @get:OutputDirectory
    public val failureDirectory: DirectoryProperty = objectFactory.directoryProperty()

    internal enum class RunMode {
        Record,
        Verify,
    }

    internal fun runScreenshotTests(mode: RunMode) {
        val outputDir = projectLayout.getReportDir(variantName.get())

        val testOutput = connectedTestOutput.asFile.get()
        val emulatorSpecificFolder = testOutput.listFiles()?.singleOrNull()
            ?: return logger.warn(
                "Expected single folder under path=$testOutput, " +
                    "got=${testOutput.list()?.joinToString(separator = "\n")}. Assuming the module does not have any tests",
            )

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
        when (mode) {
            RunMode.Record -> recorder.record()

            RunMode.Verify -> {
                when (val result = recorder.verify(comparisonMethod = comparisonMethod.get())) {
                    is VerificationResult.Mismatch -> {
                        result.items.forEach { item ->
                            logger.warn("Image ${item.key} has changed. difference=${item.difference}")
                        }

                        val message = when (result.items.size) {
                            1 -> "One screenshot has changed"
                            else -> "${result.items.size} screenshots have changed"
                        }
                        error("Verification failed - $message. Open ${failureDirectory.asFile.get()} to review the diff")
                    }

                    VerificationResult.NoImages -> logger.warn("Did not find any screenshots.")

                    VerificationResult.Success -> Unit
                }
            }
        }

        logger.quiet("Open the following url in a browser to view the results: ")
        logger.quiet("  ${htmlOutput.reportEntrypoint}")
    }

    internal companion object {
        internal fun ProjectLayout.getReportDir(variantName: String): File =
            buildDirectory.file("reports/screenshots${variantName.replaceFirstChar(Char::titlecase)}").get().asFile
    }
}
