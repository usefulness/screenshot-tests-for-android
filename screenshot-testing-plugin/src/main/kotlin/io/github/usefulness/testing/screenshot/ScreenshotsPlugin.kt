package io.github.usefulness.testing.screenshot

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.AndroidTest
import com.android.build.api.variant.HasAndroidTest
import com.android.build.gradle.internal.scope.InternalArtifactType
import io.github.usefulness.testing.screenshot.generated.ScreenshotTestBuildConfig
import io.github.usefulness.testing.screenshot.tasks.CleanScreenshotsTask
import io.github.usefulness.testing.screenshot.tasks.RecordScreenshotTestTask
import io.github.usefulness.testing.screenshot.tasks.ScreenshotTask
import io.github.usefulness.testing.screenshot.tasks.VerifyScreenshotTestTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class ScreenshotsPlugin : Plugin<Project> {
    internal companion object {
        const val GROUP = "Screenshot Test"
        const val DEPENDENCY_GROUP = "io.github.usefulness"
        const val DEPENDENCY_CORE = "screenshot-testing-core"
        const val TEST_RUNNER_CLASS = "io.github.usefulness.testing.screenshot.DefaultScreenshotRunner"
    }

    override fun apply(project: Project) = with(project) {
        val screenshotExtensions = extensions.create("screenshots", ScreenshotsPluginExtension::class.java)

        afterEvaluate {
            if (screenshotExtensions.addDependencies.get()) {
                it.dependencies.add(
                    "androidTestImplementation",
                    "$DEPENDENCY_GROUP:$DEPENDENCY_CORE:${ScreenshotTestBuildConfig.PLUGIN_VERSION}",
                )
                it.dependencies.add(
                    "androidTestUtil",
                    "androidx.test.services:test-services:${ScreenshotTestBuildConfig.ANDROID_TEST_SERVICES_VERSION}",
                )
            }
        }
        val android = extensions.getByName("android") as CommonExtension<*, *, *, *, *, *>
        android.defaultConfig {
            testInstrumentationRunnerArguments["useTestStorageService"] = "true"
            testInstrumentationRunner = TEST_RUNNER_CLASS
        }

        val androidComponents = extensions.getByName("androidComponents") as AndroidComponentsExtension<*, *, *>
        androidComponents.onVariants { variant ->
            val androidTest = (variant as? HasAndroidTest)?.androidTest
            if (androidTest != null) {
                generateTasksFor(androidTest, screenshotExtensions)
            }
        }
    }

    private inline fun <reified T : ScreenshotTask> Project.registerTask(
        name: String,
        variant: AndroidTest,
        screenshotExtensions: ScreenshotsPluginExtension,
    ) = project.tasks.register(name, T::class.java) { task ->
        task.variantName.set(variant.name)
        task.pythonExecutable.set(screenshotExtensions.pythonExecutable)
        task.tolerance.set(screenshotExtensions.tolerance)
        task.referenceDirectory.set(screenshotExtensions.referenceDirectory)
        task.failureDirectory.set(screenshotExtensions.failureDirectory)

        val androidTestTask = "connected${variant.name.replaceFirstChar(Char::titlecase)}"
        task.dependsOn(androidTestTask)
        task.mustRunAfter(androidTestTask)

        task.dependsOn(CleanScreenshotsTask.taskName(variant.name))
    }

    private fun Project.generateTasksFor(variant: AndroidTest, screenshotExtensions: ScreenshotsPluginExtension) {
        val variantName = variant.name

        tasks.register(CleanScreenshotsTask.taskName(variantName))
        val record = registerTask<RecordScreenshotTestTask>(
            name = RecordScreenshotTestTask.taskName(variantName),
            variant = variant,
            screenshotExtensions = screenshotExtensions,
        )
        val verify = registerTask<VerifyScreenshotTestTask>(
            name = VerifyScreenshotTestTask.taskName(variantName),
            variant = variant,
            screenshotExtensions = screenshotExtensions,
        )

        variant.artifacts.use(record)
            .wiredWith(RecordScreenshotTestTask::connectedTestOutput)
            .toListenTo(InternalArtifactType.CONNECTED_ANDROID_TEST_ADDITIONAL_OUTPUT)

        variant.artifacts.use(verify)
            .wiredWith(VerifyScreenshotTestTask::connectedTestOutput)
            .toListenTo(InternalArtifactType.CONNECTED_ANDROID_TEST_ADDITIONAL_OUTPUT)
    }
}
