package fowo.parser

import fowo.model.Dependency

object CmakeParser {
    // Regex for pkg_check_modules — handles multi-word module lists
    private val pkgRegex = Regex("""pkg_check_modules\s*\(\s*\w+\s+(?:REQUIRED\s+)?(?:IMPORTED_TARGET\s+)?(?:GLOBAL\s+)?([^)]+)\)""")
    // Improved find_package regex: captures name and optional version, tolerates REQUIRED/COMPONENTS/etc. anywhere
    private val findPkgRegex = Regex("""find_package\s*\(\s*(\w+)(?:\s+([\d.]+))?[^)]*\)""")

    fun parse(content: String): List<Dependency> {
        val deps = mutableListOf<Dependency>()
        
        // Match pkg_check_modules
        pkgRegex.findAll(content).forEach { matchResult ->
            val modulesRaw = matchResult.groups[1]?.value ?: ""
            // Module list e.g. "hyprlang>=0.6.7 aquamarine>=0.9.3"
            val tokens = modulesRaw.split(Regex("\\s+")).filter { it.isNotBlank() }
            for (token in tokens) {
                if (token.startsWith("$")) continue // skip CMake variables
                // Capture name and optional operator+version (e.g. "hyprlang>=0.6.7" or "cairo")
                val match = Regex("""([a-zA-Z0-9_.-]+)((?:>=|<=|=|>|<)([0-9.]+))?""").find(token)
                if (match != null) {
                    val name = match.groups[1]?.value
                    val operator = match.groups[2]?.value  // e.g. ">=0.6.7" — full operator+version
                    if (name != null && name.any { it.isLetter() }) {
                        // Preserve the full constraint string (e.g. ">=0.6.7") for the SAT resolver
                        deps.add(Dependency(name, operator))
                    }
                }
            }
        }
        
        // Match find_package
        findPkgRegex.findAll(content).forEach { matchResult ->
            val name = matchResult.groups[1]?.value
            val ver = matchResult.groups[2]?.value
            if (name != null && name != "Threads" && name != "PkgConfig" && name.any { it.isLetter() }) {
                // find_package versions are minimum required, so treat as >=
                val constraint = if (ver != null) ">=$ver" else null
                deps.add(Dependency(name, constraint))
            }
        }
        
        return deps.distinctBy { it.name }
    }
}
