package io.github.usefulness.testing.screenshot.verification

private class ResourceLoader

internal fun loadResource(path: String) = ResourceLoader::class.java.getResourceAsStream(path).let(::checkNotNull)
