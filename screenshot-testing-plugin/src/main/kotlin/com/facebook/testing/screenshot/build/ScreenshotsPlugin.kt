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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import io.github.usefulness.testing.screenshot.generated.ScreenshotTestBuildConfig
import com.usefulness.testing.screenshot.build.ScreenshotTask
import org.gradle.api.Plugin
import org.gradle.api.Project

open class ScreenshotsPluginExtension {
    /** The directory to store recorded screenshots in */
    var recordDir = "screenshots"

    /** Whether to have the plugin dependency automatically add the core dependency */
    var addDeps = true

    /** Whether to store screenshots in device specific folders */
    var multipleDevices = false

    /** The python executable to use */
    var pythonExecutable = "python"

    /** The directory to compare screenshots from in verify only mode */
    var referenceDir: String? = null

    /** The directory to save failed screenshots */
    var failureDir: String? = null

    /** Whether to tar the screenshots in an archive file to transfer */
    var bundleResults = false
}

class ScreenshotsPlugin : Plugin<Project> {
    companion object {
        const val GROUP = "Screenshot Test"
        const val DEPENDENCY_GROUP = "io.github.usefulness.testing.screenshot"
        const val DEPENDENCY_CORE = "core"
        const val SCREENSHOT_TESTS_RUN_ID = "single_test_id"
    }

    private lateinit var screenshotExtensions: ScreenshotsPluginExtension

    override fun apply(project: Project) {
        val extensions = project.extensions
        screenshotExtensions = extensions.create("screenshots", ScreenshotsPluginExtension::class.java)

        project.afterEvaluate {
            if (screenshotExtensions.addDeps) {
                it.dependencies.add("androidTestImplementation", "$DEPENDENCY_GROUP:$DEPENDENCY_CORE:${ScreenshotTestBuildConfig.VERSION}")
            }
        }
        val androidExtension = getProjectExtension(project)
        androidExtension.testVariants.configureEach { generateTasksFor(project, it) }
        androidExtension.defaultConfig.testInstrumentationRunnerArguments["SCREENSHOT_TESTS_RUN_ID"] = SCREENSHOT_TESTS_RUN_ID
    }

    private fun getProjectExtension(project: Project): TestedExtension {
        val extensions = project.extensions
        val plugins = project.plugins
        return when {
            plugins.hasPlugin("com.android.application") -> extensions.findByType(AppExtension::class.java)!!
            plugins.hasPlugin("com.android.library") -> extensions.findByType(LibraryExtension::class.java)!!
            else -> throw IllegalArgumentException("Screenshot Test plugin requires Android's plugin")
        }
    }

    private fun <T : ScreenshotTask> registerTask(
        project: Project,
        name: String,
        variant: TestVariant,
        clazz: Class<T>,
    ) = project.tasks.register(name, clazz) { task ->
        task.init(variant, screenshotExtensions)
    }

    private fun generateTasksFor(project: Project, variant: TestVariant) {
        val variantName = variant.name
        variant.outputs.configureEach {
            if (it is ApkVariantOutput) {
                val cleanScreenshots = registerTask(
                    project,
                    CleanScreenshotsTask.taskName(variantName),
                    variant,
                    CleanScreenshotsTask::class.java,
                )
                registerTask(
                    project,
                    PullScreenshotsTask.taskName(variantName),
                    variant,
                    PullScreenshotsTask::class.java,
                )
                    .dependsOn(cleanScreenshots)

                registerTask(
                    project,
                    RunScreenshotTestTask.taskName(variantName),
                    variant,
                    RunScreenshotTestTask::class.java,
                )

                registerTask(
                    project,
                    RecordScreenshotTestTask.taskName(variantName),
                    variant,
                    RecordScreenshotTestTask::class.java,
                )

                registerTask(
                    project,
                    VerifyScreenshotTestTask.taskName(variantName),
                    variant,
                    VerifyScreenshotTestTask::class.java,
                )
            }
        }
    }
}
