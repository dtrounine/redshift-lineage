package io.github.dtrounine.lineage

data class LineageInfo(
    val lineage: Map<String, Set<String>>,
    val sources: Set<String>
) {
    companion object {
        fun newEmpty(): LineageInfo {
            return LineageInfo(
                lineage = emptyMap(),
                sources = emptySet()
            )
        }
    }
}
