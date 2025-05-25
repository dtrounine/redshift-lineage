package io.github.dtrounine.lineage.output.yaml

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.encodeToStream
import io.github.dtrounine.lineage.model.LineageData
import io.github.dtrounine.lineage.output.OutputFormat
import java.io.OutputStream

class YamlOutputFormat: OutputFormat("yaml") {

    override fun write(lineageData: LineageData, output: OutputStream) {
        Yaml.default.encodeToStream(lineageData, output)
    }
}
