package io.github.usefulness.testing.screenshot.tasks

import io.github.usefulness.testing.screenshot.ScreenshotsPlugin
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import javax.inject.Inject

open class VerifyScreenshotTestTask @Inject constructor(
    objectFactory: ObjectFactory,
    layout: ProjectLayout,
) : PullScreenshotsTask(objectFactory, layout) {

    internal companion object {
        internal fun taskName(variantName: String) = "verify${variantName.replaceFirstChar(Char::titlecase)}ScreenshotTest"
    }

    @get:InputDirectory
    override val referenceDirectory = objectFactory.directoryProperty()

    init {
        description = "Installs and runs screenshot tests, then verifies their output against previously recorded screenshots"
        group = ScreenshotsPlugin.GROUP
        verify = true
    }
}
