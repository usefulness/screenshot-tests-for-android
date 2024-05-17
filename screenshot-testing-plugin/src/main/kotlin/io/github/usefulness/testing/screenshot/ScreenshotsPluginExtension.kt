package io.github.usefulness.testing.screenshot

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class ScreenshotsPluginExtension @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
) {

    /** Whether to have the plugin dependency automatically add the core dependency */
    val addDependencies: Property<Boolean> = objectFactory.property(Boolean::class.java)
        .value(true)

    /** The directory to store recorded screenshots in */
    val referenceDirectory: DirectoryProperty = objectFactory.directoryProperty()
        .value(projectLayout.projectDirectory.dir("screenshots"))

    /** The directory to save failed screenshots */
    val failureDirectory: DirectoryProperty = objectFactory.directoryProperty()
        .value(projectLayout.buildDirectory.dir("reports/failedScreenshots"))

    /** Root-mean-square error value, @see https://github.com/usefulness/screenshot-tests-for-android/pull/190#issue-2025888191 */
    val tolerance: Property<Float> = objectFactory.property(Float::class.java).value(0.0f)
}
