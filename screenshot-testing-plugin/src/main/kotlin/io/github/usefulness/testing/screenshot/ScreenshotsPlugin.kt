package io.github.usefulness.testing.screenshot

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.AndroidTest
import com.android.build.api.variant.HasAndroidTest
import com.android.build.gradle.internal.scope.InternalArtifactType
import io.github.usefulness.testing.screenshot.generated.ScreenshotTestBuildConfig
import io.github.usefulness.testing.screenshot.tasks.CleanScreenshotsTask
import io.github.usefulness.testing.screenshot.tasks.RecordScreenshotTestTask
import io.github.usefulness.testing.screenshot.tasks.RunScreenshotTestsTask
import io.github.usefulness.testing.screenshot.tasks.VerifyScreenshotTestTask
import org.gradle.api.Plugin
import org.gradle.api.Project

public class ScreenshotsPlugin : Plugin<Project> {
    internal companion object {
        internal const val GROUP = "Screenshot Test"
        internal const val TEST_RUNNER_CLASS = "io.github.usefulness.testing.screenshot.DefaultScreenshotRunner"
    }

    public override fun apply(project: Project): Unit = with(project) {
        val screenshotExtensions = extensions.create("screenshots", ScreenshotsPluginExtension::class.java)

        afterEvaluate {
            if (screenshotExtensions.addDependencies.get()) {
                it.dependencies.add(
                    "androidTestImplementation",
                    "io.github.usefulness:screenshot-testing-core:${ScreenshotTestBuildConfig.PLUGIN_VERSION}",
                )
                it.dependencies.add(
                    "androidTestUtil",
                    "androidx.test.services:test-services:${ScreenshotTestBuildConfig.ANDROID_TEST_SERVICES_VERSION}",
                )
            }
        }
        val android = extensions.getByName("android") as CommonExtension
        android.defaultConfig.apply {
            testInstrumentationRunnerArguments["useTestStorageService"] = "true"
            testInstrumentationRunner = TEST_RUNNER_CLASS
        }

        val androidComponents = extensions.getByName("androidComponents") as AndroidComponentsExtension<*, *, *>
        androidComponents.onVariants { variant ->
            val androidTest = (variant as? HasAndroidTest)?.androidTest
            if (androidTest != null) {
                generateTasksFor(
                    variant = androidTest,
                    screenshotExtensions = screenshotExtensions,
                )
            }
        }
    }

    private inline fun <reified T : RunScreenshotTestsTask> Project.registerTask(
        name: String,
        variantName: String,
        screenshotExtensions: ScreenshotsPluginExtension,
    ) = project.tasks.register(name, T::class.java) { task ->
        task.variantName.set(variantName)
        task.comparisonMethod.set(screenshotExtensions.comparisonMethod)
        task.referenceDirectory.set(screenshotExtensions.referenceDirectory)
        task.failureDirectory.set(screenshotExtensions.failureDirectory)

        task.dependsOn(CleanScreenshotsTask.taskName(variantName))
    }

    private fun Project.generateTasksFor(variant: AndroidTest, screenshotExtensions: ScreenshotsPluginExtension) {
        val variantName = variant.name

        tasks.register(CleanScreenshotsTask.taskName(variantName))
        val record = registerTask<RecordScreenshotTestTask>(
            name = RecordScreenshotTestTask.taskName(variantName),
            variantName = variantName,
            screenshotExtensions = screenshotExtensions,
        )
        val verify = registerTask<VerifyScreenshotTestTask>(
            name = VerifyScreenshotTestTask.taskName(variantName),
            variantName = variantName,
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
