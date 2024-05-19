package io.github.usefulness.testing.screenshot

import io.github.usefulness.testing.screenshot.internal.Poko
import java.lang.reflect.Method

@Poko
class TestMethodInfo(
    val className: String,
    val methodName: String,
)

object TestNameDetector {
    private const val JUNIT4_TEST = "org.junit.Test"
    private const val JUNIT5_TEST = "org.junit.jupiter.api.Test"

    @JvmStatic
    @Suppress("ThrowingExceptionsWithoutMessageOrCause")
    fun getTestMethodInfo() = Throwable().stackTrace.firstNotNullOfOrNull { element ->
        try {
            var className = element.className
            val classSyntheticStart = className.indexOf("$")
            if (classSyntheticStart >= 0) {
                className = className.substring(0, classSyntheticStart)
            }
            val clazz = Class.forName(className)
            var methodName = element.methodName
            val methodSyntheticStart = methodName.indexOf("$")
            if (methodSyntheticStart >= 0) {
                methodName = methodName.substring(0, methodSyntheticStart)
            }

            val method = clazz.getMethod(methodName)
            if (isTestMethod(method)) {
                TestMethodInfo(className = className, methodName = methodName)
            } else {
                null
            }
        } catch (ignored: NoSuchMethodException) {
            null
        } catch (ignored: ClassNotFoundException) {
            null
        }
    }

    private fun isTestMethod(method: Method) = method.annotations.any {
        val qualifiedName = it.annotationClass.qualifiedName
        qualifiedName.equals(JUNIT5_TEST, ignoreCase = true) || qualifiedName.equals(JUNIT4_TEST, ignoreCase = true)
    }
}
