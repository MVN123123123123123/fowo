package fowo.parser

import fowo.model.Dependency

object AutotoolsParser {
    private val depRegex = Regex("""PKG_CHECK_MODULES\(\s*\[?\w+\]?\s*,\s*\[?(.+?)\]?\s*[,)]""")
    // AC_CHECK_LIB(library, function, ...) — extracts the library name
    private val checkLibRegex = Regex("""AC_CHECK_LIB\(\s*\[?(\w+)\]?\s*,""")

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
        
        // Extract AC_CHECK_LIB dependencies
        checkLibRegex.findAll(content).forEach { matchResult ->
            val libName = matchResult.groups[1]?.value
            if (libName != null && !libName.startsWith("$")) {
                deps.add(Dependency(libName))
            }
        }

        return deps.distinctBy { it.name }
    }
}
