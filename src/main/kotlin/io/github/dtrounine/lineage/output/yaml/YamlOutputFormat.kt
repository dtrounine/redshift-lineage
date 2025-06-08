package io.github.dtrounine.lineage.output.yaml

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.encodeToStream
import io.github.dtrounine.lineage.output.OutputFormat
import io.github.dtrounine.lineage.output.model.LineageReport
import java.io.OutputStream

class YamlOutputFormat: OutputFormat("yaml") {

    override fun write(lineageReport: LineageReport, output: OutputStream) {
        Yaml.default.encodeToStream(lineageReport, output)
    }

}
