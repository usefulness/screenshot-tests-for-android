package io.github.usefulness.testing.screenshot.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

abstract class ScreenshotTask(objectFactory: ObjectFactory) : DefaultTask() {

    @get:Input
    internal val variantName = objectFactory.property(String::class.java)

    @get:Input
    internal val pythonExecutable = objectFactory.property(String::class.java)

    @get:Input
    internal val tolerance = objectFactory.property(Float::class.java)

    @get:Optional
    @get:InputDirectory
    internal val connectedTestOutput = objectFactory.directoryProperty()

    abstract val referenceDirectory: DirectoryProperty

    @get:OutputDirectory
    internal val failureDirectory = objectFactory.directoryProperty()
}
