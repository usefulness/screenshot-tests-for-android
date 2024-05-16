package io.github.usefulness.testing.screenshot.verification

import java.io.File

internal object MetadataParser {

    fun parseMetadata(source: File): MetaData {
        require(source.exists()) { "metadata.json does not exist at ${source.absolutePath}" }

        TODO()
    }

    data class MetaData(
        val name: String,
    )
}
