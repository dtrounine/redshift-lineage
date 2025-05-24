package io.github.dtrounine.lineage.output

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val prettyJson = Json {
    prettyPrint = true       // Enable pretty print
    prettyPrintIndent = "  " // Optional: set custom indent (default is 4 spaces)
}
