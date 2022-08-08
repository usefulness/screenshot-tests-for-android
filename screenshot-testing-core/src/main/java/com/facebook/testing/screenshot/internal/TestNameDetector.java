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

package com.facebook.testing.screenshot.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import androidx.annotation.Nullable;
import kotlin.Pair;

/**
 * Detect the test name and class that is being run currently.
 */
public class TestNameDetector {
    private static final String JUNIT_TEST_CASE = "junit.framework.TestCase";
    private static final String JUNIT_RUN_WITH = "org.junit.runner.RunWith";
    private static final String JUNIT_TEST = "org.junit.Test";
    private static final String UNKNOWN = "unknown";

    private TestNameDetector() {
    }

    public static @Nullable Pair<String, String> getTestMethodInfo() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            try {
                String className = element.getClassName();
                int classSyntheticStart = className.indexOf("$");
                if (classSyntheticStart >= 0) {
                    className = className.substring(0, classSyntheticStart);
                }
                Class<?> clazz = Class.forName(className);
                String methodName = element.getMethodName();
                int methodSyntheticStart = methodName.indexOf("$");
                if (methodSyntheticStart >= 0) {
                    methodName = methodName.substring(0, methodSyntheticStart);
                }
                Method method = clazz.getMethod(methodName);
                if (isTestMethod(method)) {
                    return new Pair<>(className, methodName);
                }
            } catch (NoSuchMethodException ignored) {
                // Not actionable, move onto the next element
            } catch (ClassNotFoundException ignored) {
                // Not actionable, move onto the next element
            }
        }

        return null;
    }

    private static boolean isTestMethod(Method method) {
        return hasAnnotation(method.getAnnotations(), JUNIT_TEST);
    }

    private static boolean hasAnnotation(Annotation[] annotations, String annotationCanonicalName) {
        for (Annotation annotation : annotations) {
            if (annotationCanonicalName.equalsIgnoreCase(
                    annotation.annotationType().getCanonicalName())) {
                return true;
            }
        }
        return false;
    }
}
