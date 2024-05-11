package io.github.usefulness.testing.screenshot

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.annotation.OptIn
import androidx.test.annotation.ExperimentalTestApi
import androidx.test.services.storage.TestStorage
import com.facebook.testing.screenshot.internal.Registry
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Provides a directory for an Album to store its screenshots in.
 */
internal class ScreenshotDirectories(private val mContext: Context) {

    @OptIn(ExperimentalTestApi::class)
    fun openOutputFile(name: String): OutputStream = TestStorage().openOutputFile(name)

    @OptIn(ExperimentalTestApi::class)
    fun openInputFile(name: String): InputStream = TestStorage().openInputFile(name)

    fun get(type: String): File {
        checkPermissions()
        return getSdcardDir(type)
    }

    private fun checkPermissions() {
        for (permission in REQUIRED_PERMISSIONS) {
            if (mContext.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                continue
            }
            val targetContext = Registry.getRegistry().instrumentation.targetContext
            grantPermission(targetContext, permission)
            grantPermission(mContext, permission)
        }
    }

    private fun grantPermission(context: Context, permission: String) {
        val automation = Registry.getRegistry().instrumentation.uiAutomation
        val pfd = automation.executeShellCommand("pm grant ${context.packageName} $permission")
        val buffer = ByteArray(1024)
        FileInputStream(pfd.fileDescriptor).use { stream ->
            while (stream.read(buffer) != -1) {
                // Consume stdout to ensure the command completes
            }
        }
    }

    private fun getSdcardDir(type: String): File {
        val externalStorage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val libraryRoot = externalStorage.resolve("screenshots")
        libraryRoot.delete()
        check(!libraryRoot.exists())
        val child = libraryRoot.resolve(mContext.packageName).resolve("screenshots-$type")

        return child
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
