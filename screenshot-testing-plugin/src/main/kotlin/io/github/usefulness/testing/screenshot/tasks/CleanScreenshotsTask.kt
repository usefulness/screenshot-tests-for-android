package io.github.usefulness.testing.screenshot.tasks

import io.github.usefulness.testing.screenshot.ScreenshotsPlugin
import io.github.usefulness.testing.screenshot.tasks.RunScreenshotTestsTask.Companion.getReportDir
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

public open class CleanScreenshotsTask @Inject constructor(
    objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout,
) : DefaultTask() {

    internal companion object {

        internal fun taskName(variantName: String) = "clean${variantName.replaceFirstChar(Char::titlecase)}Screenshots"
    }

    @Input
    public val variantName: Property<String> = objectFactory.property(String::class.java)

    init {
        description = "Clean last generated screenshot report"
        group = ScreenshotsPlugin.GROUP
    }

    @TaskAction
    public fun cleanScreenshots() {
        val outputDir = projectLayout.getReportDir(variantName.get())
        outputDir.deleteRecursively()
    }
}
