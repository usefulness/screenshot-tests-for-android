# Screenshot Tests for Android


[![version](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/io/github/usefulness/screenshot-testing-plugin/maven-metadata.xml?label=gradle)](https://plugins.gradle.org/search?term="io.github.usefulness")
![Maven Central](https://img.shields.io/maven-central/v/io.github.usefulness/screenshot-testing-core?style=plastic)

## Quick start
1. `build.gradle`

```groovy
plugins {
    id("io.github.usefulness.screenshot-testing-plugin") version "{{ version }}"
}
```
2. `androidTest/kotlin/SampleTest.kt`

```kotlin
class SampleTest

    @Test
    fun foo() {
        launchActivity<MainActivity>().onActivity(Screenshot::snapActivity)
    }
}
```

3. run `./gradlew recordDebugAndroidTestScreenshotTest`
4. modify the view, run `./gradlew verifyDebugAndroidTestScreenshotTest` observe failure with a report

## License

screenshot-tests-for-android is Apache-2-licensed.
