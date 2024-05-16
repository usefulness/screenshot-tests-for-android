package io.github.usefulness.testing.screenshot.tasks

import io.github.usefulness.testing.screenshot.ScreenshotsPlugin
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import javax.inject.Inject

open class VerifyScreenshotTestTask @Inject constructor(
    objectFactory: ObjectFactory,
    layout: ProjectLayout,
    execOperations: ExecOperations,
) : PullScreenshotsTask(objectFactory, layout, execOperations) {

    companion object {
        fun taskName(variantName: String) = "verify${variantName.replaceFirstChar(Char::titlecase)}ScreenshotTest"
    }

    init {
        description = "Installs and runs screenshot tests, then verifies their output against previously recorded screenshots"
        group = ScreenshotsPlugin.GROUP
        verify = true
    }
}
