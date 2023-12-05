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

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.AndroidTest
import com.android.build.api.variant.HasAndroidTest
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.usefulness.testing.screenshot.build.ScreenshotTask
import io.github.usefulness.testing.screenshot.generated.ScreenshotTestBuildConfig
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

    /** Root-mean-square error value, @see https://github.com/usefulness/screenshot-tests-for-android/pull/190#issue-2025888191 */
    var tolerance = 0.0f
}

class ScreenshotsPlugin : Plugin<Project> {
    companion object {
        const val GROUP = "Screenshot Test"
        const val DEPENDENCY_GROUP = "io.github.usefulness"
        const val DEPENDENCY_CORE = "screenshot-testing-core"
        const val TEST_RUNNER_CLASS = "io.github.usefulness.testing.screenshot.DefaultScreenshotRunner"
        const val SCREENSHOT_TESTS_RUN_ID = "single_test_id"
    }

    private lateinit var screenshotExtensions: ScreenshotsPluginExtension

    override fun apply(project: Project) = with(project) {
        screenshotExtensions = extensions.create("screenshots", ScreenshotsPluginExtension::class.java)

        afterEvaluate {
            if (screenshotExtensions.addDeps) {
                it.dependencies.add("androidTestImplementation", "$DEPENDENCY_GROUP:$DEPENDENCY_CORE:${ScreenshotTestBuildConfig.VERSION}")
            }
        }
        val androidComponents = extensions.getByName("androidComponents") as AndroidComponentsExtension<*, *, *>
        androidComponents.onVariants { variant ->
            val androidTest = (variant as? HasAndroidTest)?.androidTest
            if (androidTest != null) {
                generateTasksFor(androidTest)
            }
        }
        val androidExtension = getProjectExtension(this)
        androidExtension.defaultConfig.testInstrumentationRunner = TEST_RUNNER_CLASS
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

    private inline fun <reified T : ScreenshotTask> Project.registerTask(name: String, variant: AndroidTest) =
        project.tasks.register(name, T::class.java) { task ->
            task.init(variant, screenshotExtensions)
        }

    private fun Project.generateTasksFor(variant: AndroidTest) {
        val variantName = variant.name

        val cleanScreenshots = registerTask<CleanScreenshotsTask>(name = CleanScreenshotsTask.taskName(variantName), variant = variant)
        registerTask<PullScreenshotsTask>(name = PullScreenshotsTask.taskName(variantName), variant = variant)
            .configure { pull -> pull.dependsOn(cleanScreenshots) }

        registerTask<RunScreenshotTestTask>(name = RunScreenshotTestTask.taskName(variantName), variant = variant)
        registerTask<RecordScreenshotTestTask>(name = RecordScreenshotTestTask.taskName(variantName), variant = variant)
        registerTask<VerifyScreenshotTestTask>(name = VerifyScreenshotTestTask.taskName(variantName), variant = variant)
    }
}
