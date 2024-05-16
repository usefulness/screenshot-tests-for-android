package io.github.usefulness.testing.screenshot.tasks

import io.github.usefulness.testing.screenshot.ScreenshotsPlugin
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

open class PullScreenshotsTask @Inject constructor(
    objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout,
    private val execOperations: ExecOperations,
) : ScreenshotTask(objectFactory = objectFactory) {

    companion object {
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
        val codeSource = ScreenshotsPlugin::class.java.protectionDomain.codeSource
        val jarFile = File(codeSource.location.toURI().path)
        val outputDir = projectLayout.getReportDir(variantName.get())

        assert(if (verify) outputDir.exists() else !outputDir.exists())

        val testOutput = connectedTestOutput.asFile.get()
        val emulatorSpecificFolder = testOutput.listFiles()?.singleOrNull()
            ?: error("Expected single folder under path=$testOutput, got=${testOutput.list()?.joinToString(separator = "\n")}")

        execOperations.exec { exec ->
            exec.executable = pythonExecutable.get()
            exec.environment("PYTHONPATH", jarFile)

            exec.args = mutableListOf(
                "-m",
                "android_screenshot_tests.pull_screenshots",
                "--source",
                emulatorSpecificFolder.absolutePath,
                "--temp-dir",
                outputDir.absolutePath,
            )
                .apply {
                    if (verify) {
                        add("--verify")
                    } else if (record) {
                        add("--record")
                    }

                    if (verify || record) {
                        add(referenceDirectory.get().asFile.path)
                    }

                    if (verify) {
                        add("--tolerance")
                        add(tolerance.get().toString())
                    }

                    if (verify && failureDirectory.isPresent) {
                        failureDirectory.get().asFile.deleteRecursively()
                        add("--failure-dir")
                        add(failureDirectory.asFile.get().path)
                    }
                }

            println(exec.args)
        }
    }
}
