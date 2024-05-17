package io.github.usefulness.testing.screenshot.tasks

import io.github.usefulness.testing.screenshot.ScreenshotsPlugin
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class RecordScreenshotTestTask @Inject constructor(
    objectFactory: ObjectFactory,
    layout: ProjectLayout,
) : RunScreenshotTestsTask(objectFactory, layout) {

    internal companion object {
        internal fun taskName(variantName: String) = "record${variantName.replaceFirstChar(Char::titlecase)}ScreenshotTest"
    }

    @get:OutputDirectory
    public override val referenceDirectory: DirectoryProperty = super.referenceDirectory

    init {
        description = "Installs and runs screenshot tests, then records their output for later verification"
        group = ScreenshotsPlugin.GROUP
    }

    @TaskAction
    fun run() = runScreenshotTests(mode = RunMode.Record)
}
