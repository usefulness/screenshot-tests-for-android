package com.usefulness.testing.screenshot.build

import com.android.build.api.variant.AndroidTest
import com.facebook.testing.screenshot.build.ScreenshotsPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

abstract class ScreenshotTask(
    objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout,
) : DefaultTask() {

    @get:Input
    internal val recordDir = objectFactory.property(String::class.java)

    @get:Input
    internal val multipleDevices = objectFactory.property(Boolean::class.java)

    @get:Input
    internal val pythonExecutable = objectFactory.property(String::class.java)

    @get:Optional
    @get:Input
    internal val referenceDir = objectFactory.property(String::class.java)

    @get:Optional
    @get:Input
    internal val failureDir = objectFactory.property(String::class.java)

    @get:Optional
    @get:OutputDirectory
    internal val failureOutput = objectFactory.fileProperty()

    @get:Input
    internal val bundleResults = objectFactory.property(Boolean::class.java)

    @get:Input
    internal val variantName = objectFactory.property(String::class.java)

    @get:Input
    internal val tolerance = objectFactory.property(Float::class.java)

    open fun init(variant: AndroidTest, extension: ScreenshotsPluginExtension) {
        recordDir.set(extension.recordDir)
        multipleDevices.set(extension.multipleDevices)
        pythonExecutable.set(extension.pythonExecutable)
        referenceDir.set(extension.referenceDir)
        failureDir.set(extension.failureDir)
        failureOutput.set(extension.failureDir?.let(projectLayout.projectDirectory::file))
        bundleResults.set(extension.bundleResults)
        tolerance.set(extension.tolerance)
        variantName.set(variant.name)
    }
}
