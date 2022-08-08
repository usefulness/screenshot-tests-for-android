package com.facebook.testing.screenshot.build

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class DupaTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {

    @TaskAction
    fun dupa() {
        execOperations.exec { exec ->
            exec.executable = "which"
            exec.args = listOf("python")
        }
        println("DUPA1-A python taskName=$name")
        execOperations.exec { exec ->
            exec.executable = "sh"
            exec.args = listOf("pytho", "--version")
        }
        println("DUPA1-B python taskName=$name")
        execOperations.exec { exec ->
            exec.executable = "sh"
            exec.args = listOf("python3", "--version")
            println("Dupa env=${exec.environment}")
        }
    }
}
