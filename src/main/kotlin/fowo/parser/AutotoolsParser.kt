package fowo.parser

import fowo.model.Dependency

object AutotoolsParser {
    private val depRegex = Regex("""PKG_CHECK_MODULES\(\s*\[?\w+\]?\s*,\s*\[?(.+?)\]?\s*[,)]""")

    fun parse(content: String): List<Dependency> {
        val deps = mutableListOf<Dependency>()
        depRegex.findAll(content).forEach { matchResult ->
            val modulesRaw = matchResult.groups[1]?.value ?: ""
            val tokens = modulesRaw.split(Regex("\\s+")).filter { it.isNotBlank() }
            for (token in tokens) {
                if (token.startsWith("$")) continue // skip variables
                val match = Regex("""([a-zA-Z0-9_-]+)(?:>=([0-9.]+))?""").find(token)
                if (match != null) {
                    val name = match.groups[1]?.value
                    val ver = match.groups[2]?.value
                    if (name != null) {
                        deps.add(Dependency(name, ver))
                    }
                }
            }
        }
        return deps.distinctBy { it.name }
    }
}
