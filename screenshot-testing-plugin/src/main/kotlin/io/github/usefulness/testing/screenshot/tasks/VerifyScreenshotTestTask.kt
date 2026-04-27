package io.github.usefulness.testing.screenshot.tasks

import io.github.usefulness.testing.screenshot.ScreenshotsPlugin
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Runs connected screenshot tests and verifies their outputs")
public open class VerifyScreenshotTestTask @Inject constructor(
    objectFactory: ObjectFactory,
    layout: ProjectLayout,
) : RunScreenshotTestsTask(objectFactory, layout) {

    internal companion object {
        internal fun taskName(variantName: String) = "verify${variantName.replaceFirstChar(Char::titlecase)}ScreenshotTest"
    }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public override val referenceDirectory: DirectoryProperty = super.referenceDirectory

    init {
        description = "Installs and runs screenshot tests, then verifies their output against previously recorded screenshots"
        group = ScreenshotsPlugin.GROUP
    }

    @TaskAction
    public fun run(): Unit = runScreenshotTests(mode = RunMode.Verify)
}
