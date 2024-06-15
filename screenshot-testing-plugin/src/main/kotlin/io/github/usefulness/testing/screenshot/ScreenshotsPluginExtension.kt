package io.github.usefulness.testing.screenshot

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public open class ScreenshotsPluginExtension @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
) {

    /** Whether to have the plugin dependency automatically add the core dependency */
    public val addDependencies: Property<Boolean> = objectFactory.property(Boolean::class.java)
        .value(true)

    /** The directory to store recorded screenshots in */
    public val referenceDirectory: DirectoryProperty = objectFactory.directoryProperty()
        .value(projectLayout.projectDirectory.dir("screenshots"))

    /** The directory to save failed screenshots */
    public val failureDirectory: DirectoryProperty = objectFactory.directoryProperty()
        .value(projectLayout.buildDirectory.dir("reports/failedScreenshots"))

    @Deprecated("use comparisonMethod instead", replaceWith = ReplaceWith("comparisonMethod = RootMeanSquareErrorValue(tolerance = ...)"))
    public val tolerance: Property<Float> = objectFactory.property(Float::class.java).value(0.0f)

    public val comparisonMethod: Property<ComparisonMethod> = objectFactory.property(ComparisonMethod::class.java)
        .value(ComparisonMethod.DropboxDiffer())
}

public sealed interface ComparisonMethod {

    public data class DropboxDiffer(
        val maxDistance: Float = 0.001f,
        val hShift: Int = 0,
        val vShift: Int = 0,
    ) : ComparisonMethod

    /**
     * Root-mean-square error value, @see https://github.com/usefulness/screenshot-tests-for-android/pull/190#issue-2025888191
     */
    public data class RootMeanSquareErrorValue(val tolerance: Float = 0f) : ComparisonMethod
}
