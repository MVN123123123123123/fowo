package fowo.parser

import fowo.model.Dependency

object CargoParser {
    // Matches simple `name = "version"` and `name = { version = "ver", ... }` patterns
    private val simpleDepRegex = Regex("""^(\S+)\s*=\s*"([^"]+)"\s*$""")
    private val tableDepRegex = Regex("""^(\S+)\s*=\s*\{.*?version\s*=\s*"([^"]+)".*\}\s*$""")

    fun parse(content: String): List<Dependency> {
        val deps = mutableListOf<Dependency>()
        var inDependencies = false
        
        for (line in content.lines()) {
            val t = line.trim()
            if (t.startsWith("[") && t.endsWith("]")) {
                inDependencies = t == "[dependencies]"
                continue
            }
            if (inDependencies && t.isNotBlank() && !t.startsWith("#")) {
                // Try table syntax first: name = { version = "1.0", features = [...] }
                val tableMatch = tableDepRegex.find(t)
                if (tableMatch != null) {
                    val name = tableMatch.groups[1]?.value ?: continue
                    val ver = tableMatch.groups[2]?.value
                    deps.add(Dependency(name, ver))
                    continue
                }
                // Try simple syntax: name = "1.0"
                val simpleMatch = simpleDepRegex.find(t)
                if (simpleMatch != null) {
                    val name = simpleMatch.groups[1]?.value ?: continue
                    val ver = simpleMatch.groups[2]?.value
                    deps.add(Dependency(name, ver))
                    continue
                }
                // Fallback: just extract the name
                val name = t.substringBefore("=").trim()
                if (name.isNotEmpty()) {
                    deps.add(Dependency(name))
                }
            }
        }
        return deps
    }
}
