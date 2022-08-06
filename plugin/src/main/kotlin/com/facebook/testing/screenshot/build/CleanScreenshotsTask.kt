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

import com.facebook.testing.screenshot.build.PullScreenshotsTask.Companion.getReportDir
import com.usefulness.testing.screenshot.build.ScreenshotTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class CleanScreenshotsTask @Inject constructor(
    objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout,
) : ScreenshotTask(objectFactory) {

    companion object {
        fun taskName(variantName: String) = "clean${variantName.replaceFirstChar(Char::titlecase)}Screenshots"
    }

    init {
        description = "Clean last generated screenshot report"
        group = ScreenshotsPlugin.GROUP
    }

    @TaskAction
    fun cleanScreenshots() {
        val outputDir = projectLayout.getReportDir(variantName.get())
        project.delete(outputDir)
    }
}
