package io.github.usefulness.testing.screenshot.tasks

import io.github.usefulness.testing.screenshot.ScreenshotsPlugin
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import javax.inject.Inject

open class RecordScreenshotTestTask @Inject constructor(
    objectFactory: ObjectFactory,
    layout: ProjectLayout,
    execOperations: ExecOperations,
) : PullScreenshotsTask(objectFactory, layout, execOperations) {

    companion object {
        fun taskName(variantName: String) = "record${variantName.replaceFirstChar(Char::titlecase)}ScreenshotTest"
    }

    init {
        description = "Installs and runs screenshot tests, then records their output for later verification"
        group = ScreenshotsPlugin.GROUP
        record = true
    }
}