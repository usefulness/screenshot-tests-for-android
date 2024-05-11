/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.testing.screenshot.build

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidTest
import com.usefulness.testing.screenshot.build.ScreenshotTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

open class PullScreenshotsTask @Inject constructor(
    objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout,
    private val execOperations: ExecOperations,
) : ScreenshotTask(objectFactory = objectFactory, projectLayout = projectLayout) {

    companion object {
        fun taskName(variantName: String) = "pull${variantName.replaceFirstChar(Char::titlecase)}Screenshots"

        internal fun ProjectLayout.getReportDir(variantName: String): File =
            buildDirectory.file("screenshots${variantName.replaceFirstChar(Char::titlecase)}").get().asFile
    }

    @InputDirectory
    protected val apkDirectory: DirectoryProperty = objectFactory.directoryProperty()

    @Input
    protected var verify = false

    @Input
    protected var record = false

    init {
        description = "Pull screenshots from your device"
        group = ScreenshotsPlugin.GROUP
        outputs.upToDateWhen { false }
    }

    override fun init(variant: AndroidTest, extension: ScreenshotsPluginExtension) {
        super.init(variant, extension)

        apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
    }

    @TaskAction
    fun pullScreenshots() {
        val codeSource = ScreenshotsPlugin::class.java.protectionDomain.codeSource
        val jarFile = File(codeSource.location.toURI().path)
        val referenceDir = referenceDir.orNull?.let(::File)
        val outputDir = if (verify && referenceDir != null) {
            referenceDir
        } else {
            projectLayout.getReportDir(variantName.get())
        }

        assert(if (verify) outputDir.exists() else !outputDir.exists())

        val testedApk = getTestedApk()

        execOperations.exec { exec ->
            exec.executable = pythonExecutable.get()
            exec.environment("PYTHONPATH", jarFile)

            exec.args = mutableListOf(
                "-m",
                "android_screenshot_tests.pull_screenshots",
                "--apk",
                testedApk.absolutePath,
                "--temp-dir",
                outputDir.absolutePath,
            )
                .apply {
                    if (verify) {
                        add("--verify")
                    } else if (record) {
                        add("--record")
                    }

                    if (verify || record) {
                        add(recordDir.get())
                    }

                    if (verify) {
                        add("--tolerance")
                        add(tolerance.get().toString())
                    }

                    if (verify && failureDir.isPresent) {
                        failureOutput.get().asFile.deleteRecursively()
                        add("--failure-dir")
                        add(failureDir.get())
                    }

                    if (multipleDevices.get()) {
                        add("--multiple-devices")
                        add("true")
                    }

                    if (verify && referenceDir != null) {
                        add("--no-pull")
                    }

                    if (bundleResults.get()) {
                        add("--bundle-results")
                    }
                }

            println(exec.args)
        }
    }

    private fun getTestedApk() = apkDirectory.get().asFile.listFiles()?.singleOrNull { it.extension == "apk" }
        ?: error("Failed to pick target apk. APKs = [${apkDirectory.orNull?.asFile?.listFiles()}]")
}
